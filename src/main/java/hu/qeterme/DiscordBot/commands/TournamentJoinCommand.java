package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class TournamentJoinCommand implements Command {
	String command;

	public TournamentJoinCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User user = event.getMessage().getAuthor().get();
		if (!DiscordBot.isTournamentState()) {
			return user.getPrivateChannel()
					.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_FAIL_NOT_AVAILABLE)))
					.then();
		} else {
			List<String> args = new ArrayList<>(Arrays.asList(event.getMessage().getContent()
					.replace(DiscordBot.getMention1(), "")
					.replace(DiscordBot.getMention2(), "")
					.replace(command + " ", "").trim().split(" ")));

			String nick = Connector.getNick(user.getId());
			boolean inTournament = Connector.inTournament(nick);
			int tournamentHowMany = DiscordBot.getTournamentHowMany() - 1;

			if (args.get(0).equalsIgnoreCase("-")) {
				//withdraw
				if (inTournament) {
					// Tournamentbe jelentkezett már, lejelentkezés következik

					event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
							.flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.TOURNAMENT_CHANNEL))))
							.cast(TextChannel.class)
							.flatMapMany(textChannel ->
									textChannel.getMessageById(Connector.removeFromTournament(nick))
											.flatMap(message -> message.delete())
							).subscribe();

					user.getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_WITHDRAWED)))
							.subscribe();
				} else {
					// Nem jelentkezett még tournamentbe
					user.getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_FAIL_WITHDRAW)))
							.subscribe();
				}
			} else {
				//jelentkezés
				if (inTournament) {
					// Már jelentkezett tournamentbe
					user.getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_FAIL_ALREADY_IN)))
							.subscribe();
				} else {
					// Nem jelentkezett még
					if (args.stream().anyMatch(nick::equalsIgnoreCase)) {
						// Ha az argumentumok közt ott a saját neve :facepalm:
						user.getPrivateChannel()
								.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_FAIL_YOURSELF)))
								.subscribe();
					} else {
						// Argumentumoknál nem írta be saját magát
						System.out.println(args.size() + ": " + tournamentHowMany);
						if (args.size() > tournamentHowMany) {
							// Túl sokan vannak
							user.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_FAIL_TOO_MUCH)))
									.subscribe();
						} else if (args.size() < tournamentHowMany) {
							// Túl kevesen vannak
							user.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_FAIL_NOT_ENOUGH)))
									.subscribe();
						} else {
							// Sikeresen túljutott az akadályokon, jelentkezés következik!
							event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
									.subscribe(guild -> {
										guild.getChannelById(Snowflake.of(getSetting(Settings.TOURNAMENT_CHANNEL)))
												.cast(TextChannel.class)
												.subscribe(textChannel -> textChannel.createMessage(getMessage(Messages.TOURNAMENT_HACK_COMING_SOON))
														.flatMap(message -> message.edit(messageCreateSpec -> {
															messageCreateSpec.setContent("");
															messageCreateSpec.setEmbed(embedCreateSpec -> {
																String[] users = new String[6];
																users[0] = nick;
																for (int i = 1; i < args.size() + 1; i++) {
																	users[i] = args.get(i - 1);
																}

																embedCreateSpec.setTitle(getMessage(Messages.TOURNAMENT_TITLE, "" + Connector.addToTournament(message.getId(), users)));

																StringBuilder stringBuilder = new StringBuilder();
																for (var arg : users) {
																	if (arg != null) {
																		Snowflake snowflakeFromNick = Connector.getSnowflakeFromNick(arg);
																		stringBuilder.append((snowflakeFromNick == null ? arg : "<@" + snowflakeFromNick.asString() + ">") + ", ");
																	}
																}
																stringBuilder.deleteCharAt(stringBuilder.lastIndexOf(","));

																embedCreateSpec.setDescription(stringBuilder.toString());
															});
														})).subscribe());
									});

							user.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_JOIN_SUCCESS)))
									.subscribe();
							return Mono.empty();
						}
					}
				}
			}

			return Mono.empty();
		}
	}
}
