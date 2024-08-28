package io.github.stumper66.villagerannouncer;

import java.util.logging.Logger;

public class Log {
    private final static Logger log = Logger.getLogger("VillagerAnnouncer");

    public static void inf(final String text){
        log.info(text);
    }

    public static void war(final String text){
        log.warning(text);
    }
}
