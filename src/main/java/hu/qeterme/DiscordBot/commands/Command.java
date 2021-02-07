package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public interface Command {
	String getCommand();

	Mono<Void> execute(MessageCreateEvent event);
}
