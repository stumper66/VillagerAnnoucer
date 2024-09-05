package io.github.stumper66.villagerannouncer;

import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class PaperListeners implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityInteractEvent(final @NotNull PlayerTradeEvent event){
        final VillagerAnnouncer main = VillagerAnnouncer.getInstance();
        if (!main.onlyBroadcastIfTradedWith) return;

        final PersistentDataContainer pdc = event.getVillager().getPersistentDataContainer();
        String ids = null;
        if (pdc.has(main.keyTraders, PersistentDataType.STRING))
             ids = pdc.get(main.keyTraders, PersistentDataType.STRING);

        final String playerId = event.getPlayer().getUniqueId().toString();

        if (ids == null)
            ids = playerId;
        else if (ids.contains(playerId))
            return;
        else
            ids += "," + playerId;

        pdc.set(main.keyTraders, PersistentDataType.STRING, ids);
    }
}
