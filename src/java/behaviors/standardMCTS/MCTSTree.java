package behaviors.standardMCTS;

import java.util.List;

import net.demilich.metastone.game.behaviour.IBehaviour;

import behaviors.util.ActionValuePair;

public class MCTSTree
{
    IBehaviour rolloutBehavior;
    double exploreFactor;
    MCTSNode root;

    public MCTSTree(IBehaviour rolloutBehavior, double exploreFactor, MCTSNode root)
    {
        this.rolloutBehavior = rolloutBehavior;
        this.exploreFactor = exploreFactor;
        this.root = root;
    }

    public List<ActionValuePair> run(int iterations)
    {
        for(int i = 0; i < iterations; i++) {
            root.step(exploreFactor);
        }
        return root.getChildValues(root.getContext().getActivePlayerId());
    }
}
