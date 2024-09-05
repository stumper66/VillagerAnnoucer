package io.github.stumper66.villagerannouncer;

import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class Log {
    private final static Logger log = Logger.getLogger("VillagerAnnouncer");
    private final static String PREFIX = "[VillagerAnnouncer] ";
    private static Boolean isRunningSpigot;

    private static boolean getIsRunningSpigot(){
        if (isRunningSpigot == null) {
            try {
                Class.forName("net.md_5.bungee.api.ChatColor");
                isRunningSpigot = true;
            } catch (ClassNotFoundException ignored) {
                isRunningSpigot = false;
            }
        }

        return isRunningSpigot;
    }

    // use this function for testing messages so you will remember to remove them later
    @Deprecated()
    public static void infTemp(final String text) {
        inf(text);
    }

    public static void inf(final String text){
        if (getIsRunningSpigot()) {
            Bukkit.getServer().getConsoleSender().sendMessage(MessageUtils.colorizeAll(PREFIX + text));
        }
        else{
            log.info(MessageUtils.colorizeAll(text));
        }
    }

    public static void war(final String text){
        log.warning(text);
    }
}
