package io.github.meeples10.chunkanalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {

    public static final String NAME = "ChunkAnalyzer";
    private static int count = 0;
    private static List<Vector2> analyzed = new ArrayList<Vector2>();
    private static HashMap<String, HashMap<Integer, Long>> blockCounts = new HashMap<String, HashMap<Integer, Long>>();
    private static String world = "world";
    private static int heightLimit = 256;
    private static int messageFrequency = 1000;
    private static int stopAfter = 400000;

    @Override
    public void onEnable() {
        this.getCommand("chunkanalyzer").setExecutor(new CommandCA());
        Bukkit.getPluginManager().registerEvents(this, this);
        File cfg = new File(getDataFolder(), "config.yml");
        if(!cfg.exists()) {
            getDataFolder().mkdirs();
            saveDefaultConfig();
        }
        FileConfiguration c = YamlConfiguration.loadConfiguration(cfg);
        world = c.getString("world");
        heightLimit = c.getInt("height-limit");
        messageFrequency = c.getInt("message-frequency");
        stopAfter = c.getInt("stop-after");
    }

    @Override
    public void onDisable() {
        long time = System.currentTimeMillis();
        File f = new File(getDataFolder(), "block-distributions-" + world + "-" + time + ".csv");
        String s = "id";
        for(int i = 0; i < heightLimit; i++) {
            s += "," + i;
        }
        long blockCount = 0;
        for(String key : blockCounts.keySet()) {
            s += "\n" + key;
            for(int i = 0; i < heightLimit; i++) {
                if(!blockCounts.get(key).containsKey(i)) {
                    s += ",0";
                    continue;
                }
                blockCount += blockCounts.get(key).get(i);
                s += "," + blockCounts.get(key).get(i);
            }
        }
        writeFile(f, s);
        File stats = new File(getDataFolder(), "statistics-" + time + ".log");
        writeFile(stats, "Chunks analyzed: " + count + "\nTotal blocks: " + blockCount + "\nUnique block types: "
                + blockCounts.size());
    }

    private static void writeFile(File f, String s) {
        FileWriter out = null;
        try {
            out = new FileWriter(f);
            out.write(s);
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if(out != null) try {
                out.close();
            } catch(IOException ignore) {}
        }
    }

    public static void broadcast(String s, boolean players) {
        Bukkit.getPluginManager().getPlugin(NAME).getLogger().info(s);
        if(players) {
            for(Player p : Bukkit.getServer().getOnlinePlayers()) {
                p.sendMessage(s);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent e) {
        if(count >= stopAfter) return;
        Chunk c = e.getChunk();
        if(alreadyAnalyzed(c.getX(), c.getZ())) return;
        if(!c.getWorld().getName().equals(world)) return;
        count++;
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < heightLimit; y++) {
                    String key = c.getBlock(x, y, z).getType().getKey().getKey();
                    if(!blockCounts.containsKey(key)) {
                        blockCounts.put(key, new HashMap<Integer, Long>());
                    }
                    if(!blockCounts.get(key).containsKey(y)) {
                        blockCounts.get(key).put(y, 1L);
                    } else {
                        blockCounts.get(key).put(y, blockCounts.get(key).get(y) + 1L);
                    }
                }
            }
        }
        broadcast(
                "ChunkLoadEvent: [" + ChatColor.GREEN + c.getX() + ChatColor.RESET + ", " + ChatColor.GREEN + c.getZ()
                        + ChatColor.WHITE + "] (#" + ChatColor.YELLOW + count + ChatColor.RESET + ")",
                count % messageFrequency == 0);
    }

    private static boolean alreadyAnalyzed(int x, int z) {
        for(Vector2 v : analyzed) {
            if(v.equals(x, z)) return true;
        }
        return false;
    }
}