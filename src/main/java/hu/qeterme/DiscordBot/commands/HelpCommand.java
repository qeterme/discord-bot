package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Settings;
import reactor.core.publisher.Mono;

import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class HelpCommand implements Command {
	String command;

	public HelpCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User user = event.getMessage().getAuthor().get();
		Mono<Message> msg = event.getGuild().flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.HELPOP_CHANNEL)))
				.cast(TextChannel.class)
				.flatMap(textChannel -> textChannel.createMessage(user.getUsername() + " ⇉ " + event.getMessage().getContent()
						.replace(DiscordBot.getMention1(), "")
						.replace(DiscordBot.getMention2(), "")
						.replace(command + "", "").trim())));
		return Mono.when(msg);
	}
}
