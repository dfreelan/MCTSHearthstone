package behaviors.standardMCTS;

import net.demilich.metastone.game.behaviour.IBehaviour;

public class MCTSTree
{
    IBehaviour rolloutBehavior;
    MCTSNode root;

    public void run(int iterations)
    {
        for(int i = 0; i < iterations; i++) {
            root.step();
        }
    }
}
