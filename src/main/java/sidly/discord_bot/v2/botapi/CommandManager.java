package sidly.discord_bot.v2.botapi;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import sidly.discord_bot.v2.ErrorManager;
import sidly.discord_bot.v2.event.CommandRegistrationEvent;

import java.io.File;
import java.net.URL;
import java.util.*;

public class CommandManager extends ListenerAdapter {
    private static final Map<String, Command> registry = new HashMap<>();

    public static void register(Command command) {
        if (registry.containsKey(command.getName())) {
            ErrorManager.error("failed to register command " + command.getName() + " as it already exists");
        }
        registry.put(command.getName(), command);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        registry.get(event.getName()).run(event);
    }

    public static void onRegister(CommandRegistrationEvent event) {
        for(Command command : registry.values()) {
            event.addCommand(command);
        }
    }

    public static void loadCommands() {
        try {
            for (Class<?> clazz : getClasses("sidly.discord_bot.v2.commands")) {
                Class.forName(clazz.getName());
            }
        } catch (Exception e) {
            ErrorManager.error(e);
        }
    }

    public static List<Class<?>> getClasses(String packageName) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<URL> resources = classLoader.getResources(path);

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            File directory = new File(resource.getFile());

            if (directory.exists()) {
                findClasses(directory, packageName, classes);
            }
        }

        return classes;
    }

    private static void findClasses(File directory, String packageName, List<Class<?>> classes) {
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                findClasses(file, packageName + "." + file.getName(), classes);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "." +
                        file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException e) {
                    ErrorManager.error("failed to scan classes for commands" + e.getMessage());
                }
            }
        }
    }
}
