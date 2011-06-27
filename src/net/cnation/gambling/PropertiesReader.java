package net.cnation.gambling;

import java.io.*;
import java.util.Properties;

public class PropertiesReader {

    Properties p = new Properties();
    String file;

    public PropertiesReader(String f) {
        try {
            p.load(new FileInputStream(new File(f)));
            file = f;
        } catch (IOException e) {
            createNewFile(f);
            System.out.println(f + " missing, generating file.");
        }

    }

    public void createNewFile(String f) {
        File dataFile = new File(f);
        String folder = f.substring(0, f.lastIndexOf("/"));
        try {
            (new File(folder)).mkdirs();
            dataFile.createNewFile();
            p.load(new FileInputStream(new File(f)));
        } catch (IOException e) {
            System.out.println("Error in generating file");
        }

    }

    public int readInt(String key, int def) {
        Integer i;
        try {
            String s = p.getProperty(key);
            if (s == null) {
                p.put(key, "" + def);
                save();
                s = "" + def;
            }
            i = Integer.parseInt(s);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException reading " + key + " from properties.");
            return def;
        }
        return i;
    }

    public int readInt(String key) {
        Integer i;
        try {
            i = Integer.parseInt(p.getProperty(key));
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException reading " + key + " from properties.");
            return 0;
        }
        return i;
    }

    public String readString(String key, String def) {
        String t = p.getProperty(key);
        if (t == null) {
            p.put(key, def);
            save();
            t = def;
        }
        return t;
    }

    public String readString(String key) {
        String t = p.getProperty(key);
        if (t == null) {
            System.out.println("Cound not find key " + key + ".");
        }
        return t;
    }

    public boolean readBoolean(String key, boolean def) {
        Boolean i;
        try {
            String b = p.getProperty(key);
            if (b == null) {
                p.put(key, "" + def);
                save();
                b = "" + def;
            }
            i = Boolean.parseBoolean(b);
        } catch (Exception e) {
            System.out.println("FormatException reading " + key + " from properties.");
            return def;
        }
        return i;

    }

    public boolean readBoolean(String key) {
        Boolean i;
        try {
            i = Boolean.parseBoolean(p.getProperty(key));
        } catch (Exception e) {
            System.out.println("NumberFormatException reading " + key + " from properties.");
            return false;
        }
        return i;

    }

    public long readLong(String key, long def) {
        Long i;
        try {
            String s = p.getProperty(key);
            if (s == null) {
                p.put(key, "" + def);
                save();
                s = "" + def;
            }
            i = Long.parseLong(s);
        } catch (Exception e) {
            System.out.println("FormatException reading " + key + " from properties.");
            p.put(key, "" + def);
            return def;
        }
        return i;
    }

    public long readLong(String key) {
        Long i;
        try {
            i = Long.parseLong(p.getProperty(key));
        } catch (Exception e) {
            System.out.println("NumberFormatException reading " + key + " from properties.");
            return -1;
        }
        return i;

    }

    public double readDouble(String key, double def) {
        double i;
        try {
            String s = p.getProperty(key);
            if (s == null) {
                p.put(key, "" + def);
                save();
                s = "" + def;
            }
            i = Double.parseDouble(s);
        } catch (Exception e) {
            System.out.println("FormatException reading " + key + " from properties.");
            return def;
        }
        return i;
    }

    public double readDouble(String key) {
        double i;
        try {
            i = Double.parseDouble(p.getProperty(key));
        } catch (Exception e) {
            System.out.println("NumberFormatException reading " + key + " from properties.");
            return -1;
        }
        return i;
    }

    public void setInt(String key, int def) {
        p.setProperty(key, "" + def);
    }

    public void setString(String key, String def) {
        p.setProperty(key, "" + def);
    }

    public void setBoolean(String key, boolean def) {
        p.setProperty(key, "" + def);
    }

    public void setLong(String key, long def) {
        p.setProperty(key, "" + def);
    }

    public void setDouble(String key, double def) {
        p.setProperty(key, "" + def);
    }

    public void save() {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            p.store(stream, null);
        } catch (IOException ex) {
        } finally {
            try {
                if (stream != null)
                    stream.close();
            } catch (IOException ex) {
            }
        }
    }

}
