package behaviors.standardMCTS;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import behaviors.util.ActionValuePair;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.cards.Card;

import behaviors.simulation.SimulationContext;

/**
 * Created by dfreelan on 6/16/16.
 */
public class MCTSBehavior extends Behaviour
{
    double exploreFactor;
    int numTrees;
    int numIterations;
    IBehaviour rolloutBehavior;

    public MCTSBehavior(double exploreFactor, int numTrees, int numIterations, IBehaviour rolloutBehavior)
    {
        super();
        this.exploreFactor = exploreFactor;
        this.numTrees = numTrees;
        this.numIterations = numIterations;
        this.rolloutBehavior = rolloutBehavior;
    }

    @Override
    public GameAction requestAction(GameContext gameContext, Player player, List<GameAction> list)
    {
        MCTSTree[] trees = new MCTSTree[numTrees];
        double[][] accumulateStats = new double[numTrees][];

        MCTSNode root = new MCTSNode(new SimulationContext(gameContext), null);
        root.getContext().setBehavior(rolloutBehavior);

        for(int i = 0; i < numTrees; i++) {
            trees[i] = new MCTSTree(exploreFactor, root);
            accumulateStats[i] = new double[list.size()];
        }

        Map<Integer, Integer> hashToIndex = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            hashToIndex.put(actionHash(list.get(i)), i);
        }

        IntStream.range(0, numTrees).parallel().forEach((int i) -> runForest(trees, accumulateStats, hashToIndex, i));

        //collapse accumulated stats into 1D array using a functional interface
        //find action with highest value
        //return that action

        return null;
    }

    public void runForest(MCTSTree[] trees, double[][] accumulateStats, Map<Integer, Integer> actionHashToIndex, int treeIndex)
    {
        trees[treeIndex].run(numIterations / numTrees);

        MCTSNode root = trees[treeIndex].getRoot();
        List<ActionValuePair> actionValues = root.getChildValues(root.getContext().getActivePlayerId());
        for(ActionValuePair actionValue : actionValues) {
            int actionIndex = actionHashToIndex.get(actionValue.action);
            accumulateStats[treeIndex][actionIndex] = actionValue.value;
        }
    }

    public int actionHash(GameAction action)
    {
        return action.getSource().hashCode() + action.getTargetKey().hashCode() * 31;
    }

    @Override
    public String getName() {
        return "MCTSBehavior";
    }

    @Override
    public List<Card> mulligan(GameContext gameContext, Player player, List<Card> list) {
        return null;
    }
}
