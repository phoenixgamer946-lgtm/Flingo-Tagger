// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.lwkslick.flingotagger.TierCache;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public record PlayerInfo(String name, Map<String, Ranking> rankings) {

    public record Ranking(int tier, int pos, @Nullable Integer peakTier, @Nullable Integer peakPos,
                          boolean retired) {

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

    private static final String FIRESTORE_BASE =
            "https://firestore.googleapis.com/v1/projects/flingotier/databases/(default)/documents";
    private static final String API_KEY = "AIzaSyAT9YzdiAoDxTRyTicpgLa9-twp1Z1iWfo";

    /** Query Firestore for a player by username. Returns null if not found. */
    public static CompletableFuture<@Nullable PlayerInfo> fetchByName(
            HttpClient client, Gson gson, Logger logger, String username) {

        String url = FIRESTORE_BASE + ":runQuery?key=" + API_KEY;
        String body = """
                {"structuredQuery":{"from":[{"collectionId":"players"}],"where":{"fieldFilter":{"field":{"fieldPath":"name"},"op":"EQUAL","value":{"stringValue":"%s"}}},"limit":1}}
                """.formatted(username).strip();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(json -> parseFirestoreResponse(gson, json, username))
                .whenComplete((ignored, t) -> {
                    if (t != null) logger.warn("Error fetching Firestore player {}", username, t);
                });
    }

    private static @Nullable PlayerInfo parseFirestoreResponse(Gson gson, String json, String username) {
        try {
            JsonArray arr = gson.fromJson(json, JsonArray.class);
            if (arr == null || arr.isEmpty()) return null;
            JsonElement first = arr.get(0);
            if (!first.isJsonObject()) return null;
            JsonObject obj = first.getAsJsonObject();
            if (!obj.has("document")) return null;

            JsonObject fields = obj.getAsJsonObject("document").getAsJsonObject("fields");

            // Parse tiers map
            Map<String, Ranking> rankings = new LinkedHashMap<>();
            if (fields.has("tiers")) {
                JsonObject tiersFields = fields.getAsJsonObject("tiers")
                        .getAsJsonObject("mapValue")
                        .getAsJsonObject("fields");
                for (Map.Entry<String, JsonElement> entry : tiersFields.entrySet()) {
                    String tierStr = entry.getValue().getAsJsonObject()
                            .get("stringValue").getAsString();
                    Ranking r = parseTierString(tierStr);
                    if (r != null) rankings.put(entry.getKey(), r);
                }
            }
            return new PlayerInfo(username, rankings);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses tier strings like "HT1", "LT4", "HT5".
     * H = high (pos 0), L = low (pos 1). Empty string = unranked (returns null).
     */
    private static @Nullable Ranking parseTierString(String tierStr) {
        if (tierStr == null || tierStr.isBlank()) return null;
        try {
            // Format: [H|L]T[number]
            char posChar = tierStr.charAt(0);
            int pos = (posChar == 'H') ? 0 : 1;
            int tier = Integer.parseInt(tierStr.substring(2));
            return new Ranking(tier, pos, null, null, false);
        } catch (Exception e) {
            return null;
        }
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