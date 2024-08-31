package io.github.stumper66.villagerannouncer;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.MessageUtil;
import org.bukkit.Bukkit;
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

    public void sendMessage(final String message){
        final var mainChannel = DiscordSRV.getPlugin().getMainTextChannel();
        if (mainChannel == null){
            if (!mainChannelWasNull){
                Log.war("DiscordSRV main channel was null, unable to send messages");
                mainChannelWasNull = true;
            }
            return;
        }

        mainChannel.sendMessage(
                MessageUtil.strip(message)
        ).queue();
    }

    public void reset(){
        isInstalled = null;
        mainChannelWasNull = false;
    }
}
