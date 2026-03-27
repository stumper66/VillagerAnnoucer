package io.github.stumper66.villagerannouncer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class Log {
    private final static Logger log = Logger.getLogger("VillagerAnnouncer");

    // use this function for testing messages so you will remember to remove them later
    @Deprecated()
    public static void infTemp(final String text) {
        inf(text);
    }

    public static void inf(final String text){
        final Component comp = MiniMessage.miniMessage().deserialize(text);
        Bukkit.getConsoleSender().sendMessage(comp);
    }

    public static void war(final String text){
        log.warning(text);
    }
}
