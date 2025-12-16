package sidly.discord_bot;

import sidly.discord_bot.commands.AllSlashCommands;
import sidly.discord_bot.commands.inactivity_promotion.RequirementList;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public Map<Roles, String> roles = new HashMap<>();
    public Map<ClassRoles, String> classRoles = new HashMap<>();
    public Map<LvlRoles, String> lvlRoles = new HashMap<>();
    public Map<AllSlashCommands, Roles> roleRequirements = new HashMap<>();
    public Map<String, Boolean> allowedChannels = new HashMap<>();
    public Map<Channels, String> channels = new HashMap<>();
    public Map<Settings, String> other = new HashMap<>();
    public Map<Utils.RankList, RequirementList> promotionRequirements = new HashMap<>();

    public Config() {
        // default values
        for (Settings setting : Settings.values()) {
            other.put(setting, "");
        }
        other.put(Settings.MaxContentCompletion, "1133");

        for (Roles role : Roles.values()) {
            roles.put(role, "");
        }

        for (Channels channel : Channels.values()) {
            channels.put(channel, "");
        }

        for (LvlRoles lvlRole : LvlRoles.values()) {
            lvlRoles.put(lvlRole, "");
        }

        for (ClassRoles classRole : ClassRoles.values()) {
            classRoles.put(classRole, "");
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
        roleRequirements.put(AllSlashCommands.addinactivityexeption, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.checkforpromotions, Roles.StrategistRole);
        roleRequirements.put(AllSlashCommands.checkforinactivity, Roles.StrategistRole);
        roleRequirements.put(AllSlashCommands.removeverification, Roles.OwnerRole);
        roleRequirements.put(AllSlashCommands.editconfiglvlrole, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigchannel, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigother, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigrole, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.addtrackedguild, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.removetrackedguild, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.sendselfassignedrolemessage, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.editconfigclassrole, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.say, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.sendwarrolesmessage, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.stoptimer, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.starttimer, Roles.ChiefRole);
        roleRequirements.put(AllSlashCommands.sendanniping, Roles.StrategistRole);
        roleRequirements.put(AllSlashCommands.createanniparties, Roles.StrategistRole);
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
        WynnAdminRole,
        WynnContentTeamRole,
        WynnModeratorRole,
        WynnVetRole,
        AnniRole,
        EventsRole,
        BombBellRole,
        TheyThemRole,
        HeHimRole,
        SheHerRole,
        WarPingRole,
        WarrerRole
    }

    public enum ClassRoles {
        Archer,
        Boltslinger,
        Trapper,
        Sharpshooter,
        Warrior,
        Fallen,
        BattleMonk,
        Paladin,
        Mage,
        RiftWalker,
        LightBender,
        Arcanist,
        Assassin,
        Trickster,
        Shadestepper,
        Acrobat,
        Shaman,
        Summoner,
        Ritualist,
        Acolyte;

        public static Config.ClassRoles getRoleFromName(String name) {
            return switch (name.toLowerCase()) {
                case "warrior" -> Config.ClassRoles.Warrior;
                case "fallen" -> Config.ClassRoles.Fallen;
                case "battle monk", "battlemonk" -> Config.ClassRoles.BattleMonk;
                case "paladin" -> Config.ClassRoles.Paladin;
                case "mage" -> Config.ClassRoles.Mage;
                case "riftwalker" -> ClassRoles.RiftWalker;
                case "light bender", "lightbender" -> Config.ClassRoles.LightBender;
                case "arcanist" -> Config.ClassRoles.Arcanist;
                case "archer" -> Config.ClassRoles.Archer;
                case "boltslinger" -> Config.ClassRoles.Boltslinger;
                case "trapper" -> Config.ClassRoles.Trapper;
                case "sharpshooter" -> Config.ClassRoles.Sharpshooter;
                case "assassin" -> Config.ClassRoles.Assassin;
                case "trickster" -> Config.ClassRoles.Trickster;
                case "shadestepper" -> Config.ClassRoles.Shadestepper;
                case "acrobat" -> Config.ClassRoles.Acrobat;
                case "shaman" -> Config.ClassRoles.Shaman;
                case "summoner" -> Config.ClassRoles.Summoner;
                case "ritualist" -> Config.ClassRoles.Ritualist;
                case "acolyte" -> Config.ClassRoles.Acolyte;
                default -> null; // unknown selection
            };
        }

        }

    public enum Channels {
        ModerationChannel,
        ConsoleLogChannel,
        WelcomeChannel
    }

    public enum Settings {
        Token,
        YourGuildPrefix,
        MaxContentCompletion,
        YourDiscordServerId,
        ApiToken,
        GuildLeaveMessage,
        GuildJoinMessage,
        ApiToken1,
        ApiToken2,
        ApiToken3,
        ApiToken4,
        ApiToken5,
        ApiToken6,
        ApiToken7,
        ApiToken8,
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
