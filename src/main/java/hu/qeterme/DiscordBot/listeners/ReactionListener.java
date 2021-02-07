package hu.qeterme.DiscordBot.listeners;

import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.event.domain.message.ReactionRemoveEvent;
import reactor.core.publisher.Mono;

public class ReactionListener {
	public static Mono<Void> handleAdd(ReactionAddEvent event) {
		System.out.println("handleadd");
		return Mono.empty();
	}

	public static Mono<Void> handleRemove(ReactionRemoveEvent event) {
		System.out.println("handleremove");
		return Mono.empty();
	}
}
