package me.nouridin.p2pTrader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.*;

/**
 * Manages trade state and operations
 */
public class TradeManager {
    
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, Inventory> activeTrades = new HashMap<>();
    private final Set<UUID> confirmedTrades = new HashSet<>();
    private final Map<Inventory, UUID> leftSidePlayers = new HashMap<>();
    
    /**
     * Adds a pending trade request
     */
    public void addPendingRequest(UUID requester, UUID target) {
        pendingRequests.put(target, requester);
    }
    
    /**
     * Checks if there's a pending request from target to requester
     */
    public boolean hasPendingRequest(UUID requester, UUID target) {
        return pendingRequests.containsKey(requester) && 
               pendingRequests.get(requester).equals(target);
    }
    
    /**
     * Removes a pending request
     */
    public void removePendingRequest(UUID playerId) {
        pendingRequests.remove(playerId);
    }
    
    /**
     * Adds an active trade
     */
    public void addActiveTrade(UUID player1, UUID player2, Inventory inventory) {
        activeTrades.put(player1, inventory);
        activeTrades.put(player2, inventory);
        leftSidePlayers.put(inventory, player1); // player1 is always left side
    }
    
    /**
     * Checks if a player has an active trade
     */
    public boolean hasActiveTrade(UUID playerId) {
        return activeTrades.containsKey(playerId);
    }
    
    /**
     * Gets the trade inventory for a player
     */
    public Inventory getTradeInventory(UUID playerId) {
        return activeTrades.get(playerId);
    }
    
    /**
     * Gets the other player in a trade
     */
    public Player getOtherPlayer(UUID currentPlayer, Inventory inv) {
        for (Map.Entry<UUID, Inventory> entry : activeTrades.entrySet()) {
            if (entry.getValue().equals(inv) && !entry.getKey().equals(currentPlayer)) {
                return Bukkit.getPlayer(entry.getKey());
            }
        }
        return null;
    }
    
    /**
     * Checks if a player is on the left side of the trade GUI
     */
    public boolean isPlayerLeftSide(UUID playerId, Inventory inv) {
        UUID leftSidePlayer = leftSidePlayers.get(inv);
        return leftSidePlayer != null && leftSidePlayer.equals(playerId);
    }
    
    /**
     * Adds a player confirmation
     */
    public void addConfirmation(UUID playerId) {
        confirmedTrades.add(playerId);
    }
    
    /**
     * Checks if a player has confirmed
     */
    public boolean hasConfirmed(UUID playerId) {
        return confirmedTrades.contains(playerId);
    }
    
    /**
     * Removes all confirmations for players in a trade
     */
    public void resetConfirmations(Inventory inv) {
        List<UUID> playersInTrade = getPlayersInTrade(inv);
        for (UUID playerId : playersInTrade) {
            confirmedTrades.remove(playerId);
        }
    }
    
    /**
     * Gets all players in a specific trade
     */
    public List<UUID> getPlayersInTrade(Inventory inv) {
        List<UUID> players = new ArrayList<>();
        for (Map.Entry<UUID, Inventory> entry : activeTrades.entrySet()) {
            if (entry.getValue().equals(inv)) {
                players.add(entry.getKey());
            }
        }
        return players;
    }
    
    /**
     * Checks if both players in a trade have confirmed
     */
    public boolean areBothPlayersConfirmed(Inventory inv) {
        List<UUID> players = getPlayersInTrade(inv);
        if (players.size() != 2) return false;
        
        return confirmedTrades.contains(players.get(0)) && 
               confirmedTrades.contains(players.get(1));
    }
    
    /**
     * Removes a trade completely
     */
    public void removeTrade(Inventory inv) {
        List<UUID> players = getPlayersInTrade(inv);
        for (UUID playerId : players) {
            activeTrades.remove(playerId);
            confirmedTrades.remove(playerId);
        }
        leftSidePlayers.remove(inv);
    }
    
    /**
     * Cleans up all data for a player (when they quit)
     */
    public void cleanupPlayer(UUID playerId) {
        pendingRequests.remove(playerId);
        
        // Clean up active trades for this player
        if (activeTrades.containsKey(playerId)) {
            Inventory inv = activeTrades.get(playerId);
            leftSidePlayers.remove(inv);
        }
        
        activeTrades.remove(playerId);
        confirmedTrades.remove(playerId);
    }
    
    /**
     * Cleans up trade when inventory is closed
     * NOTE: This method is now deprecated in favor of the enhanced closure handling in TradeEventHandler
     * It's kept for backward compatibility but the new system handles synchronized closure properly
     */
    @Deprecated
    public void cleanupTradeOnClose(UUID playerId) {
        if (activeTrades.containsKey(playerId)) {
            Inventory inv = activeTrades.get(playerId);
            activeTrades.values().removeIf(value -> value.equals(inv));
            activeTrades.remove(playerId);
            confirmedTrades.remove(playerId);
            leftSidePlayers.remove(inv);
        }
    }
}