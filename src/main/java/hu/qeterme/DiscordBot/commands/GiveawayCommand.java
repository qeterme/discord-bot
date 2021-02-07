package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.objects.Giveaway;
import hu.qeterme.DiscordBot.timerTasks.GiveawayEndTask;
import hu.qeterme.DiscordBot.timerTasks.GiveawayUpdaterTask;
import hu.qeterme.DiscordBot.utils.DateUtil;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Timer;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class GiveawayCommand implements Command {
	String command;

	public GiveawayCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User user = event.getMessage().getAuthor().get();
		if (!Connector.isOperator(user.getId())) {
			return user.getPrivateChannel()
					.flatMap(ch -> ch.createMessage(getMessage(Messages.DO_NOT_HAVE_PERMISSION)))
					.then();
		} else {
			String[] args = event.getMessage().getContent()
					.replace(DiscordBot.getMention1(), "")
					.replace(DiscordBot.getMention2(), "")
					.replace(command + "", "").trim().split(" ");

			if (args[0].equalsIgnoreCase("-")) {
				// ha töröl
				try {
					int id = Integer.parseInt(args[1]);

					if (Giveaway.getGiveaway(id) != null) {
						// van ilyen giveaway, töröljük
						Giveaway giveaway = Giveaway.getGiveaway(id);

						event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
								.flatMap(guild -> guild.getChannelById(giveaway.getChannelID()).cast(TextChannel.class)
										.flatMap(textChannel -> textChannel.getMessageById(giveaway.getMessageID()))
										.flatMap(message -> message.delete()))
								.subscribe();

						Giveaway.removeGiveaway(giveaway);

						event.getMessage().getChannel()
								.flatMap(messageChannel -> messageChannel.createMessage(getMessage(Messages.GIVEAWAY_DELETED)))
								.subscribe();
					} else {
						// nincs ilyen giveaway
						event.getMessage().getChannel()
								.flatMap(messageChannel -> messageChannel.createMessage(getMessage(Messages.GIVEAWAY_FAIL)))
								.subscribe();
					}
				} catch (NumberFormatException e) {
					event.getMessage().getChannel()
							.flatMap(channel -> channel.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT)))
							.subscribe();
				}
			} else if (args[0].equalsIgnoreCase("újra")) {
				// újrahúzás
				try {
					int id = Integer.parseInt(args[1]);

					if (Giveaway.getGiveaway(id) != null) {
						// van ilyen giveaway, újrahúzunk
						Giveaway giveaway = Giveaway.getGiveaway(id);

						event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
								.flatMap(guild -> guild.getChannelById(giveaway.getChannelID()).cast(TextChannel.class)
										.flatMap(textChannel -> {
													giveaway.draw();
													textChannel.getMessageById(giveaway.getMessageID()).flatMap(message -> {
														message.removeAllReactions().subscribe();
														return message.edit(messageEditSpec ->
																messageEditSpec.setEmbed(embedCreateSpec -> {
																	embedCreateSpec.setTitle(getMessage(Messages.GIVEAWAY_TITLE, giveaway.getName(), giveaway.getHowMany() + ""));
																	embedCreateSpec.setDescription(getMessage(Messages.GIVEAWAY_STATS, Connector.getGiveawayJoinedCount(giveaway) + "", giveaway.getDrawed()));
																	embedCreateSpec.setFooter(getMessage(Messages.GIVEAWAY_FOOTER, giveaway.getId() + ""), getSetting(Settings.GIVEAWAY_IMAGE));
																	embedCreateSpec.setColor(new Color(62, 183, 249));
																	embedCreateSpec.setTimestamp(giveaway.getEndTime());
																})
														);
													}).subscribe();

													textChannel.createMessage(getMessage(Messages.GIVEAWAY_WINNERS_REDRAW, giveaway.getName(), giveaway.getId() + "", giveaway.getDrawed())).subscribe();
													return Mono.empty();
												}
										)).subscribe();
					} else {
						// nincs ilyen
						event.getMessage().getChannel()
								.flatMap(messageChannel -> messageChannel.createMessage(getMessage(Messages.GIVEAWAY_FAIL)))
								.subscribe();
					}
				} catch (NumberFormatException e) {
					event.getMessage().getChannel()
							.flatMap(channel -> channel.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT)))
							.subscribe();
				}
			} else {
				// új giveaway
				if (!args[0].matches("(\\d+[h])?(\\d+[m])?(\\d+[s])?")) {
					// rossz időformátum
					event.getMessage().getChannel()
							.flatMap(channel -> channel.createMessage(getMessage(Messages.WRONG_TIME_FORMAT)))
							.subscribe();
				} else {
					// jó időformátum
					try {
						int end = DateUtil.getTime(args[0]);
						int howMany = Integer.parseInt(args[1]);
						Date futureDate = DateUtil.getFuture(end);
						StringBuilder name = new StringBuilder();
						for (int i = 2; i < args.length; i++) {
							name.append(args[i]).append(" ");
						}

						event.getMessage().getChannel()
								.subscribe(textChannel -> textChannel.createMessage(getMessage(Messages.GIVEAWAY_COMING_SOON))
										.flatMap(message -> {
											message.addReaction(ReactionEmoji.unicode(getMessage(Messages.GIVEAWAY_EMOJI))).onErrorResume(e -> Mono.empty()).subscribe();
											return message.edit(messageCreateSpec -> {
												messageCreateSpec.setContent("");
												messageCreateSpec.setEmbed(embedCreateSpec -> {
													Giveaway giveaway = new Giveaway(textChannel.getId(), message.getId(), name.toString(), howMany, futureDate.toInstant());
													embedCreateSpec.setTitle(getMessage(Messages.GIVEAWAY_TITLE, giveaway.getName(), giveaway.getHowMany() + ""));
													embedCreateSpec.setDescription(getMessage(Messages.GIVEAWAY_BODY, DateUtil.getFormattedRemainingTime(giveaway.getEndTime())));
													embedCreateSpec.setFooter(getMessage(Messages.GIVEAWAY_FOOTER, giveaway.getId() + ""), getSetting(Settings.GIVEAWAY_IMAGE));
													embedCreateSpec.setColor(new Color(62, 183, 249));
													embedCreateSpec.setTimestamp(giveaway.getEndTime());

													Timer updater = new Timer();
													Timer endTimer = new Timer();

													updater.schedule(new GiveawayUpdaterTask(giveaway, event), 0, 5000);
													endTimer.schedule(new GiveawayEndTask(giveaway, event, updater), Date.from(giveaway.getEndTime()));

													DiscordBot.getTimers().add(updater);
													DiscordBot.getTimers().add(endTimer);
												});
											});
										}).subscribe());
					} catch (NumberFormatException e) {
						// rossz embermennyiség
						event.getMessage().getChannel()
								.flatMap(channel -> channel.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT)))
								.subscribe();
					}
				}
			}

			return Mono.empty();
		}
	}
}
