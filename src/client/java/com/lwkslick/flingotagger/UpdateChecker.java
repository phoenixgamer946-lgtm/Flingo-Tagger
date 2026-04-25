package com.lwkslick.flingotagger;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateChecker {
    private static final String MODRINTH_API =
            "https://api.modrinth.com/v2/project/flingo-tagger/version?loaders=[%22fabric%22]&game_versions=[%221.21.11%22]";
    private static final String CURRENT_VERSION = "1.0.0";
    private static final AtomicBoolean notified = new AtomicBoolean(false);
    private static String latestVersion = null;

    public static void check() {
        FlingoTaggerClient.getHttpClient().sendAsync(
                HttpRequest.newBuilder().uri(URI.create(MODRINTH_API)).build(),
                HttpResponse.BodyHandlers.ofString()
        ).thenAccept(resp -> {
            try {
                String body = resp.body();
                int vi = body.indexOf("\"version_number\":\"");
                if (vi == -1) return;
                String ver = body.substring(vi + 18, body.indexOf("\"", vi + 18));
                if (!ver.equals(CURRENT_VERSION)) {
                    latestVersion = ver;
                    scheduleNotify();
                }
            } catch (Exception ignored) {}
        });
    }

    private static void scheduleNotify() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (notified.get()) return;
            if (mc.player == null) return;
            notified.set(true);
            Component msg = Component.literal("[FlingoTagger] ")
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("Update available: v" + latestVersion + " — ")
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("[Download]")
                            .withStyle(s -> s.withColor(0x55FFFF)
                                    .withClickEvent(new ClickEvent.OpenUrl(
                                            URI.create("https://modrinth.com/mod/flingo-tagger")
                                    ))));
            mc.player.displayClientMessage(msg, false);
        });
    }
}