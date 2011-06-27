package net.milkbowl.vault.modules.economy.plugins;

import net.milkbowl.vault.modules.economy.Economy;
import net.milkbowl.vault.modules.economy.EconomyResponse;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.api.NoLoanPermittedException;
import com.earth2me.essentials.api.UserDoesNotExistException;

public class Economy_Essentials implements Economy {
    private String name = "Essentials Economy";
    private Plugin plugin = null;
    private PluginManager pluginManager = null;
    private Essentials ess = null;
    private EconomyServerListener economyServerListener = null;
    
    public Economy_Essentials(Plugin plugin) {
        this.plugin = plugin;
        pluginManager = this.plugin.getServer().getPluginManager();

        economyServerListener = new EconomyServerListener(this);
        
        this.pluginManager.registerEvent(Type.PLUGIN_ENABLE, economyServerListener, Priority.Monitor, plugin);
        this.pluginManager.registerEvent(Type.PLUGIN_DISABLE, economyServerListener, Priority.Monitor, plugin);
        
        // Load Plugin in case it was loaded before
        if (ess == null) {
            Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials != null && essentials.isEnabled()) {
                ess = (Essentials) essentials;
                log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), name));
            }
        }
    }    
    
    @Override
    public boolean isEnabled() {
        if(ess == null) {
            return false;
        } else {
            return ess.isEnabled();
        }
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public EconomyResponse getBalance(String playerName) {
        double balance;
        EconomyResponse.ResponseType type;
        String errorMessage = null;
        
        try {
            balance = com.earth2me.essentials.api.Economy.getMoney(playerName);
            type = EconomyResponse.ResponseType.SUCCESS;
        } catch (UserDoesNotExistException e) {
            if(createPlayerAccount(playerName)) {
                balance = 0;
                type = EconomyResponse.ResponseType.SUCCESS;
            } else {
                balance = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "User does not exist";
            }
        }
        
        return new EconomyResponse(balance, balance, type, errorMessage);
    }
    
    private boolean createPlayerAccount(String playerName) {
        try {
            com.earth2me.essentials.api.Economy.add(playerName, 0);
            return true;
        } catch (UserDoesNotExistException e1) {
            return false;
        } catch (NoLoanPermittedException e1) {
            return false;
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        double balance;
        EconomyResponse.ResponseType type;
        String errorMessage = null;
        
        try {
            com.earth2me.essentials.api.Economy.subtract(playerName, amount);
            balance = com.earth2me.essentials.api.Economy.getMoney(playerName);
            type = EconomyResponse.ResponseType.SUCCESS;
        } catch (UserDoesNotExistException e) {
            if (createPlayerAccount(playerName)) {
                return withdrawPlayer(playerName, amount);
            } else {
                amount = 0;
                balance = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "User does not exist";
            }
        } catch (NoLoanPermittedException e) {
            try {
                balance = com.earth2me.essentials.api.Economy.getMoney(playerName);
                amount = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "Loan was not permitted";
            } catch (UserDoesNotExistException e1) {
                amount = 0;
                balance = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "User does not exist";
            }
        }
        
        return new EconomyResponse(amount, balance, type, errorMessage);
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        double balance;
        EconomyResponse.ResponseType type;
        String errorMessage = null;
        
        try {
            com.earth2me.essentials.api.Economy.add(playerName, amount);
            balance = com.earth2me.essentials.api.Economy.getMoney(playerName);
            type = EconomyResponse.ResponseType.SUCCESS;
        } catch (UserDoesNotExistException e) {
            if(createPlayerAccount(playerName)) {
                return depositPlayer(playerName, amount);
            } else {
                amount = 0;
                balance = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "User does not exist";
            }
        } catch (NoLoanPermittedException e) {
            try {
                balance = com.earth2me.essentials.api.Economy.getMoney(playerName);
                amount = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "Loan was not permitted";
            } catch (UserDoesNotExistException e1) {
                balance = 0;
                amount = 0;
                type = EconomyResponse.ResponseType.FAILURE;
                errorMessage = "Loan was not permitted";
            }
        }
        
        return new EconomyResponse(amount, balance, type, errorMessage);
    }
    
    private class EconomyServerListener extends ServerListener {
        Economy_Essentials economy = null;
        
        public EconomyServerListener(Economy_Essentials economy) {
            this.economy = economy;
        }
        
        public void onPluginEnable(PluginEnableEvent event) {
            if (economy.ess == null) {
                Plugin essentials = plugin.getServer().getPluginManager().getPlugin("Essentials");

                if (essentials != null && essentials.isEnabled()) {
                    economy.ess = (Essentials) essentials;
                    log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }
        
        public void onPluginDisable(PluginDisableEvent event) {
            if (economy.ess != null) {
                if (event.getPlugin().getDescription().getName().equals("Essentials")) {
                    economy.ess = null;
                    log.info(String.format("[%s][Economy] %s unhooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }
    }

    @Override
    public String format(double amount) {
        return com.earth2me.essentials.api.Economy.format(amount);
    }
}