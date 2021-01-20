package me.marveldc.itemraffles;

import org.bukkit.ChatColor;

import java.util.concurrent.TimeUnit;

public class Utility {
    public static String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String translate(String message, Object... variables) {
        for (int i = 0; i < variables.length - 1; i++) {
            message = message.replace("{" + i + "}", variables[i].toString());
        }

        return translate(message);
    }

    public static long parseDuration(String text) {
        if (text.isEmpty()) {
            return -1;
        }

        char[] split = text.toLowerCase().trim().toCharArray();
        long duration = 0;
        StringBuilder numbers = new StringBuilder();

        for (char character : split) {
            if (Character.isDigit(character)) {
                numbers.append(character);
                continue;
            }

            String string = numbers.toString();
            if (string.isEmpty()) {
                continue;
            }

            int number = Integer.parseInt(string);
            switch (character) {
                case 's':
                    duration += number * 1000L;
                    break;
                case 'm':
                    duration += number * 60000L;
                    break;
                case 'h':
                    duration += number * 3600000L;
                    break;
                case 'd':
                    duration += number * 86400000L;
                    break;
                default:
                    return -1;
            }

            numbers.setLength(0);
        }

        return duration <= 0 || duration >= 259200000L ? -1 : duration;
    }

    public static String getCountdown(long endTime) {
        final long toHours = TimeUnit.MILLISECONDS.toHours(endTime);
        final long toMinutes = TimeUnit.MILLISECONDS.toMinutes(endTime);
        final long toSeconds = TimeUnit.MILLISECONDS.toSeconds(endTime);

        final long minutes = toMinutes - TimeUnit.HOURS.toMinutes(toHours);
        final long seconds = toSeconds - TimeUnit.MINUTES.toMinutes(toMinutes);

        return String.format("%02dm, %02ds", minutes < 0 ? 0 : minutes, seconds < 0 ? 0 : seconds);
    }
}
