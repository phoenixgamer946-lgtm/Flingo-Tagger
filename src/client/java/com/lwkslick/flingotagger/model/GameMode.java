// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger.model;

import net.minecraft.network.chat.Component;

import java.util.Optional;

public record GameMode(String id, String title) {
    public static final GameMode NONE = new GameMode("__none__", "§cNone§r");

    public boolean isNone() {
        return this.id.equals(NONE.id);
    }

    // !! SWAP icon mappings once Flingo gamemode IDs are confirmed !!
    // Currently using MCTiers icon unicode slots as placeholders for testing only.
    // These textures MUST be replaced with original Flingo icons before public release.
    public Optional<Character> icon() {
        char c = switch (this.id) {
            case "axe"     -> '\uE001';
            case "mace"    -> '\uE002';
            case "nethop"  -> '\uE003';
            case "pot"     -> '\uE004';
            case "smp"     -> '\uE005';
            case "spear"   -> '\uE006';
            case "sword"   -> '\uE007';
            case "uhc"     -> '\uE008';
            case "vanilla" -> '\uE009';
            default        -> 0;
        };
        return c == 0 ? Optional.empty() : Optional.of(c);
    }

    public Component asStyled(boolean withDefaultDot) {
        Optional<Character> ic = icon();
        Component name = Component.literal(this.title);
        if (ic.isPresent()) {
            return Component.literal(String.valueOf(ic.get()) + " ").append(name);
        }
        if (withDefaultDot) {
            return Component.literal("• ").append(name);
        }
        return name;
    }
}