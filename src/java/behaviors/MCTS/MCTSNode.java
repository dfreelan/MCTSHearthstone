package behaviors.MCTS;

import behaviors.util.IFilter;

/**
 * Created by dfreelan on 6/22/16.
 */
public interface MCTSNode {
    public void step(double exploreFactor, IFilter actionPrune);
}
