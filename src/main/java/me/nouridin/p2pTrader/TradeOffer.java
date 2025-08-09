package me.nouridin.p2pTrader;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a player's trade offer containing a list of items
 */
public class TradeOffer {
    private final UUID playerId;
    private final String playerName;
    private final List<TradeItem> items;
    private boolean confirmed;
    
    public TradeOffer(Player player) {
        this.playerId = player.getUniqueId();
        this.playerName = player.getName();
        this.items = new ArrayList<>();
        this.confirmed = false;
    }
    
    public void addItem(ItemStack itemStack, int amount) {
        // Check if we already have this item type
        for (TradeItem existingItem : items) {
            if (existingItem.getItemStack().isSimilar(itemStack)) {
                // Remove the existing item and add a new one with combined amount
                items.remove(existingItem);
                items.add(new TradeItem(itemStack, existingItem.getAmount() + amount));
                return;
            }
        }
        
        // Add new item
        items.add(new TradeItem(itemStack, amount));
    }
    
    public void removeItem(int index) {
        if (index >= 0 && index < items.size()) {
            items.remove(index);
        }
    }
    
    public void clearItems() {
        items.clear();
    }
    
    public List<TradeItem> getItems() {
        return new ArrayList<>(items);
    }
    
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
    
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
    
    public void resetConfirmation() {
        this.confirmed = false;
    }
    
    /**
     * Validates that the player has all the items in their inventory
     */
    public boolean validateInventory(Player player) {
        for (TradeItem tradeItem : items) {
            if (!hasEnoughItems(player, tradeItem.getItemStack(), tradeItem.getAmount())) {
                return false;
            }
        }
        return true;
    }
    
    private boolean hasEnoughItems(Player player, ItemStack itemStack, int requiredAmount) {
        int totalAmount = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(itemStack)) {
                totalAmount += invItem.getAmount();
            }
        }
        return totalAmount >= requiredAmount;
    }
    
    /**
     * Removes the traded items from the player's inventory
     */
    public boolean removeItemsFromInventory(Player player) {
        // First validate we have everything
        if (!validateInventory(player)) {
            return false;
        }
        
        // Remove items
        for (TradeItem tradeItem : items) {
            removeItemFromInventory(player, tradeItem.getItemStack(), tradeItem.getAmount());
        }
        
        return true;
    }
    
    private void removeItemFromInventory(Player player, ItemStack itemStack, int amountToRemove) {
        int remaining = amountToRemove;
        
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null && invItem.isSimilar(itemStack)) {
                int takeAmount = Math.min(remaining, invItem.getAmount());
                remaining -= takeAmount;
                
                if (takeAmount >= invItem.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    invItem.setAmount(invItem.getAmount() - takeAmount);
                }
            }
        }
    }
    
    /**
     * Validates that the player has enough inventory space for all items
     */
    public boolean validateInventorySpace(Player player) {
        // Create a copy of the player's inventory to simulate adding items
        ItemStack[] inventoryContents = player.getInventory().getContents().clone();
        
        for (TradeItem tradeItem : items) {
            ItemStack itemToAdd = tradeItem.createItemStackWithAmount();
            
            if (!canFitInInventory(inventoryContents, itemToAdd)) {
                return false;
            }
            
            // Simulate adding the item to the inventory copy
            addItemToInventorySimulation(inventoryContents, itemToAdd);
        }
        
        return true;
    }
    
    /**
     * Checks if an item can fit in the given inventory
     */
    private boolean canFitInInventory(ItemStack[] inventory, ItemStack itemToAdd) {
        int remainingAmount = itemToAdd.getAmount();
        
        // First, try to stack with existing items
        for (int i = 0; i < inventory.length && remainingAmount > 0; i++) {
            ItemStack slot = inventory[i];
            if (slot != null && slot.isSimilar(itemToAdd)) {
                int maxStack = slot.getMaxStackSize();
                int canAdd = maxStack - slot.getAmount();
                if (canAdd > 0) {
                    remainingAmount -= Math.min(canAdd, remainingAmount);
                }
            }
        }
        
        // Then, check for empty slots
        if (remainingAmount > 0) {
            int emptySlots = 0;
            for (ItemStack slot : inventory) {
                if (slot == null) {
                    emptySlots++;
                }
            }
            
            int maxStack = itemToAdd.getMaxStackSize();
            int slotsNeeded = (int) Math.ceil((double) remainingAmount / maxStack);
            
            return emptySlots >= slotsNeeded;
        }
        
        return true;
    }
    
    /**
     * Simulates adding an item to inventory (for space validation)
     */
    private void addItemToInventorySimulation(ItemStack[] inventory, ItemStack itemToAdd) {
        int remainingAmount = itemToAdd.getAmount();
        
        // First, try to stack with existing items
        for (int i = 0; i < inventory.length && remainingAmount > 0; i++) {
            ItemStack slot = inventory[i];
            if (slot != null && slot.isSimilar(itemToAdd)) {
                int maxStack = slot.getMaxStackSize();
                int canAdd = maxStack - slot.getAmount();
                if (canAdd > 0) {
                    int addAmount = Math.min(canAdd, remainingAmount);
                    slot.setAmount(slot.getAmount() + addAmount);
                    remainingAmount -= addAmount;
                }
            }
        }
        
        // Then, use empty slots
        for (int i = 0; i < inventory.length && remainingAmount > 0; i++) {
            if (inventory[i] == null) {
                int maxStack = itemToAdd.getMaxStackSize();
                int addAmount = Math.min(maxStack, remainingAmount);
                
                ItemStack newStack = itemToAdd.clone();
                newStack.setAmount(addAmount);
                inventory[i] = newStack;
                remainingAmount -= addAmount;
            }
        }
    }
    
    /**
     * Gets the number of inventory slots needed for all items
     */
    public int getRequiredInventorySlots(Player player) {
        ItemStack[] inventoryContents = player.getInventory().getContents().clone();
        int slotsUsed = 0;
        
        for (TradeItem tradeItem : items) {
            ItemStack itemToAdd = tradeItem.createItemStackWithAmount();
            int remainingAmount = itemToAdd.getAmount();
            
            // Try to stack with existing items first
            for (int i = 0; i < inventoryContents.length && remainingAmount > 0; i++) {
                ItemStack slot = inventoryContents[i];
                if (slot != null && slot.isSimilar(itemToAdd)) {
                    int maxStack = slot.getMaxStackSize();
                    int canAdd = maxStack - slot.getAmount();
                    if (canAdd > 0) {
                        int addAmount = Math.min(canAdd, remainingAmount);
                        slot.setAmount(slot.getAmount() + addAmount);
                        remainingAmount -= addAmount;
                    }
                }
            }
            
            // Calculate slots needed for remaining items
            if (remainingAmount > 0) {
                int maxStack = itemToAdd.getMaxStackSize();
                slotsUsed += (int) Math.ceil((double) remainingAmount / maxStack);
            }
        }
        
        return slotsUsed;
    }
    
    /**
     * Gives the traded items to the player with proper space handling
     */
    public boolean giveItemsToPlayer(Player player) {
        // First validate space
        if (!validateInventorySpace(player)) {
            return false;
        }
        
        // Give items to player
        for (TradeItem tradeItem : items) {
            ItemStack itemToGive = tradeItem.createItemStackWithAmount();
            
            // Use addItem which handles stacking automatically
            java.util.HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(itemToGive);
            
            // If there's leftover (shouldn't happen due to validation), drop it
            if (!leftover.isEmpty()) {
                for (ItemStack leftoverItem : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftoverItem);
                }
            }
        }
        
        return true;
    }
}