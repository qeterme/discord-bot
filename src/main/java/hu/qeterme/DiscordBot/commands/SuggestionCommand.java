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
import hu.qeterme.DiscordBot.timerTasks.SuggestionExpireTask;
import hu.qeterme.DiscordBot.timerTasks.SuggestionUpdaterTask;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class SuggestionCommand implements Command {
	String command;

	public SuggestionCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User user = event.getMessage().getAuthor().get();
		Mono<Object> msg = event.getGuild().flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.SUGGESTION_CHANNEL)))
				.cast(TextChannel.class)
				.flatMap(textChannel -> textChannel.createMessage(getMessage(Messages.SUGGESTION_COMING_SOON))
						.flatMap(message -> {
							Mono<Void> edit = message.edit(messageCreateSpec -> {
								messageCreateSpec.setContent("");
								messageCreateSpec.setEmbed(embedCreateSpec -> {
									embedCreateSpec.setThumbnail(user.getAvatarUrl());
									embedCreateSpec.setTitle(getMessage(Messages.SUGGESTION_SOMEBODYS_SUGGESTION, Connector.getNick(user.getId())));

									String suggestion = event.getMessage().getContent()
											.replace(DiscordBot.getMention1(), "")
											.replace(DiscordBot.getMention2(), "")
											.replace(command + "", "").trim();

									embedCreateSpec.setDescription(suggestion);
									embedCreateSpec.setColor(new Color(239, 129, 34));
									embedCreateSpec.setFooter(getMessage(Messages.SUGGESTION_FOOTER, message.getId().asString()), "https://i.imgur.com/qnJ3ZA1.jpg");

									DateTimeFormatter formatter = DateTimeFormatter
											.ofLocalizedDateTime(FormatStyle.MEDIUM)
											.withLocale(new Locale("hu", "HU"))
											.withZone(ZoneId.systemDefault());

									Instant now = Instant.now();
									Instant future = Instant.now().plus(1, ChronoUnit.DAYS);

									embedCreateSpec.addField(getMessage(Messages.SUGGESTION_STATE), getMessage(Messages.SUGGESTION_STATE_VOTING), true);
									embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_IN), formatter.format(now), true);
									embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_OUT), formatter.format(future), true);

									Connector.addSuggestion(message.getId(), user.getId(), suggestion, now, future);

									Timer updater = new Timer();
									Timer expire = new Timer();

									updater.schedule(new SuggestionUpdaterTask(message.getId(), event), 0, 3600 * 5 * 1000);
									expire.schedule(new SuggestionExpireTask(message.getId(), event, updater), Date.from(future));

									DiscordBot.getSuggestionTimers().put(message.getId(), expire);
								});
							}).onErrorResume(throwable -> {
								System.out.println(throwable);
								return null;
							}).then();

							Mono<Void> reaction = Flux.just(getMessage(Messages.SUGGESTION_EMOJI_UP), getMessage(Messages.SUGGESTION_EMOJI_DOWN))
									.flatMap(emoji -> message.addReaction(ReactionEmoji.unicode(emoji)))
									.onErrorResume(throwable -> {
										System.out.println(throwable);
										return null;
									})
									.then();
							return Mono.when(edit, reaction);
						})));
		return Mono.when(msg);
	}
}
