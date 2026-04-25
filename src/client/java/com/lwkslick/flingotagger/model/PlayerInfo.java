// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.lwkslick.flingotagger.TierCache;
import org.jetbrains.annotations.Nullable;
import com.google.gson.Gson;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.UUID;

public record PlayerInfo(String uuid, String name, Map<String, Ranking> rankings) {

    public record Ranking(int tier, int pos,
                          @Nullable @SerializedName("peak_tier") Integer peakTier,
                          @Nullable @SerializedName("peak_pos") Integer peakPos,
                          long attained, boolean retired) {

        public int comparableTier() { return tier * 2 + pos; }

        public int comparablePeak() {
            if (peakTier == null || peakPos == null) return Integer.MAX_VALUE;
            return peakTier * 2 + peakPos;
        }

        public NamedRanking asNamed(GameMode mode) {
            return new NamedRanking(mode, this);
        }
    }

    public record NamedRanking(@Nullable GameMode mode, Ranking ranking) {}

    public static CompletableFuture<PlayerInfo> search(HttpClient client, Gson gson, Logger logger, String apiUrl, String query) {
        String endpoint = apiUrl + "/v2/profile/by-name/" + query;
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(s -> gson.fromJson(s, PlayerInfo.class))
                .whenComplete((ignored, t) -> {
                    if (t != null) logger.warn("Error searching player {}", query, t);
                });
    }

    public static CompletableFuture<Map<String, Ranking>> getRankings(HttpClient client, Gson gson, Logger logger, String apiUrl, UUID uuid) {
        String endpoint = apiUrl + "/v2/profile/" + uuid + "/rankings";
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint)).GET().build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(s -> gson.fromJson(s, new TypeToken<Map<String, Ranking>>() {}))
                .whenComplete((ignored, t) -> {
                    if (t != null) logger.warn("Error getting rankings for {}", uuid, t);
                });
    }

    public static Optional<NamedRanking> getHighestRanking(Map<String, Ranking> rankings) {
        return rankings.entrySet().stream()
                .filter(e -> e.getKey() != null)
                .min(Comparator.comparingInt(e -> e.getValue().comparableTier()))
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())));
    }

    public List<NamedRanking> getSortedTiers() {
        List<NamedRanking> tiers = new ArrayList<>(this.rankings.entrySet().stream()
                .map(e -> e.getValue().asNamed(TierCache.findModeOrUgly(e.getKey())))
                .toList());
        tiers.sort(Comparator.comparing((NamedRanking a) -> a.ranking().retired(), Boolean::compare)
                .thenComparingInt(a -> a.ranking().tier())
                .thenComparingInt(a -> a.ranking().pos()));
        return tiers;
    }
}