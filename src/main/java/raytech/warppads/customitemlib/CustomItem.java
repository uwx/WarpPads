package raytech.warppads.customitemlib;

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

    CustomItem(ItemStack item, String displayName, String unlocalizedName, List<ShapedRecipe> recipes) {
        this(item, displayName, unlocalizedName, recipes.toArray(new ShapedRecipe[0]));
    }

    CustomItem(ItemStack item, String displayName, String unlocalizedName, ShapedRecipe[] recipes) {
        this.item = item;
        this.displayName = displayName;
        this.unlocalizedName = unlocalizedName;
        this.recipes = recipes;
    }

    public ItemStack get() {
        return new ItemStack(item);
    }

    public ItemStack getStackOf(int size) {
        ItemStack stack = new ItemStack(item);
        stack.setAmount(size);
        return stack;
    }

    /**
     * Checks whether a given {@link ItemStack} was created through the {@link #get()} or {@link #getStackOf(int)}
     * methods for this instance, or through crafting.
     *
     * @param item The item to check against
     * @return {@code true} if the item stack is of the current custom item, {@code false} otherwise
     */
    public boolean matches(ItemStack item) {
        return item != null
                && this.item.getType() == item.getType()
                && item.hasItemMeta()
                && Objects.requireNonNull(item.getItemMeta()).hasLocalizedName()
                && unlocalizedName.equals(item.getItemMeta().getLocalizedName());
    }

    public static final class CustomItemData<T> extends CustomItem {
        public final T data;

        private CustomItemData(T data, ItemStack item, String displayName, String unlocalizedName, ShapedRecipe[] recipes) {
            super(item, displayName, unlocalizedName, recipes);
            this.data = data;
        }
    }

    /**
     * Attaches a custom field to a custom item.
     * @param attachedData The data of the field to attach
     * @param <T> The type of the field to attach
     * @return A new {@link CustomItemData} whose {@link CustomItemData#data} field corresponds to {@code attachedData}
     */
    public <T> CustomItemData<T> attachData(T attachedData) {
        return new CustomItemData<>(attachedData, item, displayName, unlocalizedName, recipes);
    }
}
