package me.nouridin.p2pTrader;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;

/**
 * Handles validation logic for trades
 */
public class TradeValidator {
    
    /**
     * Validates if two players can start a trade
     */
    public static TradeValidationResult validateTradeRequest(Player requester, Player target) {
        // Check if target exists and is online
        if (target == null || !target.isOnline()) {
            return new TradeValidationResult(false, "Player not found or offline.");
        }
        
        // Check if trying to trade with self
        if (requester.equals(target)) {
            return new TradeValidationResult(false, "You can't trade with yourself!");
        }
        
        // Check if players are in the same world
        if (!requester.getWorld().equals(target.getWorld())) {
            return new TradeValidationResult(false, "You can only trade with players in the same world!");
        }
        
        // Check distance between players
        double distance = requester.getLocation().distance(target.getLocation());
        if (distance > TradeConfig.MAX_TRADE_DISTANCE) {
            return new TradeValidationResult(false, 
                String.format("You are too far away! You must be within %.1f blocks of %s to trade. (Current distance: %.1f blocks)", 
                    TradeConfig.MAX_TRADE_DISTANCE, target.getName(), distance));
        }
        
        return new TradeValidationResult(true, "");
    }
    
    /**
     * Checks if two players are close enough to complete a trade
     */
    public static boolean arePlayersCloseEnough(Player player1, Player player2) {
        if (player1 == null || player2 == null) return false;
        
        // Check if players are in the same world
        if (!player1.getWorld().equals(player2.getWorld())) return false;
        
        // Check distance
        double distance = player1.getLocation().distance(player2.getLocation());
        return distance <= TradeConfig.MAX_TRADE_DISTANCE;
    }
    
    /**
     * Checks if an item is a system item (GUI elements)
     */
    public static boolean isSystemItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || meta.displayName() == null) return false;
        
        Component displayNameComponent = meta.displayName();
        if (displayNameComponent == null) return false;
        
        String displayName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayNameComponent);
        
        // Check for system items by their display names
        return displayName.equals("Click to Confirm") ||
               displayName.equals("✓ CONFIRMED") ||
               displayName.equals(" ") ||  // Divider glass pane
               displayName.contains(" - READY") ||
               displayName.contains(" - NOT READY");
    }
    
    /**
     * Validates if a click in the trade GUI is allowed
     */
    public static boolean isValidTradeSlot(int slot, int column, boolean isLeftSide) {
        // Check if it's in the trade area (slots 0-44)
        if (slot < 0 || slot > TradeConfig.TRADE_AREA_END) return false;
        
        // Cancel clicks on divider column
        if (column == TradeConfig.DIVIDER_COLUMN) return false;
        
        // Check if player is clicking on their correct side
        if (isLeftSide) {
            return column <= TradeConfig.LEFT_SIDE_MAX_COLUMN;
        } else {
            return column >= TradeConfig.RIGHT_SIDE_MIN_COLUMN;
        }
    }
    
    /**
     * Result class for trade validation
     */
    public static class TradeValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        public TradeValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}