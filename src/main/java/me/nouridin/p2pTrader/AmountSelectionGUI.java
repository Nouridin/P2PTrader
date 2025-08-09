package me.nouridin.p2pTrader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;

/**
 * GUI for selecting the amount of items to trade
 */
public class AmountSelectionGUI {
    
    private static final int GUI_SIZE = 27;
    
    /**
     * Custom inventory holder for amount selection GUI
     */
    public static class AmountSelectionHolder implements InventoryHolder {
        private final ItemStack item;
        private final int maxAmount;
        
        public AmountSelectionHolder(ItemStack item, int maxAmount) {
            this.item = item;
            this.maxAmount = maxAmount;
        }
        
        @Override
        public Inventory getInventory() {
            return null; // Not needed for our use case
        }
        
        public ItemStack getItem() {
            return item;
        }
        
        public int getMaxAmount() {
            return maxAmount;
        }
    }
    
    public static void openAmountSelection(Player player, ItemStack item, int maxAmount) {
        AmountSelectionHolder holder = new AmountSelectionHolder(item, maxAmount);
        Component title = Component.text("Select Amount: " + getItemName(item))
            .color(NamedTextColor.BLUE);
        Inventory gui = Bukkit.createInventory(holder, GUI_SIZE, title);
        
        // Show the item being selected
        ItemStack displayItem = item.clone();
        displayItem.setAmount(1);
        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(getItemName(item)).color(NamedTextColor.YELLOW));
            meta.lore(Arrays.asList(
                Component.text("Available: " + maxAmount).color(NamedTextColor.GRAY),
                Component.text("Click buttons to select amount").color(NamedTextColor.GRAY)
            ));
            displayItem.setItemMeta(meta);
        }
        gui.setItem(4, displayItem);
        
        // Amount selection buttons
        if (maxAmount >= 1) gui.setItem(10, createAmountButton(1));
        if (maxAmount >= 8) gui.setItem(11, createAmountButton(8));
        if (maxAmount >= 16) gui.setItem(12, createAmountButton(16));
        if (maxAmount >= 32) gui.setItem(13, createAmountButton(32));
        if (maxAmount >= 64) gui.setItem(14, createAmountButton(64));
        
        // Max amount button
        gui.setItem(15, createMaxAmountButton(maxAmount));
        
        // Cancel button
        gui.setItem(22, createCancelButton());
        
        player.openInventory(gui);
    }
    
    private static ItemStack createAmountButton(int amount) {
        ItemStack button = new ItemStack(Material.GREEN_WOOL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Select " + amount).color(NamedTextColor.GREEN));
            meta.lore(Arrays.asList(
                Component.text("Click to select " + amount + " items").color(NamedTextColor.GRAY)
            ));
            button.setItemMeta(meta);
        }
        return button;
    }
    
    private static ItemStack createMaxAmountButton(int maxAmount) {
        ItemStack button = new ItemStack(Material.GREEN_WOOL); // Using GREEN_WOOL instead of LIME_WOOL
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Select All (" + maxAmount + ")").color(NamedTextColor.GREEN));
            meta.lore(Arrays.asList(
                Component.text("Click to select all " + maxAmount + " items").color(NamedTextColor.GRAY)
            ));
            button.setItemMeta(meta);
        }
        return button;
    }
    
    private static ItemStack createCancelButton() {
        ItemStack button = new ItemStack(Material.RED_WOOL);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Cancel").color(NamedTextColor.RED));
            meta.lore(Arrays.asList(
                Component.text("Click to cancel").color(NamedTextColor.GRAY)
            ));
            button.setItemMeta(meta);
        }
        return button;
    }
    
    private static String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().displayName() != null) {
            Component displayName = item.getItemMeta().displayName();
            if (displayName != null) {
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }
    
    public static boolean isAmountSelectionGUI(Inventory inventory) {
        return inventory.getHolder() instanceof AmountSelectionHolder;
    }
    
    public static int getSelectedAmount(ItemStack clickedItem, Inventory inventory) {
        if (clickedItem == null || !clickedItem.hasItemMeta()) return -1;
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.displayName() == null) return -1;
        
        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) return -1;
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayNameComponent);
        
        if (displayName.equals("Cancel")) {
            return -1;
        }
        
        // Handle regular "Select X" buttons
        if (displayName.startsWith("Select ") && !displayName.startsWith("Select All")) {
            try {
                String amountStr = displayName.substring("Select ".length());
                return Integer.parseInt(amountStr);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        
        // Handle "Select All (X)" button
        if (displayName.startsWith("Select All")) {
            // First try to parse from the display name
            if (displayName.contains("(") && displayName.contains(")")) {
                try {
                    int startIndex = displayName.indexOf("(") + 1;
                    int endIndex = displayName.indexOf(")");
                    String amountStr = displayName.substring(startIndex, endIndex);
                    return Integer.parseInt(amountStr);
                } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                    // Parsing failed, continue to fallback
                }
            }
            
            // Fallback: Get max amount from inventory holder
            if (inventory != null && inventory.getHolder() instanceof AmountSelectionHolder) {
                AmountSelectionHolder holder = (AmountSelectionHolder) inventory.getHolder();
                return holder.getMaxAmount();
            }
        }
        
        return -1;
    }
    
    // Keep the old method for backward compatibility but mark as deprecated
    @Deprecated
    public static int getSelectedAmount(ItemStack clickedItem) {
        return getSelectedAmount(clickedItem, null);
    }
}