package behaviors.standardMCTS;

import java.util.List;

import behaviors.util.IFilter;
import net.demilich.metastone.game.behaviour.IBehaviour;

import behaviors.util.ActionValuePair;

public class MCTSTree
{
    double exploreFactor;
    MCTSNode root;
    IFilter actionPrune;

    public MCTSTree(double exploreFactor, MCTSNode root, IFilter actionPrune)
    {
        this.exploreFactor = exploreFactor;
        this.root = root;
        this.actionPrune = actionPrune;
    }

    public List<ActionValuePair> run(int iterations)
    {
        for(int i = 0; i < iterations; i++) {
            root.step(exploreFactor, actionPrune);
        }
        return root.getChildValues(root.getContext().getActivePlayerId());
    }

    public MCTSNode getRoot() { return root; }
}
