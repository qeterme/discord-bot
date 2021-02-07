package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;

public class PurgeCommand implements Command {
	String command;

	public PurgeCommand() {
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
			user.getPrivateChannel()
					.flatMap(channel -> {
						return channel.createMessage(getMessage(Messages.DO_NOT_HAVE_PERMISSION));
					})
					.subscribe();
		} else {
			event.getMessage().getChannel().subscribe(messageChannel -> {
				int count = 0;
				try {
					count = Integer.parseInt(event.getMessage().getContent()
							.replace(DiscordBot.getMention1(), "")
							.replace(DiscordBot.getMention2(), "")
							.replace(command + "", "").trim());

					Flux.just(event.getMessage().getId())
							.flatMap(messageChannel::getMessagesBefore)
							.take(count)
							.flatMap(Message::delete)
							.subscribe();
				} catch (NumberFormatException e) {
					user.getPrivateChannel()
							.flatMap(channel -> {
								return channel.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT));
							})
							.subscribe();
				}
			});
		}

		return event.getMessage().delete().then();
	}
}
