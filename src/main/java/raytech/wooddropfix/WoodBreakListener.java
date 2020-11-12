package raytech.wooddropfix;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class WoodBreakListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!event.isDropItems() || event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }

        Material mat = null;

        switch (event.getBlock().getType()) {
            case OAK_WOOD: case STRIPPED_OAK_WOOD: mat = Material.OAK_LOG; break;
            case SPRUCE_WOOD: case STRIPPED_SPRUCE_WOOD: mat = Material.SPRUCE_LOG; break;
            case BIRCH_WOOD: case STRIPPED_BIRCH_WOOD: mat = Material.BIRCH_LOG; break;
            case JUNGLE_WOOD: case STRIPPED_JUNGLE_WOOD: mat = Material.JUNGLE_LOG; break;
            case ACACIA_WOOD: case STRIPPED_ACACIA_WOOD: mat = Material.ACACIA_LOG; break;
            case DARK_OAK_WOOD: case STRIPPED_DARK_OAK_WOOD: mat = Material.DARK_OAK_LOG; break;
        }

        // NB: Will not respect Fortune, etc.
        if (mat != null) {
            event.setDropItems(false); // Alternatively, can cancel the event, but this should keep the XP drops.
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), new ItemStack(mat));
        }
    }
}
