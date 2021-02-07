package hu.qeterme.DiscordBot.objects;

import discord4j.core.event.domain.Event;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.timerTasks.GiveawayEndTask;
import hu.qeterme.DiscordBot.timerTasks.GiveawayUpdaterTask;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Timer;

public class Giveaway {
	private static List<Giveaway> onGoingGiveaways;
	private int id;
	private Snowflake channelID;
	private Snowflake messageID;
	private String name;
	private int howMany;
	private Instant endTime;
	private List<User> joined;
	private String drawed;

	public Giveaway(int id, Snowflake channelID, Snowflake messageID, String name, int howMany, Instant endTime) {
		this.id = id;
		this.channelID = channelID;
		this.messageID = messageID;
		this.name = name;
		this.howMany = howMany;
		this.endTime = endTime;
	}

	public Giveaway(Snowflake channelID, Snowflake messageID, String name, int howMany, Instant endTime) {
		this.id = Connector.addGiveaway(channelID, messageID, name, howMany, Date.from(endTime));
		this.channelID = channelID;
		this.messageID = messageID;
		this.name = name;
		this.howMany = howMany;
		this.endTime = endTime;
	}

	public static List<Giveaway> getOnGoingGiveaways() {
		return onGoingGiveaways;
	}

	public static Giveaway getGiveaway(int id) {
		Giveaway ga = null;
		if (Connector.getGiveaway(id) != null) {
			ga = Connector.getGiveaway(id);
		}
		if (onGoingGiveaways.stream().filter(giveaway -> giveaway.getId() == id).findFirst().orElse(null) != null) {
			ga = onGoingGiveaways.stream().filter(giveaway -> giveaway.getId() == id).findFirst().orElse(null);
		}

		return ga;
	}

	public static void removeGiveaway(Giveaway giveaway) {
		if (giveaway != null) {
			onGoingGiveaways.remove(giveaway);
			Connector.removeGiveaway(giveaway.getId());
		}
	}

	public static int loadGiveaways(Event event) {
		onGoingGiveaways = Connector.getOngoingGiveaways();

		for (Giveaway giveaway : onGoingGiveaways) {
			Timer updater = new Timer();
			Timer end = new Timer();

			updater.schedule(new GiveawayUpdaterTask(giveaway, event), 0, 5000);
			end.schedule(new GiveawayEndTask(giveaway, event, updater), Date.from(giveaway.getEndTime()));

			DiscordBot.getTimers().add(updater);
			DiscordBot.getTimers().add(end);
		}

		return onGoingGiveaways.size();
	}

	public String getDrawed() {
		return drawed;
	}

	public void updateJoined() {
		Connector.updateGiveawayJoined(id, joined);
	}

	public String draw() {
		StringBuilder stringBuilder = new StringBuilder();
		List<Snowflake> draw = Connector.drawGiveaway(id);
		for (var drawed : draw) {
			stringBuilder.append("<@" + drawed.asString() + "> ");
		}

		this.drawed = stringBuilder.toString();

		return this.drawed;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Snowflake getChannelID() {
		return channelID;
	}

	public Snowflake getMessageID() {
		return messageID;
	}

	public String getName() {
		return name;
	}

	public int getHowMany() {
		return howMany;
	}

	public Instant getEndTime() {
		return endTime;
	}

	public List<User> getJoined() {
		return joined;
	}

	public void setJoined(List<User> joined) {
		this.joined = joined;
	}

	public Date getEndTimeDate() {
		return Date.from(endTime);
	}
}
