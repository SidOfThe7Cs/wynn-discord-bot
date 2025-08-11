package sidly.discord_bot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;

public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File JAR_DIR;

    static {
        try {
            JAR_DIR = new File(MainEntrypoint.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static final File CONFIG_FILE = new File(JAR_DIR, "config.json");
    private static final File DATABASE_FILE = new File(JAR_DIR,"database.json");

    private static Config config = new Config();
    private static Database dataBase = new Database();

    public static String getSetting(Config.Settings opt) {
        return config.settings.get(opt);
    }

    public static Config getConfigInstance(){
        return config;
    }

    public static Database getDatabaseInstance(){
        return dataBase;
    }

    public static void save() {
        save(config, CONFIG_FILE);
        save(dataBase, DATABASE_FILE);
    }

    public static void load() {
        config = load(config, CONFIG_FILE, new Config());
        dataBase = load(dataBase, DATABASE_FILE, new Database());
    }

    private static <T> void save(T object, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(object, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> T load(T currentInstance, File file, T defaults) {

        if (!file.exists()) {
            System.out.println(file.getName() + " not found. Creating default.");
            save(defaults, file);
            return defaults;
        }

        try (FileReader reader = new FileReader(file)) {
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) currentInstance.getClass();

            T loaded = GSON.fromJson(reader, clazz);
            if (loaded == null) {
                System.err.println("Failed to load " + file.getName() + ", using default.");
                return currentInstance;
            }

            // Merge missing defaults
            mergeMissingDefaults(loaded, defaults);
            save(loaded, file); // keep file up to date
            return loaded;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load " + file.getName());
        }
        return currentInstance;
    }


    @SuppressWarnings("unchecked")
    public static <T> void mergeMissingDefaultsUNUSED(T target, T defaults) {
        if (target == null || defaults == null) {
            throw new IllegalArgumentException("Target and defaults must not be null");
        }

        Class<?> clazz = target.getClass();

        for (Field field : clazz.getDeclaredFields()) {
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

    @SuppressWarnings("unchecked")
    public static <T> void mergeMissingDefaults(T target, T defaults) {
        if (target == null || defaults == null) return;

        Class<?> clazz = target.getClass();
        while (clazz != null) { // Go up inheritance chain
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object targetValue = field.get(target);
                    Object defaultValue = field.get(defaults);

                    if (targetValue == null) {
                        // If target is null, set it to default value
                        field.set(target, defaultValue);
                    } else if (targetValue instanceof Map && defaultValue instanceof Map) {
                        Map<Object, Object> targetMap = (Map<Object, Object>) targetValue;
                        Map<Object, Object> defaultMap = (Map<Object, Object>) defaultValue;

                        // Add any missing entries from defaults into target map
                        for (Map.Entry<Object, Object> entry : defaultMap.entrySet()) {
                            targetMap.putIfAbsent(entry.getKey(), entry.getValue());
                        }
                    }
                    // You can add similar logic for Collections if needed

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            clazz = clazz.getSuperclass();
        }
    }



    public static void reloadConfig(SlashCommandInteractionEvent event) {
        config = load(config, CONFIG_FILE, new Config());
        event.reply("reloaded").setEphemeral(true).queue();
    }
}
