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
        other.put(Settings.MaxContentCompletion, "1128");

        for (Roles role : Roles.values()) {
            roles.put(role, "");
        }

        for (Channels channel : Channels.values()) {
            channels.put(channel, "");
        }

        for (LvlRoles lvlRole : LvlRoles.values()) {
            lvlRoles.put(lvlRole, "");
        }

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
        GuildRaidsRole,
        GiveawayRole,
        TrialEcoRole,
        TrialTankRole,
        TrialDpsRole,
        TrialHealerRole,
        TrialSoloRole,
    }

    public enum Channels {
        ModerationChannel,
        ConsoleLogChannel,
        WelcomeChannel
    }

    public enum Settings {
        Token,
        YourGuildPrefix,
        MaxContentCompletion
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
