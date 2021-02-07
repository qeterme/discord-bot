package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Color;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import reactor.core.publisher.Mono;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;

public class ListCmdsCommand implements Command {
	String command;

	public ListCmdsCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User user = event.getMessage().getAuthor().get();
		Mono<Void> msg = user.getPrivateChannel()
				.flatMap(channel -> {
					return channel.createMessage(messageCreateSpec -> {
						messageCreateSpec.setEmbed(embedCreateSpec -> {
							embedCreateSpec.setColor(new Color(192, 168, 0));
							embedCreateSpec.setTitle(getMessage(Messages.LISTCMDS_TITLE));
							embedCreateSpec.setDescription(getMessage(Messages.LISTCMDS_USAGE) +
									(Connector.isOperator(user.getId()) ? getMessage(Messages.LISTCMDS_USAGE_OP) : ""));
							embedCreateSpec.setThumbnail(user.getAvatarUrl());
						});
					});
				})
				.then();
		return Mono.when(msg);
	}
}
