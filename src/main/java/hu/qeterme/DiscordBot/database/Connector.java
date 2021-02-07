package hu.qeterme.DiscordBot.database;

import discord4j.core.object.entity.User;
import discord4j.rest.util.Snowflake;
import hu.qeterme.DiscordBot.objects.Giveaway;

import java.sql.*;
import java.time.Instant;
import java.util.Date;
import java.util.*;

public class Connector {
	static final String DB_URL = "jdbc:mysql://localhost/retep";
	static final String USER = "root";
	static final String PASS = "";
	//static Connection connection;
/*
	static {
		connect();
	}

	public static void connect() {
		try {
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
		} catch (SQLException throwables) {
			throwables.printStackTrace();
		}
	}*/

	public static void setSetting(String setting, String value) {
		String SQL = "UPDATE `settings` SET `value` = '" + value + "' WHERE `settings`.`id` = '" + setting + "';";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement()) {
			System.out.println(setting + value);
			statement.executeUpdate(SQL);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static String getSetting(String setting) {
		String settingToReturn = "";

		String SQL = "SELECT * FROM `settings` WHERE `id`='" + setting + "'";
		return getString(settingToReturn, SQL, "value");
	}

	public static String getMessage(String message) {
		String messageToReturn = "";

		String SQL = "SELECT * FROM `l10n` WHERE `id`='" + message + "'";
		return getString(messageToReturn, SQL, "value");
	}

	public static String getCommand(String commandClass) {
		String commandToReturn = "";

		String SQL = "SELECT * FROM `commands` WHERE `id`='" + commandClass + "'";
		return getString(commandToReturn, SQL, "command");
	}

	private static String getString(String messageToReturn, String SQL, String column) {
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				messageToReturn = resultSet.getString(column);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return messageToReturn;
	}

	public static Set<String> getSwearwords() {
		Set<String> swearwords = new HashSet<>();

		String SQL = "SELECT swearword FROM swearwords";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				swearwords.add(resultSet.getString("swearword"));
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return swearwords;
	}

	public static Snowflake getRoleId(Snowflake snowflake) {
		Snowflake snowflakeToReturn = null;

		String SQL = "SELECT ranks.snowflake FROM ranks WHERE ranks.rankname=(SELECT users.rank FROM users WHERE users.snowflake='" + snowflake.asString() + "')";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				snowflakeToReturn = Snowflake.of(resultSet.getString("snowflake"));
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return snowflakeToReturn;
	}

	public static String getNick(Snowflake snowflake) {
		String nick = "";

		String SQL = "SELECT users.username FROM users WHERE users.snowflake='" + snowflake.asString() + "'";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				nick = resultSet.getString("username");
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return nick;
	}

	public static Snowflake getSnowflakeFromNick(String nick) {
		Snowflake snowflake = null;

		String SQL = "SELECT users.snowflake FROM users WHERE users.username='" + nick + "'";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				snowflake = Snowflake.of(resultSet.getString("snowflake"));
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return snowflake;
	}

	public static boolean isOperator(Snowflake snowflake) {
		boolean operator = false;

		String SQL = "SELECT ranks.operator FROM ranks WHERE ranks.rankname=(SELECT users.rank FROM users WHERE users.snowflake='" + snowflake.asString() + "')";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				operator = resultSet.getBoolean("operator");
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return operator;
	}

	public static boolean inTournament(String nick) {
		boolean in = false;
		String SQL = "SELECT user1 FROM tournament WHERE user1 LIKE '" + nick + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				in = resultSet.getString("user1").equalsIgnoreCase(nick);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return in;
	}

	public static Snowflake removeFromTournament(String nick) {
		Snowflake snowflake = null;
		String SQLid = "SELECT messageid FROM tournament WHERE user1 LIKE '" + nick + "'";
		String SQLdelete = "DELETE FROM tournament WHERE user1 = '" + nick + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(SQLid);
			while (resultSet.next()) {
				snowflake = Snowflake.of(resultSet.getString("messageid"));
			}

			statement.executeUpdate(SQLdelete);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return snowflake;
	}

	public static int addToTournament(Snowflake message, String[] users) {
		StringBuilder stringBuilder = new StringBuilder("INSERT INTO tournament (");
		for (int i = 0; i < users.length; i++) {
			int a = i + 1;
			if (users[i] != null) {
				stringBuilder.append("user").append(a).append(", ");
			}
		}
		stringBuilder.append(" messageid) VALUES (");
		for (String user : users) {
			if (user != null) {
				stringBuilder.append("'").append(user).append("', ");
			}
		}
		stringBuilder.append("'").append(message.asString()).append("')");

		String SQLlastId = "SELECT LAST_INSERT_ID()";

		int lastId = -1;
		System.out.println(stringBuilder.toString());

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			statement.executeUpdate(stringBuilder.toString());

			ResultSet resultSet = statement.executeQuery(SQLlastId);

			while (resultSet.next()) {
				lastId = resultSet.getInt("LAST_INSERT_ID()");
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return lastId;
	}

	public static int addGiveaway(Snowflake channel, Snowflake message, String name, int howmany, Date end) {
		String SQLadd = "INSERT INTO giveaway_list (channelid, messageid, name, howmany, end)" +
				" VALUES ('" + channel.asString() + "'," +
				"'" + message.asString() + "'," +
				"'" + name + "',"
				+ howmany + ","
				+ "?)";
		String SQLlastId = "SELECT LAST_INSERT_ID()";

		int lastId = -1;

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			PreparedStatement preparedStatement = connection.prepareStatement(SQLadd);
			preparedStatement.setTimestamp(1, new Timestamp(end.getTime()));
			preparedStatement.executeUpdate();

			ResultSet resultSet = statement.executeQuery(SQLlastId);

			while (resultSet.next()) {
				lastId = resultSet.getInt("LAST_INSERT_ID()");
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
		return lastId;
	}

	public static void removeGiveaway(int id) {
		String SQLdelete = "DELETE FROM giveaway_list WHERE id = " + id;

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			statement.executeUpdate(SQLdelete);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Giveaway getGiveaway(int id) {
		Giveaway giveaway = null;
		String SQLid = "SELECT id, channelid, messageid, name, howmany, end FROM giveaway_list WHERE id = " + id;

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(SQLid);
			while (resultSet.next()) {
				Snowflake channelID = Snowflake.of(resultSet.getString("channelid"));
				Snowflake messageID = Snowflake.of(resultSet.getString("messageid"));
				String name = resultSet.getString("name");
				int howMany = resultSet.getInt("howmany");
				Instant end = resultSet.getTimestamp("end").toInstant();

				giveaway = new Giveaway(id, channelID, messageID, name, howMany, end);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return giveaway;
	}

	public static int getGiveawayJoinedCount(Giveaway giveaway) {
		int joined = 0;
		String SQLid = "SELECT COUNT(joined) FROM `giveaway_joined` WHERE id = " + giveaway.getId();

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(SQLid);
			while (resultSet.next()) {
				joined = resultSet.getInt("COUNT(joined)");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return joined;
	}

	public static List<Snowflake> drawGiveaway(int id) {
		List<Snowflake> snowflakes = new ArrayList<>();
		int howmany = 0;

		String SQLempty = "DELETE FROM giveaway_winners WHERE id = " + id;
		String SQLhowmany = "SELECT giveaway_list.howmany FROM giveaway_list WHERE id = " + id;
		String SQLselect = "SELECT joined FROM giveaway_joined WHERE id = " + id + " ORDER BY RAND() LIMIT ?";
		String SQLput = "INSERT INTO giveaway_winners (id, winner, nick) VALUES (" + id + ", ?, (SELECT users.username FROM users WHERE users.snowflake = ?))";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			PreparedStatement preparedPut = connection.prepareStatement(SQLput);
			PreparedStatement preparedSelect = connection.prepareStatement(SQLselect);

			statement.executeUpdate(SQLempty);
			ResultSet resultId = statement.executeQuery(SQLhowmany);
			while (resultId.next()) {
				howmany = resultId.getInt("howmany");
			}

			preparedSelect.setInt(1, howmany);
			ResultSet resultSet = preparedSelect.executeQuery();
			while (resultSet.next()) {
				snowflakes.add(Snowflake.of(resultSet.getString("joined")));
				preparedPut.setString(1, resultSet.getString("joined"));
				preparedPut.setString(2, resultSet.getString("joined"));
				preparedPut.executeUpdate();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return snowflakes;
	}

	/**
	 * @return List: 0: id, 1: enddate
	 */
	public static List<Giveaway> getOngoingGiveaways() {
		List<Giveaway> giveaways = new ArrayList<>();
		String SQLid = "SELECT id, channelid, messageid, name, howmany, end FROM giveaway_list WHERE `end` >= NOW()";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQLid);

			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				Snowflake channelID = Snowflake.of(resultSet.getString("channelid"));
				Snowflake messageID = Snowflake.of(resultSet.getString("messageid"));
				String name = resultSet.getString("name");
				int howMany = resultSet.getInt("howmany");
				Instant end = resultSet.getTimestamp("end").toInstant();

				Giveaway giveaway = new Giveaway(id, channelID, messageID, name, howMany, end);
				giveaways.add(giveaway);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return giveaways;
	}

	/**
	 * @return List: 0: id, 1: enddate
	 */
	public static Map<Integer, Date> getLastGiveaway() {
		Map<Integer, Date> snowflakes = new HashMap<>();
		String SQLid = "SELECT id, end FROM giveaway_list ORDER BY id DESC LIMIT 1";

		return getIntegerDateMap(snowflakes, SQLid);
	}

	private static Map<Integer, Date> getIntegerDateMap(Map<Integer, Date> snowflakes, String SQLid) {
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(SQLid);
			while (resultSet.next()) {
				int id = resultSet.getInt("id");
				snowflakes.put(id, resultSet.getTimestamp("end"));
				System.out.println(id);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return snowflakes;
	}

	public static void updateGiveawayJoined(int id, List<User> users) {
		String SQLupload = "INSERT INTO giveaway_joined (id, joined) VALUES (?, ?)";
		String SQLempty = "DELETE FROM giveaway_joined WHERE id = " + id;

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			statement.executeUpdate(SQLempty);
			for (var user : users) {
				if (!user.getId().asString().equals("704252325463719946")) {
					PreparedStatement preparedStatement = connection.prepareStatement(SQLupload);
					preparedStatement.setInt(1, id);
					preparedStatement.setString(2, user.getId().asString());

					preparedStatement.executeUpdate();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, Date> getMuted() {
		Map<String, Date> snowflakes = new HashMap<>();
		String SQLid = "SELECT id, end FROM muted WHERE `end` >= NOW()";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(SQLid);
			while (resultSet.next()) {
				String id = resultSet.getString("id");
				snowflakes.put(id, resultSet.getTimestamp("end"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return snowflakes;
	}

	public static boolean isMuted(String id) {
		boolean is = false;
		String SQL = "SELECT id FROM muted WHERE id LIKE '" + id + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				is = resultSet.getString("id").equals(id);
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return is;
	}

	public static void addMuted(Snowflake user, Date end) {
		String SQLadd = "INSERT INTO muted (id, end) VALUES ('" + user.asString() + "', ?)";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQLadd);
			preparedStatement.setTimestamp(1, new Timestamp(end.getTime()));
			preparedStatement.executeUpdate();
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static void removeMuted(Snowflake user) {
		String SQLremove = "DELETE FROM muted WHERE id = " + user.asString();

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			statement.executeUpdate(SQLremove);
		} catch (SQLException ex) {
			ex.printStackTrace();
		}
	}

	public static void addSuggestion(Snowflake message, Snowflake user, String suggestion, Instant now, Instant future) {
		String SQL = "INSERT INTO suggestions (msg_snowflake, user_snowflake, suggestion, added, timeout) VALUES (?, ?, ?, ?, ?)";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQL);
			preparedStatement.setString(1, message.asString());
			preparedStatement.setString(2, user.asString());
			preparedStatement.setString(3, suggestion);
			preparedStatement.setTimestamp(4, Timestamp.from(now));
			preparedStatement.setTimestamp(5, Timestamp.from(future));

			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void updateSuggestionReactors(Snowflake message, int up, int down) {
		String SQLupdate = "UPDATE suggestions SET upvote = ?, downvote = ? WHERE msg_snowflake = ?";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQLupdate);
			preparedStatement.setInt(1, up);
			preparedStatement.setInt(2, down);
			preparedStatement.setString(3, message.asString());
			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Snowflake getUserFromSuggestion(Snowflake msg) {
		Snowflake toReturn = null;
		String SQL = "SELECT user_snowflake FROM suggestions WHERE msg_snowflake = '" + msg.asString() + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(SQL);
			while (resultSet.next()) {
				toReturn = Snowflake.of(resultSet.getString("user_snowflake"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toReturn;
	}

	public static String getSuggestion(Snowflake msg) {
		String toReturn = null;
		String SQL = "SELECT suggestion FROM suggestions WHERE msg_snowflake = '" + msg.asString() + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(SQL);
			while (resultSet.next()) {
				toReturn = resultSet.getString("suggestion");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toReturn;
	}

	public static String getSuggestionState(Snowflake msg) {
		String toReturn = null;
		String SQL = "SELECT state FROM suggestions WHERE msg_snowflake = '" + msg.asString() + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(SQL);
			while (resultSet.next()) {
				toReturn = resultSet.getString("state");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toReturn;
	}

	public static Date getSuggestionDate(Snowflake msg, String which) {
		Date toReturn = null;
		String SQL = "SELECT " + (which.equals("added") ? "added" : "timeout") + " FROM suggestions WHERE msg_snowflake = '" + msg.asString() + "'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement()) {
			ResultSet resultSet = statement.executeQuery(SQL);
			while (resultSet.next()) {
				toReturn = resultSet.getTimestamp((which.equals("added") ? "added" : "timeout"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return toReturn;
	}

	public static Map<String, Date> getOngoingSuggestions() {
		Map<String, Date> snowflakes = new HashMap<>();
		String SQLid = "SELECT msg_snowflake, timeout FROM suggestions WHERE `timeout` >= NOW() AND state = '0'";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();

			ResultSet resultSet = statement.executeQuery(SQLid);
			while (resultSet.next()) {
				String id = resultSet.getString("msg_snowflake");
				snowflakes.put(id, resultSet.getTimestamp("timeout"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return snowflakes;
	}

	public static boolean suggestionHasComment(Snowflake msg) {
		boolean toReturn = false;
		String SQL = "SELECT note FROM suggestions WHERE msg_snowflake = ?";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQL);
			preparedStatement.setString(1, msg.asString());
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				toReturn = resultSet.getString("note") != null;
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return toReturn;
	}

	public static List<String> getSuggestionComment(Snowflake msg) {
		List<String> list = new ArrayList<>();
		String SQL = "SELECT staff_snowflake, note FROM suggestions WHERE msg_snowflake = '" + msg.asString() + "'";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL);

			while (resultSet.next()) {
				list.add(resultSet.getString("staff_snowflake"));
				list.add(resultSet.getString("note"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return list;
	}

	public static void putSuggestionComment(Snowflake message, Snowflake commenter, String comment) {
		String SQL = "UPDATE suggestions SET staff_snowflake = ?, note = ? WHERE msg_snowflake = ?";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQL);

			preparedStatement.setString(1, commenter.asString());
			preparedStatement.setString(2, comment);
			preparedStatement.setString(3, message.asString());

			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void modifySuggestionState(Snowflake message, int state) {
		String SQL = "UPDATE suggestions SET state = ? WHERE msg_snowflake = ?";

		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQL);

			preparedStatement.setString(1, state + "");
			preparedStatement.setString(2, message.asString());

			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void addUserChannel(Snowflake channel, Snowflake user) {
		String SQL = "INSERT INTO userchannels (channel, user) VALUES (?, ?)";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			PreparedStatement preparedStatement = connection.prepareStatement(SQL);
			preparedStatement.setString(1, channel.asString());
			preparedStatement.setString(2, user.asString());

			preparedStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static void removeUserChannel(Snowflake channel) {
		String SQL = "DELETE FROM userchannels WHERE channel = '" + channel.asString() + "'";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
			Statement statement = connection.createStatement();
			statement.executeUpdate(SQL);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public static Map<Snowflake, Snowflake> getUserChannels() {
		Map<Snowflake, Snowflake> map = new HashMap<>();
		String SQL = "SELECT channel, user FROM userchannels";
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				map.put(Snowflake.of(resultSet.getString("channel")),
						Snowflake.of(resultSet.getString("user")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return map;
	}

	public static List<Snowflake> getMembers() {
		List<Snowflake> members = new ArrayList<>();

		String SQL = "SELECT users.snowflake FROM users";
		return getSnowflakes(members, SQL);
	}

	public static List<Snowflake> getRoles() {
		List<Snowflake> roles = new ArrayList<>();

		String SQL = "SELECT ranks.snowflake FROM ranks";
		return getSnowflakes(roles, SQL);
	}

	private static List<Snowflake> getSnowflakes(List<Snowflake> snowflakes, String SQL) {
		try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS);
		     Statement statement = connection.createStatement();
		     ResultSet resultSet = statement.executeQuery(SQL)) {
			while (resultSet.next()) {
				snowflakes.add(Snowflake.of(resultSet.getString("snowflake")));
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
		}

		return snowflakes;
	}
}
