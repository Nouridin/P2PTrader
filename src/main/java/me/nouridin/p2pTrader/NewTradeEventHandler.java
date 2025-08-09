package me.nouridin.p2pTrader;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

/**
 * Event handler for the new list-based trading system
 */
public class NewTradeEventHandler implements Listener {
    
    private final NewTradeManager tradeManager;
    
    public NewTradeEventHandler(NewTradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        
        // Handle amount selection GUI
        if (AmountSelectionGUI.isAmountSelectionGUI(e.getInventory())) {
            e.setCancelled(true);
            
            ItemStack clickedItem = e.getCurrentItem();
            if (clickedItem == null) return;
            
            int selectedAmount = AmountSelectionGUI.getSelectedAmount(clickedItem, e.getInventory());
            
            if (selectedAmount == -1) {
                // Cancel or invalid selection
                player.closeInventory();
                player.sendMessage(Component.text("Item selection cancelled.").color(NamedTextColor.YELLOW));
                return;
            }
            
            // Get the item from the GUI (should be in slot 4)
            ItemStack displayItem = e.getInventory().getItem(4);
            if (displayItem == null) {
                player.closeInventory();
                player.sendMessage(Component.text("Error: Could not find item to add.").color(NamedTextColor.RED));
                return;
            }
            
            // Add the item to the trade
            TradeSession session = tradeManager.getTradeSession(player.getUniqueId());
            if (session != null) {
                // Create a clean version of the item (remove the GUI metadata)
                ItemStack itemToAdd = displayItem.clone();
                itemToAdd.setAmount(1);
                if (itemToAdd.hasItemMeta()) {
                    ItemMeta meta = itemToAdd.getItemMeta();
                    meta.displayName(null);
                    meta.lore(null);
                    itemToAdd.setItemMeta(meta);
                }
                
                session.addItemToOffer(player.getUniqueId(), itemToAdd, selectedAmount);
                player.sendMessage(Component.text("Added " + selectedAmount + "x " + 
                                 getItemName(itemToAdd) + " to your trade offer!").color(NamedTextColor.GREEN));
            }
            
            player.closeInventory();
        }
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        
        // Check if player has an active trade
        TradeSession session = tradeManager.getTradeSession(uuid);
        if (session != null) {
            Player otherPlayer = session.getOtherPlayer(uuid);
            if (otherPlayer != null && otherPlayer.isOnline()) {
                otherPlayer.sendMessage(Component.text("Trade cancelled: " + e.getPlayer().getName() + " left the game.").color(NamedTextColor.RED));
            }
        }
        
        // Clean up all player data
        tradeManager.cleanupPlayer(uuid);
    }
    
    private String getItemName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().displayName() != null) {
            Component displayName = item.getItemMeta().displayName();
            if (displayName != null) {
                return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
            }
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }
}