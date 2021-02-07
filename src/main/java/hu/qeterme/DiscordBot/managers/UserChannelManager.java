package hu.qeterme.DiscordBot.managers;

import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.VoiceChannel;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;

import java.util.HashMap;
import java.util.Map;

public class UserChannelManager {
	private static Map<Snowflake, Snowflake> userChannels = new HashMap<>();

	public static void loadUserChannels() {
		userChannels = Connector.getUserChannels();
	}

	public static void addUserChannel(Snowflake channel, Snowflake user) {
		Connector.addUserChannel(channel, user);
		userChannels.put(channel, user);
	}

	public static void addUserChannel(VoiceChannel channel, User user) {
		Connector.addUserChannel(channel.getId(), user.getId());
		userChannels.put(channel.getId(), user.getId());
	}

	public static void removeUserChannel(Snowflake channel) {
		Connector.removeUserChannel(channel);
		userChannels.remove(channel);
	}

	public static void removeUserChannel(VoiceChannel voiceChannel) {
		Connector.removeUserChannel(voiceChannel.getId());
		userChannels.remove(voiceChannel.getId());
	}

	public static boolean isChannelIn(Snowflake channel) {
		return userChannels.containsKey(channel);
	}

	public static boolean isUserIn(Snowflake user) {
		return userChannels.containsValue(user);
	}

	public static Snowflake getChannelByUser(Snowflake user) {
		return userChannels
				.entrySet().stream()
				.filter(entry -> user.equals(entry.getValue()))
				.map(Map.Entry::getKey)
				.findFirst().orElse(null);
	}

	public static Map<Snowflake, Snowflake> getUserChannels() {
		return userChannels;
	}

	public static int count() {
		return userChannels.size();
	}
}
