package behaviors.MCTS;

import java.util.List;

import behaviors.standardMCTS.MCTSStandardNode;
import behaviors.util.IFilter;
import behaviors.util.ActionValuePair;

public class MCTSTree
{
    private double exploreFactor;
    private MCTSNode root;

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
        return root.getChildScores(root.getContext().getActivePlayerId());
    }

    public MCTSNode getRoot() { return root; }
}
