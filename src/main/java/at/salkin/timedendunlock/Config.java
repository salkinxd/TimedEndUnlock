package at.salkin.timedendunlock;

import org.quiltmc.loader.api.QuiltLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Config {
    private static final String DEFAULT_CONTENT = """
            # End unlock date and time (server-local), format: yyyy-MM-dd HH:mm, or "never" to keep it locked indefinitely.
            unlock_datetime = "2026-12-31 20:00"

            # Message sent to a player who uses an eye of ender on an end portal frame too early.
            # {datetime} is replaced with the formatted unlock date and time.
            unlock_message = "The End is not open yet. It will open on {datetime}."

            # Message sent instead when the End is locked indefinitely (unlock_datetime = "never").
            lock_message_indefinite = "The End is closed indefinitely. An administrator will announce when it reopens."

            # Pattern for {datetime}: y = year, M = month, d = day, H = 24-hour, m = minute, s = second
            datetime_format = "dd/MM/yyyy HH:mm"

            # Big title shown in the center of every player's screen when the End opens,
            # together with the end portal sound.
            title_message = "The End has been opened!"
            """;

    public static final String NEVER = "never";

    private static final String FALLBACK_DATETIME = "2026-12-31 20:00";
    private static final String FALLBACK_MESSAGE = "The End is not open yet. It will open on {datetime}.";
    private static final String FALLBACK_INDEFINITE_MESSAGE = "The End is closed indefinitely. An administrator will announce when it reopens.";
    private static final String FALLBACK_FORMAT = "dd/MM/yyyy HH:mm";
    private static final String FALLBACK_TITLE = "The End has been opened!";
    private static final DateTimeFormatter FALLBACK_FORMATTER = DateTimeFormatter.ofPattern(FALLBACK_FORMAT);

    private static LocalDateTime unlockAt = LocalDateTime.MIN;
    private static String message = FALLBACK_MESSAGE;
    private static String indefiniteMessage = FALLBACK_INDEFINITE_MESSAGE;
    private static String title = FALLBACK_TITLE;
    private static DateTimeFormatter formatter = FALLBACK_FORMATTER;

    public static void load() {
        Path file = QuiltLoader.getConfigDir().resolve("timed_end_unlock.toml");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_CONTENT, StandardCharsets.UTF_8);
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            unlockAt = parseUnlock(value(content, "unlock_datetime", FALLBACK_DATETIME));
            message = value(content, "unlock_message", FALLBACK_MESSAGE);
            indefiniteMessage = value(content, "lock_message_indefinite", FALLBACK_INDEFINITE_MESSAGE);
            title = value(content, "title_message", FALLBACK_TITLE);
            try {
                formatter = DateTimeFormatter.ofPattern(value(content, "datetime_format", FALLBACK_FORMAT));
            } catch (IllegalArgumentException e) {
                TimedEndUnlock.LOGGER.warn("Invalid datetime_format in {}, using {}", file, FALLBACK_FORMAT, e);
            }
        } catch (Exception e) {
            TimedEndUnlock.LOGGER.error("Failed to load {}, End is open", file, e);
            unlockAt = LocalDateTime.MIN;
        }
    }

    public static boolean isOpen() {
        return !LocalDateTime.now().isBefore(unlockAt);
    }

    public static String message() {
        return indefinite() ? indefiniteMessage : message.replace("{datetime}", unlockAt.format(formatter));
    }

    private static boolean indefinite() {
        return unlockAt.equals(LocalDateTime.MAX);
    }

    public static String format(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }

    public static String title() {
        return title;
    }

    public static void setUnlockDatetime(String value) {
        Path file = QuiltLoader.getConfigDir().resolve("timed_end_unlock.toml");
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = Pattern.compile("(?m)^(\\s*unlock_datetime\\s*=\\s*)\"[^\"]*\"").matcher(content);
            String updated = matcher.find()
                    ? content.substring(0, matcher.start()) + matcher.group(1) + "\"" + value + "\"" + content.substring(matcher.end())
                    : content + "\n# End unlock date and time (server-local), format: yyyy-MM-dd HH:mm\nunlock_datetime = \"" + value + "\"\n";
            Files.writeString(file, updated, StandardCharsets.UTF_8);
        } catch (Exception e) {
            TimedEndUnlock.LOGGER.error("Failed to write {} (unlock_datetime = {})", file, value, e);
        }
    }

    private static LocalDateTime parseUnlock(String value) {
        return NEVER.equalsIgnoreCase(value) ? LocalDateTime.MAX : LocalDateTime.parse(value.replace(' ', 'T'));
    }

    private static String value(String content, String key, String fallback) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*\"([^\"]*)\"").matcher(content);
        String found = matcher.find() ? matcher.group(1) : null;
        return found == null ? fallback : found;
    }
}
