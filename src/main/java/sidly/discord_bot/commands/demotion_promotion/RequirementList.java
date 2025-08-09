package sidly.discord_bot.commands.demotion_promotion;

import java.util.ArrayList;
import java.util.List;

public class RequirementList {
    private List<Requirement> requirements;
    private int optionalQuantityRequired;

    public RequirementList() {
        this.requirements = new ArrayList<>();
        this.optionalQuantityRequired = 0;
    }

    public List<Requirement> getRequirements() {
        return requirements;
    }

    public void addRequirement(Requirement requirement) {
        this.requirements.add(requirement);
    }

    public int getOptionalQuantityRequired() {
        return optionalQuantityRequired;
    }

    public void setOptionalQuantityRequired(int optionalQuantityRequired) {
        if (optionalQuantityRequired <= requirements.size()) {
            this.optionalQuantityRequired = optionalQuantityRequired;
        }
    }

    public boolean isEmpty(){
        return this.requirements.isEmpty();
    }
}
