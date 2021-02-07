package hu.qeterme.DiscordBot.commands;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.User;
import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;
import hu.qeterme.DiscordBot.enums.Messages;
import hu.qeterme.DiscordBot.enums.Settings;
import hu.qeterme.DiscordBot.managers.UserChannelManager;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static hu.qeterme.DiscordBot.enums.Messages.getMessage;
import static hu.qeterme.DiscordBot.enums.Settings.getSetting;

public class GetRoomCommand implements Command {
	String command;

	public GetRoomCommand() {
		this.command = Connector.getCommand(this.getClass().getName());
	}

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Mono<Void> execute(MessageCreateEvent event) {
		PermissionSet goodPermissions = PermissionSet.of(Permission.CONNECT, Permission.STREAM);
		Set<PermissionOverwrite> set = new HashSet<>();
		set.add(PermissionOverwrite.forMember(event.getMessage().getAuthor().get().getId(), goodPermissions, PermissionSet.none()));

		final User user = event.getMessage().getAuthor().get();
		event.getClient().getGuildById(Snowflake.of(getSetting(Settings.GUILD)))
				.subscribe(guild -> {
							AtomicBoolean inVoice = new AtomicBoolean(false);
							guild.getMemberById(user.getId()).subscribe(member -> {
								if (member.getVoiceState().block() != null) inVoice.set(true);
							});
							if (inVoice.get()) {
								if (!UserChannelManager.isUserIn(user.getId())) {
									guild.createVoiceChannel(voiceChannelCreateSpec -> {
										String nick = Connector.getNick(user.getId());
										String name = nick.length() == 0 ? user.getUsername() : nick;
										voiceChannelCreateSpec.setName(getMessage(Messages.GETROOM_SOMEBODYS_ROOM, name));
										voiceChannelCreateSpec.setParentId(Snowflake.of(getSetting(Settings.VOICE_ROOM_CATEGORY)));
										voiceChannelCreateSpec.setPermissionOverwrites(set);
									})
											.onErrorResume(e -> Mono.empty())
											.subscribe(voiceChannel -> UserChannelManager.addUserChannel(voiceChannel, user));

									guild.getMemberById(user.getId()).subscribe(member -> {
										member.edit(guildMemberEditSpec -> {
											guildMemberEditSpec.setNewVoiceChannel(UserChannelManager.getChannelByUser(user.getId()));
										}).subscribe();
									});

									user.getPrivateChannel()
											.flatMap(channel -> channel.createMessage(getMessage(Messages.GETROOM_REPLY)))
											.subscribe();
								} else {
									user.getPrivateChannel()
											.flatMap(channel -> channel.createMessage(getMessage(Messages.GETROOM_FAILED)))
											.subscribe();
								}

							} else {
								user.getPrivateChannel()
										.flatMap(channel -> channel.createMessage(getMessage(Messages.GETROOM_JOIN_FIRST)))
										.subscribe();
							}
						}
				);


		return Mono.empty();
	}
}
