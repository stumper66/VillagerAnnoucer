package io.github.stumper66.villagerannouncer;

import github.scarsz.discordsrv.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DiscordSRVManager {
    public Boolean isInstalled;
    public boolean mainChannelWasNull;

    public boolean getIsInstalled(){
        if (isInstalled == null){
            final Plugin plugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
            isInstalled = plugin != null && plugin.isEnabled();
        }

        return isInstalled;
    }

    public void sendMessage(final Component component){
        final var mainChannel = DiscordSRV.getPlugin().getMainTextChannel();
        if (mainChannel == null){
            if (!mainChannelWasNull){
                Log.war("DiscordSRV main channel was null, unable to send messages");
                mainChannelWasNull = true;
            }
            return;
        }

        final String text = PlainTextComponentSerializer.plainText().serialize(component);
        mainChannel.sendMessage(text).queue();
    }

    public void sendTestMessage(final CommandSender sender){
        final var mainChannel = DiscordSRV.getPlugin().getMainTextChannel();
        if (mainChannel == null){
            sender.sendMessage("MainChannel was null from DiscordSRV");
            return;
        }

        final String channelId = mainChannel.getId();
        final String channelName = mainChannel.getName();
        String testMessage = "This is a test message";
        if (sender instanceof Player player)
            testMessage += " from " + player.getName();

        mainChannel.sendMessage(testMessage).queue();
        sender.sendMessage("Test message sent to channel name: '" + channelName + "', id: " + channelId);
    }

    public void reset(){
        isInstalled = null;
        mainChannelWasNull = false;
    }
}
