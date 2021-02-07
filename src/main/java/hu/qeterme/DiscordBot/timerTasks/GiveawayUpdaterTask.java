package hu.qeterme.DiscordBot.timerTasks;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.objects.Giveaway;
import hu.qeterme.DiscordBot.utils.DateUtil;
import reactor.core.publisher.Mono;

import java.util.TimerTask;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class GiveawayUpdaterTask extends TimerTask {
	private Giveaway giveaway;
	private final Event event;

	public GiveawayUpdaterTask(Giveaway giveaway, Event event) {
		this.giveaway = giveaway;
		this.event = event;
	}

	@Override
	public void run() {
		event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
				.flatMap(guild -> guild.getChannelById(giveaway.getChannelID()).cast(TextChannel.class).flatMap(textChannel -> {
					textChannel.getMessageById(giveaway.getMessageID()).flatMap(message -> {
						giveaway.setJoined(message.getReactors(ReactionEmoji.unicode(getMessage(Messages.GIVEAWAY_EMOJI)))
								.collectList().block());
						giveaway.updateJoined();

						return message.edit(messageEditSpec ->
								messageEditSpec.setEmbed(embedCreateSpec -> {
									embedCreateSpec.setTitle(getMessage(Messages.GIVEAWAY_TITLE, giveaway.getName(), giveaway.getHowMany() + ""));
									embedCreateSpec.setDescription(getMessage(Messages.GIVEAWAY_BODY, DateUtil.getFormattedRemainingTime(giveaway.getEndTime())));
									embedCreateSpec.setFooter(getMessage(Messages.GIVEAWAY_FOOTER, giveaway.getId() + ""), getSetting(Settings.GIVEAWAY_IMAGE));
									embedCreateSpec.setColor(new Color(62, 183, 249));
									embedCreateSpec.setTimestamp(giveaway.getEndTime());
								})
						);
					}).subscribe();

					return Mono.empty();
				})).subscribe();
	}
}
