package net.shiroha233.roadweaverpg.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.shiroha233.roadweaverpg.client.config.ConfigScreenBuilder;

public class RoadWeaverModMenuApi implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreenBuilder::create;
    }
}
