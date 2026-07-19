package semmiedev.disc_jockey;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public record Note(int nbsInstrumentId, byte note) {
    private static final int NBS_INSTRUMENT_COUNT = 16;

    public static boolean isSupportedNbsId(int instrumentId, int vanillaInstrumentCount) {
        int supportedCount = Math.min(vanillaInstrumentCount, NBS_INSTRUMENT_COUNT);
        return instrumentId >= 0 && instrumentId < supportedCount;
    }

    public NoteBlockInstrument instrument() {
        return MinecraftInstruments.NBS[nbsInstrumentId];
    }

    public static Set<NoteBlockInstrument> playableInstruments() {
        return InstrumentBlocks.BLOCKS.keySet();
    }

    public static Block instrumentBlock(NoteBlockInstrument instrument) {
        return InstrumentBlocks.BLOCKS.get(instrument);
    }

    private static final class MinecraftInstruments {
        private static final NoteBlockInstrument[] NBS = new NoteBlockInstrument[]{
                NoteBlockInstrument.HARP,
                NoteBlockInstrument.BASS,
                NoteBlockInstrument.BASEDRUM,
                NoteBlockInstrument.SNARE,
                NoteBlockInstrument.HAT,
                NoteBlockInstrument.GUITAR,
                NoteBlockInstrument.FLUTE,
                NoteBlockInstrument.BELL,
                NoteBlockInstrument.CHIME,
                NoteBlockInstrument.XYLOPHONE,
                NoteBlockInstrument.IRON_XYLOPHONE,
                NoteBlockInstrument.COW_BELL,
                NoteBlockInstrument.DIDGERIDOO,
                NoteBlockInstrument.BIT,
                NoteBlockInstrument.BANJO,
                NoteBlockInstrument.PLING
        };
    }

    private static final class InstrumentBlocks {
        private static final Map<NoteBlockInstrument, Block> BLOCKS = create();

        private static Map<NoteBlockInstrument, Block> create() {
            EnumMap<NoteBlockInstrument, Block> blocks = new EnumMap<>(NoteBlockInstrument.class);
            blocks.put(NoteBlockInstrument.HARP, Blocks.AIR);
            blocks.put(NoteBlockInstrument.BASEDRUM, Blocks.STONE);
            blocks.put(NoteBlockInstrument.SNARE, Blocks.SAND);
            blocks.put(NoteBlockInstrument.HAT, Blocks.GLASS);
            blocks.put(NoteBlockInstrument.BASS, Blocks.OAK_PLANKS);
            blocks.put(NoteBlockInstrument.FLUTE, Blocks.CLAY);
            blocks.put(NoteBlockInstrument.BELL, Blocks.GOLD_BLOCK);
            blocks.put(NoteBlockInstrument.GUITAR, Blocks.WOOL.white());
            blocks.put(NoteBlockInstrument.CHIME, Blocks.PACKED_ICE);
            blocks.put(NoteBlockInstrument.XYLOPHONE, Blocks.BONE_BLOCK);
            blocks.put(NoteBlockInstrument.IRON_XYLOPHONE, Blocks.IRON_BLOCK);
            blocks.put(NoteBlockInstrument.COW_BELL, Blocks.SOUL_SAND);
            blocks.put(NoteBlockInstrument.DIDGERIDOO, Blocks.PUMPKIN);
            blocks.put(NoteBlockInstrument.BIT, Blocks.EMERALD_BLOCK);
            blocks.put(NoteBlockInstrument.BANJO, Blocks.HAY_BLOCK);
            blocks.put(NoteBlockInstrument.PLING, Blocks.GLOWSTONE);
            blocks.put(NoteBlockInstrument.TRUMPET, Blocks.COPPER_BLOCK.weathering().unaffected());
            blocks.put(NoteBlockInstrument.TRUMPET_EXPOSED, Blocks.COPPER_BLOCK.weathering().exposed());
            blocks.put(NoteBlockInstrument.TRUMPET_WEATHERED, Blocks.COPPER_BLOCK.weathering().weathered());
            blocks.put(NoteBlockInstrument.TRUMPET_OXIDIZED, Blocks.COPPER_BLOCK.weathering().oxidized());
            return Collections.unmodifiableMap(blocks);
        }
    }
}
