package behaviors.MCTS;

import java.util.List;

import behaviors.standardMCTS.MCTSStandardNode;
import behaviors.util.IFilter;

import behaviors.util.ActionValuePair;

public class MCTSTree
{
    double exploreFactor;
    MCTSStandardNode root;
    IFilter actionPrune;

    public MCTSTree(double exploreFactor, MCTSStandardNode root, IFilter actionPrune)
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

    public MCTSStandardNode getRoot() { return root; }
}
