package net.cnation.gambling;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class SqlManager {
    static final Logger log = Logger.getLogger("Minecraft");
    private JavaPlugin plugin = null;

    public SqlManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public int countGameSigns(String g) {
        int count = 0;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamesigns WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                count++;
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to count game signs for game: " + g);
        }
        return count;

    }

    public int countPlayerGames(String n) {
        int count = 0;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM games WHERE owner=?");
            p.setString(1, n);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                count++;
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to count games owned by " + n);
        }
        return count;

    }

    public int countGameRules(String g) {
        int count = 0;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamerules WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                count++;
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to count game rules for game: " + g);

        }
        return count;
    }

    public String getGameOwner(String g) {
        String ret = null;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM games WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next() && ret == null) {
                ret = rs.getString("owner");
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to count game rules for game: " + g);

        }
        return ret;
    }

    public void removeGameRule(String g, int v) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("DELETE FROM gamerules WHERE game=? AND min=?");
            p.setString(1, g);
            p.setInt(2, v);
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to add  game: " + g);
        }
    }

    public String getCaseCorrectGameName(String g) {
        String n = "";
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM games WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                n = rs.getString("game");
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to read highest payout for game: " + g);
        }
        return n;
    }

    public int getHighestGamePayout(String g) {
        int m = 0;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamerules WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                if (rs.getInt("multiplier") > m) {
                    m = rs.getInt("multiplier");
                }
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to read highest payout for game: " + g);
        }
        return m;
    }

    public void addGame(Player pl, String g) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("INSERT INTO games (game, owner) VALUES (?,?)");
            p.setString(1, g);
            p.setString(2, pl.getName());
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to add  game: " + g);
        }
    }

    public boolean isPlayerOwner(Player p, String g) {
        if (getGameOwner(g).equalsIgnoreCase(p.getName())) {
            return true;
        }
        return false;
    }

    public boolean doesGameRuleExist(String g, int v) {
        boolean ret = false;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamerules WHERE game=? AND min=?");
            p.setString(1, g);
            p.setInt(2, v);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                ret = true;
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to check existance for game: " + g);

        }
        return ret;
    }

    public boolean doesGameExist(String g) {
        boolean ret = false;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM games WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                ret = true;
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to check existance for game: " + g);

        }
        return ret;
    }

    public void addGameRule(String g, int v, int m) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("INSERT INTO gamerules(game, min, multiplier) VALUES (?,?,?)");
            p.setString(1, g);
            p.setInt(2, v);
            p.setInt(3, m);
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to add rule for game: " + g);
        }
    }

    public void addGameSign(String g, Location l) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("INSERT INTO gamesigns(game, location) VALUES (?,?)");
            p.setString(1, g);
            p.setString(2, l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ());
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to add sign for game: " + g);
        }
    }

    public String getGameAtLocation(Location l) {
        String ret = null;
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamesigns WHERE location=?");
            p.setString(2, l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ());
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                ret = rs.getString("game");
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to check sign existance at location: " + l);

        }
        return ret;
    }

    public void removeGame(String g) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("DELETE FROM gamerules WHERE game=?");
            p.setString(1, g);
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to delete game rules for: " + g);

        }
    }

    public void removeGameRules(String g) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("DELETE FROM games WHERE game=?");
            p.setString(1, g);
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to delete game: " + g);

        }
    }

    public void removeGameSign(Location l) {
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("DELETE FROM gamesigns WHERE location=?");
            p.setString(1, l.getWorld().getName() + "," + l.getX() + "," + l.getY() + "," + l.getZ());
            p.executeUpdate();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to remove sign at location: " + l);
            e.printStackTrace();

        }
    }

    public HashMap<Integer, Integer> getGameRules(String g) {
        HashMap<Integer, Integer> output = new HashMap<Integer, Integer>();
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamerules WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                output.put(rs.getInt("min"), rs.getInt("multiplier"));
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to read rules for game: " + g);
        }
        return output;
    }

    public ArrayList<String> getGamesOfPlayer(String n) {
        ArrayList<String> output = new ArrayList<String>();
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM games WHERE owner=?");
            p.setString(1, n);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                output.add(rs.getString("game"));
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to read game list for " + n);
        }
        return output;
    }

    public ArrayList<Location> getGameSigns(String g) {
        ArrayList<Location> output = new ArrayList<Location>();
        try {
            Connection c = getDatabaseConnection();
            PreparedStatement p = c.prepareStatement("SELECT * FROM gamesigns WHERE game=?");
            p.setString(1, g);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                String l = rs.getString("location");
                String[] lparts = l.split(",");

                output.add(new Location(plugin.getServer().getWorld(lparts[0]), Double.parseDouble(lparts[1]), Double.parseDouble(lparts[2]), Double.parseDouble(lparts[3])));
            }
            rs.close();
            p.close();
            c.close();
        } catch (SQLException e) {
            log.warning("[Gambling] Failed to read sign locations for game: " + g);
        }
        return output;
    }

    public Connection getDatabaseConnection() {
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
}