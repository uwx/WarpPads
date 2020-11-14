package raytech.warppads;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import raytech.warppads.WarpData.Warp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

public final class SpigotPlugin extends JavaPlugin implements Listener {
    public final Map<World, WarpData> warpDataMap = new HashMap<>();
    private final Object warpDataFileMutex = new Object();
    private final List<Integer> runningSchedulers = new ArrayList<>();

    private final List<CustomItem> customItemCache = new ArrayList<>();
    public CustomItem warpPadT1;

    public static final Particle.DustOptions warpLineParticle = new Particle.DustOptions(Color.RED, 1);
    public static final Particle.DustOptions warpLineHighlightParticle = new Particle.DustOptions(Color.PURPLE, 1);

    @Override
    public void onLoad() {
        reloadWarps();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        warpPadT1 = createItem(
                ChatColor.LIGHT_PURPLE + "Warp Pad - Tier 1",
                "warp_pad_tier_1",
                420001,
                Material.IRON_NUGGET,
                def -> def.withLore(
                        "Right-click to place a warp pad.",
                        "Rename this item in an anvil to set its label.",
                        "Two warp pads of this type, within 1000 blocks of",
                        "each other, can be teleported between."),
                recipe -> recipe
                        .shape(
                                " q ",
                                "qgq",
                                "aq "
                        )
                        .setIngredient('q', Material.QUARTZ)
                        .setIngredient('g', Material.GOLD_BLOCK)
                        .setIngredient('a', Material.GOLDEN_APPLE)
        );

        Config.loadConfig(this);

        reloadWarps();

        runSchedulers();

        Objects.requireNonNull(this.getCommand("warppads")).setExecutor(new CommandWarpPads(this));

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
                warpDataMap.put(world, WarpData.load(warpdataFile));
            }
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
            warpDataMap.forEach((world, warpData) -> {
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

            warpDataMap.forEach((world, warpData) -> {
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

    public CustomItem createItem(String displayName, String unlocalizedName, int modelId, Material material, Consumer<CustomItemDefinition> createItem, Consumer<ShapedRecipe>... createRecipes) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.setDisplayName(displayName);
        meta.setLocalizedName(unlocalizedName); // Will be hidden from player but not changeable
        meta.setCustomModelData(modelId);
        createItem.accept(new CustomItemDefinition(item, meta));
        item.setItemMeta(meta);

        List<ShapedRecipe> recipes = new ArrayList<>();
        int recipeIndex = 0;
        for (Consumer<ShapedRecipe> recipeConsumer : createRecipes) {
            NamespacedKey key = new NamespacedKey(this, unlocalizedName + (recipeIndex == 0 ? "" : "_" + recipeIndex));
            ShapedRecipe recipe = new ShapedRecipe(key, item);
            recipes.add(recipe);

            recipeConsumer.accept(recipe);
            Bukkit.addRecipe(recipe);

            recipeIndex++;
        }

        CustomItem customItem = new CustomItem(item, displayName, unlocalizedName, recipes);
        customItemCache.add(customItem);
        return customItem;
    }

    /**
     * Adds all custom item recipes to the player's recipe book upon join.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        for (CustomItem customItem : customItemCache) {
            for (ShapedRecipe recipe : customItem.recipes) {
                event.getPlayer().discoverRecipe(recipe.getKey());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerUse(PlayerInteractEvent event){
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (warpPadT1.matches(mainHandItem)) {
            event.setCancelled(true);

            if (event.getClickedBlock() == null) {
                return;
            }

            int placeX = event.getClickedBlock().getX() + event.getBlockFace().getModX();
            int placeY = event.getClickedBlock().getY() + event.getBlockFace().getModY();
            int placeZ = event.getClickedBlock().getZ() + event.getBlockFace().getModZ();
            String label = mainHandItem.getItemMeta().getDisplayName();

            Block blockAtLocation = player.getWorld().getBlockAt(placeX, placeY, placeZ);
            if (!blockAtLocation.isEmpty() && !blockAtLocation.isLiquid() && blockAtLocation.getType() != Material.GRASS) {
                return;
            }

            WarpData worldWarpData = warpDataMap.get(player.getWorld());
            if (worldWarpData == null) {
                worldWarpData = new WarpData();
                warpDataMap.put(player.getWorld(), worldWarpData);
            }

            Warp warp = new Warp(placeX, placeY, placeZ, label);
            worldWarpData.warps.put(new BlockVector(placeX, placeY, placeZ), warp);

            // Save all warps to the warpdata.txt file in the world directory.
            saveWarpsToFile(player.getWorld());

            mainHandItem.setAmount(mainHandItem.getAmount() - 1);
            player.getInventory().setItemInMainHand(mainHandItem.getAmount() != 0 ? mainHandItem : null);
            blockAtLocation.setType(Material.QUARTZ_SLAB);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        WarpData warpData = warpDataMap.get(event.getBlock().getWorld());
        if (warpData == null) {
            return;
        }

        int blockX = event.getBlock().getX();
        int blockY = event.getBlock().getY();
        int blockZ = event.getBlock().getZ();
        Warp removedWarp = warpData.warps.remove(new BlockVector(blockX, blockY, blockZ));
        if (removedWarp == null) {
            return;
        }

        event.setDropItems(false);

        ItemStack item = warpPadT1.get();
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.setDisplayName(removedWarp.label);
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
                WarpData warpData = warpDataMap.get(world);
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
        WarpData warpData = warpDataMap.get(player.getWorld());
        if (warpData == null) {
            return;
        }

        Location location = player.getLocation();
        int x = location.getBlock().getX();
        int y = location.getBlock().getY();
        int z = location.getBlock().getZ();

        Warp warp = warpData.warps.get(new BlockVector(x, y, z));
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
            WarpData warpData = warpDataMap.get(player.getWorld());
            if (warpData == null) {
                return;
            }

            Location location = player.getLocation();
            int x = location.getBlock().getX();
            int y = location.getBlock().getY();
            int z = location.getBlock().getZ();
            if (!warpData.warps.containsKey(new BlockVector(x, y, z))) {
                return;
            }

            Warp warp = getClosestWarp(player, warpData, null);

            player.teleport(new Location(event.getPlayer().getWorld(), warp.x + 0.5, warp.y + 0.5, warp.z + 0.5), TeleportCause.PLUGIN);
        }
    }

    /**
     * Renders particle lines towards all warps a player is in range of
     * @param warpData The {@link WarpData} instance for the world the player is in
     * @param player The player themselves
     */
    private static void renderWarps(WarpData warpData, Player player) {
        // List of warps within teleport range
        List<Warp> reachableWarps = new LinkedList<>();

        Warp closestWarp = getClosestWarp(player, warpData, reachableWarps);

        for (Warp warp : reachableWarps) {
            if (warp == closestWarp) {
                renderWarpLine(player.getWorld(), player.getEyeLocation(), warp, warpLineHighlightParticle);

                String message = ChatColor.RED + "Sneak to warp to " + ChatColor.LIGHT_PURPLE + warp.label;
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
            } else {
                renderWarpLine(player.getWorld(), player.getEyeLocation(), warp, warpLineParticle);
            }
        }
    }

    /**
     * Gets the closest warp a player is facing for a given {@link WarpData}. Optionally collects a list of all
     * reachable warps from the player's location.
     *
     * @param player The player to compute warps from
     * @param warpData The warp data for the player's world
     * @param reachableWarps A list to fill with all warps reachable by the player
     * @return The closest warp to the player's head direction, or {@code null} if there are no warps reachable by the
     *         player
     */
    private static Warp getClosestWarp(Player player, WarpData warpData, List<Warp> reachableWarps) {
        Location location = player.getLocation();
        double playerX = location.getX();
        double playerY = location.getY();
        double playerZ = location.getZ();

        double eyeHeight = player.getEyeHeight();

        Vector headDirection = location.getDirection();

        // To avoid getting the square root of distances, we do all the
        final int squaredDistLimit = Config.warpPadT1Range * Config.warpPadT1Range;

        // Highlighted warp (line is closest to player's head) and angle distance score (lower is better)
        float shortestDistance = Float.MAX_VALUE;
        Warp closestWarp = null;

        for (Warp warp : warpData.warps.values()) {
            double distanceSquared = VectorUtil.distanceSquared(playerX, playerY, playerZ, warp.x, warp.y, warp.z);

            if (distanceSquared <= squaredDistLimit) {
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
        }

        return closestWarp;
    }

    /**
     * Renders a line of particles between an origin location and a warp, at eye height
     * @param world The world the warp is in
     * @param origin The origin (source location)
     * @param warp The warp (destination location)
     */
    private static void renderWarpLine(World world, Location origin, Warp warp, Particle.DustOptions dustOptions) {
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


