package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
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

import static hu.qeterme.DiscordBot.enums.Messages.*;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class SuggestionModifyCommand implements Command {
	String command;

	public SuggestionModifyCommand() {
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
			long longSnowflake = 0;
			try {
				longSnowflake = Long.parseLong(args[0]);
				Snowflake snowflake = Snowflake.of(longSnowflake);

				if (Connector.getSuggestion(snowflake).isEmpty()) {
					// nincs ilyen suggestion
					user.getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.SUGGESTION_NO_SUGGESTION)))
							.subscribe();
				} else {
					// van ilyen suggestion, lets see whats the argument
					if (args[1].equalsIgnoreCase("comment")) {
						if (args.length < 3) {
							user.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.SUGGESTION_MODIFY_FAILED)))
									.subscribe();
						} else {
							StringBuilder comment = new StringBuilder();
							for (int i = 2; i < args.length; i++) {
								comment.append(args[i]).append(" ");
							}

							Connector.putSuggestionComment(snowflake, user.getId(), comment.toString());

							event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
									.flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.SUGGESTION_CHANNEL)))
											.cast(TextChannel.class).flatMap(textChannel -> {
												textChannel.getMessageById(snowflake).subscribe(message -> {
													message.removeAllReactions().subscribe();
													message.edit(messageEditSpec -> {
														messageEditSpec.setEmbed(embedCreateSpec -> {
															Mono<Member> suggester = guild.getMemberById(Connector.getUserFromSuggestion(snowflake));
															embedCreateSpec.setThumbnail(suggester.block().getAvatarUrl());
															embedCreateSpec.setTitle(getMessage(Messages.SUGGESTION_SOMEBODYS_SUGGESTION, Connector.getNick(suggester.block().getId())));
															embedCreateSpec.setDescription(Connector.getSuggestion(snowflake));
															embedCreateSpec.setColor(new Color(239, 129, 34));
															embedCreateSpec.setFooter(getMessage(Messages.SUGGESTION_FOOTER, message.getId().asString()), "https://i.imgur.com/qnJ3ZA1.jpg");

															DateTimeFormatter formatter = DateTimeFormatter
																	.ofLocalizedDateTime(FormatStyle.MEDIUM)
																	.withLocale(new Locale("hu", "HU"))
																	.withZone(ZoneId.systemDefault());

															Instant added = Connector.getSuggestionDate(snowflake, "added").toInstant();
															Instant timeout = Connector.getSuggestionDate(snowflake, "timeout").toInstant();

															embedCreateSpec.addField(getMessage(Messages.SUGGESTION_COMMENT, Connector.getNick(user.getId())), comment.toString(), false);
															Messages state;
															switch (Connector.getSuggestionState(snowflake)) {
																case "0":
																	state = SUGGESTION_STATE_VOTING;
																	break;
																case "1":
																	state = SUGGESTION_STATE_REJECTED;
																	break;
																case "2":
																	state = SUGGESTION_STATE_IMPLEMENTED;
																	break;
																default:
																	state = SUGGESTION_STATE_TIMEOUT;
																	break;
															}
															embedCreateSpec.addField(getMessage(Messages.SUGGESTION_STATE), getMessage(state), true);
															embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_IN), formatter.format(added), true);
															embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_OUT), formatter.format(timeout), true);
														});
													}).subscribe();
												});
												return Mono.empty();
											})).subscribe();


							user.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.SUGGESTION_MODIFIED, snowflake.asString())))
									.subscribe();
						}
					} else if (args[1].equalsIgnoreCase("implemented") || args[1].equalsIgnoreCase("rejected")) {
						if (args.length >= 3) {
							StringBuilder comment = new StringBuilder();
							for (int i = 2; i < args.length; i++) {
								comment.append(args[i]).append(" ");
							}

							Connector.putSuggestionComment(snowflake, user.getId(), comment.toString());
						}

						if (args[1].equalsIgnoreCase("implemented")) {
							Connector.modifySuggestionState(snowflake, 2);
						} else {
							Connector.modifySuggestionState(snowflake, 1);
						}

						event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
								.flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.SUGGESTION_CHANNEL)))
										.cast(TextChannel.class).flatMap(textChannel -> {
											textChannel.getMessageById(snowflake).subscribe(message -> {
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
												Connector.updateSuggestionReactors(snowflake, up.size(), down.size());

												message.removeAllReactions().subscribe();
												message.edit(messageEditSpec -> {
													messageEditSpec.setEmbed(embedCreateSpec -> {
														Mono<Member> suggester = guild.getMemberById(Connector.getUserFromSuggestion(snowflake));
														embedCreateSpec.setThumbnail(suggester.block().getAvatarUrl());
														embedCreateSpec.setTitle(getMessage(Messages.SUGGESTION_SOMEBODYS_SUGGESTION, Connector.getNick(suggester.block().getId())));
														embedCreateSpec.setDescription(Connector.getSuggestion(snowflake));
														embedCreateSpec.setColor(new Color(239, 129, 34));
														embedCreateSpec.setFooter(getMessage(Messages.SUGGESTION_FOOTER, message.getId().asString()), "https://i.imgur.com/qnJ3ZA1.jpg");

														DateTimeFormatter formatter = DateTimeFormatter
																.ofLocalizedDateTime(FormatStyle.MEDIUM)
																.withLocale(new Locale("hu", "HU"))
																.withZone(ZoneId.systemDefault());

														Instant added = Connector.getSuggestionDate(snowflake, "added").toInstant();
														Instant timeout = Connector.getSuggestionDate(snowflake, "timeout").toInstant();

														if (Connector.suggestionHasComment(snowflake)) {
															List<String> comment = Connector.getSuggestionComment(snowflake);
															Snowflake commenter = Snowflake.of(comment.get(0));
															String commentText = comment.get(1);

															embedCreateSpec.addField(getMessage(Messages.SUGGESTION_COMMENT, Connector.getNick(commenter)), commentText, false);
														}
														Messages state;
														switch (Connector.getSuggestionState(snowflake)) {
															case "0":
																state = SUGGESTION_STATE_VOTING;
																break;
															case "1":
																state = SUGGESTION_STATE_REJECTED;
																break;
															case "2":
																state = SUGGESTION_STATE_IMPLEMENTED;
																break;
															default:
																state = SUGGESTION_STATE_TIMEOUT;
																break;
														}
														embedCreateSpec.addField(getMessage(Messages.SUGGESTION_STATE), getMessage(state), true);
														embedCreateSpec.addField(getMessage(Messages.SUGGESTION_RESULTS_TITLE), getMessage(Messages.SUGGESTION_RESULTS, upemoji, up.size() + "", downemoji, down.size() + ""), true);
														embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_IN), formatter.format(added), true);
														embedCreateSpec.addField(getMessage(Messages.SUGGESTION_DATE_OUT), formatter.format(timeout), true);
													});
												}).subscribe();
											});
											return Mono.empty();
										})).subscribe();


						user.getPrivateChannel()
								.flatMap(ch -> ch.createMessage(getMessage(Messages.SUGGESTION_MODIFIED, snowflake.asString())))
								.subscribe();
					}
				}
			} catch (NumberFormatException e) {
				user.getPrivateChannel()
						.flatMap(ch -> ch.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT)))
						.subscribe();
			}

			return Mono.empty();
		}
	}
}
