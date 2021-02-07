package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.presence.Status;
import discord4j.rest.util.Image;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import reactor.core.publisher.Mono;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;

public class ServerInfoCommand implements Command {
	String command;

	public ServerInfoCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		return event.getMessage().getChannel()
				.flatMap(channel -> channel.createMessage(messageCreateSpec -> {
					messageCreateSpec.setEmbed(embedCreateSpec -> {
						Guild guild = event.getGuild().cast(Guild.class).block();
						embedCreateSpec.setAuthor(guild.getName(), null, guild.getIconUrl(Image.Format.JPEG).get());

						long online = guild.getMembers().flatMap(Member::getPresence)
								.map(Presence::getStatus).filter(status -> !status.equals(Status.OFFLINE)).count().block();
						embedCreateSpec.addField(getMessage(Messages.SERVERINFO_MEMBERS), online + "/" + guild.getMemberCount(), true);
						embedCreateSpec.addField(getMessage(Messages.SERVERINFO_REGION), guild.getRegionId(), true);
						embedCreateSpec.addField(getMessage(Messages.SERVERINFO_OWNER), guild.getOwner().block().getTag(), false);
					});
				}))
				.then();
	}
}
