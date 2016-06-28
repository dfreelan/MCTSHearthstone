package behaviors.MCTSCritic;

import behaviors.MCTS.MCTSNode;
import behaviors.simulation.SimulationContext;
import behaviors.standardMCTS.MCTSStandardNode;
import behaviors.util.IFilter;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;

import java.util.List;

public class MCTSMixedNeuralNode extends MCTSNode
{
    private MCTSStandardNode standardNode;
    private MCTSNeuralNode neuralNode;
    private double standardWeight;

    public MCTSMixedNeuralNode(MCTSStandardNode standardNode, MCTSNeuralNode neuralNode, double standardWeight)
    {
        this.standardNode = standardNode;
        this.neuralNode = neuralNode;
        this.standardWeight = standardWeight;

        if(standardWeight < 0 || standardWeight > 1) {
            throw new RuntimeException("Error: standard weight must be between 0 and 1");
        }
    }

    public MCTSMixedNeuralNode(SimulationContext current, GameAction action, List<GameAction> rootActions, Player rootPlayer, IFilter actionPrune)
    {
        super(current, action, rootActions, rootPlayer, actionPrune);
    }

    @Override
    public MCTSNode nodeFactoryMethod(SimulationContext context, GameAction possibleAction, List<GameAction> rootActions, Player rootPlayer)
    {
        MCTSMixedNeuralNode node = new MCTSMixedNeuralNode(context, possibleAction, rootActions, rootPlayer, actionPrune);
        node.standardNode = (MCTSStandardNode) standardNode.nodeFactoryMethod(context, possibleAction, rootActions, rootPlayer);
        node.neuralNode = (MCTSNeuralNode) neuralNode.nodeFactoryMethod(context, possibleAction, rootActions, rootPlayer);
        node.standardWeight = standardWeight;
        return node;
    }

    @Override
    public double rollOut(MCTSNode node, List<GameAction> validActions)
    {
        if(standardWeight >= 0.99999) {
            return standardNode.rollOut(node, validActions);
        } else if(standardWeight < 0.00001) {
            return neuralNode.rollOut(node, validActions);
        } else {
            return standardWeight * standardNode.rollOut(node, validActions) + (1 - standardWeight) * neuralNode.rollOut(node, validActions);
        }
    }
}
