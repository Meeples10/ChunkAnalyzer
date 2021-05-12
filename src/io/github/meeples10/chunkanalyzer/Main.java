package io.github.meeples10.chunkanalyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {

    public static final String NAME = "ChunkAnalyzer";
    private static int count = 0;
    private static List<Vector2> analyzed = new ArrayList<Vector2>();
    private static HashMap<String, HashMap<Integer, Long>> blockCounts = new HashMap<String, HashMap<Integer, Long>>();
    private static String worldName = "world";
    private static World world;
    private static int heightLimit = 256;
    private static int messageFrequency = 1000;
    private static boolean printToConsole = false;
    private static int stopAfter = 400000;
    private static long teleportDelay = 30;
    private static int teleportDistance = 0;
    private static int spiralSteps = 0;
    private static BukkitTask spiralTask;

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
        worldName = c.getString("world");
        world = Bukkit.getServer().getWorld(worldName);
        heightLimit = c.getInt("height-limit");
        messageFrequency = c.getInt("message-frequency");
        printToConsole = c.getBoolean("print-to-console");
        stopAfter = c.getInt("stop-after");
        if(stopAfter == 0) stopAfter = Integer.MAX_VALUE;
        teleportDelay = c.getLong("teleport-delay");
        teleportDistance = c.getInt("teleport-distance");
        if(teleportDistance == 0) {
            teleportDistance = Bukkit.getServer().getViewDistance() / 2;
        }
    }

    @Override
    public void onDisable() {
        if(spiralTask != null && !spiralTask.isCancelled()) {
            spiralTask.cancel();
        }
        long time = System.currentTimeMillis();
        File f = new File(getDataFolder(), "block-distributions-" + worldName + "-" + time + ".csv");
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
        if(printToConsole) {
            Bukkit.getPluginManager().getPlugin(NAME).getLogger().info(s);
        }
        if(messageFrequency != 0 && players) {
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
        if(!c.getWorld().equals(world)) return;
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

    /**
     * Credit to davedwards (https://stackoverflow.com/users/1248974/davedwards) on
     * StackOverflow for this spiral code: https://stackoverflow.com/a/45333503
     */
    public static Vector2 spiral(int step) {
        int dx = 0;
        int dz = 1;
        int segmentLength = 1;
        int x = 0;
        int z = 0;
        int segmentPassed = 0;
        // FIXME: loop causes severe lag spikes after plugin has been running for some
        // time
        for(int n = 0; n < step; ++n) {
            x += dx;
            z += dz;
            ++segmentPassed;
            if(segmentPassed == segmentLength) {
                segmentPassed = 0;
                int buffer = dz;
                dz = -dx;
                dx = buffer;
                if(dx == 0) {
                    ++segmentLength;
                }
            }
        }
        return new Vector2(x, z);
    }

    public static void resetSpiral() {
        if(spiralTask != null && !spiralTask.isCancelled()) {
            spiralTask.cancel();
        }
        spiralSteps = 0;
        spiralTask = new BukkitRunnable() {
            @Override
            public void run() {
                Vector2 v = spiral(spiralSteps);
                for(Player p : Bukkit.getServer().getOnlinePlayers()) {
                    p.teleport(new Location(world, v.x * 16.0 * teleportDistance, 64.0, v.z * 16.0 * teleportDistance));
                }
                spiralSteps++;
            }
        }.runTaskTimer(Bukkit.getPluginManager().getPlugin(NAME), 0, teleportDelay);
    }
}