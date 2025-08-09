package me.nouridin.p2pTrader;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the new list-based trading system
 */
public class NewTradeManager {
    
    private final Map<UUID, UUID> pendingRequests = new HashMap<>();
    private final Map<UUID, TradeSession> activeTrades = new HashMap<>();
    
    /**
     * Sends a trade request
     */
    public void sendTradeRequest(Player requester, Player target) {
        pendingRequests.put(target.getUniqueId(), requester.getUniqueId());
    }
    
    /**
     * Checks if there's a pending request
     */
    public boolean hasPendingRequest(Player player, Player target) {
        UUID requesterId = pendingRequests.get(player.getUniqueId());
        return requesterId != null && requesterId.equals(target.getUniqueId());
    }
    
    /**
     * Accepts a trade request and creates a new session
     */
    public TradeSession acceptTradeRequest(Player player, Player target) {
        if (hasPendingRequest(player, target)) {
            pendingRequests.remove(player.getUniqueId());
            
            TradeSession session = new TradeSession(target, player);
            activeTrades.put(player.getUniqueId(), session);
            activeTrades.put(target.getUniqueId(), session);
            
            return session;
        }
        return null;
    }
    
    /**
     * Gets the active trade session for a player
     */
    public TradeSession getTradeSession(UUID playerId) {
        return activeTrades.get(playerId);
    }
    
    /**
     * Checks if a player has an active trade
     */
    public boolean hasActiveTrade(UUID playerId) {
        return activeTrades.containsKey(playerId);
    }
    
    /**
     * Cancels a trade session
     */
    public void cancelTrade(UUID playerId) {
        TradeSession session = activeTrades.get(playerId);
        if (session != null) {
            // Remove both players from active trades
            Player player1 = session.getPlayer1();
            Player player2 = session.getPlayer2();
            
            if (player1 != null) {
                activeTrades.remove(player1.getUniqueId());
            }
            if (player2 != null) {
                activeTrades.remove(player2.getUniqueId());
            }
        }
    }
    
    /**
     * Completes a trade and removes the session
     */
    public void completeTrade(UUID playerId) {
        cancelTrade(playerId); // Same cleanup process
    }
    
    /**
     * Cleans up all data for a player (when they quit)
     */
    public void cleanupPlayer(UUID playerId) {
        pendingRequests.remove(playerId);
        cancelTrade(playerId);
        
        // Remove any pending requests TO this player
        pendingRequests.entrySet().removeIf(entry -> entry.getValue().equals(playerId));
    }
    
    /**
     * Removes a pending request
     */
    public void removePendingRequest(UUID playerId) {
        pendingRequests.remove(playerId);
    }
}