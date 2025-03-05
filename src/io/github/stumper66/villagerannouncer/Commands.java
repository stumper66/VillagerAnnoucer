package io.github.stumper66.villagerannouncer;

import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
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
        if (!sender.hasPermission("villagerannouncer")){
            sender.sendMessage("Access denied");
            return true;
        }

        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]))
            doReload(sender);
        else if (args.length >= 1 && "test-sound".equalsIgnoreCase(args[0])) {
            final String soundName = args.length >= 2 ? args[1] : null;
            doTestSound(sender, soundName);
        }
        else
            sender.sendMessage("Villager Announcer " + VillagerAnnouncer.getInstance().getDescription().getVersion() +
                    "\nOptions: reload / test-sound");

        return true;
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

        VillagerAnnouncer.getInstance().loadConfig(sender);
        VillagerAnnouncer.getInstance().discordSRVManager.reset();
        sender.sendMessage("Reloaded the config");
    }

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (args.length == 1) {
            final List<String> suggestions = new LinkedList<>();
            if (sender.hasPermission("villagerannouncer.reload"))
                suggestions.add("reload");
            if (sender.hasPermission("villagerannouncer.test-sound"))
                suggestions.add("test-sound");

            return suggestions;
        }
        else if (args.length == 2){
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
