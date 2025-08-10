package sidly.discord_bot;

import sidly.discord_bot.commands.AllSlashCommands;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<Settings, String> settings = new HashMap<>();
    public Map<AllSlashCommands, Settings> roleRequirements = new HashMap<>();

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
        settings.put(Settings.MemberRole, "");
        settings.put(Settings.Lvl105Role, "");
        settings.put(Settings.ChampionRole, "");
        settings.put(Settings.HeroPlusRole, "");
        settings.put(Settings.HeroRole, "");
        settings.put(Settings.VipPlusRole, "");
        settings.put(Settings.VipRole, "");
        settings.put(Settings.OneHundredPercentContentCompletionRole, "");
        settings.put(Settings.ModerationChannel, "");


        roleRequirements.put(AllSlashCommands.shutdown, Settings.OwnerRole);
        roleRequirements.put(AllSlashCommands.reloadconfig, Settings.OwnerRole);
        roleRequirements.put(AllSlashCommands.editconfigoption, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.checkforupdates, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.addpromotionrequirement, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.setpromotionoptionalrequirement, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.removepromotionrequirement, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.setrolerequirement, Settings.OwnerRole);
        roleRequirements.put(AllSlashCommands.removerolerequirement, Settings.OwnerRole);
        roleRequirements.put(AllSlashCommands.addpromotionexeption, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.adddemotionexeption, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.addinactivityexeption, Settings.ChiefRole);
        roleRequirements.put(AllSlashCommands.checkfordemotions, Settings.StrategistRole);
        roleRequirements.put(AllSlashCommands.checkforpromotions, Settings.StrategistRole);
        roleRequirements.put(AllSlashCommands.checkforinactivity, Settings.StrategistRole);
    }

    public enum Settings {
        Token,
        OwnerRole,
        ChiefRole,
        StrategistRole,
        CaptainRole,
        RecruiterRole,
        RecruitRole,
        VerifiedRole,
        YourGuildPrefix,
        MemberRole,
        Lvl105Role,
        ChampionRole,
        HeroPlusRole,
        HeroRole,
        VipPlusRole,
        VipRole,
        OneHundredPercentContentCompletionRole,
        ModerationChannel

    }
}
