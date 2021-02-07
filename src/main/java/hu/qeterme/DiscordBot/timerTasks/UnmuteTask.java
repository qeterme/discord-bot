package hu.qeterme.DiscordBot.timerTasks;

import discord4j.core.event.domain.Event;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;

import java.util.TimerTask;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class UnmuteTask extends TimerTask {
	private final Snowflake user;
	private final Event event;

	public UnmuteTask(Snowflake user, Event event) {
		this.user = user;
		this.event = event;
	}

	public Snowflake getUser() {
		return user;
	}

	@Override
	public void run() {
		event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD))).subscribe(guild -> {
			guild.getMemberById(user).subscribe(member -> {
				member.getPrivateChannel().subscribe(privateChannel -> {
					privateChannel.createMessage(getMessage(Messages.MUTE_UNMUTED)).subscribe();
				});
				member.removeRole(Snowflake.of(getSetting(Settings.MUTED_ROLE))).subscribe();
			});
		});

		Connector.removeMuted(user);
	}
}
