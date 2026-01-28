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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
// Meechie's Toggle
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;

public class VillagerAnnouncer extends JavaPlugin {
    private static VillagerAnnouncer instance;
    NamespacedKey keyWasVillager;
    NamespacedKey keyTraders;
    public YamlConfiguration config;
    boolean playSound;
    boolean isEnabled;
    boolean onlyBroadcastIfTradedWith;
    private boolean isRunningPaper;
    SoundInfo soundsNormal;
    SoundInfo soundsWanderingTrader;
    private DiscordSRVManager discordSRVManager;
    private EssentialsXDiscord essentialsXDiscord;
    @Nullable DiscordInterface discordInterface;
    DiscordPluginName discordPluginName = DiscordPluginName.NONE;
    public BukkitAudiences adventure;

    // Meechie's Toggle -- pb loves Meechie
    private final Set<UUID> mutedPlayers = new HashSet<>();
    private File mutedFile;
    private FileConfiguration mutedConfig;

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
        // Meechie Toggle --- pb loved old men
        registerCommands();
        loadConfig(null);
        loadMutedPlayers();
        registerListeners();
        discordSRVManager = new DiscordSRVManager();
        essentialsXDiscord = new EssentialsXDiscord();
        checkForDiscordPlugins();

        Log.inf("Villager Announcer loaded");
    }

    private void checkForDiscordPlugins(){
        if (discordSRVManager.getIsInstalled()) {
            this.discordPluginName = DiscordPluginName.DISCORDSRV;
            discordInterface = discordSRVManager;
            Log.inf("Found discord plugin: DiscordSRV");
        }
        else if (essentialsXDiscord.getIsInstalled()) {
            this.discordPluginName = DiscordPluginName.ESSENTIALSX_DISCORD;
            discordInterface = essentialsXDiscord;
            Log.inf("Found discord plugin: EssentialsX Discord");
        }
        else
            this.discordPluginName = DiscordPluginName.NONE;
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
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            config = new YamlConfiguration();
            return;
        }

        config = YamlConfiguration.loadConfiguration(file);
        config.options().copyDefaults(true);
        final int fileVersion = config.getInt("file-version");

        if (fileVersion < 7){
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
    // Meechie's Toggle --- hi pb!
    private void loadMutedPlayers() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        mutedFile = new File(getDataFolder(), "muted.yml");
        if (!mutedFile.exists()) {
            try {
                mutedFile.createNewFile();
            } catch (IOException e) {
                Log.war("Unable to create muted.yml");
                e.printStackTrace();
            }
        }

        mutedConfig = YamlConfiguration.loadConfiguration(mutedFile);
        mutedPlayers.clear();

        for (String s : mutedConfig.getStringList("muted")) {
            try {
                mutedPlayers.add(UUID.fromString(s));
            } catch (IllegalArgumentException ignored) {
                Log.war("Invalid UUID in muted.yml: " + s);
            }
        }
    }

    private void saveMutedPlayers() {
        if (mutedConfig == null || mutedFile == null) return;

        List<String> out = mutedPlayers.stream().map(UUID::toString).toList();
        mutedConfig.set("muted", out);

        try {
            mutedConfig.save(mutedFile);
        } catch (IOException e) {
            Log.war("Unable to save muted.yml");
            e.printStackTrace();
        }
    }
// Meechie Toggle n stuffs
    public boolean isMuted(@NotNull Player player) {
        return mutedPlayers.contains(player.getUniqueId());
    }

        public boolean toggleMuted(@NotNull Player player) {
            UUID id = player.getUniqueId();
            boolean nowMuted;

            if (mutedPlayers.contains(id)) {
                mutedPlayers.remove(id);
                nowMuted = false;
            } else {
                mutedPlayers.add(id);
                nowMuted = true;
            }

            saveMutedPlayers();
            return nowMuted;
        }


    private void parseSoundConfig(){
        this.soundsNormal = new SoundInfo();
        this.soundsWanderingTrader = new SoundInfo();

        playSound = config.getBoolean("play-sound");
        if (!playSound) return;

        this.soundsNormal.soundsToPlay.addAll(parseSounds("sound-name"));
        this.soundsWanderingTrader.soundsToPlay.addAll(parseSounds("sound-name-wandering-trader"));

        this.soundsNormal.reset();
        this.soundsWanderingTrader.reset();
    }

    private @NotNull List<Sound> parseSounds(final String configName){
        final String soundName = config.getString(configName);
        final List<String> soundNames = config.getStringList(configName);
        final List<Sound> results = new LinkedList<>();

        if (soundNames.isEmpty() && (soundName == null || soundName.isEmpty()))
            return Collections.emptyList();
        else if (soundNames.isEmpty())
            soundNames.add(soundName);

        for (final String name : soundNames) {
            try {
                Sound sound = Sound.valueOf(name.toUpperCase());
                results.add(sound);
            } catch (Exception ignored) {
                Log.war("Invalid sound name: " + soundName);
            }
        }

        return results;
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
