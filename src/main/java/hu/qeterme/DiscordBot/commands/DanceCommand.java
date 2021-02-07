package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.rest.util.Color;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import reactor.core.publisher.Mono;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;


public class DanceCommand implements Command {
	String command;

	public DanceCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		return event.getMessage().getChannel()
				.flatMap(channel -> channel.createMessage(messageCreateSpec -> {
					messageCreateSpec.setEmbed(embedCreateSpec -> {
						embedCreateSpec.setImage("https://media.giphy.com/media/8gRgYeXD3rKUcjWlzB/giphy.gif");
						embedCreateSpec.setTitle(getMessage(Messages.DANCE) + " " + this.getCommand());
						embedCreateSpec.setColor(new Color(62, 56, 242));
					});
				}))
				.then();
	}
}
