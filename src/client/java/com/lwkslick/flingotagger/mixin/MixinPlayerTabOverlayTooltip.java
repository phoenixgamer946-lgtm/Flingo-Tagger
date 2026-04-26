// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
package com.lwkslick.flingotagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.lwkslick.flingotagger.FlingoTaggerClient;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlayTooltip {
    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    @Nullable
    public Component addHoverTooltip(Component original, @Local PlayerInfo entry) {
        if (!FlingoTaggerClient.getManager().getConfig().isEnabled()) return original;
        if (!FlingoTaggerClient.getManager().getConfig().isShowHoverTooltip()) return original;
        Component tooltip = FlingoTaggerClient.buildHoverTooltip(entry.getProfile().id());
        if (tooltip == null) return original;
        return original.copy().withStyle(s -> s.withHoverEvent(
                new net.minecraft.network.chat.HoverEvent.ShowText(tooltip)
        ));
    }
}