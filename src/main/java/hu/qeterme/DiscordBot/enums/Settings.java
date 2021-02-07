package hu.qeterme.DiscordBot.enums;

import hu.qeterme.DiscordBot.database.Connector;
import reactor.util.annotation.Nullable;

import java.text.MessageFormat;

public enum Settings {
	GUILD,
	TOKEN,
	VOICE_ROOM_CATEGORY,
	SUGGESTION_CHANNEL,
	HELPOP_CHANNEL,
	TOURNAMENT_CHANNEL,
	TOURNAMENT_HOWMANY,
	TOURNAMENT_OPEN,
	MUTED_ROLE,
	GIVEAWAY_IMAGE,
	;

	public static String getSetting(Settings settings, @Nullable String... parameters) {
		return MessageFormat.format(settings.toString(), parameters);
	}

	public static void setSetting(Settings setting, String value) {
		Connector.setSetting(setting.name().toLowerCase(), value);
	}

	@Override
	public String toString() {
		return Connector.getSetting(name().toLowerCase());
	}
}
