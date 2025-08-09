package me.nouridin.p2pTrader;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Command handler for the new list-based trading system
 */
public class NewTradeCommand implements CommandExecutor {
    
    private final NewTradeManager tradeManager;
    
    public NewTradeCommand(NewTradeManager tradeManager) {
        this.tradeManager = tradeManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("This command can only be used by players!").color(NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;
        
        // Check permission
        if (!player.hasPermission(TradeConfig.TRADE_PERMISSION)) {
            player.sendMessage(Component.text("You don't have permission to use this command!").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length == 0) {
            showHelp(player);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "request":
                return handleTradeRequest(player, args);
            case "accept":
                return handleTradeAccept(player, args);
            case "add":
                return handleAddItem(player);
            case "remove":
                return handleRemoveItem(player, args);
            case "confirm":
                return handleConfirm(player);
            case "cancel":
                return handleCancel(player);
            case "status":
                return handleStatus(player);
            default:
                // If no subcommand, treat as trade request
                return handleTradeRequest(player, args);
        }
    }
    
    private boolean handleTradeRequest(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /trade <player> or /trade request <player>").color(NamedTextColor.RED));
            return true;
        }
        
        String targetName = args.length > 1 ? args[1] : args[0];
        Player target = Bukkit.getPlayerExact(targetName);
        
        // Validate the trade request
        TradeValidator.TradeValidationResult validation = TradeValidator.validateTradeRequest(player, target);
        if (!validation.isValid()) {
            player.sendMessage(Component.text(validation.getErrorMessage()).color(NamedTextColor.RED));
            return true;
        }
        
        // Check if either player is already in a trade
        if (tradeManager.hasActiveTrade(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a trading session!").color(NamedTextColor.RED));
            return true;
        }
        
        if (tradeManager.hasActiveTrade(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is already in a trading session!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if this is accepting a pending request
        if (tradeManager.hasPendingRequest(player, target)) {
            // Accept the trade
            TradeSession session = tradeManager.acceptTradeRequest(player, target);
            if (session != null) {
                player.sendMessage(Component.text("Trade started with " + target.getName() + "!").color(NamedTextColor.GREEN));
                target.sendMessage(Component.text(player.getName() + " accepted your trade request!").color(NamedTextColor.GREEN));
                session.updateBothPlayers();
            }
        } else {
            // Send new trade request
            tradeManager.sendTradeRequest(player, target);
            player.sendMessage(Component.text("Trade request sent to " + target.getName()).color(NamedTextColor.GREEN));
            target.sendMessage(Component.text(player.getName() + " wants to trade with you!").color(NamedTextColor.YELLOW));
            target.sendMessage(Component.text("Type '/trade accept " + player.getName() + "' to accept.").color(NamedTextColor.YELLOW));
        }
        
        return true;
    }
    
    private boolean handleTradeAccept(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /trade accept <player>").color(NamedTextColor.RED));
            return true;
        }
        
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
            return true;
        }
        
        // Check if either player is already in a trade
        if (tradeManager.hasActiveTrade(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a trading session!").color(NamedTextColor.RED));
            return true;
        }
        
        if (tradeManager.hasActiveTrade(target.getUniqueId())) {
            player.sendMessage(Component.text(target.getName() + " is already in a trading session!").color(NamedTextColor.RED));
            return true;
        }
        
        if (tradeManager.hasPendingRequest(player, target)) {
            TradeSession session = tradeManager.acceptTradeRequest(player, target);
            if (session != null) {
                player.sendMessage(Component.text("Trade started with " + target.getName() + "!").color(NamedTextColor.GREEN));
                target.sendMessage(Component.text(player.getName() + " accepted your trade request!").color(NamedTextColor.GREEN));
                session.updateBothPlayers();
            }
        } else {
            player.sendMessage(Component.text("No pending trade request from " + target.getName()).color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleAddItem(Player player) {
        TradeSession session = tradeManager.getTradeSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trade!").color(NamedTextColor.RED));
            return true;
        }
        
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType() == Material.AIR) {
            player.sendMessage(Component.text("You must hold an item in your hand!").color(NamedTextColor.RED));
            return true;
        }
        
        // Count how many of this item the player has
        int totalAmount = 0;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(handItem)) {
                totalAmount += invItem.getAmount();
            }
        }
        
        if (totalAmount == 0) {
            player.sendMessage(Component.text("You don't have this item!").color(NamedTextColor.RED));
            return true;
        }
        
        // If stackable and more than 1, open amount selection GUI
        if (handItem.getMaxStackSize() > 1 && totalAmount > 1) {
            AmountSelectionGUI.openAmountSelection(player, handItem, totalAmount);
        } else {
            // Add single item directly
            session.addItemToOffer(player.getUniqueId(), handItem, 1);
            player.sendMessage(Component.text("Added " + getItemName(handItem) + " to your trade offer!").color(NamedTextColor.GREEN));
        }
        
        return true;
    }
    
    private boolean handleRemoveItem(Player player, String[] args) {
        TradeSession session = tradeManager.getTradeSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trade!").color(NamedTextColor.RED));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /trade remove <item_number>").color(NamedTextColor.RED));
            return true;
        }
        
        try {
            int itemIndex = Integer.parseInt(args[1]) - 1; // Convert to 0-based index
            session.removeItemFromOffer(player.getUniqueId(), itemIndex);
            player.sendMessage(Component.text("Removed item from your trade offer!").color(NamedTextColor.GREEN));
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Invalid item number!").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleConfirm(Player player) {
        TradeSession session = tradeManager.getTradeSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trade!").color(NamedTextColor.RED));
            return true;
        }
        
        if (session.confirmTrade(player.getUniqueId())) {
            // Trade completed
            tradeManager.completeTrade(player.getUniqueId());
        }
        
        return true;
    }
    
    private boolean handleCancel(Player player) {
        TradeSession session = tradeManager.getTradeSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trade!").color(NamedTextColor.RED));
            return true;
        }
        
        Player otherPlayer = session.getOtherPlayer(player.getUniqueId());
        tradeManager.cancelTrade(player.getUniqueId());
        
        player.sendMessage(Component.text("Trade cancelled!").color(NamedTextColor.RED));
        if (otherPlayer != null) {
            otherPlayer.sendMessage(Component.text("Trade cancelled by " + player.getName() + "!").color(NamedTextColor.RED));
        }
        
        return true;
    }
    
    private boolean handleStatus(Player player) {
        TradeSession session = tradeManager.getTradeSession(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Component.text("You are not in a trade!").color(NamedTextColor.RED));
            return true;
        }
        
        session.updateBothPlayers();
        return true;
    }
    
    private void showHelp(Player player) {
        player.sendMessage(Component.text("╔══════════════════════════════════════════════════════════════╗").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("║                    P2PTrader v1.0.0                          ║").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("║              Professional Trading Commands                   ║").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("╠══════════════════════════════════════════════════════════════╣").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("║ /trade <player>      - Send secure trade request             ║").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("║ /trade accept <player> - Accept trade request                ║").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("║ /trade add           - Add item from hand to trade           ║").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("║ /trade remove <#>    - Remove item from trade                ║").color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("║ /trade confirm       - Confirm your trade                    ║").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("║ /trade cancel        - Cancel current trade                  ║").color(NamedTextColor.RED));
        player.sendMessage(Component.text("║ /trade status        - Show detailed trade status            ║").color(NamedTextColor.AQUA));
        player.sendMessage(Component.text("╠══════════════════════════════════════════════════════════════╣").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("║ Features: Anti-Scam • Hover Info • Session Protection        ║").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("║ Developed by Nouridin - https://github.com/Nouridin          ║").color(NamedTextColor.GRAY));
        player.sendMessage(Component.text("╚═══════════════���═════════════════════════════════════ ════╝").color(NamedTextColor.GOLD));
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