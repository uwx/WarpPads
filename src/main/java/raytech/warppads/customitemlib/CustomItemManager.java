package raytech.warppads.customitemlib;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Handles creating and performing generic operations on custom items.
 *
 * @author Maxine
 * @since 16/11/2020
 */
public final class CustomItemManager implements Listener {
    private final List<CustomItem> customItemCache = new ArrayList<>();
    private final JavaPlugin plugin;

    public CustomItemManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Defines a new custom item tied to this {@link CustomItemManager}, and returns a handle that can be used to
     * interact with it.
     *
     * @param displayName The in-game display name of the item
     * @param unlocalizedName The resource key for the item, such as {@code my_item}
     * @param modelId The {@code custom_model_data} tag for the item
     * @param material The material the item will inherit (also determines which model JSON file it uses)
     * @param createItem A function that takes a {@link CustomItemDefinition} that cna be used to alter the root item's
     *                   traits
     * @param createRecipes One or more functions that take a {@link ShapedRecipe} (with preconfigured key) that define
     *                      recipes, each of which output a clone of the root item
     * @return A {@link CustomItem} instance that can be used to interact with the new item
     */
    @SafeVarargs
    public final CustomItem createItem(String displayName, String unlocalizedName, int modelId, Material material, Consumer<CustomItemDefinition> createItem, Consumer<ShapedRecipe>... createRecipes) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = Objects.requireNonNull(item.getItemMeta());
        meta.setDisplayName(displayName);
        meta.setLocalizedName(unlocalizedName); // Will be hidden from player but not changeable
        meta.setCustomModelData(modelId);
        createItem.accept(new CustomItemDefinition(item, meta));
        item.setItemMeta(meta); // Meta has to be set to the item after it's done being tampered with

        List<ShapedRecipe> recipes = new ArrayList<>();
        int recipeIndex = 0;
        for (Consumer<ShapedRecipe> recipeConsumer : createRecipes) {
            NamespacedKey key = new NamespacedKey(plugin, unlocalizedName + (recipeIndex == 0 ? "" : "_" + recipeIndex));
            ShapedRecipe recipe = new ShapedRecipe(key, item);
            recipes.add(recipe);

            recipeConsumer.accept(recipe);
            plugin.getServer().addRecipe(recipe);

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
}
