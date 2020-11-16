package raytech.warppads;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import raytech.warppads.WarpData.Warp;
import raytech.warppads.customitemlib.CustomItem.CustomItemData;
import raytech.warppads.customitemlib.CustomItemManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class SpigotPlugin extends JavaPlugin implements Listener {
    public final GlobalWarps warps = new GlobalWarps();
    private final AccessList accessList = new AccessList();
    private CustomItemManager itemManager;

    private final Object warpDataFileMutex = new Object();
    private final List<Integer> runningSchedulers = new ArrayList<>();

    public CustomItemData<WarpTier>[] warpPads;

    @Override
    public void onLoad() {
        reloadWarps();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        itemManager = new CustomItemManager(this);

        List<CustomItemData<WarpTier>> warpPadsList = new ArrayList<>();
        for (WarpTier tier : WarpTier.values()) {
            warpPadsList.add(itemManager.createItem(
                    ChatColor.LIGHT_PURPLE + "Warp Pad - Tier " + tier.tierName,
                    "warp_pad_tier_" + tier.tierName,
                    420001 + tier.id, // 0-indexed
                    Material.IRON_NUGGET,
                    def -> def.withLore(
                            "Right-click to place a warp pad.",
                            "Rename this item in an anvil to set its label.",
                            "Two warp pads of this type, within " + (tier.range != -1 ? tier.range : "unlimited") + " blocks of",
                            "each other, can be teleported between.",
                            "",
                            "Right-click the pad with a dye in hand to color",
                            "its label. Right-click with a bucket of water to",
                            "restore the default label. Dyes will not be",
                            "consumed.",
                            "",
                            "Right-click the pad with a diamond to mark it as",
                            "private. It will be only accessible to you and",
                            "players you grant access to with /warpallow. You",
                            "may disallow access with /warpdeny."),
                    tier::buildRecipe
            ).attachData(tier));
        }
        this.warpPads = warpPadsList.toArray(new CustomItemData[0]);

        Config.loadConfig(this);

        reloadWarps();

        runSchedulers();

        Objects.requireNonNull(getCommand("warppads")).setExecutor(new CommandWarpPads(this));
        Objects.requireNonNull(getCommand("warpallow")).setExecutor(new CommandWarpAllow(this, accessList));
        Objects.requireNonNull(getCommand("warpdeny")).setExecutor(new CommandWarpDeny(this, accessList));

        // Register our own event hooks
        getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * Reloads the saved warps for all of the server's worlds from the warpdata.txt file in the world folder, if it
     * exists.
     */
    private void reloadWarps() {
        File worldsRoot = getServer().getWorldContainer();
        for (World world : getServer().getWorlds()) {
            File warpdataFile = new File(worldsRoot, world.getName() + "/warpdata.txt");
            if (warpdataFile.exists()) {
                warps.loadWarps(world, WarpData.load(warpdataFile));
            }
        }

        if (!getDataFolder().exists() && !getDataFolder().mkdir()) {
            getLogger().severe("Was unable to create data directory " + getDataFolder());
        }

        File accessListFile = new File(getDataFolder(), "access-list.yml");
        if (accessListFile.exists()) {
            accessList.loadFromFile(accessListFile);
        }
    }

    public void saveAccessList() {
        File accessListFile = new File(getDataFolder(), "access-list.yml");
        String accessListSerialized = accessList.saveToString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(accessListFile))) {
            writer.write(accessListSerialized);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Clears previously-running schedulers and runs schedulers for particle rendering.
     */
    public void runSchedulers() {
        // Clear all previously running schedulers
        for (Integer runningScheduler : runningSchedulers) {
            getServer().getScheduler().cancelTask(runningScheduler);
        }
        runningSchedulers.clear();

        // Run scheduler for drawing particles near players standing on warp pads
        runningSchedulers.add(getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            warps.forEach((world, warpData) -> {
                for (Player player : warpData.playersStandingOnWarps) {
                    renderWarps(warpData, player);
                }
            });
        }, 5L, 5L));

        // Run scheduler for drawing particles over warp pads
        Random random = new Random();
        runningSchedulers.add(getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            int rgb = random.nextInt();
            Particle.DustOptions particleColor = new Particle.DustOptions(Color.fromRGB(rgb & 0x00FFFFFF), 1);

            warps.forEach((world, warpData) -> {
                for (Warp warp : warpData.warps.values()) {
                    if (!world.isChunkLoaded(warp.x >> 4, warp.z >> 4) || !WorldUtil.hasNearbyPlayers(world, warp.x, warp.y, warp.z, Config.warpDecorationVisibilityDistance)) {
                        continue;
                    }

                    //getLogger().info("Loading particle");

                    world.spawnParticle(Particle.REDSTONE, warp.x + 0.5, warp.y + 0.5, warp.z + 0.5, 10, 0.5, 0.5, 0.5, 1, particleColor);
                }
            });
        }, Config.warpDecorationUpdateRate, Config.warpDecorationUpdateRate));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerUse(PlayerInteractEvent event){
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        switch (mainHandItem.getType()) {
            case DIAMOND:
                tryMakePrivate(event);
                return;
            // Color a warp block or remove its color
            case WATER_BUCKET:
                tryColor(event.getClickedBlock(), Warp.highlightParticle.getColor(), Warp.defaultLabelColor);
                player.getInventory().setItemInMainHand(new ItemStack(Material.BUCKET));
                return;
            case BONE_MEAL:
            case WHITE_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0xF9FFFE), ChatColor.WHITE);
                return;
            case ORANGE_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0xF9801D), ChatColor.GOLD);
                return;
            case MAGENTA_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0xC74EBD), ChatColor.LIGHT_PURPLE);
                return;
            case LIGHT_BLUE_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x3AB3DA), ChatColor.AQUA);
                return;
            case YELLOW_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0xFED83D), ChatColor.YELLOW);
                return;
            case LIME_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x80C71F), ChatColor.GREEN);
                return;
            case PINK_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0xF38BAA), ChatColor.LIGHT_PURPLE);
                return;
            case GRAY_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x474F52), ChatColor.DARK_GRAY);
                return;
            case LIGHT_GRAY_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x9D9D97), ChatColor.GRAY);
                return;
            case CYAN_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x169C9C), ChatColor.DARK_AQUA);
                return;
            case PURPLE_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x8932B8), ChatColor.DARK_PURPLE);
                return;
            case BLUE_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x3C44AA), ChatColor.DARK_BLUE);
                return;
            case COCOA_BEANS:
            case BROWN_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x835432), ChatColor.DARK_RED);
                return;
            case GREEN_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x5E7C16), ChatColor.DARK_GREEN);
                return;
            case RED_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0xB02E26), ChatColor.RED);
                return;
            case INK_SAC:
            case BLACK_DYE:
                tryColor(event.getClickedBlock(), Color.fromRGB(0x1D1D21), ChatColor.BLACK);
                return;
        }

        // Place new warp pad
        for (CustomItemData<WarpTier> warpPad : warpPads) {
            if (!warpPad.matches(mainHandItem)) {
                continue;
            }

            event.setCancelled(true);

            if (event.getClickedBlock() == null) {
                return;
            }

            placeWarpPad(event, warpPad.data);
            return;
        }
    }

    private void tryMakePrivate(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) { // Necessary, I presume for air?
            return;
        }

        Warp warp = warps.get(block.getWorld(), block.getX(), block.getY(), block.getZ());
        if (warp == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!warp.authorUUID.equals(player.getUniqueId())) {
            String playerName = warp.getAuthorName(getServer());
            player.sendMessage(ChatColor.DARK_RED + "You do not own this Warp Pod. It is owned by " + playerName + ".");
        }

        warp.isPrivate = true;

        // Take one diamond off
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        mainHandItem.setAmount(mainHandItem.getAmount() - 1);
        player.getInventory().setItemInMainHand(mainHandItem.getAmount() != 0 ? mainHandItem : null);
    }

    /**
     * Detects whether a block is a warp, and gives it the provided highlight color if so. Does not consume any item.
     * @param block The warp's block to set the highlight color of
     * @param color The highlight color to set the warp to
     * @param chatColor The label color to set the warp to
     */
    private void tryColor(Block block, Color color, ChatColor chatColor) {
        if (block == null) { // Necessary, I presume for air?
            return;
        }

        Warp warp = warps.get(block.getWorld(), block.getX(), block.getY(), block.getZ());
        if (warp == null) {
            return;
        }

        warp.highlightColor = new Particle.DustOptions(color, 1);
        warp.labelColor = chatColor;
    }

    private void placeWarpPad(PlayerInteractEvent event, WarpTier tier) {
        Player player = event.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        int placeX = event.getClickedBlock().getX() + event.getBlockFace().getModX();
        int placeY = event.getClickedBlock().getY() + event.getBlockFace().getModY();
        int placeZ = event.getClickedBlock().getZ() + event.getBlockFace().getModZ();
        String label = mainHandItem.getItemMeta().getDisplayName();

        // Remove color code from label. This also removes the italics code added by renaming the item.
        if (label.startsWith(Character.toString(ChatColor.COLOR_CHAR))) {
            label = label.substring(2);
        }

        Block blockAtLocation = player.getWorld().getBlockAt(placeX, placeY, placeZ);
        if (!blockAtLocation.isEmpty() && !blockAtLocation.isLiquid() && blockAtLocation.getType() != Material.GRASS) {
            return;
        }

        warps.add(player.getWorld(), new Warp(tier, player.getUniqueId(), placeX, placeY, placeZ, label));

        // Save all warps to the warpdata.txt file in the world directory.
        saveWarpsToFile(player.getWorld());

        mainHandItem.setAmount(mainHandItem.getAmount() - 1);
        player.getInventory().setItemInMainHand(mainHandItem.getAmount() != 0 ? mainHandItem : null);
        blockAtLocation.setType(tier.displayBlock);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        int blockX = event.getBlock().getX();
        int blockY = event.getBlock().getY();
        int blockZ = event.getBlock().getZ();
        Warp removedWarp = warps.remove(event.getBlock().getWorld(), blockX, blockY, blockZ);
        if (removedWarp == null) {
            return;
        }

        event.setDropItems(false);

        ItemStack item = warpPads[removedWarp.tier.id].get();
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.setDisplayName(removedWarp.labelColor + removedWarp.label);
        item.setItemMeta(meta);

        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);

        saveWarpsToFile(event.getPlayer().getWorld());
    }

    /**
     * Asynchronously save all warps for a given world to the warpdata.txt file in the world directory.
     * @param world The world whose warps to save
     */
    public void saveWarpsToFile(World world) {
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (warpDataFileMutex) {
                WarpData warpData = warps.getWarps(world);
                if (warpData == null) {
                    return;
                }

                File warpdataFile = new File(getServer().getWorldContainer().getAbsolutePath(), world.getName() + "/warpdata.txt");
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(warpdataFile))) {
                    for (Warp warpToSave : warpData.warps.values()) {
                        writer.write(warpToSave.save() + "\n");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        WarpData warpData = warps.getWarps(player.getWorld());
        if (warpData == null) {
            return;
        }

        Warp warp = warpData.getWarpUnderPlayer(player);
        if (warp == null) {
            warpData.playersStandingOnWarps.remove(player);
        } else {
            warpData.playersStandingOnWarps.add(player);
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (event.isSneaking()) {
            WarpData warpData = warps.getWarps(player.getWorld());
            if (warpData == null) {
                return;
            }

            Warp warpUnderPlayer = warpData.getWarpUnderPlayer(player);
            if (warpUnderPlayer == null) {
                return;
            }

            Warp warp = getClosestWarp(player, warpUnderPlayer, warpData, null);
            if (warp == null) {
                return;
            }

            Location eyeLocation = player.getEyeLocation();
            player.teleport(new Location(event.getPlayer().getWorld(), warp.x + 0.5, warp.y + 0.5, warp.z + 0.5, eyeLocation.getYaw(), eyeLocation.getPitch()), TeleportCause.PLUGIN);
        }
    }

    /**
     * Renders particle lines towards all warps a player is in range of
     * @param warpData The {@link WarpData} instance for the world the player is in
     * @param player The player themselves
     */
    private void renderWarps(WarpData warpData, Player player) {
        // List of warps within teleport range
        List<Warp> reachableWarps = new LinkedList<>();

        Warp warpUnderPlayer = warpData.getWarpUnderPlayer(player);
        if (warpUnderPlayer == null) {
            return;
        }

        Warp closestWarp = getClosestWarp(player, warpUnderPlayer, warpData, reachableWarps);

        for (Warp warp : reachableWarps) {
            if (warp == closestWarp) {
                renderWarpLine(player.getWorld(), player.getEyeLocation(), warp, true);

                String message = ChatColor.RED + "Sneak to warp to " + warp.labelColor + warp.label;
                if (warp.isPrivate) {
                    message += " \u2B50 " + ChatColor.AQUA + warp.getAuthorName(getServer());
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            } else {
                renderWarpLine(player.getWorld(), player.getEyeLocation(), warp, false);
            }
        }
    }

    /**
     * Gets the closest warp a player is facing for a given {@link WarpData}. Optionally collects a list of all
     * reachable warps from the player's location.
     *
     * @param player The player to compute warps from
     * @param warpUnderPlayer
     * @param warpData The warp data for the player's world
     * @param reachableWarps A list to fill with all warps reachable by the player
     * @return The closest warp to the player's head direction, or {@code null} if there are no warps reachable by the
     *         player
     */
    private Warp getClosestWarp(Player player, Warp warpUnderPlayer, WarpData warpData, List<Warp> reachableWarps) {
        Location location = player.getLocation();
        double playerX = location.getX();
        double playerY = location.getY();
        double playerZ = location.getZ();

        double eyeHeight = player.getEyeHeight();

        Vector headDirection = location.getDirection();

        // To avoid getting the square root of distances, we do all the math in squared numbers
        final int squaredDistLimit
                = warpUnderPlayer.tier.range == -1 ? -1 : warpUnderPlayer.tier.range * warpUnderPlayer.tier.range;
        final int squaredDistMinimum = 3 * 3;

        // Highlighted warp (line is closest to player's head) and angle distance score (lower is better)
        float shortestDistance = Float.MAX_VALUE;
        Warp closestWarp = null;

        for (Warp warp : warpData.warps.values()) {
            // Omit inaccessible private warps
            if (warp.isPrivate && warp.authorUUID != player.getUniqueId() && !accessList.contains(warp.authorUUID, player.getUniqueId())) {
                continue;
            }

            double distanceSquared = VectorUtil.distanceSquared(playerX, playerY, playerZ, warp.x, warp.y, warp.z);

            if (distanceSquared < squaredDistMinimum || (squaredDistLimit != -1 && distanceSquared > squaredDistLimit)) {
                continue;
            }

            float angleDistance = VectorUtil.subtractNormalizeDistanceSquared(
                    // warp (destination)
                    warp.x + 0.5, warp.y + eyeHeight, warp.z + 0.5,
                    // player location (origin)
                    playerX + 0.5, playerY + eyeHeight, playerZ + 0.5,
                    // head direction (to compare against)
                    headDirection.getX(), headDirection.getY(), headDirection.getZ()
            );

            if (angleDistance < shortestDistance) {
                shortestDistance = angleDistance;
                closestWarp = warp;
            }

            if (reachableWarps != null) {
                reachableWarps.add(warp);
            }
        }

        return closestWarp;
    }

    /**
     * Renders a line of particles between an origin location and a warp, at eye height
     * @param world The world the warp is in
     * @param origin The origin (source location)
     * @param warp The warp (destination location)
     * @param highlighted Whether to draw the warp's highlight particle color, or the default.
     */
    private static void renderWarpLine(World world, Location origin, Warp warp, boolean highlighted) {
        final Particle.DustOptions dustOptions = highlighted ? warp.highlightColor : Warp.particle;

        // https://bukkit.org/threads/making-a-particle-line-from-point-1-to-point-2.465415/#post-3558666
        final float gap = 1f;
        final int maxIterations = Config.warpLineIterationCount;

        Vector originVector = origin.toVector();
        Vector destinationVector = new Vector(warp.x + 0.5, warp.y + 1.5, warp.z + 0.5); // At eye height, centered in block
        float totalDistance = (float) originVector.distance(destinationVector);

        Vector step = destinationVector.clone().subtract(originVector).normalize().multiply(gap);

        originVector.add(step.clone().multiply(2)); // add a gap between the player's eyes and the line

        float currentStep = 0;
        for (int i = 0; i < maxIterations && currentStep < totalDistance; i++) {
            world.spawnParticle(Particle.REDSTONE, originVector.getX(), originVector.getY(), originVector.getZ(), 1, dustOptions);
            originVector.add(step);
            currentStep += gap;
        }
    }
}


