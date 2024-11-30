package io.github.stumper66.villagerannouncer;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.FileUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;

public class VillagerAnnouncer extends JavaPlugin {
    private static VillagerAnnouncer instance;
    NamespacedKey keyWasVillager;
    NamespacedKey keyTraders;
    public YamlConfiguration config;
    boolean playSound;
    boolean isEnabled;
    boolean onlyBroadcastIfTradedWith;
    private boolean isRunningPaper;
    Sound soundToPlayNormal;
    Sound soundToPlayWanderingTrader;
    DiscordSRVManager discordSRVManager;
    public BukkitAudiences adventure;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        keyWasVillager = new NamespacedKey(this, "wasvillager");
        keyTraders = new NamespacedKey(this, "traders");
        checkForPaper();
        registerCommands();
        loadConfig(null);
        registerListeners();
        discordSRVManager = new DiscordSRVManager();

        Log.inf("Villager Announcer loaded");
    }

    private void checkForPaper(){
        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            isRunningPaper = true;
        } catch (ClassNotFoundException ignored) { }
    }

    private void registerListeners(){
        Bukkit.getPluginManager().registerEvents(new EventListeners(), this);
        if (isRunningPaper)
            Bukkit.getPluginManager().registerEvents(new PaperListeners(), this);
    }

    private void registerCommands(){
        final PluginCommand cmd = getCommand("villagerannouncer");
        if (cmd == null)
            Log.inf("VillagerAnnouncer: Command &b/villageranouncer&7 is unavailable, is it not registered in plugin.yml?");
        else
            cmd.setExecutor(new Commands());
    }

    void loadConfig(CommandSender whoReloaded){
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

        if (fileVersion < 6){
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

        parseSoundConfig();

        onlyBroadcastIfTradedWith = config.getBoolean("only-broadcast-if-traded-with");
        if (onlyBroadcastIfTradedWith && !isRunningPaper){
            final String msg = "only-broadcast-if-traded-with is a Paper only feature and will be disabled since this server is not a Paper server.";
            Log.war(msg);
            if (whoReloaded instanceof Player)
                whoReloaded.sendMessage(MessageUtils.colorizeAll("VillagerAnnouncer: &c") + msg);
            onlyBroadcastIfTradedWith = false;
        }
    }

    private void parseSoundConfig(){
        playSound = config.getBoolean("play-sound");
        if (!playSound) return;

        final String soundName = config.getString("sound-name");
        if (soundName == null || soundName.isEmpty())
            soundToPlayNormal = null;
        else{
            try{
                soundToPlayNormal = Sound.valueOf(soundName.toUpperCase());
            }
            catch (Exception ignored){
                Log.war("Invalid sound name: " + soundName);
                soundToPlayNormal = null;
            }
        }

        final String soundNameWanderingTrader = config.getString("sound-name-wandering-trader");
        if (soundNameWanderingTrader == null || soundNameWanderingTrader.isEmpty())
            soundToPlayWanderingTrader = null;
        else{
            try{
                soundToPlayWanderingTrader = Sound.valueOf(soundNameWanderingTrader.toUpperCase());
            }
            catch (Exception ignored){
                Log.war("Invalid sound name (wandering trader): " + soundNameWanderingTrader);
                soundToPlayWanderingTrader = null;
            }
        }
    }

    public static VillagerAnnouncer getInstance(){
        return instance;
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }
}
