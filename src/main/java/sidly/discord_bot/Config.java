package sidly.discord_bot;

import sidly.discord_bot.commands.AllSlashCommands;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<Settings, String> settings = new HashMap<>();
    public Map<LvlRoles, String> lvlRoles = new HashMap<>();
    public Map<AllSlashCommands, Settings> roleRequirements = new HashMap<>();
    public Map<String, Boolean> allowedChannels = new HashMap<>();

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
        settings.put(Settings.ChampionRole, "");
        settings.put(Settings.HeroPlusRole, "");
        settings.put(Settings.HeroRole, "");
        settings.put(Settings.VipPlusRole, "");
        settings.put(Settings.VipRole, "");
        settings.put(Settings.OneHundredPercentContentCompletionRole, "");
        settings.put(Settings.ModerationChannel, "");
        settings.put(Settings.ConsoleLogChannel, "");


        lvlRoles.put(LvlRoles.Lvl1Role, "");
        lvlRoles.put(LvlRoles.Lvl5Role, "");
        lvlRoles.put(LvlRoles.Lvl10Role, "");
        lvlRoles.put(LvlRoles.Lvl15Role, "");
        lvlRoles.put(LvlRoles.Lvl20Role, "");
        lvlRoles.put(LvlRoles.Lvl25Role, "");
        lvlRoles.put(LvlRoles.Lvl30Role, "");
        lvlRoles.put(LvlRoles.Lvl35Role, "");
        lvlRoles.put(LvlRoles.Lvl40Role, "");
        lvlRoles.put(LvlRoles.Lvl45Role, "");
        lvlRoles.put(LvlRoles.Lvl50Role, "");
        lvlRoles.put(LvlRoles.Lvl55Role, "");
        lvlRoles.put(LvlRoles.Lvl60Role, "");
        lvlRoles.put(LvlRoles.Lvl65Role, "");
        lvlRoles.put(LvlRoles.Lvl70Role, "");
        lvlRoles.put(LvlRoles.Lvl75Role, "");
        lvlRoles.put(LvlRoles.Lvl80Role, "");
        lvlRoles.put(LvlRoles.Lvl85Role, "");
        lvlRoles.put(LvlRoles.Lvl90Role, "");
        lvlRoles.put(LvlRoles.Lvl95Role, "");
        lvlRoles.put(LvlRoles.Lvl100Role, "");
        lvlRoles.put(LvlRoles.Lvl105Role, "");
        lvlRoles.put(LvlRoles.Lvl106Role, "");


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
        roleRequirements.put(AllSlashCommands.removeverification, Settings.OwnerRole);
        roleRequirements.put(AllSlashCommands.editconfiglvlroleoption, Settings.ChiefRole);
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
        ChampionRole,
        HeroPlusRole,
        HeroRole,
        VipPlusRole,
        VipRole,
        OneHundredPercentContentCompletionRole,
        ModerationChannel,
        UnVerifiedRole,
        ConsoleLogChannel
    }

    public enum LvlRoles {
        Lvl1Role,
        Lvl5Role,
        Lvl10Role,
        Lvl15Role,
        Lvl20Role,
        Lvl25Role,
        Lvl30Role,
        Lvl35Role,
        Lvl40Role,
        Lvl45Role,
        Lvl50Role,
        Lvl55Role,
        Lvl60Role,
        Lvl65Role,
        Lvl70Role,
        Lvl75Role,
        Lvl80Role,
        Lvl85Role,
        Lvl90Role,
        Lvl95Role,
        Lvl100Role,
        Lvl105Role,
        Lvl106Role

    }
}
