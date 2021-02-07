package hu.qeterme.DiscordBot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.commands.*;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.listeners.*;
import reactor.core.publisher.Mono;

import java.util.*;

import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class DiscordBot {
	private static final Set<String> swearwords;
	private static final ArrayList<Timer> timers = new ArrayList<>();
	private static final HashMap<Snowflake, Timer> muteTimers = new HashMap<>();
	private static final HashMap<Snowflake, Timer> suggestionTimers = new HashMap<>();
	private static DiscordClient discordClient;
	private static boolean tournamentState;
	private static int tournamentHowMany;
	private static String mention;
	private static String mention1;
	private static String mention2;
	private static Map<Snowflake, Snowflake> userChannels;

	static {
		CommandManager.addCommand(new PingCommand());
		CommandManager.addCommand(new DanceCommand());
		CommandManager.addCommand(new GetRoomCommand());
		CommandManager.addCommand(new HelpCommand());
		CommandManager.addCommand(new SuggestionCommand());
		CommandManager.addCommand(new SuggestionModifyCommand());
		CommandManager.addCommand(new ListCmdsCommand());
		CommandManager.addCommand(new ServerInfoCommand());
		CommandManager.addCommand(new PurgeCommand());
		CommandManager.addCommand(new LoveMeCommand());
		CommandManager.addCommand(new MuteCommand());
		CommandManager.addCommand(new UnmuteCommand());
		CommandManager.addCommand(new GiveawayCommand());
		CommandManager.addCommand(new TournamentJoinCommand());
		CommandManager.addCommand(new TournamentSettingsCommand());

		swearwords = Connector.getSwearwords();
	}

	public static void main(String[] args) {
		discordClient = DiscordClientBuilder.create(getSetting(Settings.TOKEN)).build();
		discordClient.withGateway(client -> {
			client.getEventDispatcher().on(ReadyEvent.class)
					.subscribe(ready -> {
						ReadyListener.handle(ready, client);
					});

			client.getSelfId().subscribe(snowflake -> mention = snowflake.asString());
			client.getSelfId().subscribe(snowflake -> mention1 = "<@" + snowflake.asString() + ">");
			client.getSelfId().subscribe(snowflake -> mention2 = "<@!" + snowflake.asString() + ">");

			client.getEventDispatcher().on(ReadyEvent.class)
					.subscribe(event -> ReadyListener.handle(event, client));

			Mono<Void> onMessage = client.on(MessageCreateEvent.class)
					.flatMap(MessageCreateListener::handle)
					.then();

			Mono<Void> onReactionAdd = client.on(ReactionAddEvent.class)
					.flatMap(ReactionListener::handleAdd)
					.then();

			Mono<Void> onReactionRemove = client.on(ReactionRemoveEvent.class)
					.flatMap(ReactionListener::handleRemove)
					.then();

			Mono<Void> onMemberJoin = client.on(MemberJoinEvent.class)
					.flatMap(MemberJoinListener::handle)
					.then();

			Mono<Void> onVoiceStateUpdate = client.on(VoiceStateUpdateEvent.class)
					.flatMap(VoiceStateUpdateListener::handle)
					.then();

			client.updatePresence(Presence.online(Activity.watching("You"))).subscribe();

			return Mono.when(onMessage, onReactionAdd, onReactionRemove, onMemberJoin, onVoiceStateUpdate);
		}).block();
	}

	public static Map<Snowflake, Snowflake> getUserChannels() {
		return userChannels;
	}

	public static void setUserChannels(Map<Snowflake, Snowflake> userChannels) {
		DiscordBot.userChannels = userChannels;
	}

	public static DiscordClient getDiscordClient() {
		return discordClient;
	}

	public static ArrayList<Timer> getTimers() {
		return timers;
	}

	public static int getTournamentHowMany() {
		return tournamentHowMany;
	}

	public static void setTournamentHowMany(int tournamentHowMany) {
		DiscordBot.tournamentHowMany = tournamentHowMany;
	}

	public static boolean isTournamentState() {
		return tournamentState;
	}

	public static void setTournamentState(boolean tournamentState) {
		DiscordBot.tournamentState = tournamentState;
	}

	public static HashMap<Snowflake, Timer> getMuteTimers() {
		return muteTimers;
	}

	public static Set<String> getSwearwords() {
		return swearwords;
	}

	public static HashMap<Snowflake, Timer> getSuggestionTimers() {
		return suggestionTimers;
	}

	public static String getMention() {
		return mention;
	}

	public static String getMention1() {
		return mention1;
	}

	public static String getMention2() {
		return mention2;
	}

}
