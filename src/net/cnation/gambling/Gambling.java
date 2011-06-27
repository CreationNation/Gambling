package net.cnation.gambling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.Permissions;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

import net.milkbowl.modules.economy.EconomyManager;
import net.milkbowl.modules.permission.PermissionManager;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Gambling extends JavaPlugin {
    PropertiesReader properties = new PropertiesReader("plugins/Gambling/settings.properties");

    public HashMap<String, String> editing = new HashMap<String, String>();
    HashMap<String, String> placing = new HashMap<String, String>();
    HashMap<String, Hand> hands = new HashMap<String, Hand>();
    HashMap<String, Deck> decks = new HashMap<String, Deck>();
    HashMap<String, HashMap<String, Integer>> bets = new HashMap<String, HashMap<String, Integer>>();

    SecureRandom r = new SecureRandom();

    GamblingBlockListener blockListener = new GamblingBlockListener(this);
    GamblingPlayerListener playerListener = new GamblingPlayerListener(this);
    boolean isSQLEnabled = properties.readBoolean("sqlEnabled", true);

    // Managers
    private EconomyManager econManager = null;
    private PermissionManager permManager = null;
    private SqlManager sqlManager = null;

    // Logging
    private final Logger log = Logger.getLogger("Minecraft");

    public void onDisable() {
        log.info(String.format("[%s] %s", this.getDescription().getName(), "Disabled"));
    }

    public void onEnable() {
        log.info(String.format("[%s] %s", this.getDescription().getName(), "Enabled"));
        System.out.println("[" + this.getDescription().getName() + " " + this.getDescription().getVersion() + "] Enabled");
        if (isSQLEnabled) {
            try {
                Connection c = getDatabaseConnection();
                PreparedStatement p = c.prepareStatement("CREATE TABLE IF NOT EXISTS `gamesigns` (`game` TEXT NOT NULL ,`location` TEXT NOT NULL) ENGINE = MYISAM");
                p.executeUpdate();
                p.clearBatch();
                p = c.prepareStatement("CREATE TABLE IF NOT EXISTS `gamerules` (`game` TEXT NOT NULL, `min` TEXT NOT NULL, `multiplier` TEXT NOT NULL) ENGINE = MYISAM");
                p.executeUpdate();
                p.clearBatch();

                p = c.prepareStatement("CREATE TABLE IF NOT EXISTS `games` (`game` TEXT NOT NULL, `owner` TEXT NOT NULL) ENGINE = MYISAM");

                p.executeUpdate();
                p.close();
                c.close();
            } catch (SQLException e) {
                System.out.println("[Gambling] Failed to create databases! Plugin will break if casino sign privlidges are given.");
                e.printStackTrace();
            }
        }

        // Register Calls
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.High, this);

        // Register Managers
        setEconManager(new EconomyManager(this));
        if (!getEconManager().load()) {
            // No valid economies, display error message and disables
            log.warning(String.format("[%s] %s", this.getDescription().getName(), "Economy not found!"));
            getPluginLoader().disablePlugin(this);
        }

        setPermManager(new PermissionManager(this));
        if (!getPermManager().load()) {
            // no valid permissions, display error message and disables
            log.warning(String.format("[%s] %s", this.getDescription().getName(), "Permissions not found!"));
            getPluginLoader().disablePlugin(this);
        }

        setSqlManager(new SqlManager(this));
    }

    public void setEconManager(EconomyManager econManager) {
        this.econManager = econManager;
    }

    public EconomyManager getEconManager() {
        return econManager;
    }

    public void setPermManager(PermissionManager permManager) {
        this.permManager = permManager;
    }

    public PermissionManager getPermManager() {
        return permManager;
    }

    public void setSqlManager(SqlManager sqlManager) {
        this.sqlManager = sqlManager;
    }

    public SqlManager getSqlManager() {
        return sqlManager;
    }

    public static Connection getDatabaseConnection() {
        PropertiesReader p = new PropertiesReader("mysql.properties");
        String db = p.readString("db");
        String user = p.readString("user");
        String driver = p.readString("driver");
        String pass = p.readString("pass");
        try {
            Class.forName(driver).newInstance();
            return DriverManager.getConnection(db, user, pass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] split) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            return false;
        }

        String name = command.getName().toLowerCase();
        if (name.equalsIgnoreCase("game") && permManager.hasPermission(player, "gambling.casinosign")) {
            if (isSQLEnabled) {
                if (split.length > 0) {
                    String cmd = split[0];
                    if (cmd.equalsIgnoreCase("create")) {
                        if (split.length >= 2) {
                            String n = "";
                            for (int i = 1; i < split.length; i++) {
                                n += split[i] + " ";

                            }
                            n = n.trim();
                            if (n.length() <= 13) {
                                if (!sqlManager.doesGameExist(n)) {
                                    int max = properties.readInt("maxGames", 0);
                                    if (max == 0 || sqlManager.countPlayerGames(player.getName()) < max) {
                                        sqlManager.addGame(player, n);
                                        player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.WHITE + n + ChatColor.GREEN + " has been created.");
                                    } else {
                                        player.sendMessage(ChatColor.RED + "You already own the maximum number of games");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "Game already exists.");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "Name is too long.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Missing arguments.");
                        }
                        return true;
                    } else if (cmd.equalsIgnoreCase("edit")) {
                        if (split.length > 0) {
                            String n = "";
                            for (int i = 1; i < split.length; i++) {
                                n += split[i] + " ";

                            }
                            n = n.trim();
                            if (sqlManager.doesGameExist(n)) {
                                if (sqlManager.getGameOwner(n).equalsIgnoreCase(player.getName())) {
                                    if (editing.get(player.getName()) == null) {
                                        n = sqlManager.getCaseCorrectGameName(n);
                                        editing.put(player.getName(), n);
                                        player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "You are now editing " + ChatColor.WHITE + n + ChatColor.GREEN + ".");
                                    } else {
                                        player.sendMessage(ChatColor.RED + "You are already editing " + ChatColor.WHITE + editing.get(player.getName()));
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "You do not own this game.");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "Invalid game.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Invalid arguments.");
                        }
                        return true;
                    } else if (cmd.equalsIgnoreCase("delete")) {
                        String g = editing.get(player.getName());
                        if (g != null) {
                            sqlManager.removeGame(g);
                            sqlManager.removeGameRules(g);
                            player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.WHITE + g + ChatColor.GREEN + " has been deleted.");
                            editing.remove(player.getName());
                            ArrayList<Location> s = sqlManager.getGameSigns(g);
                            if (s.size() != 0) {
                                for (int i = 0; i < s.size(); i++) {
                                    Location l = s.get(i);
                                    Block b = l.getWorld().getBlockAt((int) (l.getX()), (int) (l.getY()), (int) (l.getZ()));
                                    b.setType(Material.AIR);
                                    Block b2 = l.getWorld().getBlockAt((int) (l.getX()), (int) (l.getY()) - 1, (int) (l.getZ()));
                                    b2.setType(Material.AIR);
                                    sqlManager.removeGameSign(l);
                                }
                                player.getInventory().addItem(new ItemStack(Material.SIGN, (s.size()) * 2));
                                player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.WHITE + s.size() + ChatColor.GREEN + " casino signs removed.");

                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You are not editing a game.");

                        }
                        return true;
                    } else if (cmd.equalsIgnoreCase("count")) {
                        String g = editing.get(player.getName());
                        if (g != null) {
                            player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "You have placed " + ChatColor.WHITE + sqlManager.countGameSigns(g) + ChatColor.GREEN + " casino signs.");
                        } else {
                            player.sendMessage(ChatColor.RED + "You are not editing a game.");
                        }
                        return true;
                    } else if (cmd.equalsIgnoreCase("help")) {
                        player.sendMessage(ChatColor.BLUE + "- - - G a m e  C o m m a n d s - - -");
                        player.sendMessage(ChatColor.YELLOW + "     [] = Required, <> = Optional");
                        player.sendMessage(ChatColor.GREEN + "/game create [Name]" + ChatColor.WHITE + " - Creates a new game.");
                        player.sendMessage(ChatColor.GREEN + "/game edit [Name]" + ChatColor.WHITE + " - Sets the game to edit.");
                        player.sendMessage(ChatColor.GREEN + "/game rule add [Minimum] [Multipler]" + ChatColor.WHITE + " - Adds a game rule (e.g. 50+ pays x2).");
                        player.sendMessage(ChatColor.GREEN + "/game rule remove [Minimum]" + ChatColor.WHITE + " - Removes a specific rule.");
                        player.sendMessage(ChatColor.GREEN + "/game rule list" + ChatColor.WHITE + " - Shows all rules");
                        player.sendMessage(ChatColor.GREEN + "/game place [Bet]" + ChatColor.WHITE + " - Places a game sign automatically.");
                        player.sendMessage(ChatColor.GREEN + "/game cancel" + ChatColor.WHITE + " - Stop placing a game sign.");
                        player.sendMessage(ChatColor.GREEN + "/game close" + ChatColor.WHITE + " - Stop editing the current game.");
                        player.sendMessage(ChatColor.GREEN + "/game count" + ChatColor.WHITE + " - Displays number of game signs placed.");
                        player.sendMessage(ChatColor.GREEN + "/game delete" + ChatColor.WHITE + " - Deletes a game.");
                        return true;
                    } else if (cmd.equalsIgnoreCase("rule")) {
                        String g = editing.get(player.getName());
                        if (g != null) {
                            g = sqlManager.getCaseCorrectGameName(g);

                            if (split.length >= 3) {
                                if (split[1].equalsIgnoreCase("add")) {
                                    if (split.length >= 4) {
                                        int min = -1;
                                        int mult = -1;
                                        try {
                                            min = Integer.parseInt(split[2]);
                                            mult = Integer.parseInt(split[3]);
                                        } catch (NumberFormatException e) {
                                        }
                                        if (min > 0 && mult >= 0 && min <= 100) {
                                            if (sqlManager.countGameRules(g) < 3) {
                                                if (!sqlManager.doesGameRuleExist(g, min)) {
                                                    sqlManager.addGameRule(g, min, mult);
                                                    player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "Rule added to " + ChatColor.WHITE + g + ChatColor.GREEN + ": " + ChatColor.WHITE + min + "+ " + ChatColor.GREEN + "pays " + ChatColor.WHITE + "x" + mult);
                                                } else {
                                                    player.sendMessage(ChatColor.RED + "Rule already exists.");
                                                }
                                            } else {
                                                player.sendMessage(ChatColor.RED + "Game already has maximum amount of rules.");

                                            }
                                        } else {
                                            player.sendMessage(ChatColor.RED + "Invalid numbers.");

                                        }
                                    }

                                    return true;
                                } else if (split[1].equalsIgnoreCase("remove")) {
                                    int min = 0;
                                    int mult = 0;
                                    try {
                                        min = Integer.parseInt(split[2]);
                                    } catch (NumberFormatException e) {
                                    }
                                    if (min != 0) {
                                        sqlManager.removeGameRule(g, min);
                                        player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "Rule removed from " + g + ChatColor.GREEN + ": " + ChatColor.WHITE + min + "+ ");
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Invalid numbers.");

                                    }
                                    return true;

                                }
                            }
                            if (split[1].equalsIgnoreCase("list")) {
                                HashMap<Integer, Integer> rules = sqlManager.getGameRules(g);
                                Iterator<Integer> iter = rules.keySet().iterator();
                                int[] values = new int[rules.size()];
                                int i = 0;
                                while (iter.hasNext()) {
                                    int next = iter.next();
                                    values[i] = next;
                                    i++;
                                }

                                Arrays.sort(values);
                                player.sendMessage(ChatColor.BLUE + "- - - Rules List - - -");
                                for (i = 0; i < values.length; i++) {
                                    int output = rules.get(values[i]);

                                    if (i + 1 < values.length) {
                                        player.sendMessage(ChatColor.BLUE + "[" + (i + 1) + "] " + ChatColor.GREEN + "Range: " + ChatColor.WHITE + values[i] + "-" + (values[i + 1] - 1) + ChatColor.GREEN + ", Multiplier: " + ChatColor.WHITE + output);
                                    } else {
                                        if (values[i] != 100) {
                                            player.sendMessage(ChatColor.BLUE + "[" + (i + 1) + "] " + ChatColor.GREEN + "Range: " + ChatColor.WHITE + values[i] + "+" + ChatColor.GREEN + ", Multiplier: " + ChatColor.WHITE + output);
                                        } else {
                                            player.sendMessage(ChatColor.BLUE + "[" + (i + 1) + "] " + ChatColor.GREEN + "Range: " + ChatColor.WHITE + values[i] + ChatColor.GREEN + ", Multiplier: " + ChatColor.WHITE + output);

                                        }
                                    }
                                }
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You are not editing a game.");
                            return true;
                        }
                    } else if (cmd.equalsIgnoreCase("close")) {
                        String g = editing.get(player.getName());
                        if (g != null) {
                            String gs = sqlManager.getCaseCorrectGameName(g);
                            g = gs;

                            editing.remove(player.getName());
                            player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "You are no longer editing " + ChatColor.WHITE + g);

                        } else {
                            player.sendMessage(ChatColor.RED + "You are not editing a game.");
                        }
                        return true;
                    } else if (cmd.equalsIgnoreCase("place")) {
                        if (split.length == 2) {
                            String g = editing.get(player.getName());
                            if (g != null) {
                                String gs = sqlManager.getCaseCorrectGameName(g);
                                g = gs;
                                if (sqlManager.countGameRules(g) != 0) {
                                    int b = 0;
                                    try {
                                        b = Integer.parseInt(split[1]);
                                    } catch (NumberFormatException e) {

                                    }
                                    if (b != 0) {
                                        int max = properties.readInt("maxSigns", 0);
                                        if (max == 0 || sqlManager.countGameSigns(g) < max) {
                                            placing.put(player.getName(), g + ":" + b);
                                            player.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "Right-click a block to place a sign for " + ChatColor.WHITE + g + ChatColor.GREEN + ".");
                                        } else {
                                            player.sendMessage(ChatColor.RED + "You cannot place any more casino signs.");
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED + "Invalid bet.");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "This game has no rules.");
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "You are not editing a game.");
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Invalid arguments.");
                        }
                        return true;
                    } else if (cmd.equalsIgnoreCase("cancel")) {
                        if (placing.get(player.getName()) != null) {
                            placing.remove(player.getName());
                            player.sendMessage(ChatColor.BLUE + "[Gambling] You are no longer attempting to place a sign");

                        } else {
                            player.sendMessage(ChatColor.RED + "You are not attempting to place a sign.");
                        }
                        return true;

                    }
                } else {
                    ArrayList<String> games = sqlManager.getGamesOfPlayer(player.getName());
                    if (games.size() != 0) {
                        player.sendMessage(ChatColor.BLUE + "- - - Games List - - -");
                        for (int i = 0; i < games.size(); i++) {
                            player.sendMessage(ChatColor.BLUE + "[" + (i + 1) + "] " + ChatColor.GREEN + games.get(i));
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You own no games.");
                    }
                    return true;
                }
            } else {
                player.sendMessage(ChatColor.RED + "Invalid command.");
                return true;
            }
        } else if (name.equalsIgnoreCase("hand")) {
            if (split.length > 0) {
                if (split[0].equalsIgnoreCase("fold")) {
                    if (hands.get(player.getName()) != null) {
                        hands.get(player.getName()).clearHand();
                        sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has folded his hand.");
                        hands.remove(player.getName());

                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");
                    }
                    return true;

                } else if (split[0].equalsIgnoreCase("reveal")) {
                    if (hands.get(player.getName()) != null) {
                        ArrayList<Card> c = hands.get(player.getName()).getCards();
                        if (c.size() != 0) {
                            sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has revealed his hand.");

                            for (int i = 0; i < c.size(); i++) {
                                sendLocalMessage(player, ChatColor.GREEN + "[" + (i + 1) + "] " + ChatColor.WHITE + "The " + c.get(i).getValue() + " of " + c.get(i).getSuit());
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You do not have a hand.");

                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");

                    }
                    return true;
                } else if (split[0].equalsIgnoreCase("show")) {
                    if (hands.get(player.getName()) != null) {
                        sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + player.getName() + " has shown his hand.");
                        ArrayList<Card> c = hands.get(player.getName()).getCards();
                        if (c.size() != 0) {
                            for (int i = 0; i < c.size(); i++) {
                                sendLocalMessage(player, ChatColor.GREEN + "[" + (i + 1) + "] " + ChatColor.WHITE + "The " + c.get(i).getValue() + " of " + c.get(i).getSuit());
                            }
                            hands.remove(player.getName());
                            return true;
                        } else {
                            player.sendMessage(ChatColor.RED + "You do not have a hand.");
                            return true;
                        }

                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");
                        return true;

                    }
                } else if (split[0].equalsIgnoreCase("pot")) {
                    if (hands.get(player.getName()) != null) {
                        String owner = hands.get(player.getName()).getDeckOwner();
                        if (bets.get(owner) != null) {
                            Iterator<String> iter = bets.get(owner).keySet().iterator();
                            int t = 0;
                            while (iter.hasNext()) {
                                String next = iter.next();
                                t += bets.get(owner).get(next);
                            }
                            if (t != 0) {
                                player.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "The pot contains " + ChatColor.WHITE + t + " Bux.");

                            } else {
                                player.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "The pot is empty.");
                            }
                            return true;
                        } else {
                            player.sendMessage(ChatColor.RED + "Pot has already been paid.");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");
                        return true;
                    }

                } else if (split[0].equalsIgnoreCase("bet")) {
                    if (split.length == 2) {
                        if (hands.get(player.getName()) != null) {
                            if (bets.get(hands.get(player.getName()).getDeckOwner()) != null) {
                                int b = 0;
                                try {
                                    b = Integer.parseInt(split[1]);
                                } catch (NumberFormatException e) {
                                }
                                if (b != 0) {
                                    HashMap<String, Integer> bt = bets.get(hands.get(player.getName()).getDeckOwner());
                                    if (bt != null) {
                                        if (econManager.getBalance(player.getName()).amount >= b) {
                                            int bet = 0;
                                            if (bt.get(player.getName()) != null) {
                                                bet = bt.get(player.getName());
                                                bt.remove(player.getName());
                                            }
                                            bet += b;

                                            bt.put(player.getName(), bet);
                                            econManager.withdrawPlayer(player.getName(), b);
                                            sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has bet " + ChatColor.WHITE + econManager.format(bet));
                                            return true;
                                        } else {
                                            player.sendMessage(ChatColor.RED + "You cannot afford this bet.");
                                            return true;

                                        }
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "Pot has already been paid.");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You do not have a hand.");
                            return true;

                        }
                    } else {
                        if (hands.get(player.getName()) != null) {
                            if (bets.get(hands.get(player.getName()).getDeckOwner()) != null) {
                                Integer t = bets.get(hands.get(player.getName()).getDeckOwner()).get(player.getName());
                                if (t != null) {
                                    player.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "Your total bet is " + ChatColor.WHITE + econManager.format(t));
                                } else {
                                    player.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "Your total bet is " + ChatColor.WHITE + econManager.format(0));

                                }
                                return true;
                            } else {
                                player.sendMessage(ChatColor.RED + "Pot has already been paid.");
                                return true;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "You do not have a hand.");
                            return true;
                        }
                    }
                } else if (split[0].equalsIgnoreCase("call")) {
                    if (hands.get(player.getName()) != null) {
                        String owner = hands.get(player.getName()).getDeckOwner();
                        if (bets.get(owner) != null) {
                            int toCall = getHighestPlayerBet(owner);
                            if (toCall != 0) {
                                int bal = 0;
                                if (bets.get(owner).get(player.getName()) != null) {
                                    bal = bets.get(owner).get(player.getName());
                                }
                                if (bal < toCall) {
                                    if (econManager.getBalance(player.getName()).amount >= toCall) {
                                        econManager.withdrawPlayer(player.getName(), toCall);
                                        bets.get(owner).remove(player.getName());
                                        bets.get(owner).put(player.getName(), toCall);
                                        sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has called.");
                                        return true;
                                    } else {
                                        player.sendMessage(ChatColor.RED + "You cannot afford this bet.");
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + "You have already called.");

                                }
                            } else {
                                player.sendMessage(ChatColor.RED + "There is no pot.");

                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Pot has already been paid.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");

                    }
                    return true;

                } else if (split[0].equalsIgnoreCase("toss")) {
                    if (hands.get(player.getName()) != null) {
                        if (split.length == 2) {
                            int d = -1;
                            try {
                                d = Integer.parseInt(split[1]);
                            } catch (NumberFormatException e) {
                            }
                            if (d != -1 && d < hands.get(player.getName()).getCards().size()) {
                                Card c = hands.get(player.getName()).removeCard(d - 1);
                                player.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "You tossed the " + ChatColor.WHITE + c.getValue() + " of " + c.getSuit() + ChatColor.GREEN + ".");
                                sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has tossed a card.");

                            } else {
                                player.sendMessage(ChatColor.RED + "Invalid card number.");
                            }
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");
                    }
                    return true;

                } else if (split[0].equalsIgnoreCase("release")) {
                    if (hands.get(player.getName()) != null) {
                        hands.remove(player.getName());
                        sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has released his hand.");
                        return true;

                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");
                        return true;

                    }
                } else if (split[0].equalsIgnoreCase("help")) {
                    player.sendMessage(ChatColor.BLUE + "- - - H a n d  C o m m a n d s - - -");
                    player.sendMessage(ChatColor.YELLOW + "     [] = Required, <> = Optional");

                    player.sendMessage(ChatColor.GREEN + "/hand" + ChatColor.WHITE + " - Displays your hand.");
                    player.sendMessage(ChatColor.GREEN + "/hand show" + ChatColor.WHITE + " - Displays your hand to players around you, then clears it.");
                    player.sendMessage(ChatColor.GREEN + "/hand fold" + ChatColor.WHITE + " - Removes your hand without showing it.");
                    player.sendMessage(ChatColor.GREEN + "/hand toss [Number]" + ChatColor.WHITE + " - Removes a specific card from your hand.");
                    player.sendMessage(ChatColor.GREEN + "/hand reveal" + ChatColor.WHITE + " - Displays your hand without clearing it.");
                    player.sendMessage(ChatColor.GREEN + "- - - - - - - - - - - - - - - - - - - - - - - - - - ");
                    player.sendMessage(ChatColor.GREEN + "/hand pot" + ChatColor.WHITE + " - Displays amount of money in the pot");
                    player.sendMessage(ChatColor.GREEN + "/hand call" + ChatColor.WHITE + " - Calls to the highest bet.");
                    player.sendMessage(ChatColor.GREEN + "/hand bet" + ChatColor.WHITE + " - Displays how much money you have bet.");
                    player.sendMessage(ChatColor.GREEN + "/hand bet [Amount]" + ChatColor.WHITE + " - Bets an amount of money.");

                    return true;
                }
            } else {
                if (hands.get(player.getName()) != null) {
                    ArrayList<Card> c = hands.get(player.getName()).getCards();
                    if (c.size() != 0) {
                        player.sendMessage(ChatColor.BLUE + "- - - Your Hand - - -");

                        for (int i = 0; i < c.size(); i++) {
                            player.sendMessage(ChatColor.GREEN + "[" + (i + 1) + "] " + ChatColor.WHITE + "The " + c.get(i).getValue() + " of " + c.get(i).getSuit());
                        }
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have a hand.");

                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have a hand.");

                }
                return true;
            }

        } else if (name.equalsIgnoreCase("deck") && permManager.hasPermission(player, "gambling.deck")) {
            if (decks.get(player.getName()) == null) {
                decks.put(player.getName(), new Deck());
            }
            if (split.length == 1 && split[0].equalsIgnoreCase("shuffle")) {
                int t = 0;

                if (bets.get(player.getName()) != null) {
                    Iterator<String> iters = bets.get(player.getName()).keySet().iterator();
                    while (iters.hasNext()) {
                        String next = iters.next();
                        t += bets.get(player.getName()).get(next);
                    }
                }
                if (bets.get(player.getName()) == null || t == 0) {

                    decks.get(player.getName()).reloadDeck();
                    sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has shuffled the deck.");
                    Iterator<String> iter = hands.keySet().iterator();
                    int[] values = new int[hands.size()];
                    int i = 0;
                    ArrayList<String> toRemove = new ArrayList<String>();
                    while (iter.hasNext()) {
                        String next = iter.next();
                        if (hands.get(next).getDeckOwner().equalsIgnoreCase(player.getName())) {
                            toRemove.add(next);
                            i++;
                        }

                    }
                    for (int j = 0; j < toRemove.size(); j++) {
                        hands.remove(toRemove.get(j));
                    }
                    bets.put(player.getName(), new HashMap<String, Integer>());
                    if (i != 0) {
                        sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.WHITE + i + ChatColor.GREEN + " hands cleared.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot shuffle the deck when there is a pot!");
                }

                return true;
            } else if (split.length >= 2 && split[0].equalsIgnoreCase("pay")) {
                Player tg = this.getServer().getPlayer(split[1]);
                if (bets.get(player.getName()) != null) {
                    Iterator<String> iter = bets.get(player.getName()).keySet().iterator();
                    int t = 0;
                    while (iter.hasNext()) {
                        String next = iter.next();
                        t += bets.get(player.getName()).get(next);
                    }
                    if (tg != null && tg.isOnline()) {
                        if (t != 0) {
                            int payout = (int) (t * .95);
                            player.sendMessage("" + payout);
                            econManager.depositPlayer(tg.getName(), payout);
                            bets.remove(player.getName());
                            sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.WHITE + tg.getName() + ChatColor.GREEN + " has won " + ChatColor.WHITE + econManager.format(payout));
                            int dpayout = (int) (t * .05);
                            econManager.depositPlayer(player.getName(), dpayout);
                            player.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "You have been paid " + ChatColor.WHITE + econManager.format(dpayout) + ChatColor.GREEN + "for dealing.");

                            return true;

                        } else {
                            player.sendMessage(ChatColor.RED + "The pot is empty");
                            return true;
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Player not found.");
                        return true;

                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Pot has already been paid.");
                    return true;
                }
            } else if (split.length >= 1 && split[0].equalsIgnoreCase("add")) {
                int a = 1;
                if (split.length == 2) {
                    try {
                        a = Integer.parseInt(split[1]);
                    } catch (NumberFormatException e) {

                    }
                }

                if (a <= 5) {
                    for (int i = 0; i < a; i++) {
                        decks.get(player.getName()).addDeck();
                    }
                    sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has added " + a + " decks.");
                } else {
                    player.sendMessage(ChatColor.RED + "You cannot add more than five decks at once.");
                }
                return true;

            } else if (split.length == 1 && split[0].equalsIgnoreCase("clear")) {
                Iterator<String> iter = hands.keySet().iterator();
                int[] values = new int[hands.size()];
                int i = 0;
                ArrayList<String> toRemove = new ArrayList<String>();
                while (iter.hasNext()) {
                    String next = iter.next();
                    if (hands.get(next).getDeckOwner().equalsIgnoreCase(player.getName())) {
                        toRemove.add(next);
                        i++;
                    }

                }
                for (int j = 0; j < toRemove.size(); j++) {
                    hands.remove(toRemove.get(j));
                }
                if (i != 0) {
                    sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.WHITE + i + ChatColor.GREEN + " hands cleared.");
                } else {
                    player.sendMessage(ChatColor.RED + "No hands to clear.");
                }
                return true;

            } else if (split.length == 1 && split[0].equalsIgnoreCase("burn")) {

                String d = decks.get(player.getName()).drawCard();
                sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " has burnt a card.");
                return true;

            } else if (split.length == 1 && split[0].equalsIgnoreCase("draw")) {

                String d = decks.get(player.getName()).drawCard();
                if (d != null) {
                    sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + " drew: " + ChatColor.WHITE + d);
                } else {
                    sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + "'s deck is empty.");
                }
                return true;
            } else if (split.length >= 2 && split[0].equalsIgnoreCase("deal")) {

                Player tg = this.getServer().getPlayer(split[1]);
                if (tg != null && tg.isOnline()) {
                    if (hands.get(tg.getName()) == null) {
                        hands.put(tg.getName(), new Hand(player.getName(), new ArrayList<Card>()));
                    }
                    if (hands.get(tg.getName()).getDeckOwner().equalsIgnoreCase(player.getName())) {
                        int a = 1;

                        if (split.length == 3) {
                            try {
                                a = Integer.parseInt(split[2]);
                            } catch (NumberFormatException e) {
                            }
                        }

                        for (int i = 0; i < a; i++) {
                            Card d = decks.get(player.getName()).drawCardObject();
                            if (d != null) {
                                hands.get(tg.getName()).addCard(d);
                                Card c = hands.get(tg.getName()).getCards().get(hands.get(tg.getName()).getCards().size() - 1);
                                tg.sendMessage(ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + "You were delt the " + ChatColor.WHITE + c.getValue() + " of " + c.getSuit() + ChatColor.BLUE + ".");
                                ;

                            } else {
                                sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + tg.getName() + " has been dealt " + i + " cards");
                                sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + player.getName() + "'s deck is empty.");
                                return true;
                            }
                        }
                        sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.GREEN + tg.getName() + " has been dealt " + a + " cards");
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "This player's hand is not from your deck.");
                        return true;
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Player does not exist or is not online.");
                    return true;
                }
            } else if (split.length == 1 && split[0].equalsIgnoreCase("help")) {
                player.sendMessage(ChatColor.BLUE + "- - - D e a l e r   C o m m a n d s - - -");
                player.sendMessage(ChatColor.YELLOW + "        [] = Required, <> = Optional");

                player.sendMessage(ChatColor.GREEN + "/deck" + ChatColor.WHITE + " - Displays number of cards in deck.");
                player.sendMessage(ChatColor.GREEN + "/deck deal [Player] <Amount>" + ChatColor.WHITE + " - Deals cards to a player. Default 1.");
                player.sendMessage(ChatColor.GREEN + "/deck draw" + ChatColor.WHITE + " - Draws one card and displays to local players.");
                player.sendMessage(ChatColor.GREEN + "/deck burn" + ChatColor.WHITE + " - Discards the top card of the deck.");
                player.sendMessage(ChatColor.GREEN + "/deck shuffle" + ChatColor.WHITE + " - Reloads the deck and removes all hands.");
                player.sendMessage(ChatColor.GREEN + "/deck clear" + ChatColor.WHITE + " - Removes all hands.");
                player.sendMessage(ChatColor.GREEN + "/deck add <Amount>" + ChatColor.WHITE + " - Adds more cards to the deck. Default 1.");
                player.sendMessage(ChatColor.GREEN + "/deck pay [Player]" + ChatColor.WHITE + " - Gives 95% of the pot to a player, 5% to the dealer. Clears the pot.");
                return true;

            } else {
                if (decks.get(player.getName()) != null) {
                    sendLocalMessage(player, ChatColor.BLUE + "[Cards] " + ChatColor.WHITE + decks.get(player.getName()).getCardsRemaining() + ChatColor.GREEN + " cards remaining in " + player.getName() + "'s deck.");
                } else {
                    player.sendMessage(ChatColor.RED + "You do not have a deck.");
                }
                return true;
            }
        } else if (name.equalsIgnoreCase("roll") && permManager.hasPermission(player, "gambling.roll")) {
            if (split.length == 2) {
                int low = 0;
                int high = 0;
                try {
                    low = Integer.parseInt(split[0]);
                    high = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "Improper format.");
                    return false;
                }

                int rnd = rand(low, high);
                sendLocalMessage(player, ChatColor.BLUE + "[Dice] " + ChatColor.GREEN + player.getName() + " rolled a " + ChatColor.WHITE + rnd + ChatColor.YELLOW + " (" + low + "-" + high + ")");
                return true;
            }
        }
        return false;

    }

    public ArrayList<String> readFromFile(String string) {
        ArrayList<String> lines = new ArrayList<String>();
        File fl = new File(string);
        if (fl.exists()) {
            String crLine;
            FileReader flr = null;
            try {
                flr = new FileReader(string);
            } catch (FileNotFoundException e1) {

            }
            BufferedReader dInput = new BufferedReader(flr);
            try {

                while ((crLine = dInput.readLine()) != null) {

                    lines.add(crLine);
                }
            } catch (IOException e1) {

            }

            try {
                dInput.close();
            } catch (IOException e) {

            }
            try {
                flr.close();
            } catch (IOException e) {

            }
        }
        return lines;
    }

    public int getHighestPlayerBet(String owner) {
        Iterator<String> iter = bets.get(owner).keySet().iterator();
        int max = 0;
        while (iter.hasNext()) {
            String next = iter.next();
            if (bets.get(owner).get(next) > max) {
                max = bets.get(owner).get(next);
            }

        }
        return max;
    }

    public int getGameNumber() {
        int low = 1;
        int high = 100;
        Random r = new Random();
        return (int) (r.nextDouble() * (high - low) + low);
    }

    public void sendLocalMessage(Player player, String string) {
        Player[] p = this.getServer().getOnlinePlayers();
        for (int i = 0; i < p.length; i++) {
            Player t = p[i];
            if (t.getLocation().getWorld().equals(player.getLocation().getWorld())) {
                if (Math.sqrt(Math.pow((t.getLocation().getX() - player.getLocation().getX()), 2) + Math.pow((t.getLocation().getY() - player.getLocation().getY()), 2) + Math.pow((t.getLocation().getZ() - player.getLocation().getZ()), 2)) <= 20) {
                    t.sendMessage(string);
                }
            }
        }
    }

    public int rand(int low, int high) {
        return (int) (r.nextDouble() * (high - low + 1) + low);
    }

    public static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        if (length == 0) {
            return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
            if (length == 1) {
                return false;
            }
            i = 1;
        }
        for (; i < length; i++) {
            char c = str.charAt(i);
            if (c <= '/' || c >= ':') {
                return false;
            }
        }
        return true;
    }

    public void stackItem(Player player, int type) {
        Inventory inv = player.getInventory();
        int total = 0;
        ItemStack[] cont = inv.getContents();
        String name = "";
        for (int i = 0; i < cont.length; i++) {
            if (cont[i] != null) {
                if (cont[i].getType().getId() == type) {
                    name = cont[i].getType().name();
                    total += cont[i].getAmount();
                    inv.removeItem(cont[i]);
                }
            }
        }

        while (total > 0) {
            if (total > 64) {
                inv.addItem(new ItemStack(type, 64));
                total -= 64;
            } else {
                inv.addItem(new ItemStack(type, total));
                total = 0;
            }
        }
    }

}
