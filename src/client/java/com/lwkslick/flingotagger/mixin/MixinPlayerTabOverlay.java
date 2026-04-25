// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
package com.lwkslick.flingotagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.lwkslick.flingotagger.FlingoConfig;
import com.lwkslick.flingotagger.FlingoTaggerClient;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerTabOverlay.class)
public class MixinPlayerTabOverlay {
    @ModifyReturnValue(method = "getNameForDisplay", at = @At("RETURN"))
    @Nullable
    public Component prependTier(Component original, @Local PlayerInfo entry) {
        FlingoConfig config = FlingoTaggerClient.getManager().getConfig();
        if (config.isEnabled() && config.isPlayerList()) {
            return FlingoTaggerClient.appendTier(entry.getProfile().id(), original);
        } else {
            return original;
        }
    }
}