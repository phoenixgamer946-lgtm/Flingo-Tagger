// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
package com.lwkslick.flingotagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.lwkslick.flingotagger.FlingoTaggerClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class MixinPlayer {
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    public Component prependTier(Component original) {
        if (FlingoTaggerClient.getManager().getConfig().isEnabled() && FlingoTaggerClient.getManager().getConfig().isShowNametag()) {
            Player self = (Player) (Object) this;
            return FlingoTaggerClient.appendTier(self.getUUID(), original);
        } else {
            return original;
        }
    }
}