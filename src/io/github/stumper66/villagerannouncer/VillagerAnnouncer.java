package io.github.stumper66.villagerannouncer;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.FileUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;

public class VillagerAnnouncer extends JavaPlugin {
    private static VillagerAnnouncer instance;
    NamespacedKey key;
    public YamlConfiguration config;
    boolean playSound;
    boolean isEnabled;
    Sound soundToPlay;
    DiscordSRVManager discordSRVManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        key = new NamespacedKey(this, "wasvillager");
        registerCommands();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(new EventListeners(), this);
        discordSRVManager = new DiscordSRVManager();

        Log.inf("Villager Announcer loaded");
    }

    private void registerCommands(){
        final PluginCommand cmd = getCommand("villagerannouncer");
        if (cmd == null)
            Log.inf("VillagerAnnouncer: Command &b/villageranouncer&7 is unavailable, is it not registered in plugin.yml?");
        else
            cmd.setExecutor(new Commands());
    }

    void loadConfig(){
        final File file = new File(getDataFolder(), "config.yml");
        if (!file.exists())
            saveResource(file.getName(), false);

        try (final FileInputStream fs = new FileInputStream(file)) {
            new Yaml().load(fs);
        } catch (final Exception e) {
            Log.war("Unable to parse config.yml");
            e.printStackTrace();
            config = new YamlConfiguration();
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        config.options().copyDefaults(true);
        final int fileVersion = config.getInt("file-version");

        if (fileVersion < 2){
            // copy to old file
            final File backedupFile = new File(getDataFolder(),
                    "config.yml.v" + fileVersion + ".old");
            FileUtil.copy(file, backedupFile);
            Log.inf("&fFile Loader: &8(Migration) &bconfig.yml backed up to "
                    + backedupFile.getName());

            saveResource(file.getName(), true);
            FileMigrator.copyYmlValues(backedupFile, file);
            config = YamlConfiguration.loadConfiguration(file);
        }

        this.isEnabled = config.getBoolean("enabled", true);
        if (!this.isEnabled) Log.inf("Plugin is currently disabled via config");
        playSound = config.getBoolean("play-sound");
        if (playSound){
            final String soundName = config.getString("sound-name");
            if (soundName == null || soundName.isEmpty())
                playSound = false;
            else{
                try{
                    soundToPlay = Sound.valueOf(soundName.toUpperCase());
                }
                catch (Exception ignored){
                    Log.war("Invalid sound name: " + soundName);
                    playSound = false;
                }
            }
        }

    }

    public static VillagerAnnouncer getInstance(){
        return instance;
    }

    @Override
    public void onDisable() {

    }
}
