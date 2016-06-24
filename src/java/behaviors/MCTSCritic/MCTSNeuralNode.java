package behaviors.MCTSCritic;

import behaviors.MCTS.MCTSNode;
import behaviors.critic.Critic;
import behaviors.simulation.SimulationContext;

import behaviors.util.IFilter;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.IBehaviour;

import java.util.List;

/**
 * Created by dfreelan on 6/23/16.
 */
public class MCTSNeuralNode extends MCTSNode {


    Critic critic;
    public MCTSNeuralNode(IFilter actionPrune, Critic critic) {
        super(actionPrune);
        this.critic = critic;
    }

    public MCTSNeuralNode( Critic critic) {
        this.critic = critic;
    }

    public MCTSNeuralNode(SimulationContext current, GameAction action, List<GameAction> rootActions, IFilter actionPrune) {
        super(current, action, rootActions, actionPrune);
        //System.err.println("rolloutBehavior is " + rolloutBehavior);

    }

    @Override
    public MCTSNode nodeFactoryMethod(SimulationContext context, GameAction possibleAction, List<GameAction> rootActions) {
        MCTSNeuralNode node = new MCTSNeuralNode(context,possibleAction,rootActions,actionPrune);
        if(rootActions != null){
            node.critic = this.critic.clone();
        }else {
            node.critic = this.critic;
        }
        return node;
    }

    @Override
    public double rollOut(MCTSNode node, List<GameAction> validActions){
        SimulationContext simulation = node.getContext().clone();
        double value = critic.getCritique(simulation,simulation.getActivePlayer());
        if(simulation.getActivePlayerId() == 1){
            value = 1-value;
        }

        return value;

    }
}