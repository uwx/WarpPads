package raytech.warppads;

import org.bukkit.Material;
import org.bukkit.inventory.ShapedRecipe;

/**
 * Created by Maxine on 15/11/2020.
 *
 * @author Maxine
 * @since 15/11/2020
 */
public enum WarpTier {
    T1("1", Material.QUARTZ_SLAB, 500) {
        @Override
        protected void buildRecipe(ShapedRecipe recipe) {
            recipe
                    .shape(
                            " q ",
                            "qgq",
                            "aq "
                    )
                    .setIngredient('q', Material.QUARTZ)
                    .setIngredient('g', Material.GOLD_BLOCK)
                    .setIngredient('a', Material.GOLDEN_APPLE);
        }
    },
    T2("2", Material.POLISHED_BLACKSTONE_SLAB, 3000) {
        @Override
        protected void buildRecipe(ShapedRecipe recipe) {
            recipe
                    .shape(
                            " q ",
                            "qgq",
                            "aq "
                    )
                    .setIngredient('q', Material.PHANTOM_MEMBRANE)
                    .setIngredient('g', Material.EMERALD_BLOCK)
                    .setIngredient('a', Material.GHAST_TEAR);
        }
    },
    T3("3", Material.PRISMARINE_BRICK_SLAB, -1) {
        @Override
        protected void buildRecipe(ShapedRecipe recipe) {
            recipe
                    .shape(
                            " q ",
                            "qgq",
                            "aq "
                    )
                    .setIngredient('q', Material.ENDER_PEARL)
                    .setIngredient('g', Material.NETHERITE_BLOCK)
                    .setIngredient('a', Material.NETHER_STAR);
        }
    };

    private static final WarpTier[] values = values();

    /**
     * The in-game visible name of this tier.
     */
    public final String tierName;

    /**
     * The block that this warp displays as in the world. Must be a slab block.
     */
    public final Material displayBlock;

    /**
     * Circular radius in blocks, from one warp of this tier, that other warps may be reached. Must not be greater than
     * 46340.
     */
    public final int range;

    /**
     * Gets the numeric, zero-indexed ID of this warp tier.
     */
    public final int id = ordinal();

    WarpTier(String tierName, Material displayBlock, int range) {
        this.tierName = tierName;
        this.displayBlock = displayBlock;
        this.range = range;
    }

    protected abstract void buildRecipe(ShapedRecipe recipe);

    /**
     * Get a warp indexed by its id.
     * @param index The ID of the warp to get
     * @return The warp at the given index
     */
    public static WarpTier of(int index) {
        return values[index];
    }
}