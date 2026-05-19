package sidly.discord_bot.api.sub;

public class Weekly {
    public boolean completed;
    public int streak;

    @Override
    public String toString() {
        return "Weekly{" +
                "completed=" + completed +
                ", streak=" + streak +
                '}';
    }
}
