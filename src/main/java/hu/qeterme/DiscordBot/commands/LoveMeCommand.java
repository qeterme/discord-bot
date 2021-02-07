package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;
import reactor.core.publisher.Mono;

public class LoveMeCommand implements Command {
	String command;

	public LoveMeCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		Mono<Void> react = event.getMessage().addReaction(ReactionEmoji.custom(Snowflake.of("713128789881782303"), "bot_love", false));
		return Mono.when(react);
	}
}
