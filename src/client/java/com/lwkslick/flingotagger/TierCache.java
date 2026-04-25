// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger;

import com.lwkslick.flingotagger.model.GameMode;
import com.lwkslick.flingotagger.model.PlayerInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TierCache {
    // Keyed by lowercase username
    private static final Map<String, Optional<Map<String, PlayerInfo.Ranking>>> TIERS = new ConcurrentHashMap<>();
    // Populated from the first Firestore response's tiers map keys
    private static final List<GameMode> GAMEMODES = new ArrayList<>();

    public static void init() {
        // No remote gamemode fetch needed — modes come from Firestore tiers map keys
        FlingoTaggerClient.LOGGER.info("FlingoTagger TierCache initialised (Firestore backend)");
    }

    public static List<GameMode> getGamemodes() {
        return GAMEMODES.isEmpty() ? Collections.singletonList(GameMode.NONE) : GAMEMODES;
    }

    /** Register gamemodes discovered from a Firestore tiers map. */
    public static void registerGamemodes(Set<String> ids) {
        for (String id : ids) {
            if (GAMEMODES.stream().noneMatch(m -> m.id().equalsIgnoreCase(id))) {
                GAMEMODES.add(new GameMode(id, id));
            }
        }
    }

    /** Look up rankings by username (case-insensitive). Triggers async fetch if not cached. */
    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(String username) {
        String key = username.toLowerCase(Locale.ROOT);
        return TIERS.computeIfAbsent(key, ignored -> {
            PlayerInfo.fetchByName(
                    FlingoTaggerClient.getHttpClient(),
                    FlingoTaggerClient.GSON,
                    FlingoTaggerClient.LOGGER,
                    username
            ).thenAccept(info -> {
                if (info != null) {
                    registerGamemodes(info.rankings().keySet());
                    TIERS.put(key, Optional.of(info.rankings()));
                } else {
                    TIERS.put(key, Optional.empty());
                }
            });
            return Optional.empty();
        });
    }

    /** Explicit search (used by /flingotagger command). Always fetches fresh. */
    public static CompletableFuture<PlayerInfo> searchPlayer(String username) {
        return PlayerInfo.fetchByName(
                FlingoTaggerClient.getHttpClient(),
                FlingoTaggerClient.GSON,
                FlingoTaggerClient.LOGGER,
                username
        ).thenApply(info -> {
            if (info == null) throw new RuntimeException("Player not found: " + username);
            String key = username.toLowerCase(Locale.ROOT);
            registerGamemodes(info.rankings().keySet());
            TIERS.put(key, Optional.of(info.rankings()));
            return info;
        });
    }

    public static void clearCache() {
        TIERS.clear();
    }

    public static GameMode findNextMode(GameMode current) {
        if (GAMEMODES.isEmpty()) return GameMode.NONE;
        return GAMEMODES.get((GAMEMODES.indexOf(current) + 1) % GAMEMODES.size());
    }

    public static Optional<GameMode> findMode(String id) {
        return GAMEMODES.stream().filter(m -> m.id().equalsIgnoreCase(id)).findFirst();
    }

    public static GameMode findModeOrUgly(String id) {
        return findMode(id).orElseGet(() -> new GameMode(id, id));
    }

    private TierCache() {}
}