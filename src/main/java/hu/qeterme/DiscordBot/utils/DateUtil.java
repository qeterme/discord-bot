package hu.qeterme.DiscordBot.utils;

import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateUtil {
	public static int getTime(String input) {
		int returnable = 0;

		Pattern hour = Pattern.compile("\\d+[h]");
		Pattern minute = Pattern.compile("\\d+[m]");
		Pattern second = Pattern.compile("\\d+[s]");

		Matcher h = hour.matcher(input);
		Matcher m = minute.matcher(input);
		Matcher s = second.matcher(input);

		try {
			while (h.find()) {
				returnable += Integer.parseInt(h.group().replace("h", "")) * 3600;
			}
			while (m.find()) {
				returnable += Integer.parseInt(m.group().replace("m", "")) * 60;
			}
			while (s.find()) {
				returnable += Integer.parseInt(s.group().replace("s", ""));
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return returnable;
	}

	public static Date getFuture(int seconds) {
		Date currentDate = new Date();
		Calendar calendar = Calendar.getInstance();

		calendar.setTime(currentDate);
		calendar.add(Calendar.SECOND, seconds);

		return calendar.getTime();
	}

	public static String getFormattedRemainingTime(Instant end) {
		long duration = (Date.from(end).getTime() - new Date().getTime()) / 1000;
		StringBuilder stringBuilder = new StringBuilder();

		int days = 0;
		int hours = 0;
		int minutes = 0;
		int seconds = 0;

		seconds = (int) duration % 60;
		duration /= 60;
		minutes = (int) duration % 60;
		duration /= 60;
		hours = (int) duration % 60;
		duration /= 24;
		days = (int) duration % 24;

		if (days > 0) {
			stringBuilder.append(days).append("d ");
		}
		if (hours > 0) {
			stringBuilder.append(hours).append("h ");
		}
		if (minutes > 0) {
			stringBuilder.append(minutes).append("m ");
		}
		if (seconds > 0) {
			stringBuilder.append(seconds).append("s");
		}

		return stringBuilder.toString();
	}
}
