package semmiedev.disc_jockey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SongLoaderTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void parsesVersionFiveAndSkipsCustomInstruments() throws IOException {
        NbsWriter writer = new NbsWriter();
        writer.header(5, 16, 2, 2, "Test song", 1200);
        writer.loopData();
        writer.note(1, 1, 0, 45, true);
        writer.noteInSameTick(1, 16, 45, true);
        writer.endLayerAndSong();

        Song song = parse(writer, "test.nbs");

        assertEquals("Test song (test.nbs)", song.displayName);
        assertEquals(1, song.notes.size());
        assertEquals(1, song.uniqueNotes.size());
        assertEquals(1, song.unsupportedNoteCount);
        assertEquals(0, song.notes.getFirst().tick());
        assertEquals(0, song.notes.getFirst().note().nbsInstrumentId());
        assertEquals(12, song.notes.getFirst().note().note());
    }

    @Test
    void parsesVersionThreeWithoutVersionFourFields() throws IOException {
        NbsWriter writer = new NbsWriter();
        writer.header(3, 16, 1, 1, "Version 3", 1000);
        writer.note(1, 1, 5, 33, false);
        writer.endLayerAndSong();

        Song song = parse(writer, "v3.nbs");

        assertEquals(1, song.notes.size());
        assertEquals(0, song.notes.getFirst().note().note());
        assertEquals(0, song.loop);
    }

    @Test
    void parsesVersionTwoWithoutSongLength() throws IOException {
        NbsWriter writer = new NbsWriter();
        writer.header(2, 10, 0, 1, "Version 2", 1000);
        writer.note(5, 1, 0, 45, false);
        writer.endLayerAndSong();

        Song song = parse(writer, "v2.nbs");

        assertEquals(1, song.notes.size());
        assertEquals(4, song.notes.getFirst().tick());
        assertEquals(4, song.length);
    }

    @Test
    void parsesLegacyVersionZero() throws IOException {
        NbsWriter writer = new NbsWriter();
        writer.legacyHeader(8, 1, "Legacy", 1000);
        writer.note(1, 1, 9, 57, false);
        writer.endLayerAndSong();

        Song song = parse(writer, "legacy.nbs");

        assertEquals(0, song.formatVersion);
        assertEquals(10, song.vanillaInstrumentCount);
        assertEquals(9, song.notes.getFirst().note().nbsInstrumentId());
        assertEquals(24, song.notes.getFirst().note().note());
    }

    @Test
    void keepsUnsignedTickJumps() throws IOException {
        NbsWriter writer = new NbsWriter();
        writer.header(5, 16, 40_000, 1, "Long song", 1000);
        writer.loopData();
        writer.note(40_000, 1, 0, 45, true);
        writer.endLayerAndSong();

        Song song = parse(writer, "long.nbs");

        assertEquals(39_999, song.notes.getFirst().tick());
        assertEquals(40_000, song.length);
    }

    private Song parse(NbsWriter writer, String fileName) throws IOException {
        Path path = temporaryDirectory.resolve(fileName);
        Files.write(path, writer.bytes());
        return SongLoader.parseSong(path);
    }

    private static final class NbsWriter {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        void header(int version, int vanillaInstruments, int length, int height, String name, int tempo) {
            writeShort(0);
            writeByte(version);
            writeByte(vanillaInstruments);
            if (version >= 3) writeShort(length);
            writeShort(height);
            writeString(name);
            writeString("Author");
            writeString("Original author");
            writeString("Description");
            writeShort(tempo);
            writeByte(0);
            writeByte(10);
            writeByte(4);
            for (int i = 0; i < 5; i++) writeInt(0);
            writeString("");
        }

        void legacyHeader(int length, int height, String name, int tempo) {
            writeShort(length);
            writeShort(height);
            writeString(name);
            writeString("Author");
            writeString("Original author");
            writeString("Description");
            writeShort(tempo);
            writeByte(0);
            writeByte(10);
            writeByte(4);
            for (int i = 0; i < 5; i++) writeInt(0);
            writeString("");
        }

        void loopData() {
            writeByte(0);
            writeByte(0);
            writeShort(0);
        }

        void note(int tickJump, int layerJump, int instrument, int key, boolean extendedFields) {
            writeShort(tickJump);
            noteInSameTick(layerJump, instrument, key, extendedFields);
        }

        void noteInSameTick(int layerJump, int instrument, int key, boolean extendedFields) {
            writeShort(layerJump);
            writeByte(instrument);
            writeByte(key);
            if (extendedFields) {
                writeByte(100);
                writeByte(100);
                writeShort(0);
            }
        }

        void endLayerAndSong() {
            writeShort(0);
            writeShort(0);
        }

        byte[] bytes() {
            return output.toByteArray();
        }

        private void writeString(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            output.writeBytes(bytes);
        }

        private void writeByte(int value) {
            output.write(value & 0xFF);
        }

        private void writeShort(int value) {
            writeByte(value);
            writeByte(value >>> 8);
        }

        private void writeInt(int value) {
            writeByte(value);
            writeByte(value >>> 8);
            writeByte(value >>> 16);
            writeByte(value >>> 24);
        }
    }
}
