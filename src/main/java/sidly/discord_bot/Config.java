package sidly.discord_bot;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<Settings, String> settings = new HashMap<>();

    public Config() {
        // default values
        settings.put(Settings.Token, "");
        settings.put(Settings.OwnerRole, "");
        settings.put(Settings.ChiefRole, "");
        settings.put(Settings.StrategistRole, "");
        settings.put(Settings.CaptainRole, "");
        settings.put(Settings.RecruiterRole, "");
        settings.put(Settings.RecruitRole, "");
        settings.put(Settings.VerifiedRole, "");
    }

    public enum Settings {
        Token,
        OwnerRole,
        ChiefRole,
        StrategistRole,
        CaptainRole,
        RecruiterRole,
        RecruitRole,
        VerifiedRole
    }
}
