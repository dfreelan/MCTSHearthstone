package behaviors.standardMCTS;

import java.util.List;

import net.demilich.metastone.game.behaviour.IBehaviour;

import behaviors.util.ActionValuePair;

public class MCTSTree
{
    double exploreFactor;
    MCTSNode root;

    public MCTSTree(double exploreFactor, MCTSNode root)
    {
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

    public MCTSNode getRoot() { return root; }
}
