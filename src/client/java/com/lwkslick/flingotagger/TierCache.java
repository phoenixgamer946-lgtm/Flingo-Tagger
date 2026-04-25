// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger;

import com.lwkslick.flingotagger.model.GameMode;
import com.lwkslick.flingotagger.model.PlayerInfo;
import com.lwkslick.flingotagger.FlingoTaggerClient;
import com.lwkslick.flingotagger.FlingoConfig;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public class TierCache {
    private static final List<GameMode> GAMEMODES = new ArrayList<>();
    private static final Map<UUID, Optional<Map<String, PlayerInfo.Ranking>>> TIERS = new ConcurrentHashMap<>();

    public static void init() {
        try {
            GAMEMODES.clear();
            FlingoConfig cfg = FlingoTaggerClient.getManager().getConfig();
            GAMEMODES.addAll(GameMode.fetchGamemodes(
                    FlingoTaggerClient.getHttpClient(), FlingoTaggerClient.GSON, cfg.getApiUrl()).get());
            FlingoTaggerClient.LOGGER.info("Found {} gamemodes: {}", GAMEMODES.size(), GAMEMODES.stream().map(GameMode::id).toList());
        } catch (ExecutionException e) {
            FlingoTaggerClient.LOGGER.error("Failed to load gamemodes!", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static List<GameMode> getGamemodes() {
        return GAMEMODES.isEmpty() ? Collections.singletonList(GameMode.NONE) : GAMEMODES;
    }

    public static Optional<Map<String, PlayerInfo.Ranking>> getPlayerRankings(UUID uuid) {
        return TIERS.computeIfAbsent(uuid, _ -> {
            if (uuid.version() == 4) {
                FlingoConfig cfg = FlingoTaggerClient.getManager().getConfig();
                PlayerInfo.getRankings(FlingoTaggerClient.getHttpClient(), FlingoTaggerClient.GSON,
                                FlingoTaggerClient.LOGGER, cfg.getApiUrl(), uuid)
                        .thenAccept(info -> TIERS.put(uuid, Optional.ofNullable(info)));
            }
            return Optional.empty();
        });
    }

    public static CompletableFuture<PlayerInfo> searchPlayer(String query) {
        FlingoConfig cfg = FlingoTaggerClient.getManager().getConfig();
        return PlayerInfo.search(FlingoTaggerClient.getHttpClient(), FlingoTaggerClient.GSON,
                FlingoTaggerClient.LOGGER, cfg.getApiUrl(), query).thenApply(p -> {
            UUID uuid = parseUUID(p.uuid());
            TIERS.put(uuid, Optional.of(p.rankings()));
            return p;
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

    private static UUID parseUUID(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (Exception e) {
            long most = Long.parseUnsignedLong(uuid.substring(0, 16), 16);
            long least = Long.parseUnsignedLong(uuid.substring(16), 16);
            return new UUID(most, least);
        }
    }

    private TierCache() {}
}