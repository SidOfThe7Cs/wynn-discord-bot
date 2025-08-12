package sidly.discord_bot;

import sidly.discord_bot.commands.AllSlashCommands;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<Roles, String> roles = new HashMap<>();
    public Map<LvlRoles, String> lvlRoles = new HashMap<>();
    public Map<AllSlashCommands, Roles> roleRequirements = new HashMap<>();
    public Map<String, Boolean> allowedChannels = new HashMap<>();
    public Map<Channels, String> channels = new HashMap<>();
    public Map<Settings, String> other = new HashMap<>();

    public Config() {
        // default values
        other.put(Settings.Token, "");
        other.put(Settings.YourGuildPrefix, "");

        roles.put(Roles.OwnerRole, "");
        roles.put(Roles.ChiefRole, "");
        roles.put(Roles.StrategistRole, "");
        roles.put(Roles.CaptainRole, "");
        roles.put(Roles.RecruiterRole, "");
        roles.put(Roles.RecruitRole, "");
        roles.put(Roles.VerifiedRole, "");
        roles.put(Roles.MemberRole, "");
        roles.put(Roles.ChampionRole, "");
        roles.put(Roles.HeroPlusRole, "");
        roles.put(Roles.HeroRole, "");
        roles.put(Roles.VipPlusRole, "");
        roles.put(Roles.VipRole, "");
        roles.put(Roles.OneHundredPercentContentCompletionRole, "");
        roles.put(Roles.UnVerifiedRole, "");


        channels.put(Channels.ModerationChannel, "");
        channels.put(Channels.ConsoleLogChannel, "");


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


        roleRequirements.put(AllSlashCommands.shutdown, Roles.OwnerRole);
        roleRequirements.put(AllSlashCommands.reloadconfig, Roles.OwnerRole);
        roleRequirements.put(AllSlashCommands.checkforupdates, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.addpromotionrequirement, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.setpromotionoptionalrequirement, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.removepromotionrequirement, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.setrolerequirement, Roles.OwnerRole);
        roleRequirements.put(AllSlashCommands.removerolerequirement, Roles.OwnerRole);
        roleRequirements.put(AllSlashCommands.addpromotionexeption, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.adddemotionexeption, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.addinactivityexeption, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.checkfordemotions, Roles.StrategistRole);
        roleRequirements.put(AllSlashCommands.checkforpromotions, Roles.StrategistRole);
        roleRequirements.put(AllSlashCommands.checkforinactivity, Roles.StrategistRole);
        roleRequirements.put(AllSlashCommands.removeverification, Roles.OwnerRole);
        roleRequirements.put(AllSlashCommands.editconfiglvlrole, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigchannel, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigother, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigrole, Roles.ChiefRole);
    }

    public enum Roles {
        OwnerRole,
        ChiefRole,
        StrategistRole,
        CaptainRole,
        RecruiterRole,
        RecruitRole,
        VerifiedRole,
        MemberRole,
        ChampionRole,
        HeroPlusRole,
        HeroRole,
        VipPlusRole,
        VipRole,
        OneHundredPercentContentCompletionRole,
        UnVerifiedRole,
    }

    public enum Channels {
        ModerationChannel,
        ConsoleLogChannel
    }

    public enum Settings {
        Token,
        YourGuildPrefix,
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
