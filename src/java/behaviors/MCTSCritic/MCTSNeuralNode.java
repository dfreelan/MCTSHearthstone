package behaviors.MCTSCritic;

import behaviors.MCTS.MCTSNode;
import behaviors.critic.Critic;
import behaviors.critic.POVMode;
import behaviors.simulation.SimulationContext;

import behaviors.util.IFilter;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.IBehaviour;

import java.util.List;

public class MCTSNeuralNode extends MCTSNode
{
    private Critic critic;
    private POVMode povMode;

    public MCTSNeuralNode(IFilter actionPrune, Critic critic, POVMode povMode) {
        super(actionPrune);
        this.critic = critic;
        this.povMode = povMode;
    }

    public MCTSNeuralNode(Critic critic, POVMode povMode) {
        this.critic = critic;
        this.povMode = povMode;
    }

    public MCTSNeuralNode(SimulationContext current, GameAction action, List<GameAction> rootActions, Player rootPLayer, IFilter actionPrune) {
        super(current, action, rootActions, rootPLayer, actionPrune);
        //System.err.println("rolloutBehavior is " + rolloutBehavior);
    }

    @Override
    public MCTSNode nodeFactoryMethod(SimulationContext context, GameAction possibleAction, List<GameAction> rootActions, Player rootPlayer) {
        MCTSNeuralNode node = new MCTSNeuralNode(context, possibleAction, rootActions, rootPlayer, actionPrune);
        if(rootActions != null){
            node.critic = this.critic.clone();
        }else {
            node.critic = this.critic;
        }
        node.povMode = this.povMode;
        return node;
    }

    @Override
    public double rollOut(MCTSNode node, List<GameAction> validActions) {
        SimulationContext simulation = node.getContext().clone();
        Player pov = null;
        switch(povMode) {
            case SELF:
                pov = simulation.getActivePlayer();
                break;
            case OPPONENT:
                pov = simulation.getGameContext().getOpponent(simulation.getActivePlayer());
                break;
            case ROOT:
                pov = rootPlayer;
                break;
            case ROOT_OPPONENT:
                pov = simulation.getGameContext().getOpponent(rootPlayer);
                break;
            case AVERAGE:
                double p1Value = critic.getCritique(simulation, simulation.getGameContext().getPlayer1());
                double p2Value = critic.getCritique(simulation, simulation.getGameContext().getPlayer2());
                if(simulation.getActivePlayerId() == 0) {
                    p2Value = 1 - p2Value;
                } else {
                    p1Value = 1 - p1Value;
                }
                return (p1Value + p2Value) / 2;
        }

        double value = critic.getCritique(simulation, pov);
        if(simulation.getActivePlayerId() == 1){
            value = 1 - value;
        }
        return value;
    }
}