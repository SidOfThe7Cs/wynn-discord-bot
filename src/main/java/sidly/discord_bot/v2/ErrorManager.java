package sidly.discord_bot.v2;

import sidly.discord_bot.v2.botapi.Command;

public class ErrorManager {
    public static void errorInCommand(Exception e, Command command) {
        String error = "error in " + command.getName() + " " + e.getMessage();
        StackTraceElement[] stackTrace = e.getStackTrace();

        // TODO make a button to print stack trace
    }

    public static void error(String s) {

    }

    public static void error(Exception e) {

    }
    // TODO takein the server and cammand and error for every command
}
