package hu.qeterme.DiscordBot.listeners;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.managers.UserChannelManager;
import hu.qeterme.DiscordBot.objects.Giveaway;
import hu.qeterme.DiscordBot.timerTasks.*;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class ReadyListener {
	public static Mono<Void> handle(ReadyEvent event, GatewayDiscordClient client) {
				System.out.println("[Ready] Logged in as " + event.getSelf().getUsername());
		System.out.println("[Ready] Mention is " + event.getSelf().getMention());

		DiscordBot.setTournamentState(Boolean.parseBoolean(getSetting(Settings.TOURNAMENT_OPEN)));
		DiscordBot.setTournamentHowMany(Integer.parseInt(getSetting(Settings.TOURNAMENT_HOWMANY)));

		if (DiscordBot.isTournamentState()) {
			System.out.println("[Ready] Tournament joining is ongoing!");
		}

		System.out.println("[Ready] Loaded " + Giveaway.loadGiveaways(event) + " giveaways.");

		Map<String, Date> muted = Connector.getMuted();
		System.out.println("[Ready] Loaded " + muted.size() + " muted.");
		for (var mute : muted.entrySet()) {
			Timer unmute = new Timer();

			unmute.schedule(new UnmuteTask(Snowflake.of(mute.getKey()), event), mute.getValue());

			DiscordBot.getMuteTimers().put(Snowflake.of(mute.getKey()), unmute);
		}

		Map<String, Date> suggestions = Connector.getOngoingSuggestions();
		System.out.println("[Ready] Loaded " + suggestions.size() + " suggestions.");
		for (var suggestion : suggestions.entrySet()) {
			Timer updater = new Timer();
			Timer expire = new Timer();

			updater.schedule(new SuggestionUpdaterTask(Snowflake.of(suggestion.getKey()), event), 0, 3600 * 5 * 1000);
			expire.schedule(new SuggestionExpireTask(Snowflake.of(suggestion.getKey()), event, updater), suggestion.getValue());

			DiscordBot.getSuggestionTimers().put(Snowflake.of(suggestion.getKey()), expire);
		}

		UserChannelManager.loadUserChannels();
		System.out.println("[Ready] Loaded " + UserChannelManager.count() + " userChannels.");
		for (var channel : UserChannelManager.getUserChannels().keySet()) {
			client.getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
					.subscribe(guild -> {
						guild.getChannelById(channel).cast(VoiceChannel.class)
								.subscribe(voiceChannel -> {
									if (voiceChannel.getVoiceStates().collectList().block().size() == 0) {
										UserChannelManager.removeUserChannel(channel);
										voiceChannel.delete().subscribe();
									}
								});
					});
		}

		client.getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
				.subscribe(guild -> {
					List<Snowflake> members = Connector.getMembers();
					List<Snowflake> roles = Connector.getRoles();
					guild.getMembers().subscribe(member -> {
						Snowflake roleToAdd = Connector.getRoleId(member.getId());
						if (members.contains(member.getId())) {
							member.getRoles().subscribe(role -> {
								if (roles.contains(role.getId()) && !role.getId().equals(roleToAdd)) {
									member.removeRole(role.getId()).subscribe();
								}
							});
							member.addRole(Connector.getRoleId(member.getId())).subscribe();
							member.edit(guildMemberEditSpec -> guildMemberEditSpec.setNickname(Connector.getNick(member.getId())))
									.onErrorResume(e -> Mono.empty()).subscribe();
						}
					});
				});

		return Mono.empty();
	}
}
