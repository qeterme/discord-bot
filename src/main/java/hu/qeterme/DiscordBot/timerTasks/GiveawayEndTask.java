package hu.qeterme.DiscordBot.timerTasks;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.channel.TextChannel;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.objects.Giveaway;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class GiveawayEndTask extends TimerTask {
	private Giveaway giveaway;
	private final Event event;
	private final Timer updater;

	public GiveawayEndTask(Giveaway giveaway, Event event, Timer updater) {
		this.giveaway = giveaway;
		this.event = event;
		this.updater = updater;
	}

	@Override
	public void run() {
		updater.cancel();
		event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
				.flatMap(guild -> guild.getChannelById(giveaway.getChannelID()).cast(TextChannel.class).
						flatMap(textChannel -> {
							textChannel.getMessageById(giveaway.getMessageID()).flatMap(message -> {
										giveaway.setJoined(message.getReactors(ReactionEmoji.unicode(getMessage(Messages.GIVEAWAY_EMOJI)))
												.collectList().block());
										giveaway.updateJoined();
								return Mono.empty();
							}).subscribe();
							giveaway.draw();
							textChannel.getMessageById(giveaway.getMessageID()).flatMap(message -> {
								message.removeAllReactions().subscribe();
								return message.edit(messageEditSpec ->
										messageEditSpec.setEmbed(embedCreateSpec -> {
											embedCreateSpec.setTitle(getMessage(Messages.GIVEAWAY_TITLE, giveaway.getName(), giveaway.getHowMany() + ""));
											embedCreateSpec.setDescription(getMessage(Messages.GIVEAWAY_STATS, giveaway.getJoined().size() - 1 + "", giveaway.getDrawed()));
											embedCreateSpec.setFooter(getMessage(Messages.GIVEAWAY_FOOTER, giveaway.getId() + ""), getSetting(Settings.GIVEAWAY_IMAGE));
											embedCreateSpec.setColor(new Color(62, 183, 249));
											embedCreateSpec.setTimestamp(giveaway.getEndTime());
										})
								);
							}).subscribe();
							System.out.println(giveaway.getDrawed());
							textChannel.createMessage(getMessage(Messages.GIVEAWAY_WINNERS, giveaway.getName(), giveaway.getId() + "", giveaway.getDrawed())).subscribe();

							return Mono.empty();
						}
				)).subscribe();
	}
}
