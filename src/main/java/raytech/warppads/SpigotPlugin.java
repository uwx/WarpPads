package raytech.warppads;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockVector;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;
import raytech.warppads.WarpData.Warp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpigotPlugin extends JavaPlugin implements Listener {
    private final Map<World, WarpData> warpDataMap = new HashMap<>();
    private final Object warpDataWriteSynchronizationHandle = new Object();
    private CustomItem warpPad;

    public static final Particle.DustOptions warpLineParticle = new Particle.DustOptions(Color.PURPLE, 1);

    @Override
    public void onLoad() {
        reloadWarps();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        warpPad = createItem(
                ChatColor.LIGHT_PURPLE + "Warp Pad - Tier 1",
                "warp_pad_tier_1",
                () -> new ItemStack(Material.STICK),
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

        reloadWarps();

        // Run scheduler for drawing particles near players standing on warp pads
        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            warpDataMap.forEach((world, warpData) -> {
                for (Player player : warpData.playersStandingOnWarps) {
                    renderWarps(warpData, player);
                }
            });
        }, 300L, 300L);

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

    public CustomItem createItem(String displayName, String unlocalizedName, Supplier<ItemStack> createItem, Consumer<ShapedRecipe>... recipes) {
        ItemStack item = createItem.get();
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.setDisplayName(displayName);
        meta.setLocalizedName(unlocalizedName); // Will be hidden from player but not changeable
        item.setItemMeta(meta);

        int recipeIndex = 0;
        for (Consumer<ShapedRecipe> recipeConsumer : recipes) {
            NamespacedKey key = new NamespacedKey(this, unlocalizedName + (recipeIndex == 0 ? "" : "_" + recipeIndex));
            ShapedRecipe recipe = new ShapedRecipe(key, item);

            recipeConsumer.accept(recipe);
            Bukkit.addRecipe(recipe);

            recipeIndex++;
        }

        return new CustomItem(item, displayName, unlocalizedName);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerUse(PlayerInteractEvent event){
        Player player = event.getPlayer();

        if (warpPad.matches(player.getInventory().getItemInMainHand())) {
            WarpData worldWarpData = warpDataMap.get(player.getWorld());
            if (worldWarpData == null) {
                worldWarpData = new WarpData();
                warpDataMap.put(player.getWorld(), worldWarpData);
            }

            int placeX = event.getClickedBlock().getX() + event.getBlockFace().getModX();
            int placeY = event.getClickedBlock().getY() + event.getBlockFace().getModY();
            int placeZ = event.getClickedBlock().getZ() + event.getBlockFace().getModZ();
            String label = player.getInventory().getItemInMainHand().getItemMeta().getDisplayName();

            Warp warp = new Warp(placeX, placeY, placeZ, label);
            worldWarpData.warps.put(new BlockVector(placeX, placeY, placeZ), warp);

            // Save all warps to the warpdata.txt file in the world directory.
            Map<BlockVector, Warp> copyOfWarps = worldWarpData.warps;
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                synchronized (warpDataWriteSynchronizationHandle) {
                    File warpdataFile = new File(getServer().getWorldContainer().getAbsolutePath(), player.getWorld().getName() + "/warpdata.txt");
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(warpdataFile))) {
                        for (Warp warpToSave : copyOfWarps.values()) {
                            writer.write(warpToSave.save() + "\n");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            player.getWorld().getBlockAt(placeX, placeY, placeZ).setType(Material.SMOOTH_STONE_SLAB);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        WarpData warpData = warpDataMap.get(player.getWorld());
        if (warpData == null) {
            return;
        }

        Location location = player.getLocation();
        int playerX = (int) location.getX();
        int playerY = (int) location.getY();
        int playerZ = (int) location.getZ();

        Warp warp = warpData.warps.get(new BlockVector(playerX, playerY, playerZ));
        if (warp == null) {
            warpData.playersStandingOnWarps.remove(player);
        } else {
            warpData.playersStandingOnWarps.add(player);
        }
    }

    /**
     * Renders particle lines towards all warps a player is in range of
     * @param warpData The {@link WarpData} instance for the world the player is in
     * @param player The player themselves
     */
    private static void renderWarps(WarpData warpData, Player player) {
        Location location = player.getLocation();
        int playerX = (int) location.getX();
        int playerY = (int) location.getY();
        int playerZ = (int) location.getZ();

        final int squared1024 = 1024 * 1024;

        for (Warp warp : warpData.warps.values()) {
            double distanceSquared = NumberConversions.square(playerX - warp.x) + NumberConversions.square(playerY - warp.y) + NumberConversions.square(playerZ - warp.z);
            if (distanceSquared <= squared1024) {
                renderWarpLine(player.getWorld(), player.getEyeLocation(), warp);
            }
        }
    }

    /**
     * Renders a line of particles between an origin location and a warp, at eye height
     * @param world The world the warp is in
     * @param origin The origin (source location)
     * @param warp The warp (destination location)
     */
    private static void renderWarpLine(World world, Location origin, Warp warp) {
        // https://bukkit.org/threads/making-a-particle-line-from-point-1-to-point-2.465415/#post-3558666
        final float gap = 1f;
        final int maxIterations = 5;

        Vector originVector = origin.toVector();
        Vector destinationVector = new Vector(warp.x + 0.5, warp.y + 1.5, warp.z + 0.5); // At eye height, centered in block
        float totalDistance = (float) originVector.distance(destinationVector);

        Vector step = destinationVector.clone().subtract(originVector).normalize().multiply(gap);

        originVector.add(step.clone().multiply(2)); // add a gap between the player's eyes and the line

        float currentStep = 0;
        for (int i = 0; i < maxIterations && currentStep < totalDistance; i++) {
            world.spawnParticle(Particle.REDSTONE, originVector.getX(), originVector.getY(), originVector.getZ(), 1, warpLineParticle);
            originVector.add(step);
            currentStep += gap;
        }
    }
}


