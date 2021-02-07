package hu.qeterme.DiscordBot.timerTasks;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static hu.qeterme.DiscordBot.enums.Messages.*;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class SuggestionExpireTask extends TimerTask {
	private final Snowflake suggestionMessage;
	private final Event event;
	private final Timer timer;

	public SuggestionExpireTask(Snowflake suggestionMessage, Event event, Timer timer) {
		this.suggestionMessage = suggestionMessage;
		this.event = event;
		this.timer = timer;
	}

	@Override
	public void run() {
		timer.cancel();

		event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
				.flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.SUGGESTION_CHANNEL)))
						.cast(TextChannel.class).flatMap(textChannel -> {
							textChannel.getMessageById(suggestionMessage).subscribe(message -> {
								List<User> up;
								List<User> down;
								String upemoji = getMessage(SUGGESTION_EMOJI_UP);
								String downemoji = getMessage(SUGGESTION_EMOJI_DOWN);
								up = message.getReactors(ReactionEmoji.unicode(upemoji))
										.collectList().block();
								if (up != null) up.remove(event.getClient().getSelf().block());
								down = message.getReactors(ReactionEmoji.unicode(downemoji))
										.collectList().block();
								if (down != null) down.remove(event.getClient().getSelf().block());
								Connector.updateSuggestionReactors(suggestionMessage, up.size(), down.size());

								message.removeAllReactions().subscribe();
								message.edit(messageEditSpec -> {
									messageEditSpec.setEmbed(embedCreateSpec -> {
										Mono<Member> user = guild.getMemberById(Connector.getUserFromSuggestion(suggestionMessage));
										embedCreateSpec.setThumbnail(user.block().getAvatarUrl());
										embedCreateSpec.setTitle(getMessage(Messages.SUGGESTION_SOMEBODYS_SUGGESTION, Connector.getNick(user.block().getId())));
										embedCreateSpec.setDescription(Connector.getSuggestion(suggestionMessage));
										embedCreateSpec.setColor(new Color(239, 129, 34));
										embedCreateSpec.setFooter(getMessage(Messages.SUGGESTION_FOOTER, message.getId().asString()), "https://i.imgur.com/qnJ3ZA1.jpg");

										DateTimeFormatter formatter = DateTimeFormatter
												.ofLocalizedDateTime(FormatStyle.MEDIUM)
												.withLocale(new Locale("hu", "HU"))
												.withZone(ZoneId.systemDefault());

										Instant added = Connector.getSuggestionDate(suggestionMessage, "added").toInstant();
										Instant timeout = Connector.getSuggestionDate(suggestionMessage, "timeout").toInstant();

										if (Connector.suggestionHasComment(suggestionMessage)) {
											List<String> comment = Connector.getSuggestionComment(suggestionMessage);
											embedCreateSpec.addField(getMessage(Messages.SUGGESTION_COMMENT, Connector.getNick(Snowflake.of(comment.get(0)))), comment.get(1), false);
										}

										embedCreateSpec.addField(getMessage(Messages.SUGGESTION_STATE), getMessage(Messages.SUGGESTION_STATE_TIMEOUT), true);
										embedCreateSpec.addField(getMessage(Messages.SUGGESTION_RESULTS_TITLE), getMessage(Messages.SUGGESTION_RESULTS, upemoji, up.size() + "", downemoji, down.size() + ""), true);
										embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_IN), formatter.format(added), true);
										embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_OUT), formatter.format(timeout), true);
									});
								}).subscribe();
							});
							return Mono.empty();
						})).subscribe();
	}
}
