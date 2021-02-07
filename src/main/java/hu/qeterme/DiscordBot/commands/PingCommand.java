package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import hu.qeterme.DiscordBot.database.Connector;
import reactor.core.publisher.Mono;

public class PingCommand implements Command {
	String command;

	public PingCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User author = event.getMessage().getAuthor().get();

		return event.getMessage().getChannel()
				.flatMap(channel -> channel.createMessage("Pong " + author.getMention() + "!"))
				.then();
	}
}
