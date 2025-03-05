package io.github.stumper66.villagerannouncer;

import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SoundInfo {
    public int lastPlayedSound;
    final public List<Sound> soundsToPlay = new LinkedList<>();
    final private List<Sound> soundsRemainingToPlay = new LinkedList<>();
    private boolean doNotUseAlreadyPlayedSounds;
    private boolean useRandom;

    public void reset(){
        soundsRemainingToPlay.clear();
        soundsRemainingToPlay.addAll(soundsToPlay);
        lastPlayedSound = -1;
        final YamlConfiguration config = VillagerAnnouncer.getInstance().config;

        this.doNotUseAlreadyPlayedSounds = config.getBoolean(
                "do-not-use-already-played-sounds", true);
        this.useRandom = config.getBoolean(
                "randomize-sound-list", true);

        if (!soundsRemainingToPlay.isEmpty() && useRandom) {
            Collections.shuffle(soundsRemainingToPlay);
        }
    }

    public @Nullable Sound getSoundToBePlayed(){
        if (soundsToPlay.isEmpty()) return null;
        if (soundsToPlay.size() == 1)
            return soundsToPlay.getFirst();

        if (soundsRemainingToPlay.isEmpty() || lastPlayedSound + 1 >= soundsRemainingToPlay.size())
            reset();

        final Sound sound;
        if (doNotUseAlreadyPlayedSounds) {
            sound = soundsRemainingToPlay.getFirst();
            soundsRemainingToPlay.remove(0);
        }
        else {
            if (useRandom){
                final int index = ThreadLocalRandom.current().nextInt(0, soundsRemainingToPlay.size());
                sound = soundsRemainingToPlay.get(index);
            }
            else
                sound = soundsRemainingToPlay.get(++lastPlayedSound);
        }

        return sound;
    }
}
