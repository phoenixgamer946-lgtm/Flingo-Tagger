// FlingoTagger - based on TierTagger by uku3lig and netiyiy (MPL-2.0)
// https://github.com/mctiers-dev/TierTagger
package com.lwkslick.flingotagger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lwkslick.flingotagger.FlingoConfig;
import com.lwkslick.flingotagger.TierCache;
import com.lwkslick.flingotagger.model.GameMode;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.uku3lig.ukulib.config.ConfigManager;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class FlingoTaggerClient implements ClientModInitializer {
	public static final String MOD_ID = "flingo-tagger";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Gson GSON = new GsonBuilder().create();

	@Getter
	private static final ConfigManager<FlingoConfig> manager =
			ConfigManager.createDefault(FlingoConfig.class, MOD_ID);

	@Getter
	private static final HttpClient httpClient = HttpClient.newHttpClient();

	@Override
	public void onInitializeClient() {
		TierCache.init();

		// Keybind to cycle gamemodes
		net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.flingotagger.gamemode", GLFW.GLFW_KEY_UNKNOWN, KeyMapping.Category.MISC));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			// tick-based keybind check handled via KeyMapping.consumeClick() — wire up in a future PR
		});

		// /flingotagger <player> command
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) ->
				dispatcher.register(literal(MOD_ID)
						.then(argument("player", word())
								.executes(ctx -> {
									String name = getString(ctx, "player");
									net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source = ctx.getSource();
									Optional<Map<String, com.lwkslick.flingotagger.model.PlayerInfo.Ranking>> rankings =
											Optional.ofNullable(Minecraft.getInstance().getConnection())
													.flatMap(conn -> conn.getOnlinePlayers().stream()
															.filter(p -> name.equalsIgnoreCase(p.getProfile().name()))
															.findFirst()
															.map(p -> p.getProfile().id()))
													.flatMap(uuid -> TierCache.getPlayerRankings(uuid));

									if (rankings.isPresent()) {
										source.sendFeedback(printRankings(name, rankings.get()));
									} else {
										source.sendFeedback(Component.literal("[FlingoTagger] Searching..."));
										TierCache.searchPlayer(name)
												.thenAccept(p -> Minecraft.getInstance().execute(() ->
														source.sendFeedback(printRankings(name, p.rankings()))))
												.exceptionally(_ -> {
													source.sendError(Component.literal("Could not find player " + name));
													return null;
												});
									}
									return 0;
								}))));
	}

	private static Component printRankings(String name, Map<String, com.lwkslick.flingotagger.model.PlayerInfo.Ranking> rankings) {
		if (rankings.isEmpty()) return Component.literal(name + " has no tiers.");
		MutableComponent text = Component.empty().append("=== Rankings for " + name + " ===");
		rankings.forEach((m, r) -> {
			if (m == null) return;
			GameMode mode = TierCache.findModeOrUgly(m);
			Component tierText = getRankingText(r, true);
			text.append(Component.literal("\n").append(mode.asStyled(true)).append(": ").append(tierText));
		});
		return text;
	}

	// === Tier rendering ===

	public static Component appendTier(UUID uuid, Component text) {
		MutableComponent following = getPlayerTier(uuid)
				.map(entry -> {
					Component tierText = getRankingText(entry.ranking(), false);
					if (manager.getConfig().isShowIcons() && entry.mode() != null && entry.mode().icon().isPresent()) {
						return Component.literal(entry.mode().icon().get().toString()).append(tierText);
					}
					return tierText.copy();
				})
				.orElse(null);

		if (following != null) {
			following.append(Component.literal(" | ").withStyle(net.minecraft.ChatFormatting.GRAY));
			return following.append(text);
		}
		return text;
	}

	public static Optional<com.lwkslick.flingotagger.model.PlayerInfo.NamedRanking> getPlayerTier(UUID uuid) {
		GameMode mode = manager.getConfig().getGameMode();
		return TierCache.getPlayerRankings(uuid).map(rankings -> {
			com.lwkslick.flingotagger.model.PlayerInfo.Ranking ranking = rankings.get(mode.id());
			Optional<com.lwkslick.flingotagger.model.PlayerInfo.NamedRanking> highest = com.lwkslick.flingotagger.model.PlayerInfo.getHighestRanking(rankings);
			FlingoConfig.HighestMode highestMode = manager.getConfig().getHighestMode();

			if (ranking == null) {
				return (highestMode != FlingoConfig.HighestMode.NEVER && highest.isPresent())
						? highest.get() : null;
			} else {
				return (highestMode == FlingoConfig.HighestMode.ALWAYS && highest.isPresent())
						? highest.get() : ranking.asNamed(mode);
			}
		});
	}

	public static Component getRankingText(com.lwkslick.flingotagger.model.PlayerInfo.Ranking ranking, boolean showPeak) {
		if (ranking.retired() && ranking.peakTier() != null && ranking.peakPos() != null) {
			return getTierText(ranking.peakTier(), ranking.peakPos(), true);
		}
		MutableComponent tierText = getTierText(ranking.tier(), ranking.pos(), false);
		if (showPeak && ranking.comparablePeak() < ranking.comparableTier()) {
			//noinspection DataFlowIssue
			tierText.append(Component.literal(" (peak: ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GRAY)))
					.append(getTierText(ranking.peakTier(), ranking.peakPos(), false))
					.append(Component.literal(")").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.GRAY)));
		}
		return tierText;
	}

	private static MutableComponent getTierText(int tier, int pos, boolean retired) {
		StringBuilder text = new StringBuilder();
		if (retired) text.append("R");
		text.append(pos == 0 ? "H" : "L").append("T").append(tier);
		int color = getTierColor(text.toString());
		return Component.literal(text.toString()).withStyle(s -> s.withColor(color));
	}

	public static int getTierColor(String tier) {
		if (tier.startsWith("R")) return manager.getConfig().getRetiredColor();
		return manager.getConfig().getTierColors().getOrDefault(tier, 0xD3D3D3);
	}
}