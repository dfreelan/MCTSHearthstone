package behaviors.standardMCTS;

import java.util.List;

import behaviors.MCTS.MCTSNode;
import behaviors.simulation.SimulationContext;
import behaviors.util.IFilter;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;

public class MCTSStandardNode extends MCTSNode
{
    private IBehaviour rolloutBehavior;

    public MCTSStandardNode(IFilter actionPrune, IBehaviour rolloutBehavior) {
        super(actionPrune);
        this.rolloutBehavior = rolloutBehavior.clone();
    }

    public MCTSStandardNode(IBehaviour rolloutBehavior) {
        this.rolloutBehavior = rolloutBehavior.clone();
    }

    public MCTSStandardNode(SimulationContext current, GameAction action, List<GameAction> rootActions, IFilter actionPrune)
    {
        super(current, action, rootActions, null, actionPrune);
        if(rolloutBehavior != null) {
            this.context.setBehavior(rolloutBehavior);
        }
    }

    @Override
    public MCTSNode nodeFactoryMethod(SimulationContext context, GameAction possibleAction, List<GameAction> rootActions, Player rootPlayer)
    {
        MCTSStandardNode node = new MCTSStandardNode(context,possibleAction,rootActions,actionPrune);
        node.rolloutBehavior = this.rolloutBehavior.clone();
        return node;
    }

    @Override
    public double rollOut(MCTSNode node, List<GameAction> validActions)
    {
        //System.err.println("heyyo, listen what i sayyo");
        SimulationContext simulation = node.getContext().clone();
        simulation.setBehavior(rolloutBehavior);
        simulation.playFromMiddle();

        if(simulation.getWinningPlayerId() == 0 || simulation.getWinningPlayerId() == 1) {
            return 1 - simulation.getWinningPlayerId();
        } else {
            return 0.5;
        }
    }
}