package sidly.discord_bot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config.json");

    private static Config config = new Config(); // default config

    public static String get(Config.Settings opt) {
        return config.settings.get(opt);
    }

    public static Config getInstance(){
        return config;
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(config, writer);
            System.out.println("Config saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to save config.");
        }
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            System.out.println("Config file not found. Creating default.");
            save(); // Save default
            return;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            Config loaded = GSON.fromJson(reader, Config.class);
            if (loaded == null) {
                System.err.println("Config failed to load, using default.");
                return;
            }

            // Merge missing fields
            mergeMissingDefaults(loaded, new Config());
            config = loaded;
            System.out.println("Config loaded successfully.");
            save();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load config.");
        }
    }

    @SuppressWarnings("unchecked") // horrid idea lmao
    private static void mergeMissingDefaults(Config target, Config defaults) {
        for (Field field : Config.class.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object targetValue = field.get(target);
                Object defaultValue = field.get(defaults);

                if (targetValue == null) {
                    // Field is completely missing
                    field.set(target, defaultValue);
                } else if (targetValue instanceof Map && defaultValue instanceof Map) {
                    Map<Object, Object> targetMap = (Map<Object, Object>) targetValue;
                    Map<Object, Object> defaultMap = (Map<Object, Object>) defaultValue;

                    for (Map.Entry<Object, Object> entry : defaultMap.entrySet()) {
                        targetMap.putIfAbsent(entry.getKey(), entry.getValue());
                    }
                } else if (targetValue instanceof Collection<?> targetCol && defaultValue instanceof Collection<?> defaultCol) {
                    // Optionally merge missing collection values
                    for (Object item : defaultCol) {
                        if (!targetCol.contains(item)) {
                            ((Collection<Object>) targetCol).add(item);
                        }
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    public static void load(SlashCommandInteractionEvent slashCommandInteractionEvent) {
        load();
    }
}
