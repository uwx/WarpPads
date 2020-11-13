package raytech.warppads;

import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * Represents a custom item definition.
 *
 * @author Maxine
 * @since 12/11/2020
 */
public class CustomItem {
    private final ItemStack item;
    private final String displayName;
    private final String unlocalizedName;

    public CustomItem(ItemStack item, String displayName, String unlocalizedName) {
        this.item = item;
        this.displayName = displayName;
        this.unlocalizedName = unlocalizedName;
    }

    public boolean matches(ItemStack item) {
        return this.item.getType() == item.getType()
                && item.hasItemMeta()
                && Objects.requireNonNull(item.getItemMeta()).hasLocalizedName()
                && unlocalizedName.equals(item.getItemMeta().getLocalizedName());
    }
}
