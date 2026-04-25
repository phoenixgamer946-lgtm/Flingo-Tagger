// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.network.chat.Component;
import net.uku3lig.ukulib.config.option.ColorOption;
import net.uku3lig.ukulib.config.option.CyclingOption;
import net.uku3lig.ukulib.config.option.WidgetCreator;
import net.uku3lig.ukulib.config.option.widget.ButtonTab;
import net.uku3lig.ukulib.config.screen.TabbedConfigScreen;

public class FlingoConfigScreen extends TabbedConfigScreen<FlingoConfig> {

    public FlingoConfigScreen(Screen parent) {
        super("flingotagger.config.title", parent, FlingoTaggerClient.getManager());
    }

    @Override
    protected Tab[] getTabs(FlingoConfig config) {
        return new Tab[]{
                new GeneralTab(config),
                new ColorsTab(config),
        };
    }

    private class GeneralTab extends ButtonTab<FlingoConfig> {
        public GeneralTab(FlingoConfig config) {
            super(Component.translatable("flingotagger.config.tab.general"), FlingoTaggerClient.getManager());
        }

        @Override
        protected WidgetCreator[] getWidgets(FlingoConfig config) {
            return new WidgetCreator[]{
                    CyclingOption.ofBoolean(
                            "flingotagger.config.enabled",
                            config.isEnabled(),
                            config::setEnabled
                    ),
                    CyclingOption.ofBoolean(
                            "flingotagger.config.playerList",
                            config.isPlayerList(),
                            config::setPlayerList
                    ),
                    CyclingOption.ofBoolean(
                            "flingotagger.config.showIcons",
                            config.isShowIcons(),
                            config::setShowIcons
                    ),
                    CyclingOption.ofBoolean(
                            "flingotagger.config.showRetired",
                            config.isShowRetired(),
                            config::setShowRetired
                    ),
                    CyclingOption.ofTranslatableEnum(
                            "flingotagger.config.highestMode",
                            FlingoConfig.HighestMode.class,
                            config.getHighestMode(),
                            config::setHighestMode
                    ),
                    new CyclingOption<>(
                            "flingotagger.config.gameMode",
                            TierCache.getGamemodes(),
                            config.getGameMode(),
                            gm -> config.setGameMode(gm.id()),
                            gm -> net.minecraft.network.chat.Component.literal(gm.title())
                    ),
            };
        }
    }

    private class ColorsTab extends ButtonTab<FlingoConfig> {
        public ColorsTab(FlingoConfig config) {
            super(Component.translatable("flingotagger.config.tab.colors"), FlingoTaggerClient.getManager());
        }

        @Override
        protected WidgetCreator[] getWidgets(FlingoConfig config) {
            return new WidgetCreator[]{
                    new ColorOption("flingotagger.color.retired",
                            config.getRetiredColor(),
                            config::setRetiredColor),
                    new ColorOption("flingotagger.color.HT1",
                            config.getTierColors().getOrDefault("HT1", 0xe8ba3a),
                            c -> config.getTierColors().put("HT1", c)),
                    new ColorOption("flingotagger.color.LT1",
                            config.getTierColors().getOrDefault("LT1", 0xd5b355),
                            c -> config.getTierColors().put("LT1", c)),
                    new ColorOption("flingotagger.color.HT2",
                            config.getTierColors().getOrDefault("HT2", 0xc4d3e7),
                            c -> config.getTierColors().put("HT2", c)),
                    new ColorOption("flingotagger.color.LT2",
                            config.getTierColors().getOrDefault("LT2", 0xa0a7b2),
                            c -> config.getTierColors().put("LT2", c)),
                    new ColorOption("flingotagger.color.HT3",
                            config.getTierColors().getOrDefault("HT3", 0xf89f5a),
                            c -> config.getTierColors().put("HT3", c)),
                    new ColorOption("flingotagger.color.LT3",
                            config.getTierColors().getOrDefault("LT3", 0xc67b42),
                            c -> config.getTierColors().put("LT3", c)),
                    new ColorOption("flingotagger.color.HT4",
                            config.getTierColors().getOrDefault("HT4", 0x81749a),
                            c -> config.getTierColors().put("HT4", c)),
                    new ColorOption("flingotagger.color.LT4",
                            config.getTierColors().getOrDefault("LT4", 0x655b79),
                            c -> config.getTierColors().put("LT4", c)),
                    new ColorOption("flingotagger.color.HT5",
                            config.getTierColors().getOrDefault("HT5", 0x8f82a8),
                            c -> config.getTierColors().put("HT5", c)),
                    new ColorOption("flingotagger.color.LT5",
                            config.getTierColors().getOrDefault("LT5", 0x655b79),
                            c -> config.getTierColors().put("LT5", c)),
            };
        }
    }
}