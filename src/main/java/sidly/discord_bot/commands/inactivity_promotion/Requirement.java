package sidly.discord_bot.commands.inactivity_promotion;

public class Requirement {
    private RequirementType type;
    private Integer value;
    private boolean required;

    public Requirement(RequirementType type, Integer value, boolean required) {
        this.type = type;
        this.value = value;
        this.required = required;
    }

    public Integer getValue() {
        return value;
    }

    public boolean isRequired() {
        return required;
    }

    @Override
    public String toString() {
        return "Requirement{" +
                "type=" + type +
                ", value=" + value +
                ", required=" + required +
                '}';
    }

    public RequirementType getType() {
        return this.type;
    }
}
