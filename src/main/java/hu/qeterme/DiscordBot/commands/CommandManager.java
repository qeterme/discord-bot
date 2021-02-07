package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

public class CommandManager {
	private static final List<Command> commands = new ArrayList<>();

	/**
	 * Adds command to commands
	 *
	 * @param command what to add
	 */
	public static void addCommand(Command command) {
		commands.add(command);
	}

	/**
	 * Issues a command
	 *
	 * @param command what to issue
	 * @param event   what event was this in
	 * @return a Mono
	 */
	public static Mono<Void> issueCommand(String command, MessageCreateEvent event) {
		return Mono.from(getCommand(command)
				.flatMap(c -> c.execute(event)));
	}

	/**
	 * Get commands
	 *
	 * @return list of Commands
	 */
	public static List<Command> getCommands() {
		return commands;
	}

	/**
	 * Get a command from the list by name
	 *
	 * @param command what to search for
	 * @return Command
	 */
	public static Mono<Command> getCommand(String command) {
		return Flux.fromIterable(commands)
				.filter(c -> c.getCommand().equalsIgnoreCase(command))
				.next();
	}
}
