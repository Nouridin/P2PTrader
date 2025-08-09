package me.nouridin.p2pTrader;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.UUID;

/**
 * Represents a trading session between two players using the new list-based system
 */
public class TradeSession {
    private final UUID sessionId;
    private final TradeOffer player1Offer;
    private final TradeOffer player2Offer;
    private final long createdTime;
    
    public TradeSession(Player player1, Player player2) {
        this.sessionId = UUID.randomUUID();
        this.player1Offer = new TradeOffer(player1);
        this.player2Offer = new TradeOffer(player2);
        this.createdTime = System.currentTimeMillis();
    }
    
    public UUID getSessionId() {
        return sessionId;
    }
    
    public TradeOffer getPlayerOffer(UUID playerId) {
        if (player1Offer.getPlayerId().equals(playerId)) {
            return player1Offer;
        } else if (player2Offer.getPlayerId().equals(playerId)) {
            return player2Offer;
        }
        return null;
    }
    
    public TradeOffer getOtherPlayerOffer(UUID playerId) {
        if (player1Offer.getPlayerId().equals(playerId)) {
            return player2Offer;
        } else if (player2Offer.getPlayerId().equals(playerId)) {
            return player1Offer;
        }
        return null;
    }
    
    public boolean containsPlayer(UUID playerId) {
        return player1Offer.getPlayerId().equals(playerId) || player2Offer.getPlayerId().equals(playerId);
    }
    
    public Player getPlayer1() {
        return Bukkit.getPlayer(player1Offer.getPlayerId());
    }
    
    public Player getPlayer2() {
        return Bukkit.getPlayer(player2Offer.getPlayerId());
    }
    
    public Player getOtherPlayer(UUID playerId) {
        if (player1Offer.getPlayerId().equals(playerId)) {
            return getPlayer2();
        } else if (player2Offer.getPlayerId().equals(playerId)) {
            return getPlayer1();
        }
        return null;
    }
    
    public void addItemToOffer(UUID playerId, ItemStack itemStack, int amount) {
        TradeOffer offer = getPlayerOffer(playerId);
        if (offer != null) {
            offer.addItem(itemStack, amount);
            offer.resetConfirmation();
            
            // Reset other player's confirmation too
            TradeOffer otherOffer = getOtherPlayerOffer(playerId);
            if (otherOffer != null) {
                otherOffer.resetConfirmation();
            }
            
            // Notify both players
            updateBothPlayers();
        }
    }
    
    public void removeItemFromOffer(UUID playerId, int itemIndex) {
        TradeOffer offer = getPlayerOffer(playerId);
        if (offer != null) {
            offer.removeItem(itemIndex);
            offer.resetConfirmation();
            
            // Reset other player's confirmation too
            TradeOffer otherOffer = getOtherPlayerOffer(playerId);
            if (otherOffer != null) {
                otherOffer.resetConfirmation();
            }
            
            // Notify both players
            updateBothPlayers();
        }
    }
    
    public boolean confirmTrade(UUID playerId) {
        TradeOffer offer = getPlayerOffer(playerId);
        if (offer != null) {
            offer.setConfirmed(true);
            
            Player player = Bukkit.getPlayer(playerId);
            Player otherPlayer = getOtherPlayer(playerId);
            
            if (player != null && otherPlayer != null) {
                player.sendMessage(Component.text("You confirmed the trade!").color(NamedTextColor.GREEN));
                otherPlayer.sendMessage(Component.text(player.getName() + " confirmed their trade!").color(NamedTextColor.YELLOW));
                
                // Check if both players confirmed
                if (areBothPlayersConfirmed()) {
                    return executeTrade();
                } else {
                    updateBothPlayers();
                }
            }
        }
        return false;
    }
    
    public boolean areBothPlayersConfirmed() {
        return player1Offer.isConfirmed() && player2Offer.isConfirmed();
    }
    
    private boolean executeTrade() {
        Player player1 = getPlayer1();
        Player player2 = getPlayer2();
        
        if (player1 == null || player2 == null) {
            return false;
        }
        
        // Validate both players have their items
        if (!player1Offer.validateInventory(player1)) {
            player1.sendMessage(Component.text("Trade failed: You don't have all the required items!").color(NamedTextColor.RED));
            player2.sendMessage(Component.text("Trade failed: " + player1.getName() + " doesn't have all required items!").color(NamedTextColor.RED));
            return false;
        }
        
        if (!player2Offer.validateInventory(player2)) {
            player1.sendMessage(Component.text("Trade failed: " + player2.getName() + " doesn't have all required items!").color(NamedTextColor.RED));
            player2.sendMessage(Component.text("Trade failed: You don't have all the required items!").color(NamedTextColor.RED));
            return false;
        }
        
        // Check distance
        if (!TradeValidator.arePlayersCloseEnough(player1, player2)) {
            player1.sendMessage(Component.text("Trade failed: You are too far apart!").color(NamedTextColor.RED));
            player2.sendMessage(Component.text("Trade failed: You are too far apart!").color(NamedTextColor.RED));
            return false;
        }
        
        // Check inventory space for both players
        if (!player2Offer.validateInventorySpace(player1)) {
            int slotsNeeded = player2Offer.getRequiredInventorySlots(player1);
            player1.sendMessage(Component.text("Trade failed: You need " + slotsNeeded + " more inventory slots!").color(NamedTextColor.RED));
            player2.sendMessage(Component.text("Trade failed: " + player1.getName() + " doesn't have enough inventory space!").color(NamedTextColor.RED));
            return false;
        }
        
        if (!player1Offer.validateInventorySpace(player2)) {
            int slotsNeeded = player1Offer.getRequiredInventorySlots(player2);
            player1.sendMessage(Component.text("Trade failed: " + player2.getName() + " doesn't have enough inventory space!").color(NamedTextColor.RED));
            player2.sendMessage(Component.text("Trade failed: You need " + slotsNeeded + " more inventory slots!").color(NamedTextColor.RED));
            return false;
        }
        
        // Execute the trade
        if (player1Offer.removeItemsFromInventory(player1) && player2Offer.removeItemsFromInventory(player2)) {
            // Give items to each other
            boolean success1 = player1Offer.giveItemsToPlayer(player2);
            boolean success2 = player2Offer.giveItemsToPlayer(player1);
            
            if (success1 && success2) {
                player1.sendMessage(Component.text("Trade completed successfully with " + player2.getName() + "!").color(NamedTextColor.GREEN));
                player2.sendMessage(Component.text("Trade completed successfully with " + player1.getName() + "!").color(NamedTextColor.GREEN));
                return true;
            } else {
                // This shouldn't happen due to validation, but handle it gracefully
                player1.sendMessage(Component.text("Trade completed but some items were dropped on the ground!").color(NamedTextColor.YELLOW));
                player2.sendMessage(Component.text("Trade completed but some items were dropped on the ground!").color(NamedTextColor.YELLOW));
                return true;
            }
        } else {
            player1.sendMessage(Component.text("Trade failed: Could not remove items from inventory!").color(NamedTextColor.RED));
            player2.sendMessage(Component.text("Trade failed: Could not remove items from inventory!").color(NamedTextColor.RED));
            return false;
        }
    }
    
    public void updateBothPlayers() {
        Player player1 = getPlayer1();
        Player player2 = getPlayer2();
        
        if (player1 != null) {
            sendTradeUpdate(player1);
        }
        if (player2 != null) {
            sendTradeUpdate(player2);
        }
    }
    
    private void sendTradeUpdate(Player player) {
        player.sendMessage(Component.text("=== TRADE STATUS ===").color(NamedTextColor.GOLD));
        
        TradeOffer myOffer = getPlayerOffer(player.getUniqueId());
        TradeOffer otherOffer = getOtherPlayerOffer(player.getUniqueId());
        Player otherPlayer = getOtherPlayer(player.getUniqueId());
        
        if (myOffer != null && otherOffer != null && otherPlayer != null) {
            // Show my offer
            player.sendMessage(Component.text("Your offer:").color(NamedTextColor.YELLOW));
            if (myOffer.isEmpty()) {
                player.sendMessage(Component.text("  (No items)").color(NamedTextColor.GRAY));
            } else {
                for (int i = 0; i < myOffer.getItems().size(); i++) {
                    TradeItem item = myOffer.getItems().get(i);
                    Component itemComponent = createItemComponent(item, i + 1);
                    player.sendMessage(itemComponent);
                }
            }
            
            // Show other player's offer
            player.sendMessage(Component.text(otherPlayer.getName() + "'s offer:").color(NamedTextColor.YELLOW));
            if (otherOffer.isEmpty()) {
                player.sendMessage(Component.text("  (No items)").color(NamedTextColor.GRAY));
            } else {
                for (int i = 0; i < otherOffer.getItems().size(); i++) {
                    TradeItem item = otherOffer.getItems().get(i);
                    Component itemComponent = createItemComponent(item, i + 1);
                    player.sendMessage(itemComponent);
                }
            }
            
            // Show confirmation status
            Component myStatus = myOffer.isConfirmed() ? 
                Component.text("CONFIRMED").color(NamedTextColor.GREEN) : 
                Component.text("NOT CONFIRMED").color(NamedTextColor.RED);
            Component otherStatus = otherOffer.isConfirmed() ? 
                Component.text("CONFIRMED").color(NamedTextColor.GREEN) : 
                Component.text("NOT CONFIRMED").color(NamedTextColor.RED);
            
            player.sendMessage(Component.text("Status:").color(NamedTextColor.YELLOW));
            player.sendMessage(Component.text("  You: ").color(NamedTextColor.WHITE).append(myStatus));
            player.sendMessage(Component.text("  " + otherPlayer.getName() + ": ").color(NamedTextColor.WHITE).append(otherStatus));
            
            // Show inventory space information
            int slotsNeeded = otherOffer.getRequiredInventorySlots(player);
            int emptySlots = getEmptyInventorySlots(player);
            
            if (slotsNeeded > 0) {
                Component spaceStatus;
                if (emptySlots >= slotsNeeded) {
                    spaceStatus = Component.text("✓ Enough space (" + emptySlots + "/" + slotsNeeded + " slots)").color(NamedTextColor.GREEN);
                } else {
                    spaceStatus = Component.text("✗ Need " + (slotsNeeded - emptySlots) + " more slots (" + emptySlots + "/" + slotsNeeded + ")").color(NamedTextColor.RED);
                }
                player.sendMessage(Component.text("Inventory Space: ").color(NamedTextColor.YELLOW).append(spaceStatus));
            }
            
            // Show commands
            player.sendMessage(Component.text("Commands:").color(NamedTextColor.AQUA));
            player.sendMessage(Component.text("  /trade add - Add item from hand").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /trade remove <number> - Remove item").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /trade confirm - Confirm trade").color(NamedTextColor.WHITE));
            player.sendMessage(Component.text("  /trade cancel - Cancel trade").color(NamedTextColor.WHITE));
        }
        
        player.sendMessage(Component.text("==================").color(NamedTextColor.GOLD));
    }
    
    /**
     * Creates a component for an item with hover description
     */
    private Component createItemComponent(TradeItem item, int index) {
        // Create the main text
        Component mainText = Component.text("  " + index + ". " + item.getDisplayName() + 
                                          " x" + item.getAmount()).color(NamedTextColor.WHITE);
        
        // Create hover text with item description
        Component hoverText = createItemHoverText(item);
        
        // Add hover event
        return mainText.hoverEvent(HoverEvent.showText(hoverText));
    }
    
    /**
     * Creates detailed hover text for an item
     */
    private Component createItemHoverText(TradeItem item) {
        ItemStack itemStack = item.getItemStack();
        
        // Use a list to collect all components, then join them
        java.util.List<Component> components = new java.util.ArrayList<>();
        
        // Start with item name (with rarity color if possible)
        Component itemName = Component.text(item.getDisplayName()).color(getItemRarityColor(itemStack));
        components.add(itemName);
        components.add(Component.newline());
        
        // Add material type
        components.add(Component.text("Type: " + formatMaterialName(itemStack.getType().name())).color(NamedTextColor.GRAY));
        components.add(Component.newline());
        
        // Add amount
        components.add(Component.text("Amount: " + item.getAmount()).color(NamedTextColor.GRAY));
        components.add(Component.newline());
        
        // Add detailed durability information
        if (itemStack.getType().getMaxDurability() > 0) {
            int currentDurability = itemStack.getType().getMaxDurability() - itemStack.getDurability();
            int maxDurability = itemStack.getType().getMaxDurability();
            double durabilityPercent = (double) currentDurability / maxDurability * 100;
            
            NamedTextColor durabilityColor = getDurabilityColor(durabilityPercent);
            
            components.add(Component.text("Durability: " + currentDurability + "/" + maxDurability + 
                          " (" + String.format("%.1f", durabilityPercent) + "%)").color(durabilityColor));
            components.add(Component.newline());
            
            // Add durability status
            String durabilityStatus = getDurabilityStatus(durabilityPercent);
            components.add(Component.text("Condition: " + durabilityStatus).color(durabilityColor));
            components.add(Component.newline());
        }
        
        // Handle enchantment books specially
        if (itemStack.getType().name().equals("ENCHANTED_BOOK")) {
            components.add(Component.text("Enchantment Book").color(NamedTextColor.LIGHT_PURPLE));
            components.add(Component.newline());
            
            if (itemStack.hasItemMeta()) {
                org.bukkit.inventory.meta.EnchantmentStorageMeta bookMeta = 
                    (org.bukkit.inventory.meta.EnchantmentStorageMeta) itemStack.getItemMeta();
                
                if (bookMeta.hasStoredEnchants()) {
                    components.add(Component.text("Stored Enchantments:").color(NamedTextColor.LIGHT_PURPLE));
                    components.add(Component.newline());
                    
                    bookMeta.getStoredEnchants().forEach((enchant, level) -> {
                        String enchantName = formatEnchantmentName(enchant.getKey().getKey());
                        String levelRoman = getRomanNumeral(level);
                        NamedTextColor enchantColor = getEnchantmentColor(enchant.getKey().getKey());
                        
                        components.add(Component.text("  " + enchantName + " " + levelRoman).color(enchantColor));
                        components.add(Component.newline());
                        
                        // Add enchantment description
                        String description = getEnchantmentDescription(enchant.getKey().getKey(), level);
                        if (!description.isEmpty()) {
                            components.add(Component.text("    " + description).color(NamedTextColor.DARK_GRAY));
                            components.add(Component.newline());
                        }
                    });
                }
            }
        } else {
            // Add regular enchantments for non-book items
            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants()) {
                components.add(Component.text("Enchantments:").color(NamedTextColor.LIGHT_PURPLE));
                components.add(Component.newline());
                
                itemStack.getItemMeta().getEnchants().forEach((enchant, level) -> {
                    String enchantName = formatEnchantmentName(enchant.getKey().getKey());
                    String levelRoman = getRomanNumeral(level);
                    NamedTextColor enchantColor = getEnchantmentColor(enchant.getKey().getKey());
                    
                    components.add(Component.text("  " + enchantName + " " + levelRoman).color(enchantColor));
                    components.add(Component.newline());
                    
                    // Add enchantment description
                    String description = getEnchantmentDescription(enchant.getKey().getKey(), level);
                    if (!description.isEmpty()) {
                        components.add(Component.text("    " + description).color(NamedTextColor.DARK_GRAY));
                        components.add(Component.newline());
                    }
                });
            }
        }
        
        // Add custom lore if any
        if (itemStack.hasItemMeta() && itemStack.getItemMeta().lore() != null && 
            !itemStack.getItemMeta().lore().isEmpty()) {
            components.add(Component.text("Description:").color(NamedTextColor.DARK_PURPLE));
            components.add(Component.newline());
            
            for (Component loreComponent : itemStack.getItemMeta().lore()) {
                components.add(Component.text("  ").append(loreComponent.color(NamedTextColor.DARK_PURPLE)));
                components.add(Component.newline());
            }
        }
        
        // Add additional item information
        addAdditionalItemInfo(itemStack, components);
        
        // Join all components together
        Component result = Component.empty();
        for (Component comp : components) {
            result = result.append(comp);
        }
        
        return result;
    }
    
    /**
     * Gets the rarity color for an item
     */
    private NamedTextColor getItemRarityColor(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            return NamedTextColor.AQUA; // Enchanted items
        }
        
        String materialName = item.getType().name();
        if (materialName.contains("DIAMOND")) return NamedTextColor.AQUA;
        if (materialName.contains("GOLD") || materialName.contains("GOLDEN")) return NamedTextColor.YELLOW;
        if (materialName.contains("IRON")) return NamedTextColor.WHITE;
        if (materialName.contains("STONE")) return NamedTextColor.GRAY;
        if (materialName.contains("WOOD") || materialName.contains("LEATHER")) return NamedTextColor.DARK_GRAY;
        if (materialName.equals("ENCHANTED_BOOK")) return NamedTextColor.LIGHT_PURPLE;
        
        return NamedTextColor.YELLOW; // Default
    }
    
    /**
     * Gets color based on durability percentage
     */
    private NamedTextColor getDurabilityColor(double percentage) {
        if (percentage >= 80) return NamedTextColor.GREEN;
        if (percentage >= 60) return NamedTextColor.YELLOW;
        if (percentage >= 40) return NamedTextColor.GOLD;
        if (percentage >= 20) return NamedTextColor.RED;
        return NamedTextColor.DARK_RED;
    }
    
    /**
     * Gets durability status text
     */
    private String getDurabilityStatus(double percentage) {
        if (percentage >= 90) return "Pristine";
        if (percentage >= 80) return "Excellent";
        if (percentage >= 60) return "Good";
        if (percentage >= 40) return "Worn";
        if (percentage >= 20) return "Damaged";
        if (percentage >= 10) return "Heavily Damaged";
        return "Nearly Broken";
    }
    
    /**
     * Formats enchantment names properly
     */
    private String formatEnchantmentName(String enchantKey) {
        String name = enchantKey.replace("minecraft:", "").replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase())
                     .append(word.substring(1).toLowerCase());
        }
        
        return formatted.toString();
    }
    
    /**
     * Formats material names properly
     */
    private String formatMaterialName(String materialName) {
        String name = materialName.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (formatted.length() > 0) formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase())
                     .append(word.substring(1).toLowerCase());
        }
        
        return formatted.toString();
    }
    
    /**
     * Converts numbers to Roman numerals
     */
    private String getRomanNumeral(int number) {
        if (number <= 0) return String.valueOf(number);
        
        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        if (number < romanNumerals.length) {
            return romanNumerals[number];
        }
        return String.valueOf(number);
    }
    
    /**
     * Gets color for different enchantment types
     */
    private NamedTextColor getEnchantmentColor(String enchantKey) {
        // Damage enchantments
        if (enchantKey.contains("sharpness") || enchantKey.contains("smite") || 
            enchantKey.contains("bane_of_arthropods") || enchantKey.contains("power")) {
            return NamedTextColor.RED;
        }
        
        // Protection enchantments
        if (enchantKey.contains("protection") || enchantKey.contains("blast_protection") ||
            enchantKey.contains("fire_protection") || enchantKey.contains("projectile_protection")) {
            return NamedTextColor.BLUE;
        }
        
        // Utility enchantments
        if (enchantKey.contains("efficiency") || enchantKey.contains("fortune") || 
            enchantKey.contains("silk_touch") || enchantKey.contains("looting")) {
            return NamedTextColor.GREEN;
        }
        
        // Durability enchantments
        if (enchantKey.contains("unbreaking") || enchantKey.contains("mending")) {
            return NamedTextColor.YELLOW;
        }
        
        return NamedTextColor.AQUA; // Default enchantment color
    }
    
    /**
     * Gets description for enchantments
     */
    private String getEnchantmentDescription(String enchantKey, int level) {
        switch (enchantKey.replace("minecraft:", "")) {
            case "sharpness":
                return "+" + (0.5 * level + 0.5) + " attack damage";
            case "smite":
                return "+" + (2.5 * level) + " damage vs undead";
            case "bane_of_arthropods":
                return "+" + (2.5 * level) + " damage vs arthropods";
            case "protection":
                return level * 4 + "% damage reduction";
            case "fire_protection":
                return level * 8 + "% fire damage reduction";
            case "blast_protection":
                return level * 8 + "% explosion damage reduction";
            case "projectile_protection":
                return level * 8 + "% projectile damage reduction";
            case "efficiency":
                return "+" + (level * level + 1) + " mining speed";
            case "fortune":
                return "Increases block drops";
            case "silk_touch":
                return "Blocks drop themselves";
            case "unbreaking":
                return (100 / (level + 1)) + "% durability usage";
            case "mending":
                return "Repairs with XP";
            case "looting":
                return "+" + level + " max mob drops";
            case "power":
                return "+" + (25 * level) + "% bow damage";
            case "punch":
                return "+" + (3 * level) + " blocks knockback";
            case "flame":
                return "Arrows set targets on fire";
            case "infinity":
                return "Infinite arrows (requires 1)";
            default:
                return "";
        }
    }
    
    /**
     * Adds additional item-specific information
     */
    private void addAdditionalItemInfo(ItemStack item, java.util.List<Component> components) {
        String materialName = item.getType().name();
        
        // Add potion information
        if (materialName.contains("POTION")) {
            components.add(Component.text("Potion Effects:").color(NamedTextColor.LIGHT_PURPLE));
            components.add(Component.newline());
            // Note: Full potion effect parsing would require more complex code
            components.add(Component.text("  Check item tooltip for effects").color(NamedTextColor.GRAY));
            components.add(Component.newline());
        }
        
        // Add food information
        if (item.getType().isEdible()) {
            components.add(Component.text("Food Item").color(NamedTextColor.GREEN));
            components.add(Component.newline());
        }
        
        // Add fuel information
        if (isFuel(materialName)) {
            components.add(Component.text("Can be used as fuel").color(NamedTextColor.GOLD));
            components.add(Component.newline());
        }
        
        // Add stackability info
        int maxStack = item.getMaxStackSize();
        if (maxStack == 1) {
            components.add(Component.text("Not stackable").color(NamedTextColor.GRAY));
        } else {
            components.add(Component.text("Max stack: " + maxStack).color(NamedTextColor.GRAY));
        }
        components.add(Component.newline());
    }
    
    /**
     * Checks if an item can be used as fuel
     */
    private boolean isFuel(String materialName) {
        return materialName.contains("COAL") || materialName.contains("WOOD") || 
               materialName.contains("LOG") || materialName.contains("PLANK") ||
               materialName.equals("LAVA_BUCKET") || materialName.equals("BLAZE_ROD");
    }
    
    /**
     * Gets the number of empty inventory slots for a player
     */
    private int getEmptyInventorySlots(Player player) {
        int emptySlots = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                emptySlots++;
            }
        }
        return emptySlots;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
}