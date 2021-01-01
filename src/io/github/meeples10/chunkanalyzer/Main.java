package io.github.meeples10.chunkanalyzer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    public static final String NAME = "ChunkAnalyzer";

    @Override
    public void onEnable() {
        this.getCommand("chunkanalyzer").setExecutor(new CommandCA());
        Bukkit.getPluginManager().registerEvents(new ChunkListener(), this);
    }
}