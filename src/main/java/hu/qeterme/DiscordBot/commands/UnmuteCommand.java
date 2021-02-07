package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import reactor.core.publisher.Mono;

import java.util.Set;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class UnmuteCommand implements Command {
	String command;

	public UnmuteCommand() {
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
			Set<Snowflake> toUnmute = event.getMessage().getUserMentionIds();
			toUnmute.remove(Snowflake.of(DiscordBot.getMention()));

			for (Snowflake mute : toUnmute) {
				if (Connector.isMuted(mute.asString())) {

					DiscordBot.getMuteTimers().get(mute).cancel();
					DiscordBot.getMuteTimers().remove(mute);

					Connector.removeMuted(mute);

					event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD))).flatMap(guild -> {
						guild.getMemberById(mute).subscribe(member -> {
							member.removeRole(Snowflake.of(getSetting(Settings.MUTED_ROLE)))
									.subscribe();
							member.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.UNMUTE_YOU_VE_BEEN_UNMUTED)))
									.subscribe();
						});
						return Mono.empty();
					}).subscribe();

					event.getMessage().getAuthor().get().getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.UNMUTE_UNMUTED, Connector.getNick(mute))))
							.subscribe();
				}
			}

			return Mono.empty();
		}
	}
}
