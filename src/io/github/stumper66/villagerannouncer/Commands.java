package io.github.stumper66.villagerannouncer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Commands implements CommandExecutor, TabCompleter {
    public boolean onCommand(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final String @NotNull [] args) {
        if (!sender.hasPermission("villagerannouncer")){
            sender.sendMessage("Access denied");
            return true;
        }

        if (args.length >= 1 && "reload".equalsIgnoreCase(args[0]))
            doReload(sender);
        else
            sender.sendMessage("Villager Announcer " + VillagerAnnouncer.getInstance().getDescription().getVersion() +
                    "\nOptions: reload");

        return true;
    }

    private void doReload(final @NotNull CommandSender sender){
        if (!sender.hasPermission("villagerannouncer.reload")){
            sender.sendMessage("Access denied");
            return;
        }

        VillagerAnnouncer.getInstance().loadConfig();
        VillagerAnnouncer.getInstance().discordSRVManager.reset();
        sender.sendMessage("Reloaded the config");
    }

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull Command command, final @NotNull String label, final @NotNull String @NotNull [] args){
        if (sender.hasPermission("villagerannouncer") && args.length == 1)
            return List.of("reload");

        return List.of();
    }
}
