package semmiedev.disc_jockey.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import semmiedev.disc_jockey.Main;
import semmiedev.disc_jockey.Song;

public class SongListWidget extends ObjectSelectionList<SongListWidget.SongEntry> {

    public SongListWidget(Minecraft client, int width, int height, int top, int itemHeight) {
        super(client, width, height, top, itemHeight);
    }

    @Override
    public int getRowWidth() {
        return width - 40;
    }

    @Override
    protected int scrollBarX() {
        return width - 12;
    }

    @Override
    public void setSelected(@Nullable SongListWidget.SongEntry entry) {
        SongListWidget.SongEntry selectedEntry = getSelected();
        if (selectedEntry != null) selectedEntry.selected = false;
        if (entry != null) entry.selected = true;
        super.setSelected(entry);
    }

    @Override
    public void updateWidgetNarration(@NonNull NarrationElementOutput builder) {
        // Who cares
    }

    // TODO: 6/2/2022 Add a delete icon
    public static class SongEntry extends Entry<SongEntry> {
        private static final Identifier ICONS = Identifier.fromNamespaceAndPath(Main.MOD_ID, "textures/gui/icons.png");

        public final int index;
        public final Song song;

        public boolean selected, favorite;
        public SongListWidget songListWidget;

        private final Minecraft client = Minecraft.getInstance();

        private int x;
        private int y;

        public SongEntry(Song song, int index) {
            this.song = song;
            this.index = index;
        }

        @Override
        public @NonNull Component getNarration() {
            return Component.empty();
        }

        @Override
        public void extractContent(@NonNull GuiGraphicsExtractor context, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.x = getX(); this.y = getY();
            int entryWidth = getWidth();
            int entryHeight = getHeight();

            if (selected) {
                context.fill(x, y, x + entryWidth, y + entryHeight, 0xFFFFFFFF);
                context.fill(x + 1, y + 1, x + entryWidth - 1, y + entryHeight - 1, 0xFF000000);
            }

            context.centeredText(client.font, song.displayName, x + entryWidth / 2, y + 5, selected ? 0xFFFFFFFF : 0xFF808080);

            int u = (favorite ? 26 : 0) + (isOverFavoriteButton(mouseX, mouseY) ? 13 : 0);
            context.blit(RenderPipelines.GUI_TEXTURED, ICONS, x + 2, y + 2, (float)u, 0.0f, 13, 12, 52, 12);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean something) {
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX > x + 2 && mouseX < x + 15 && mouseY > y + 2 && mouseY < y + 14) {
                favorite = !favorite;
                if (favorite) {
                    Main.config.favorites.add(song.fileName);
                } else {
                    Main.config.favorites.remove(song.fileName);
                }
                return true;
            }
            songListWidget.setSelected(this);
            return true;
        }

        private boolean isOverFavoriteButton(int mouseX, int mouseY) {
            return mouseX > x + 2 && mouseX < x + 15 && mouseY > y + 2 && mouseY < y + 14;
        }
    }
}
