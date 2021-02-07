package hu.qeterme.DiscordBot.timerTasks;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.TimerTask;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class SuggestionUpdaterTask extends TimerTask {
	private final Snowflake id;
	private final Event event;

	public SuggestionUpdaterTask(Snowflake id, Event event) {
		this.id = id;
		this.event = event;
	}

	@Override
	public void run() {
		event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
				.flatMap(guild -> guild.getChannelById(Snowflake.of(getSetting(Settings.SUGGESTION_CHANNEL)))
						.cast(TextChannel.class).flatMap(textChannel -> {
							textChannel.getMessageById(id).subscribe(message -> {
								List<User> up;
								List<User> down;
								up = message.getReactors(ReactionEmoji.unicode(getMessage(Messages.SUGGESTION_EMOJI_UP)))
										.collectList().block();
								if (up != null) up.remove(event.getClient().getSelf().block());
								down = message.getReactors(ReactionEmoji.unicode(getMessage(Messages.SUGGESTION_EMOJI_DOWN)))
										.collectList().block();
								if (down != null) down.remove(event.getClient().getSelf().block());
								Connector.updateSuggestionReactors(id, up.size(), down.size());
							});

							return Mono.empty();
						})).subscribe();
	}
}
