package net.milkbowl.modules.economy.plugins;

import net.milkbowl.modules.economy.Economy;
import net.milkbowl.modules.economy.EconomyResponse;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import com.nijiko.coelho.iConomy.iConomy;
import com.nijiko.coelho.iConomy.system.Account;

public class Economy_iConomy4 implements Economy {
    private String name = "iConomy 4";
    private Plugin plugin = null;
    private PluginManager pluginManager = null;
    protected iConomy economy = null;
    private EconomyServerListener economyServerListener = null;

    public Economy_iConomy4(Plugin plugin) {
        this.plugin = plugin;
        this.pluginManager = this.plugin.getServer().getPluginManager();

        economyServerListener = new EconomyServerListener(this);

        this.pluginManager.registerEvent(Type.PLUGIN_ENABLE, economyServerListener, Priority.Monitor, plugin);
        this.pluginManager.registerEvent(Type.PLUGIN_DISABLE, economyServerListener, Priority.Monitor, plugin);

        // Load Plugin in case it was loaded before
        if (economy == null) {
            Plugin ec = plugin.getServer().getPluginManager().getPlugin("iConomy");
            if (ec != null && ec.isEnabled() && ec.getClass().getName().equals("com.nijiko.coelho.iConomy.iConomy.class")) {
                economy = (iConomy) ec;
                log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), name));
            }
        }
    }

    @Override
    public boolean isEnabled() {
        if (economy == null) {
            return false;
        } else {
            return economy.isEnabled();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String format(double amount) {
        return iConomy.getBank().format(amount);
    }

    public String getMoneyNamePlural() {
        return iConomy.getBank().getCurrency() + "s";
    }

    public String getMoneyNameSingular() {
        return iConomy.getBank().getCurrency();
    }

    @Override
    public EconomyResponse getBalance(String playerName) {
        double balance;
        EconomyResponse.ResponseType type;
        String errorMessage = null;

        balance = getAccountBalance(playerName);
        type = EconomyResponse.ResponseType.SUCCESS;

        return new EconomyResponse(balance, balance, type, errorMessage);
    }

    private double getAccountBalance(String playerName) {
        Account account = iConomy.getBank().getAccount(playerName);
        if (account == null) {
            iConomy.getBank().addAccount(playerName);
            account = iConomy.getBank().getAccount(playerName);
        }
        return account.getBalance();
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        double balance;
        EconomyResponse.ResponseType type;
        String errorMessage = null;

        if (amount < 0) {
            errorMessage = "Cannot withdraw negative funds";
            type = EconomyResponse.ResponseType.FAILURE;
            amount = 0;
            balance = getAccountBalance(playerName);

            return new EconomyResponse(amount, balance, type, errorMessage);
        }
        balance = getAccountBalance(playerName);
        if (balance >= amount) {
            Account account = iConomy.getBank().getAccount(playerName);
            if (account == null) {
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Could not find account");
            }
            account.subtract(amount);

            type = EconomyResponse.ResponseType.SUCCESS;
            balance = getAccountBalance(playerName);

            return new EconomyResponse(amount, balance, type, errorMessage);
        } else {
            errorMessage = "Error withdrawing funds";
            type = EconomyResponse.ResponseType.FAILURE;
            amount = 0;
            balance = getAccountBalance(playerName);

            return new EconomyResponse(amount, balance, type, errorMessage);
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        double balance;
        EconomyResponse.ResponseType type;
        String errorMessage = null;

        if (amount < 0) {
            errorMessage = "Cannot deposit negative funds";
            type = EconomyResponse.ResponseType.FAILURE;
            amount = 0;
            balance = getAccountBalance(playerName);

            return new EconomyResponse(amount, balance, type, errorMessage);
        }

        Account account = iConomy.getBank().getAccount(playerName);
        if (account == null) {
            iConomy.getBank().addAccount(playerName);
            account = iConomy.getBank().getAccount(playerName);
        }
        account.add(amount);
        balance = getAccountBalance(playerName);
        type = EconomyResponse.ResponseType.SUCCESS;

        return new EconomyResponse(amount, balance, type, errorMessage);
    }

    private class EconomyServerListener extends ServerListener {
        Economy_iConomy4 economy = null;

        public EconomyServerListener(Economy_iConomy4 economy) {
            this.economy = economy;
        }

        public void onPluginEnable(PluginEnableEvent event) {
            if (economy.economy == null) {
                Plugin iConomy = plugin.getServer().getPluginManager().getPlugin("iConomy");

                if (iConomy != null && iConomy.isEnabled() && iConomy.getClass().getName().equals("com.nijiko.coelho.iConomy.iConomy")) {
                    economy.economy = (iConomy) iConomy;
                    log.info(String.format("[%s][Economy] %s hooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }

        public void onPluginDisable(PluginDisableEvent event) {
            if (economy.economy != null) {
                if (event.getPlugin().getDescription().getName().equals("iConomy")) {
                    economy.economy = null;
                    log.info(String.format("[%s][Economy] %s unhooked.", plugin.getDescription().getName(), economy.name));
                }
            }
        }
    }
}