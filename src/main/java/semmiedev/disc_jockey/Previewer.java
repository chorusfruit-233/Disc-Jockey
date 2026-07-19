package semmiedev.disc_jockey;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class Previewer implements ClientTickEvents.StartLevelTick {
    public boolean running;

    private int i;
    private double tick;
    private Song song;

    public void start(Song song) {
        stop();
        if (song == null || song.notes.isEmpty()) return;
        this.song = song;
        Main.TICK_LISTENERS.addIfAbsent(this);
        running = true;
    }

    public void stop() {
        Main.TICK_LISTENERS.remove(this);
        running = false;
        i = 0;
        tick = 0;
    }

    @Override
    public void onStartTick(@NonNull ClientLevel world) {
        Minecraft client = Minecraft.getInstance();
        if (!running || song == null || client.player == null) return;

        while (running && i < song.notes.size()) {
            SongNote songNote = song.notes.get(i);
            if (songNote.tick() <= Math.round(tick)) {
                Vec3 pos = client.player.position();
                Note note = songNote.note();
                world.playLocalSound(pos.x, pos.y, pos.z, note.instrument().getSoundEvent().value(), SoundSource.RECORDS, 3, (float)Math.pow(2.0, (note.note() - 12) / 12.0), false);
                i++;
                if (i >= song.notes.size()) {
                    stop();
                    break;
                }
            } else {
                break;
            }
        }
        if (running) tick += song.tempo / 100.0 / 20.0;
    }
}
