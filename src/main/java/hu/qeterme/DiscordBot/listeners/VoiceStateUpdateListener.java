package hu.qeterme.DiscordBot.listeners;

import discord4j.core.event.domain.VoiceStateUpdateEvent;
import hu.qeterme.DiscordBot.managers.UserChannelManager;
import reactor.core.publisher.Mono;

public class VoiceStateUpdateListener {
	public static Mono<Void> handle(VoiceStateUpdateEvent event) {
		if (event.getOld().isPresent() &&
				event.getOld().get().getChannelId().isPresent() &&
				UserChannelManager.isChannelIn(event.getOld().get().getChannelId().get())) {
			event.getOld().get().getChannel().subscribe(voiceChannel -> {
				if (voiceChannel.getVoiceStates().collectList().block().size() == 0) {
					UserChannelManager.removeUserChannel(voiceChannel);
					voiceChannel.delete().subscribe();
				}
			});
		}
		return Mono.empty();
	}
}
