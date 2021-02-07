package hu.qeterme.DiscordBot.listeners;

import discord4j.core.event.domain.guild.MemberJoinEvent;
import discord4j.core.object.entity.Member;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.database.Connector;
import reactor.core.publisher.Mono;

import java.util.List;

public class MemberJoinListener {
	public static Mono<Void> handle(MemberJoinEvent event) {
		Member member = event.getMember();
		List<Snowflake> members = Connector.getMembers();
		List<Snowflake> roles = Connector.getRoles();
		if (members.contains(member.getId())) {
			member.getRoles().subscribe(role -> {
				if (roles.contains(role.getId())) {
					member.removeRole(role.getId()).subscribe();
				}
			});
			member.addRole(Connector.getRoleId(member.getId())).subscribe();
			member.edit(guildMemberEditSpec -> {
				guildMemberEditSpec.setNickname(Connector.getNick(member.getId()));
			}).subscribe();
		}
		return Mono.empty();
	}
}
