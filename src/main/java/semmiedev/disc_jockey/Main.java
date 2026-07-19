package semmiedev.disc_jockey;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;
import semmiedev.disc_jockey.gui.hud.BlocksOverlay;
import semmiedev.disc_jockey.gui.screen.DiscJockeyScreen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main implements ClientModInitializer {
    public static final String MOD_ID = "disc_jockey";
    public static final MutableComponent NAME = Component.literal("Disc Jockey");
    public static final Logger LOGGER = LogManager.getLogger("Disc Jockey");
    public static final CopyOnWriteArrayList<ClientTickEvents.StartLevelTick> TICK_LISTENERS = new CopyOnWriteArrayList<>();
    public static final Previewer PREVIEWER = new Previewer();
    public static final SongPlayer SONG_PLAYER = new SongPlayer();

    public static Path songsFolder;
    public static Config config;
    public static ConfigHolder<Config> configHolder;

    @Override
    public void onInitializeClient() {
        configHolder = AutoConfig.register(Config.class, JanksonConfigSerializer::new);
        config = configHolder.getConfig();

        songsFolder = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("songs");
        try {
            Files.createDirectories(songsFolder);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create Disc Jockey song directory " + songsFolder, exception);
        }

        SongLoader.loadSongs();

        KeyMapping openScreenKeyBind = KeyMappingHelper.registerKeyMapping(new KeyMapping(MOD_ID + ".key_bind.open_screen", GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));

        ClientTickEvents.START_CLIENT_TICK.register(new ClientTickEvents.StartTick() {
            private ClientLevel prevLevel;

            @Override
            public void onStartTick(@NonNull Minecraft client) {
                if (prevLevel != client.level) {
                    PREVIEWER.stop();
                    SONG_PLAYER.stop();
                    BlocksOverlay.hide();
                }
                prevLevel = client.level;

                if (openScreenKeyBind.consumeClick()) {
                    client.gui.setScreen(new DiscJockeyScreen());
                }
            }
        });

        ClientTickEvents.START_LEVEL_TICK.register(world -> {
            for (ClientTickEvents.StartLevelTick listener : TICK_LISTENERS) listener.onStartTick(world);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> DiscjockeyCommand.register(dispatcher));

        ClientLoginConnectionEvents.DISCONNECT.register((_, _) -> {
            PREVIEWER.stop();
            SONG_PLAYER.stop();
            BlocksOverlay.hide();
        });

        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(Main.MOD_ID, "blocks_overlay"), BlocksOverlay::render);
    }
}
