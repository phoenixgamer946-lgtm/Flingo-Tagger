// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger.model;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record GameMode(String id, String title) {
    public static final GameMode NONE = new GameMode("__none__", "§cNone§r");

    public static CompletableFuture<List<GameMode>> fetchGamemodes(HttpClient client, Gson gson, String apiUrl) {
        String endpoint = apiUrl + "/v2/mode/list";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(r -> {
                    JsonObject obj = gson.fromJson(r.body(), JsonObject.class);
                    return obj.entrySet().stream().map(e -> {
                        String title = e.getValue().getAsJsonObject().get("title").getAsString();
                        return new GameMode(e.getKey(), title);
                    }).toList();
                });
    }

    public boolean isNone() {
        return this.id.equals(NONE.id);
    }

    // !! SWAP icon mappings once Flingo gamemode IDs are confirmed !!
    // Currently using MCTiers icon unicode slots as placeholders for testing only.
    // These textures MUST be replaced with original Flingo icons before public release.
    private IconData iconAndColor() {
        return switch (this.id) {
            case "axe"              -> new IconData('\uE701', TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case "mace"             -> new IconData('\uE702', TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case "nethop", "neth_pot" -> new IconData('\uE703', TextColor.fromRgb(0x7d4a40));
            case "pot"              -> new IconData('\uE704', TextColor.fromRgb(0xff0000));
            case "smp"              -> new IconData('\uE705', TextColor.fromRgb(0xeccb45));
            case "sword"            -> new IconData('\uE706', TextColor.fromRgb(0xa4fdf0));
            case "uhc"              -> new IconData('\uE707', TextColor.fromLegacyFormat(ChatFormatting.RED));
            case "vanilla"          -> new IconData('\uE708', TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE));
            case "bed"              -> new IconData('\uE801', TextColor.fromRgb(0xff0000));
            case "bow"              -> new IconData('\uE802', TextColor.fromRgb(0x663d10));
            case "creeper"          -> new IconData('\uE803', TextColor.fromLegacyFormat(ChatFormatting.GREEN));
            case "debuff"           -> new IconData('\uE804', TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY));
            case "dia_crystal"      -> new IconData('\uE805', TextColor.fromLegacyFormat(ChatFormatting.AQUA));
            case "dia_smp"          -> new IconData('\uE806', TextColor.fromRgb(0x8c668b));
            case "elytra"           -> new IconData('\uE807', TextColor.fromRgb(0x8d8db1));
            case "manhunt"          -> new IconData('\uE808', TextColor.fromLegacyFormat(ChatFormatting.RED));
            case "minecart"         -> new IconData('\uE809', TextColor.fromLegacyFormat(ChatFormatting.GRAY));
            case "og_vanilla"       -> new IconData('\uE810', TextColor.fromLegacyFormat(ChatFormatting.GOLD));
            case "speed"            -> new IconData('\uE811', TextColor.fromRgb(0x43a9d1));
            case "trident"          -> new IconData('\uE812', TextColor.fromRgb(0x579b8c));
            default                 -> new IconData('•', TextColor.fromRgb(0xFFFFFF));
        };
    }

    public Optional<Character> icon() {
        IconData data = this.iconAndColor();
        return data.color().getValue() == 0xFFFFFF ? Optional.empty() : Optional.of(data.character());
    }

    public Component asStyled(boolean withDefaultDot) {
        IconData data = this.iconAndColor();
        if (data.color().getValue() == 0xFFFFFF && !withDefaultDot) {
            return Component.literal(this.title);
        }
        Component name = Component.literal(this.title).withStyle(s -> s.withColor(data.color()));
        return Component.literal(data.character() + " ").append(name);
    }

    private record IconData(char character, TextColor color) {}
}