package behaviors.standardMCTS;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

import behaviors.MCTS.MCTSNode;
import net.demilich.metastone.game.actions.GameAction;


import behaviors.simulation.SimulationContext;
import behaviors.util.IFilter;
import behaviors.util.ActionValuePair;
import net.demilich.metastone.game.behaviour.IBehaviour;

public class MCTSStandardNode extends MCTSNode
{

    IBehaviour rolloutBehavior;
    public MCTSStandardNode(IFilter actionPrune, IBehaviour rolloutBehavior) {
        super(actionPrune);
        this.rolloutBehavior = rolloutBehavior;
    }

    public MCTSStandardNode( IBehaviour rolloutBehavior) {
        this.rolloutBehavior = rolloutBehavior;
    }

    public MCTSStandardNode(SimulationContext current, GameAction action, List<GameAction> rootActions, IFilter actionPrune) {
        super(current, action, rootActions, actionPrune);
        //System.err.println("rolloutBehavior is " + rolloutBehavior);
        if(rolloutBehavior!=null)
            this.context.setBehavior(rolloutBehavior);
    }

    @Override
    public MCTSNode nodeFactoryMethod(SimulationContext context, GameAction possibleAction, List<GameAction> rootActions) {
        MCTSStandardNode node = new MCTSStandardNode(context,possibleAction,rootActions,actionPrune);
        node.rolloutBehavior = this.rolloutBehavior;
        return node;
    }

    @Override
    public double rollOut(MCTSNode node, List<GameAction> validActions){
            SimulationContext simulation = getContext().clone();
            simulation.setBehavior(rolloutBehavior);
            simulation.playFromMiddle();
            return 1 - simulation.getWinningPlayerId();
    }
}