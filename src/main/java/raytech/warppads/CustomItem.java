package raytech.warppads;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.List;
import java.util.Objects;

/**
 * Represents a custom item definition.
 *
 * @author Maxine
 * @since 12/11/2020
 */
public class CustomItem {
    private final ItemStack item;
    public final String displayName;
    public final String unlocalizedName;
    public final ShapedRecipe[] recipes;

    public CustomItem(ItemStack item, String displayName, String unlocalizedName, List<ShapedRecipe> recipes) {
        this.item = item;
        this.displayName = displayName;
        this.unlocalizedName = unlocalizedName;
        this.recipes = recipes.toArray(new ShapedRecipe[0]);
    }

    public ItemStack get() {
        return new ItemStack(item);
    }

    public ItemStack getStackOf(int size) {
        ItemStack stack = new ItemStack(item);
        stack.setAmount(size);
        return stack;
    }

    public boolean matches(ItemStack item) {
        return item != null
                && this.item.getType() == item.getType()
                && item.hasItemMeta()
                && Objects.requireNonNull(item.getItemMeta()).hasLocalizedName()
                && unlocalizedName.equals(item.getItemMeta().getLocalizedName());
    }
}
