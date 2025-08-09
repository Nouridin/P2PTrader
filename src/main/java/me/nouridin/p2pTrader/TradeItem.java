package me.nouridin.p2pTrader;

import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;

/**
 * Represents an item in the trade list with amount and metadata
 */
public class TradeItem {
    private final ItemStack itemStack;
    private final int amount;
    private final String displayName;
    private final String description;
    
    public TradeItem(ItemStack itemStack, int amount) {
        this.itemStack = itemStack.clone();
        this.amount = amount;
        this.displayName = getItemDisplayName(itemStack);
        this.description = getItemDescription(itemStack);
    }
    
    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().displayName() != null) {
            Component displayName = item.getItemMeta().displayName();
            if (displayName != null) {
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }
    
    private String getItemDescription(ItemStack item) {
        StringBuilder desc = new StringBuilder();
        desc.append("Type: ").append(item.getType().name());
        
        if (item.hasItemMeta()) {
            if (item.getItemMeta().lore() != null && !item.getItemMeta().lore().isEmpty()) {
                desc.append("\nLore: ");
                for (Component loreComponent : item.getItemMeta().lore()) {
                    String loreText = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(loreComponent);
                    desc.append("\n  - ").append(loreText);
                }
            }
            if (item.getItemMeta().hasEnchants()) {
                desc.append("\nEnchantments: ");
                item.getItemMeta().getEnchants().forEach((enchant, level) -> 
                    desc.append("\n  - ").append(enchant.getKey().getKey()).append(" ").append(level));
            }
        }
        
        return desc.toString();
    }
    
    public ItemStack getItemStack() {
        return itemStack.clone();
    }
    
    public int getAmount() {
        return amount;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public ItemStack createItemStackWithAmount() {
        ItemStack result = itemStack.clone();
        result.setAmount(amount);
        return result;
    }
}