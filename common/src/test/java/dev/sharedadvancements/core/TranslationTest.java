package dev.sharedadvancements.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class TranslationTest {
    private static final Pattern ARGUMENT = Pattern.compile("%(?:(\\d+)\\$)?s");
    private static List<Integer> arguments(String text) {
        var result = new ArrayList<Integer>();
        var matcher = ARGUMENT.matcher(text);
        int implicit = 1;
        while (matcher.find()) result.add(matcher.group(1)==null ? implicit++ : Integer.parseInt(matcher.group(1)));
        Collections.sort(result);
        return result;
    }
    @Test void everyTranslationPreservesKeysAndFormatArguments() throws Exception {
        var directory = Path.of(System.getProperty("translationDir"));
        JsonObject english = JsonParser.parseString(Files.readString(directory.resolve("en_us.json"))).getAsJsonObject();
        assertFalse(english.isEmpty());
        try (var files = Files.list(directory)) {
            for (var file : files.filter(p->p.toString().endsWith(".json")).toList()) {
                var translated = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                assertEquals(english.keySet(),translated.keySet(),file.toString());
                for (String key : english.keySet()) {
                    String value = translated.get(key).getAsString();
                    assertFalse(value.isBlank(),file+" "+key);
                    assertEquals(arguments(english.get(key).getAsString()),arguments(value),file+" "+key);
                    assertFalse(value.contains("\uFFFD"),file+" "+key);
                }
            }
        }
    }
}
