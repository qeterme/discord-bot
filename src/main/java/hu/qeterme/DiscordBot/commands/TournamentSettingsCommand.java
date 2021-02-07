package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import hu.qeterme.DiscordBot.DiscordBot;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import reactor.core.publisher.Mono;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.setSetting;

public class TournamentSettingsCommand implements Command {
	String command;

	public TournamentSettingsCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		final User user = event.getMessage().getAuthor().get();
		if (!Connector.isOperator(user.getId())) {
			return user.getPrivateChannel()
					.flatMap(ch -> ch.createMessage(getMessage(Messages.DO_NOT_HAVE_PERMISSION)))
					.then();
		} else {
			String[] args = event.getMessage().getContent()
					.replace(DiscordBot.getMention1(), "")
					.replace(DiscordBot.getMention2(), "")
					.replace(command + "", "").trim().split(" ");

			boolean tournamentState = DiscordBot.isTournamentState();

			if (args[0].equalsIgnoreCase("true")) {
				if (tournamentState) {
					event.getMessage().getChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_SETTINGS_NEEDTOBECLOSED)))
							.subscribe();
				} else {
					try {

						int howMany = Integer.parseInt(args[1]);

						if (howMany < 0 || howMany > 6) {
							event.getMessage().getChannel()
									.flatMap(ch ->
											ch.createMessage(getMessage(Messages.TOURNAMENT_SETTINGS_NOT_VALID)))
									.subscribe();
						} else {
							DiscordBot.setTournamentState(true);
							setSetting(Settings.TOURNAMENT_OPEN, "true");
							DiscordBot.setTournamentHowMany(howMany);
							setSetting(Settings.TOURNAMENT_HOWMANY, howMany + "");

							event.getMessage().getChannel()
									.flatMap(ch ->
											ch.createMessage(getMessage(Messages.TOURNAMENT_SETTINGS_OPENED, howMany + "")))
									.subscribe();
						}
					} catch (NumberFormatException e) {
						event.getMessage().getChannel()
								.flatMap(ch -> ch.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT)))
								.subscribe();
					} catch (ArrayIndexOutOfBoundsException e) {
						event.getMessage().getChannel()
								.flatMap(ch -> ch.createMessage(getMessage(Messages.WRONG_NUMBER_FORMAT)))
								.subscribe();
					}
				}
			} else if (args[0].equalsIgnoreCase("false")) {
				if (!tournamentState) {
					event.getMessage().getChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_SETTINGS_NEEDTOBEOPENED)))
							.subscribe();
				} else {
					DiscordBot.setTournamentState(false);
					setSetting(Settings.TOURNAMENT_OPEN, "false");

					event.getMessage().getChannel()
							.flatMap(ch -> ch.createMessage(getMessage(Messages.TOURNAMENT_SETTINGS_CLOSED)))
							.subscribe();
				}
			}
			return Mono.empty();
		}
	}
}
