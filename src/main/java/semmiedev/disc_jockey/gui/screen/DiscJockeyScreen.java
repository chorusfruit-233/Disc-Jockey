package semmiedev.disc_jockey.gui.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NonNull;
import semmiedev.disc_jockey.*;
import semmiedev.disc_jockey.gui.SongListWidget;
import semmiedev.disc_jockey.gui.SongTimeSliderWidget;
import semmiedev.disc_jockey.gui.hud.BlocksOverlay;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DiscJockeyScreen extends Screen {
    private static final MutableComponent
            SELECT_SONG = Component.translatable(Main.MOD_ID + ".screen.select_song"),
            PLAY = Component.translatable(Main.MOD_ID + ".screen.play"),
            PLAY_STOP = Component.translatable(Main.MOD_ID + ".screen.play.stop"),
            PREVIEW = Component.translatable(Main.MOD_ID + ".screen.preview"),
            PREVIEW_STOP = Component.translatable(Main.MOD_ID + ".screen.preview.stop"),
            DROP_HINT = Component.translatable(Main.MOD_ID + ".screen.drop_hint").copy().withStyle(ChatFormatting.GRAY),
            SONG_STATE_PLAYING = Component.translatable(Main.MOD_ID + ".screen.songstate.playing").withStyle(style -> style.withItalic(true).withColor(0xDDDDDD)),
            SONG_STATE_PAUSED = Component.translatable(Main.MOD_ID + ".screen.songstate.paused").withStyle(style -> style.withItalic(true).withColor(0xDDDDDD)),
            SONG_STATE_FINISHED = Component.translatable(Main.MOD_ID + ".screen.songstate.finished").withStyle(style -> style.withItalic(true).withColor(0xDDDDDD)),
            SONG_STATE_STOPPED = Component.translatable(Main.MOD_ID + ".screen.songstate.stopped").withStyle(style -> style.withItalic(true).withColor(0xDDDDDD)),
            SONG_STATE_TUNING = Component.translatable(Main.MOD_ID + ".screen.songstate.tuning").withStyle(style -> style.withItalic(true).withColor(0xDDDDDD)),
            PLEASE_SELECT_SONG = Component.translatable(Main.MOD_ID + ".screen.please_select_song").withStyle(style -> style.withItalic(true)),
            CONFIG = Component.translatable(Main.MOD_ID + ".screen.config"),
            LOADING = Component.translatable(Main.MOD_ID + ".screen.loading").withStyle(ChatFormatting.GRAY),
            NO_SONGS = Component.translatable(Main.MOD_ID + ".screen.no_songs").withStyle(ChatFormatting.GRAY),
            NO_MATCHES = Component.translatable(Main.MOD_ID + ".screen.no_matches").withStyle(ChatFormatting.GRAY)
    ;

    private StringWidget songTitle;
    private StringWidget songState;
    private CycleButton<Boolean> playPauseButton;
    private SongTimeSliderWidget timeBar;

    private SongListWidget songListWidget;
    private StringWidget listStatus;
    private Button playButton, previewButton;
    private boolean shouldFilter;
    private String query = "";
    private long lastSongRevision = -1;

    public DiscJockeyScreen() {
        super(Main.NAME);
    }

    @Override
    protected void init() {
        shouldFilter = true;

        songListWidget = new SongListWidget(minecraft, width / 2 - 10, height - 64 - 32, 32, 20);
        songListWidget.setX(width / 2);
        addRenderableWidget(songListWidget);

        List<SongListWidget.SongEntry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < SongLoader.SONGS.size(); i++) {
            Song song = SongLoader.SONGS.get(i);
            song.entry.songListWidget = songListWidget;
            if (song.entry.selected) songListWidget.setSelected(song.entry);
            entries.add(song.entry);
        }
        songListWidget.replaceEntries(entries);
        lastSongRevision = SongLoader.revision;

        listStatus = new StringWidget(width / 2 + 10, height / 2 - 10, width / 2 - 30, 20, Component.empty(), font);
        addRenderableWidget(listStatus);

        // Right panel buttons layout - dynamically centered
        int rightCenter = width / 2 + (width / 2 - 10) / 2;
        int btnY = height - 61;
        int btnW = Math.min(100, (width / 2 - 10 - 20) / 3);
        int gap = Math.min(10, (width / 2 - 10 - btnW * 3) / 2);
        int btnStart = rightCenter - (btnW * 3 + gap * 2) / 2;

        playButton = Button.builder(PLAY, _ -> {
            if (Main.SONG_PLAYER.running) {
                Main.SONG_PLAYER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelected();
                if (entry != null) {
                    Main.SONG_PLAYER.start(entry.song);
                }
            }
        }).bounds(btnStart, btnY, btnW, 20).build();
        addRenderableWidget(playButton);

        previewButton = Button.builder(PREVIEW, _ -> {
            if (Main.PREVIEWER.running) {
                Main.PREVIEWER.stop();
            } else {
                SongListWidget.SongEntry entry = songListWidget.getSelected();
                if (entry != null) Main.PREVIEWER.start(entry.song);
            }
        }).bounds(btnStart + btnW + gap, btnY, btnW, 20).build();
        addRenderableWidget(previewButton);

        addRenderableWidget(Button.builder(Component.translatable(Main.MOD_ID + ".screen.blocks"), _ -> {
            if (BlocksOverlay.itemStacks == null) {
                SongListWidget.SongEntry entry = songListWidget.getSelected();
                if (entry != null) {
                    minecraft.gui.setScreen(null);

                    BlocksOverlay.itemStacks = new ItemStack[0];
                    BlocksOverlay.amounts = new int[0];
                    BlocksOverlay.amountOfNoteBlocks = entry.song.uniqueNotes.size();

                    for (Note note : entry.song.uniqueNotes) {
                        if (Note.instrumentBlock(note.instrument()) == null) continue;
                        ItemStack itemStack = Note.instrumentBlock(note.instrument()).asItem().getDefaultInstance();
                        int index = -1;

                        for (int i = 0; i < BlocksOverlay.itemStacks.length; i++) {
                            if (BlocksOverlay.itemStacks[i].getItem() == itemStack.getItem()) {
                                index = i;
                                break;
                            }
                        }

                        if (index == -1) {
                            BlocksOverlay.itemStacks = Arrays.copyOf(BlocksOverlay.itemStacks, BlocksOverlay.itemStacks.length + 1);
                            BlocksOverlay.amounts = Arrays.copyOf(BlocksOverlay.amounts, BlocksOverlay.amounts.length + 1);

                            BlocksOverlay.itemStacks[BlocksOverlay.itemStacks.length - 1] = itemStack;
                            BlocksOverlay.amounts[BlocksOverlay.amounts.length - 1] = 1;
                        } else {
                            BlocksOverlay.amounts[index] = BlocksOverlay.amounts[index] + 1;
                        }
                    }
                }
            } else {
                BlocksOverlay.itemStacks = null;
                minecraft.gui.setScreen(null);
            }
        }).bounds(btnStart + (btnW + gap) * 2, btnY, btnW, 20).build());

        int utilityButtonWidth = 55;
        int utilityGap = 6;
        int utilityRowWidth = Math.min(width / 2 - 20, 150 + utilityGap * 2 + utilityButtonWidth * 2);
        int utilityStart = rightCenter - utilityRowWidth / 2;
        int searchW = utilityRowWidth - utilityGap * 2 - utilityButtonWidth * 2;
        EditBox searchBar = new EditBox(font, utilityStart, height - 31, searchW, 20, Component.translatable(Main.MOD_ID + ".screen.search"));
        searchBar.setHint(Component.translatable(Main.MOD_ID + ".screen.search"));
        searchBar.setResponder(query -> {
            query = query.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
            if (this.query.equals(query)) return;
            this.query = query;
            shouldFilter = true;
        });
        addRenderableWidget(searchBar);
        addRenderableWidget(Button.builder(Component.translatable(Main.MOD_ID + ".screen.reload"), _ -> {
            SongLoader.showToast = true;
            SongLoader.loadSongs();
        }).bounds(utilityStart + searchW + utilityGap, height - 31, utilityButtonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(Main.MOD_ID + ".screen.open_folder"), _ ->
                Util.getPlatform().openPath(Main.songsFolder)
        ).bounds(utilityStart + searchW + utilityGap * 2 + utilityButtonWidth, height - 31, utilityButtonWidth, 20).build());

        int leftX = 10;
        int leftWidth = width / 2 - 20;
        int topY = 32;

        songState = new StringWidget(leftX, topY, leftWidth, 20, Component.empty(), font);
        addRenderableWidget(songState);

        songTitle = new StringWidget(leftX, topY + 20, leftWidth, 20, Component.empty(), font);
        addRenderableWidget(songTitle);

        timeBar = new SongTimeSliderWidget(leftX, topY + 40, leftWidth, 30);
        addRenderableWidget(timeBar);

        int controlsY = topY + 40 + 30 + 5;
        playPauseButton = CycleButton.builder(
                (value) -> Component.literal(value ? "⏸" : "▶"),
                Main.SONG_PLAYER.running
            )
            .displayOnlyValue()
            .withValues(true, false)
            .create((width / 4) - 25, controlsY, 20, 20, Component.empty(), (_, value) -> {
                if (value) Main.SONG_PLAYER.resume();
                else Main.SONG_PLAYER.pause();
            });
        addRenderableWidget(playPauseButton);

        Button stopButton = Button.builder(Component.literal("⏹"), _ -> Main.SONG_PLAYER.stop())
                .pos((width / 4) + 5, controlsY)
                .size(20, 20)
                .build();
        addRenderableWidget(stopButton);

        // Config button in bottom left
        Button configButton = Button.builder(CONFIG, _ ->
                minecraft.gui.setScreen(me.shedaniel.autoconfig.AutoConfigClient.getConfigScreen(Config.class, this).get())
        ).pos(10, height - 30).size(100, 20).build();
        addRenderableWidget(configButton);
    }

    private static Component getPlaybackStateText() {
        boolean running = Main.SONG_PLAYER.running;
        boolean tuned = Main.SONG_PLAYER.tuned;
        boolean didSongReachEnd = Main.SONG_PLAYER.didSongReachEnd;

        if (!running) {
            if (didSongReachEnd) {
                return SONG_STATE_FINISHED;
            } else if (Main.SONG_PLAYER.getSongElapsedSeconds() == 0.0) {
                return SONG_STATE_STOPPED;
            } else {
                return SONG_STATE_PAUSED;
            }
        } else {
            if (!tuned) {
                return SONG_STATE_TUNING;
            } else {
                return SONG_STATE_PLAYING;
            }
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        context.fill(5, 32, width / 2, 32 + 20 + 20 + 30 + 5 + 20 + 5, 0x3F000000);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractRenderState(context, mouseX, mouseY, delta);

        int rightCenter = width / 2 + (width / 2 - 10) / 2;
        context.centeredText(font, DROP_HINT, width / 2, 5, 0xFFFFFFFF);
        context.centeredText(font, SELECT_SONG, rightCenter, 20, 0xFFFFFFFF);
    }

    @Override
    public void tick() {
        songState.setMessage(getPlaybackStateText());
        timeBar.update();
        playPauseButton.setValue(Main.SONG_PLAYER.running);
        songTitle.setMessage(Main.SONG_PLAYER.song != null ? Component.literal(Main.SONG_PLAYER.song.displayName) : PLEASE_SELECT_SONG);

        previewButton.setMessage(Main.PREVIEWER.running ? PREVIEW_STOP : PREVIEW);
        playButton.setMessage(Main.SONG_PLAYER.running ? PLAY_STOP : PLAY);

        if (SongLoader.revision != lastSongRevision) {
            lastSongRevision = SongLoader.revision;
            shouldFilter = true;
        }

        if (shouldFilter) {
            shouldFilter = false;
            songListWidget.setScrollAmount(0);
            List<SongListWidget.SongEntry> newEntries = new java.util.ArrayList<>();
            boolean empty = query.isEmpty();
            int favoriteIndex = 0;
            for (Song song : SongLoader.SONGS) {
                if (empty || song.searchableFileName.contains(query) || song.searchableName.contains(query)) {
                    song.entry.songListWidget = songListWidget;
                    if (song.entry.favorite) {
                        newEntries.add(favoriteIndex++, song.entry);
                    } else {
                        newEntries.add(song.entry);
                    }
                }
            }
            songListWidget.replaceEntries(newEntries);
            updateListStatus(newEntries.isEmpty());
        }

        if (SongLoader.loadingSongs) updateListStatus(true);

        boolean hasSelection = songListWidget.getSelected() != null;
        playButton.active = Main.SONG_PLAYER.running || hasSelection;
        previewButton.active = Main.PREVIEWER.running || hasSelection;
    }

    private void updateListStatus(boolean filteredListEmpty) {
        if (SongLoader.loadingSongs) {
            listStatus.setMessage(LOADING);
            listStatus.visible = true;
        } else if (filteredListEmpty) {
            listStatus.setMessage(query.isEmpty() ? NO_SONGS : NO_MATCHES);
            listStatus.visible = true;
        } else {
            listStatus.visible = false;
        }
    }

    @Override
    public void onFilesDrop(List<Path> paths) {
        String string = paths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
        if (string.length() > 300) string = string.substring(0, 300) + "...";

        minecraft.gui.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                paths.forEach(path -> {
                    Path target = null;
                    boolean copied = false;
                    try {
                        File file = path.toFile();

                        if (SongLoader.SONGS.stream().anyMatch(input -> input.fileName.equalsIgnoreCase(file.getName()))) return;

                        target = Main.songsFolder.resolve(file.getName());
                        if (!path.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize())) {
                            Files.copy(path, target);
                            copied = true;
                        }

                        Song song = SongLoader.loadSong(target.toFile());
                        if (song != null) {
                            SongLoader.addSong(song);
                        } else if (copied) {
                            Files.deleteIfExists(target);
                        }
                    } catch (Exception exception) {
                        if (copied && target != null) {
                            try {
                                Files.deleteIfExists(target);
                            } catch (IOException cleanupException) {
                                exception.addSuppressed(cleanupException);
                            }
                        }
                        Main.LOGGER.warn("Failed to import song file {} into {}", path, Main.songsFolder, exception);
                    }
                });
            }
            minecraft.gui.setScreen(this);
        }, Component.translatable(Main.MOD_ID + ".screen.drop_confirm"), Component.literal(string)));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        super.onClose();
        Thread.startVirtualThread(() -> Main.configHolder.save());
    }
}
