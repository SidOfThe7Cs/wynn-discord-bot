package sidly.discord_bot.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sidly.discord_bot.Config;
import sidly.discord_bot.MainEntrypoint;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdaterCommands {
    private static final String API_URL = "https://api.github.com/repos/SidOfThe7Cs/wynn-discord-bot/releases/latest";
    private static final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(15, TimeUnit.SECONDS)
            .build();
    private static String latestDownloadUrl = "";

    public static void checkForUpdate(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> { // true = ephemeral
            try {
                String currentVersion = getCurrentVersion();
                String latestRelease = checkForNewRelease();
                String userId = event.getUser().getId();

                if (latestRelease.equals(currentVersion)){
                    EmbedBuilder embed = new EmbedBuilder()
                            .setDescription("you are on latest version: " + currentVersion)
                            .setColor(Color.GREEN);
                    hook.editOriginalEmbeds(embed.build()).queue();
                    return;
                }

                EmbedBuilder embed = new EmbedBuilder()
                        .setDescription("current version is: " + currentVersion +
                                " latest is: " + latestRelease + "\n" + "when clicking the update button it will say interaction failed but should work you can check using /getbotversion please dont click it again")
                        .setColor(Color.CYAN);

                Button primary = Button.primary(userId + ":update.confirm", "update to " + latestRelease);

                hook.editOriginalEmbeds(embed.build())
                        .setComponents(ActionRow.of(primary))
                        .queue();
            } catch (IOException e) {
                hook.editOriginal("Error checking for updates: " + e.getMessage()).queue();
            }
        });
    }


    public static void update(){
        try {
            // 1. Validate URL
            if (latestDownloadUrl == null || latestDownloadUrl.isEmpty() || !latestDownloadUrl.startsWith("http")) {
                System.err.println("Invalid latestDownloadUrl: " + latestDownloadUrl);
                return;
            }

            // 2. Get current jar path
            String currentJarPath = new File(Config.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
                    .getPath();
            File currentJarFile = new File(currentJarPath);
            File parentDir = currentJarFile.getParentFile();

            // 3. Prepare new jar file path
            String fileName = latestDownloadUrl.substring(latestDownloadUrl.lastIndexOf('/') + 1);
            File newJarFile = new File(parentDir, fileName);

            // 4. Download file
            System.out.println("Downloading new version from: " + latestDownloadUrl);
            try (InputStream in = URI.create(latestDownloadUrl).toURL().openStream()) {
                Files.copy(in, newJarFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Downloaded to: " + newJarFile.getAbsolutePath());

            // 5. Launch new jar
            ProcessBuilder pb = new ProcessBuilder(
                    "java",
                    "-jar",
                    newJarFile.getAbsolutePath()
            );
            pb.inheritIO(); // Forward console output to this process
            pb.start();

            MainEntrypoint.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String checkForNewRelease() throws IOException {
        Request req = new Request.Builder()
                .url(API_URL)
                .header("Accept", "application/vnd.github+json")
                .build();

        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) {
                throw new IOException("GitHub API error: " + res.code());
            }

            JsonObject json = JsonParser.parseString(res.body().string()).getAsJsonObject();
            String tagName = json.get("tag_name").getAsString();
            if (tagName.startsWith("v")) tagName = tagName.substring(1);


            JsonArray assets = json.getAsJsonArray("assets");
            latestDownloadUrl = !assets.isEmpty()
                    ? assets.get(0).getAsJsonObject().get("browser_download_url").getAsString()
                    : json.get("zipball_url").getAsString();


            return tagName;

        }
    }

    public static String getCurrentVersion() {
        File dir = new File(".");
        File[] files = dir.listFiles();
        if (files == null) return null;

        Pattern pattern = Pattern.compile("wynn-discord-bot-(\\d+\\.\\d+\\.\\d+)-withDependencies\\.jar");

        String latestVersion = null;

        for (File file : files) {
            Matcher matcher = pattern.matcher(file.getName());
            if (matcher.matches()) {
                String version = matcher.group(1);
                if (latestVersion == null || compareVersions(version, latestVersion) > 0) {
                    latestVersion = version;
                }
            }
        }

        return latestVersion;
    }

    private static int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        for (int i = 0; i < Math.max(p1.length, p2.length); i++) {
            int num1 = i < p1.length ? Integer.parseInt(p1[i]) : 0;
            int num2 = i < p2.length ? Integer.parseInt(p2[i]) : 0;
            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
    }

    public static void getBotVersion(SlashCommandInteractionEvent event) {
        event.reply("bot is on version: " + getCurrentVersion()).setEphemeral(true).queue();
    }
}
