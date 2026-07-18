package semmiedev.disc_jockey;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigManager;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.autoconfig.gui.DefaultGuiProviders;
import me.shedaniel.autoconfig.gui.DefaultGuiTransformers;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigManager<Config> manager = (ConfigManager<Config>) AutoConfig.getConfigHolder(Config.class);
            GuiRegistry registry = new GuiRegistry();
            DefaultGuiProviders.apply(registry);
            DefaultGuiTransformers.apply(registry);
            ConfigScreenProvider<Config> provider = new ConfigScreenProvider<>(manager, registry, parent);
            return provider.get();
        };
    }
}
