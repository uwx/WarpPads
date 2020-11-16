package raytech.warppads.customitemlib;

import com.google.common.collect.Multimap;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/**
 * Allows for attaching data to the root {@link ItemStack} of a custom item.
 *
 * @author Maxine
 * @since 13/11/2020
 */
public class CustomItemDefinition {
    private final ItemStack item;
    private final ItemMeta meta;

    public CustomItemDefinition(ItemStack item, ItemMeta meta) {
        this.item = item;
        this.meta = meta;
    }

    public CustomItemDefinition withDisplayName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public CustomItemDefinition addEnchant(Enchantment ench, int level, boolean ignoreLevelRestriction) {
        meta.addEnchant(ench, level, ignoreLevelRestriction);
        return this;
    }

    public CustomItemDefinition removeEnchant(Enchantment ench) {
        meta.removeEnchant(ench);
        return this;
    }

    public CustomItemDefinition addItemFlags(ItemFlag... itemFlags) {
        meta.addItemFlags(itemFlags);
        return this;
    }

    public CustomItemDefinition removeItemFlags(ItemFlag... itemFlags) {
        meta.removeItemFlags(itemFlags);
        return this;
    }

    public CustomItemDefinition withUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public CustomItemDefinition addAttributeModifier(Attribute attribute, AttributeModifier modifier) {
        meta.addAttributeModifier(attribute, modifier);
        return this;
    }

    public CustomItemDefinition withAttributeModifiers(Multimap<Attribute, AttributeModifier> attributeModifiers) {
        meta.setAttributeModifiers(attributeModifiers);
        return this;
    }

    public CustomItemDefinition removeAttributeModifier(Attribute attribute) {
        meta.removeAttributeModifier(attribute);
        return this;
    }

    public CustomItemDefinition removeAttributeModifier(EquipmentSlot slot) {
        meta.removeAttributeModifier(slot);
        return this;
    }

    public CustomItemDefinition removeAttributeModifier(Attribute attribute, AttributeModifier modifier) {
        meta.removeAttributeModifier(attribute, modifier);
        return this;
    }

    public CustomItemDefinition withLore(String... lore) {
        meta.setLore(Arrays.asList(lore));
        return this;
    }
}
