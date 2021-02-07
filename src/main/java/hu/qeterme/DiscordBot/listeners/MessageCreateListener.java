package hu.qeterme.DiscordBot.listeners;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.commands.CommandManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MessageCreateListener {
	public static Mono<Void> handle(MessageCreateEvent event) {
		return Mono.just(event.getMessage())
				.filter(message -> !message.getContent().isEmpty())
				.map(Message::getContent)
				.flatMap(content -> {
					// Logolás consolera
					System.out.print(event.getMessage().getChannelId() + " - " + event.getMessage().getAuthor().get().getUsername() + ": ");
					System.out.println(event.getMessage().getContent());

					if (content.startsWith(DiscordBot.getMention1()) || content.startsWith(DiscordBot.getMention2())) {
						// Parancsok
						String newContent = content.replace(DiscordBot.getMention1(), "")
								.replace(DiscordBot.getMention2(), "").trim();
						return Flux.fromIterable(CommandManager.getCommands())
								.filter(entry -> newContent.startsWith(entry.getCommand()))
								.flatMap(entry -> {
									return CommandManager.issueCommand(entry.getCommand(), event);
								})
								.next();
					} else if (content.startsWith("Szia " + DiscordBot.getMention1()) || content.startsWith("Szia " + DiscordBot.getMention2())) {
						// Köszönés
						return Flux.just("\uD83C\uDDED", "\uD83C\uDDE6", "\uD83C\uDDF1", "\uD83C\uDDEE", "👋🏼")
								.flatMap(emoji -> event.getMessage().addReaction(ReactionEmoji.unicode(emoji)))
								.onErrorResume(e -> Mono.empty())
								.then();
					} else {
						// Káromkodásszűrő
						return Flux.fromIterable(DiscordBot.getSwearwords())
								.filter(entry -> content.toLowerCase().contains(entry))
								.flatMap(entry -> event.getMessage().delete())
								.onErrorResume(e -> Mono.empty())
								.then();
					}
				})
				.doOnError(e -> Mono.empty())
				.then();
	}
}
