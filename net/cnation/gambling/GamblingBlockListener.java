package net.cnation.gambling;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.ItemStack;

public class GamblingBlockListener extends BlockListener {
    private Gambling plugin;

    public GamblingBlockListener(Gambling plugin) {
        this.plugin = plugin;
    }

    public void onBlockBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();
        if (plugin.isSQLEnabled) {
            if (b.getType().equals(Material.WALL_SIGN)) {
                Sign tSign = (Sign) b.getState();
                Block s2 = b.getRelative(0, -1, 0);
                Block s3 = b.getRelative(0, 1, 0);
                if (tSign.getLine(2).equalsIgnoreCase("[Casino]")) {
                    plugin.getSqlManager().removeGameSign(new Location(p.getWorld(), s3.getX(), s3.getY(), s3.getZ()));
                    p.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "Casino sign unregistered.");
                    if (s3.getType().equals(Material.WALL_SIGN)) {
                        p.getInventory().addItem(new ItemStack(Material.SIGN, 1));
                    }
                    s3.setType(Material.AIR);

                } else if (s2.getType().equals(Material.WALL_SIGN)) {
                    Sign ts2 = (Sign) s2.getState();
                    p.sendMessage(ts2.getLine(2));
                    if (ts2.getLine(2).equalsIgnoreCase("[Casino]")) {
                        plugin.getSqlManager().removeGameSign(new Location(p.getWorld(), b.getX(), b.getY(), b.getZ()));
                        p.sendMessage(ChatColor.BLUE + "[Gambling] " + ChatColor.GREEN + "Casino sign unregistered.");
                        s2.setType(Material.AIR);
                        p.getInventory().addItem(new ItemStack(Material.SIGN, 1));

                    }
                }
            }
        }
    }

    public void onSignChange(SignChangeEvent e) {
        Player p = e.getPlayer();
        String[] lines = e.getLines();
        if (plugin.isSQLEnabled) {
            if (lines[2].equalsIgnoreCase("[Casino]")) {
                p.sendMessage(ChatColor.RED + "Use /game place [Bet] to place casino signs.");
                e.setCancelled(true);
            }
        }

    }

}
