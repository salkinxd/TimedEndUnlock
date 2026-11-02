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
            # End unlock date and time (server-local), format: yyyy-MM-dd HH:mm
            unlock_datetime = "2026-12-31 20:00"

            # Message sent to a player who uses an eye of ender on an end portal frame too early.
            # {datetime} is replaced with the formatted unlock date and time.
            unlock_message = "The End is not open yet. It will open on {datetime}."

            # Pattern for {datetime}: y = year, M = month, d = day, H = 24-hour, m = minute, s = second
            datetime_format = "dd/MM/yyyy HH:mm"
            """;

    private static final String FALLBACK_DATETIME = "2026-12-31 20:00";
    private static final String FALLBACK_MESSAGE = "The End is not open yet. It will open on {datetime}.";
    private static final String FALLBACK_FORMAT = "dd/MM/yyyy HH:mm";
    private static final DateTimeFormatter FALLBACK_FORMATTER = DateTimeFormatter.ofPattern(FALLBACK_FORMAT);

    private static LocalDateTime unlockAt = LocalDateTime.MIN;
    private static String message = FALLBACK_MESSAGE;
    private static DateTimeFormatter formatter = FALLBACK_FORMATTER;

    public static void load() {
        Path file = QuiltLoader.getConfigDir().resolve("timed_end_unlock.toml");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_CONTENT, StandardCharsets.UTF_8);
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            unlockAt = LocalDateTime.parse(value(content, "unlock_datetime", FALLBACK_DATETIME).replace(' ', 'T'));
            message = value(content, "unlock_message", FALLBACK_MESSAGE);
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
        return message.replace("{datetime}", unlockAt.format(formatter));
    }

    private static String value(String content, String key, String fallback) {
        Matcher matcher = Pattern.compile("(?m)^\\s*" + key + "\\s*=\\s*\"([^\"]*)\"").matcher(content);
        String found = matcher.find() ? matcher.group(1) : null;
        return found == null ? fallback : found;
    }
}
