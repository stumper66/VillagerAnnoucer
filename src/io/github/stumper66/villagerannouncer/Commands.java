package io.github.stumper66.villagerannouncer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    private List<String> soundNameSuggestions;

    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (args.length >= 1 && args[0].equalsIgnoreCase("toggle"))
            doPlayerMute(sender, label, args);
        else if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]))
            doReload(sender);
        else if (args.length >= 1 && "test-chat".equalsIgnoreCase(args[0]))
            doTestChat(sender);
        else if (args.length >= 1 && "test-sound".equalsIgnoreCase(args[0])) {
            final String soundName = args.length >= 2 ? args[1] : null;
            doTestSound(sender, soundName);
        }
        else
            showMainCommand(sender);

        return true;
    }

    private void showMainCommand(final @NotNull CommandSender sender){
        final StringBuilder sb = new StringBuilder("Villager Announcer ")
                .append(VillagerAnnouncer.getInstance().getDescription().getVersion())
                .append("\nOptions: ");
        final String[] cmds = {"reload", "test-chat", "test-sound", "toggle"};

        List<String> allowedCmds = new ArrayList<>();
        if (sender instanceof Player player) {
            for (String cmd : cmds){
                if (player.hasPermission("villagerannouncer." + cmd))
                    allowedCmds.add(cmd);
            }

            if (allowedCmds.isEmpty()){
                player.sendMessage("Access denied");
                return;
            }

            for (int i = 0; i < allowedCmds.size(); i++) {
                if (i > 0) sb.append(" / ");
                sb.append(allowedCmds.get(i));
            }

            sender.sendMessage(sb.toString());
        }
        else {
            for (int i = 0; i < cmds.length; i++) {
                if (i > 0) sb.append(" / ");
                sb.append(cmds[i]);
            }
            sender.sendMessage(sb.toString());
        }
    }

    private void doPlayerMute(final @NotNull CommandSender sender, final @NotNull String label, final String @NotNull [] args){
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command must be run by a player");
            return;
        }

        if (!player.hasPermission("villagerannouncer.toggle")) {
            player.sendMessage("Access denied");
            return;
        }

        boolean muted = false;
        boolean showOptions = false;

        if (args.length >= 2 && "mute".equalsIgnoreCase(args[1])){
            VillagerAnnouncer.getInstance().toggleSoundMuted(player, true);
            muted = true;
        }
        else if (args.length >= 2 && "unmute".equalsIgnoreCase(args[1]))
            VillagerAnnouncer.getInstance().toggleSoundMuted(player, false);
        else {
            muted = VillagerAnnouncer.getInstance().isSoundMuted(player);
            showOptions = true;
        }

        final YamlConfiguration config = VillagerAnnouncer.getInstance().config;
        final ConfigurationSection messages = config.getConfigurationSection("messages");
        if (messages == null){
            Log.war("Messages section in config.yml is null");
            return;
        }

        String keyName = muted ? "toggle-status-muted" : "toggle-status-not-muted";
        String defaultValue = "VillagerAnnouncer sounds: " + (muted ? "&aMUTED&r" : "&9NOT MUTED&r");

        String msg = messages.getString(keyName, defaultValue);

        if (showOptions) {
            if (muted) {
                keyName = "toggle-option-unmute";
                defaultValue = "<newline>To unmute run <color:gray>/" + label + " toggle unmute<reset>";
            }
            else {
                keyName = "toggle-option-mute";
                defaultValue = "<newline>To mute run <color:gray>/" + label + " toggle mute<reset>";
            }

            msg += messages.getString(keyName, defaultValue);
        }

        msg = msg.replace("%label%", label);

        final VillagerAnnouncer main = VillagerAnnouncer.getInstance();
        final Component comp = MiniMessage.miniMessage().deserialize(msg);
        main.adventure.sender(sender).sendMessage(comp);
    }

    private void doTestChat(final @NotNull CommandSender sender){
        if (!sender.hasPermission("villagerannouncer.test-chat")){
            sender.sendMessage("Access denied");
            return;
        }

        final DiscordInterface srv = VillagerAnnouncer.getInstance().discordInterface;

        if (srv == null || !srv.getIsInstalled()){
            sender.sendMessage("A supported Discord plugin is not installed or enabled");
            return;
        }

        srv.sendTestMessage(sender);
    }

    private void doTestSound(final @NotNull CommandSender sender, final @Nullable String soundName){
        if (!sender.hasPermission("villagerannouncer.test-sound")){
            sender.sendMessage("Access denied");
            return;
        }

        if (soundName == null){
            sender.sendMessage("No sound name was provided");
            return;
        }

        if (!(sender instanceof Player player)){
            sender.sendMessage("Command must be run by a player");
            return;
        }

        Sound sound;
        try{
            sound = Sound.valueOf(soundName.toUpperCase());
        }
        catch (Exception e){
            sender.sendMessage("Invalid sound name: " + soundName);
            return;
        }

        sender.sendMessage("Playing sound " + soundName.toLowerCase());
        player.playSound(player.getLocation(), sound, 1f, 1f);
    }

    private void doReload(final @NotNull CommandSender sender){
        if (!sender.hasPermission("villagerannouncer.reload")){
            sender.sendMessage("Access denied");
            return;
        }

        VillagerAnnouncer main = VillagerAnnouncer.getInstance();
        main.loadConfig(sender);
        if (main.discordInterface != null) main.discordInterface.reset();
        sender.sendMessage("Reloaded the config");
    }

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args){
        final boolean hasTogglePerms = sender.hasPermission("villagerannouncer.toggle");
        final boolean hasTestSoundPerms = sender.hasPermission("villagerannouncer.test-sound");

        if (args.length == 1) {
            final List<String> suggestions = new LinkedList<>();
            if (sender.hasPermission("villagerannouncer.reload"))
                suggestions.add("reload");
            if (hasTestSoundPerms)
                suggestions.add("test-sound");
            if (sender.hasPermission("villagerannouncer.test-chat"))
                suggestions.add("test-chat");
            if (hasTogglePerms)
                suggestions.add("toggle");

            return suggestions;
        }
        else if (hasTogglePerms && args.length == 2 && "toggle".equalsIgnoreCase(args[0]))
            return List.of("mute", "unmute");
        else if (hasTestSoundPerms && args.length == 2 && "test-sound".equalsIgnoreCase(args[0])){
            if (soundNameSuggestions == null) buildSoundNames();
            return soundNameSuggestions;
        }

        return List.of();
    }

    private void buildSoundNames(){
        final List<String> suggestions = new ArrayList<>(Sound.values().length);
        for (Sound soundName : Sound.values())
            suggestions.add(soundName.name().toLowerCase());

        Collections.sort(suggestions);
        this.soundNameSuggestions = suggestions;
    }
}
