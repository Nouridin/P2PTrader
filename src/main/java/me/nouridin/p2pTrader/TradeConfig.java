package me.nouridin.p2pTrader;

/**
 * Configuration constants for P2PTrader
 * 
 * P2PTrader - Professional P2P Trading System
 * Developed by: Nouridin
 * Website: https://github.com/Nouridin
 */
public class TradeConfig {
    
    // Plugin Information
    public static final String PLUGIN_NAME = "P2PTrader";
    public static final String PLUGIN_VERSION = "1.0.0";
    public static final String PLUGIN_AUTHOR = "Nouridin";
    public static final String PLUGIN_WEBSITE = "https://github.com/Nouridin";
    public static final String PLUGIN_PREFIX = "§6[§bP2PTrader§6] §r";
    
    // Maximum distance allowed for trading (in blocks)
    public static final double MAX_TRADE_DISTANCE = 10.0;
    
    // GUI Configuration (Legacy - kept for compatibility)
    public static final int TRADE_GUI_SIZE = 54;
    public static final int DIVIDER_COLUMN = 4;
    public static final int LEFT_CONFIRM_SLOT = 45;
    public static final int LEFT_STATUS_SLOT = 46;
    public static final int RIGHT_STATUS_SLOT = 52;
    public static final int RIGHT_CONFIRM_SLOT = 53;
    public static final int CONFIRMATION_ROW_START = 45;
    public static final int CONFIRMATION_ROW_END = 53;
    public static final int TRADE_AREA_END = 44;
    public static final int PLAYER_INVENTORY_START = 54;
    
    // Glass pane slots between confirm buttons
    public static final int GLASS_PANE_START = 47;
    public static final int GLASS_PANE_END = 51;
    
    // Trade area columns
    public static final int LEFT_SIDE_MAX_COLUMN = 3;
    public static final int RIGHT_SIDE_MIN_COLUMN = 5;
    
    // Permissions
    public static final String TRADE_PERMISSION = "P2PTrader.trade";
    public static final String ADMIN_PERMISSION = "P2Ptrader.admin";
    
    // Messages
    public static final String NO_PERMISSION = PLUGIN_PREFIX + "§cYou don't have permission to use this command!";
    public static final String PLUGIN_RELOAD = PLUGIN_PREFIX + "§aP2PTrader reloaded successfully!";
    public static final String TRADE_SUCCESS = PLUGIN_PREFIX + "§aTrade completed successfully!";
    
    private TradeConfig() {
        // Utility class - prevent instantiation
        // P2PTrader v1.0.0 by Nouridin
    }
}