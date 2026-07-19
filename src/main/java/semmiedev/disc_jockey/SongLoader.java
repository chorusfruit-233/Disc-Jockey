package semmiedev.disc_jockey;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import semmiedev.disc_jockey.gui.SongListWidget;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public final class SongLoader {
    private static final int MAX_SUPPORTED_NBS_VERSION = 5;

    public static final List<Song> SONGS = new CopyOnWriteArrayList<>();
    public static final List<String> SONG_SUGGESTIONS = new CopyOnWriteArrayList<>();
    public static volatile boolean loadingSongs;
    public static volatile boolean showToast;
    public static volatile int failedSongCount;
    public static volatile long revision;

    private SongLoader() {
    }

    public static synchronized void loadSongs() {
        if (loadingSongs) return;
        loadingSongs = true;

        Thread.startVirtualThread(() -> {
            List<Song> loadedSongs = new ArrayList<>();
            int failures = 0;

            try {
                Files.createDirectories(Main.songsFolder);
                try (Stream<Path> paths = Files.list(Main.songsFolder)) {
                    for (Path path : paths
                            .filter(Files::isRegularFile)
                            .filter(SongLoader::isNbsFile)
                            .sorted(Comparator.comparing(value -> value.getFileName().toString().toLowerCase(Locale.ROOT)))
                            .toList()) {
                        try {
                            Song song = parseSong(path);
                            logUnsupportedNotes(song);
                            loadedSongs.add(song);
                        } catch (Exception exception) {
                            failures++;
                            Main.LOGGER.error("Unable to read or parse song {}", path.getFileName(), exception);
                        }
                    }
                }
            } catch (Exception exception) {
                failures++;
                Main.LOGGER.error("Unable to scan song directory {}", Main.songsFolder, exception);
            }

            int finalFailures = failures;
            Minecraft.getInstance().execute(() -> publishSongs(loadedSongs, finalFailures));
        });
    }

    private static void publishSongs(List<Song> loadedSongs, int failures) {
        try {
            loadedSongs.sort(Comparator.comparing(song -> song.displayName.toLowerCase(Locale.ROOT)));
            loadedSongs.forEach(SongLoader::prepareSong);
            SONGS.clear();
            SONGS.addAll(loadedSongs);
            failedSongCount = failures;
            rebuildSuggestions();

            Main.config.favorites.removeIf(favorite -> SONGS.stream()
                    .map(song -> song.fileName)
                    .noneMatch(favorite::equals));
            revision++;

            Main.LOGGER.info("Loaded {} song(s) from {} ({} failed)", SONGS.size(), Main.songsFolder, failures);
            if (showToast) {
                Component message = failures == 0
                        ? Component.translatable(Main.MOD_ID + ".loading_done", SONGS.size())
                        : Component.translatable(Main.MOD_ID + ".loading_failed", SONGS.size(), failures);
                SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Main.NAME, message);
            }
        } finally {
            showToast = false;
            loadingSongs = false;
        }
    }

    public static Song loadSong(File file) throws IOException {
        if (!file.isFile() || !isNbsFile(file.toPath())) return null;
        Song song = parseSong(file.toPath());
        logUnsupportedNotes(song);
        prepareSong(song);
        return song;
    }

    private static void prepareSong(Song song) {
        song.entry = new SongListWidget.SongEntry(song);
        song.entry.favorite = Main.config != null && Main.config.favorites.contains(song.fileName);
    }

    static Song parseSong(Path path) throws IOException {
        try (BinaryReader reader = new BinaryReader(Files.newInputStream(path))) {
            Song song = new Song();
            song.fileName = sanitize(path.getFileName().toString());

            int firstLength = reader.readUShort();
            boolean newFormat = firstLength == 0;
            if (newFormat) {
                song.formatVersion = reader.readUnsignedByte();
                if (song.formatVersion < 1 || song.formatVersion > MAX_SUPPORTED_NBS_VERSION) {
                    throw new IOException("Unsupported NBS version: " + song.formatVersion);
                }
                song.vanillaInstrumentCount = reader.readUnsignedByte();
                if (song.formatVersion >= 3) song.length = reader.readUShort();
            } else {
                song.formatVersion = 0;
                song.vanillaInstrumentCount = 10;
                song.length = firstLength;
            }

            song.height = reader.readUShort();
            song.name = sanitize(reader.readString());
            song.author = sanitize(reader.readString());
            song.originalAuthor = sanitize(reader.readString());
            song.description = sanitize(reader.readString());
            song.tempo = reader.readUShort();
            if (song.tempo <= 0) throw new IOException("NBS tempo must be greater than zero");
            song.autoSaving = reader.readUnsignedByte();
            song.autoSavingDuration = reader.readUnsignedByte();
            song.timeSignature = reader.readUnsignedByte();
            song.minutesSpent = reader.readInt();
            song.leftClicks = reader.readInt();
            song.rightClicks = reader.readInt();
            song.blocksAdded = reader.readInt();
            song.blocksRemoved = reader.readInt();
            song.importFileName = sanitize(reader.readString());

            if (song.formatVersion >= 4) {
                song.loop = reader.readUnsignedByte();
                song.maxLoopCount = reader.readUnsignedByte();
                song.loopStartTick = reader.readUShort();
            }

            song.displayName = song.name.isBlank() ? song.fileName : song.name + " (" + song.fileName + ")";
            song.searchableFileName = normalizeSearch(song.fileName);
            song.searchableName = normalizeSearch(song.name);

            int tick = -1;
            int tickJump;
            while ((tickJump = reader.readUShort()) != 0) {
                tick += tickJump;
                int layer = -1;
                int layerJump;
                while ((layerJump = reader.readUShort()) != 0) {
                    layer += layerJump;

                    int instrumentId = reader.readUnsignedByte();
                    int key = reader.readUnsignedByte();
                    if (song.formatVersion >= 4) {
                        reader.readUnsignedByte(); // velocity
                        reader.readUnsignedByte(); // panning
                        reader.readShort(); // fine pitch
                    }

                    if (!Note.isSupportedNbsId(instrumentId, song.vanillaInstrumentCount)) {
                        song.unsupportedNoteCount++;
                        continue;
                    }

                    byte noteValue = (byte) Math.clamp(key - 33, 0, 24);
                    Note note = new Note(instrumentId, noteValue);
                    song.uniqueNotes.add(note);
                    song.notes.add(new SongNote(tick, layer, note));
                }
            }
            if (song.length == 0) song.length = Math.max(0, tick);

            return song;
        }
    }

    public static void addSong(Song song) {
        SONGS.removeIf(existing -> existing.fileName.equalsIgnoreCase(song.fileName));
        SONGS.add(song);
        sort();
        rebuildSuggestions();
        revision++;
    }

    public static void sort() {
        SONGS.sort(Comparator.comparing(song -> song.displayName.toLowerCase(Locale.ROOT)));
    }

    private static void rebuildSuggestions() {
        SONG_SUGGESTIONS.clear();
        SONG_SUGGESTIONS.addAll(SONGS.stream().map(song -> song.displayName).toList());
    }

    private static void logUnsupportedNotes(Song song) {
        if (song.unsupportedNoteCount > 0) {
            Main.LOGGER.warn("Skipped {} custom-instrument note(s) in {}", song.unsupportedNoteCount, song.fileName);
        }
    }

    private static boolean isNbsFile(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".nbs");
    }

    private static String normalizeSearch(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
    }

    private static String sanitize(String value) {
        return value.replace("\n", "").replace("\r", "");
    }
}
