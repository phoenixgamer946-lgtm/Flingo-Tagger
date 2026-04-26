// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
package com.lwkslick.flingotagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.lwkslick.flingotagger.FlingoTaggerClient;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChatListener.class)
public class MixinChatHud {
    @ModifyReturnValue(
            method = "decorateChatMessage",
            at = @At("RETURN")
    )
    private Component injectChatTier(Component original, PlayerChatMessage message) {
        return FlingoTaggerClient.appendChatTier(message.link().sender(), original);
    }
}