package net.cnation.gambling;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.ItemStack;

public class GamblingPlayerListener extends PlayerListener {
    private Gambling plugin;

    public GamblingPlayerListener(Gambling instance) {
        plugin = instance;
    }

    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block dest = e.getClickedBlock();
        ItemStack item = e.getItem();
        String g = plugin.placing.get(p.getName());

        if (plugin.isSQLEnabled) {
            if (g != null) {
                if (p.getInventory().contains(Material.SIGN, 2)) {
                    if (item == null) {
                        g = g.split(":")[0];
                        String gs = plugin.getSqlManager().getCaseCorrectGameName(g);
                        g = gs;
                        Block s1 = dest.getRelative(e.getBlockFace());
                        Block s2 = s1.getRelative(0, -1, 0);
                        if (s1.getType().equals(Material.AIR) && s2.getType().equals(Material.AIR)) {
                            int data = 0;
                            switch (e.getBlockFace()) {
                            case SOUTH:
                                data = 5;
                                break;
                            case NORTH:
                                data = 4;
                                break;
                            case WEST:
                                data = 3;
                                break;
                            case EAST:
                                data = 2;
                                break;
                            }
                            s1.setTypeIdAndData(Material.WALL_SIGN.getId(), (byte) data, false);
                            s2.setTypeIdAndData(Material.WALL_SIGN.getId(), (byte) data, false);
                            HashMap<Integer, Integer> rules = plugin.getSqlManager().getGameRules(g);
                            Iterator<Integer> iter = rules.keySet().iterator();
                            int[] values = new int[rules.size()];
                            int i = 0;
                            while (iter.hasNext()) {
                                int next = iter.next();
                                values[i] = next;
                                i++;
                            }

                            Arrays.sort(values);
                            Sign ts1 = (Sign) s1.getState();
                            Sign ts2 = (Sign) s2.getState();
                            ts1.setLine(0, "-" + g + "-");
                            for (i = 0; i < values.length; i++) {
                                int output = rules.get(values[i]);

                                if (i + 1 < values.length) {
                                    ts1.setLine((i + 1), (values[i] + "-" + (values[i + 1] - 1) + ": " + output));
                                } else {
                                    if (values[i] != 100) {
                                        ts1.setLine((i + 1), (values[i] + "+" + ": " + output));
                                    } else {
                                        ts1.setLine((i + 1), (values[i] + ": " + output));

                                    }
                                }
                            }
                            int bid = Integer.parseInt(plugin.placing.get(p.getName()).split(":")[1]);
                            ts2.setLine(0, "Bet: " + bid);
                            ts2.setLine(1, ChatColor.GRAY + "(Press Here)");
                            ts2.setLine(2, "[Casino]");
                            ts2.update(true);
                            ts1.update(true);

                            p.sendMessage(ChatColor.BLUE + "[Casino] " + ChatColor.GREEN + "Sign placed for " + ChatColor.WHITE + g + ChatColor.GREEN + ".");
                            Location c = new Location(p.getWorld(), ts1.getX(), ts1.getY(), ts1.getZ());
                            plugin.getSqlManager().addGameSign(g, c);

                            plugin.placing.remove(p.getName());
                            p.getInventory().remove(new ItemStack(Material.SIGN, 1));
                        } else {
                            p.sendMessage(ChatColor.RED + "Something is obstructing this location");
                        }
                    } else {
                        p.sendMessage(ChatColor.RED + "Hand must be empty to place a casino sign.");
                    }
                }
            }
            if (dest != null && dest.getType().equals(Material.WALL_SIGN)) {
                Sign ts1 = (Sign) dest.getState();
                if (ts1.getLine(2).equalsIgnoreCase("[Casino]")) {
                    if (dest.getRelative(BlockFace.UP).getType().equals(Material.WALL_SIGN)) {
                        Sign ts2 = (Sign) dest.getRelative(BlockFace.UP).getState();
                        String gm = ts2.getLine(0).split("-")[1];
                        String gs = plugin.getSqlManager().getCaseCorrectGameName(gm);
                        gm = gs;
                        int bet = Integer.parseInt(ts1.getLine(0).split(":")[1].trim());
                        if (plugin.getEconManager().getBalance(p.getName()).amount >= bet) {
                            String owner = plugin.getSqlManager().getGameOwner(gm);
                            if (owner != null) {
                                if (plugin.getEconManager().getBalance(owner).amount + bet >= bet * (plugin.getSqlManager().getHighestGamePayout(gm))) {
                                    plugin.getEconManager().depositPlayer(owner, bet);
                                    plugin.getEconManager().withdrawPlayer(p.getName(), bet);
                                    int result = plugin.rand(1, 100);
                                    HashMap<Integer, Integer> rules = plugin.getSqlManager().getGameRules(gm);
                                    Iterator<Integer> iter = rules.keySet().iterator();
                                    int[] values = new int[rules.size()];
                                    int i = 0;
                                    while (iter.hasNext()) {
                                        int next = iter.next();
                                        values[i] = next;
                                        i++;
                                    }
                                    Arrays.sort(values);
                                    boolean isDone = false;
                                    for (i = values.length - 1; i >= 0; i--) {
                                        if (values[i] <= result && !isDone) {
                                            plugin.sendLocalMessage(p, ChatColor.BLUE + "[Gambling] " + ChatColor.WHITE + p.getName() + ChatColor.GREEN + " rolled a " + ChatColor.WHITE + result + ChatColor.GREEN + " in " + gm + ", and won " + ChatColor.WHITE + plugin.getEconManager().format(bet * (rules.get(values[i]) - 1)));
                                            plugin.getEconManager().withdrawPlayer(owner, bet * rules.get(values[i]));
                                            plugin.getEconManager().depositPlayer(p.getName(), bet * rules.get(values[i]));
                                            Player tg = plugin.getServer().getPlayer(owner);
                                            if (tg != null && tg.isOnline()) {
                                                tg.sendMessage(ChatColor.BLUE + "[Gambling - Win] " + ChatColor.GREEN + p.getName() + " has won " + ChatColor.WHITE + plugin.getEconManager().format(bet * (rules.get(values[i]) - 1)) + ChatColor.GREEN + " in " + ChatColor.WHITE + gm);
                                            }
                                            isDone = true;
                                        }
                                    }
                                    if (!isDone) {
                                        plugin.sendLocalMessage(p, ChatColor.BLUE + "[Gambling] " + ChatColor.WHITE + p.getName() + ChatColor.GREEN + " rolled a " + ChatColor.WHITE + result + ChatColor.GREEN + " in " + gm + ", and lost " + ChatColor.WHITE + plugin.getEconManager().format(bet));
                                        Player tg = plugin.getServer().getPlayer(owner);
                                        if (tg != null && tg.isOnline()) {
                                            tg.sendMessage(ChatColor.BLUE + "[Gambling - Lose] " + ChatColor.GREEN + p.getName() + " has lost " + ChatColor.WHITE + plugin.getEconManager().format(bet) + ChatColor.GREEN + " in " + ChatColor.WHITE + gm);
                                        }
                                    }

                                } else {
                                    p.sendMessage(ChatColor.RED + owner + "'s balance is too low.");
                                }
                            } else {
                                p.sendMessage(ChatColor.RED + "Game has been disabled.");
                            }
                        } else {
                            p.sendMessage(ChatColor.RED + "You cannot afford this bet.");
                        }

                    } else {
                        p.sendMessage(ChatColor.RED + "Casino is missing upper sign.");

                    }
                }
            }
        }
    }

}
