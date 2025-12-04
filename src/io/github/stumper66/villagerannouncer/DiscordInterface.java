package io.github.stumper66.villagerannouncer;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

public interface DiscordInterface {
    boolean getIsInstalled();

    void sendMessage(final Component component);

    void sendTestMessage(final CommandSender sender);

    void reset();
}
