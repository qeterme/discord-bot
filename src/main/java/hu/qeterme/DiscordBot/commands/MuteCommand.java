package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.timerTasks.UnmuteTask;
import hu.qeterme.DiscordBot.utils.DateUtil;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Set;
import java.util.Timer;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class MuteCommand implements Command {
	String command;

	public MuteCommand() {
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
			Set<Snowflake> toMute = event.getMessage().getUserMentionIds();
			toMute.remove(Snowflake.of(DiscordBot.getMention()));

			for (Snowflake mute : toMute) {
				if (!Connector.isMuted(mute.asString()) && !Connector.isOperator(mute)) {
					Timer unmute = new Timer();

					Date future = DateUtil.getFuture(7200);

					unmute.schedule(new UnmuteTask(mute, event), future);

					DiscordBot.getMuteTimers().put(mute, unmute);

					Connector.addMuted(mute, future);

					event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD))).flatMap(guild -> {
						guild.getMemberById(mute).subscribe(member -> {
							member.addRole(Snowflake.of(getSetting(Settings.MUTED_ROLE)))
									.subscribe();
							member.getPrivateChannel()
									.flatMap(ch -> ch.createMessage(getMessage(Messages.MUTE_YOU_ARE_MUTED)))
									.subscribe();
						});
						return Mono.empty();
					}).subscribe();

					event.getMessage().getAuthor().get().getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.MUTE_MUTED, Connector.getNick(mute))))
							.subscribe();
				} else {
					user.getPrivateChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.MUTE_ALREADY_MUTED, event.getClient().getUserById(mute).block().getUsername())))
							.subscribe();
				}
			}

			return Mono.empty();
		}
	}
}
