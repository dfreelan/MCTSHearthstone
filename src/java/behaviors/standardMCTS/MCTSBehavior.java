package behaviors.standardMCTS;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import behaviors.util.ActionValuePair;
import behaviors.util.IArrayCompressor;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.cards.Card;

import behaviors.simulation.SimulationContext;

public class MCTSBehavior extends Behaviour
{
    private double exploreFactor;
    private int numTrees;
    private int numIterations;
    private IBehaviour rolloutBehavior;
    private IArrayCompressor<double[]> statCompressor;

    private String name = "MCTSBehavior";

    public MCTSBehavior(double exploreFactor, int numTrees, int numIterations, IBehaviour rolloutBehavior)
    {
        super();
        this.exploreFactor = exploreFactor;
        this.numTrees = numTrees;
        this.numIterations = numIterations;
        this.rolloutBehavior = rolloutBehavior;
        this.statCompressor = (double[][] accumulateStats) -> {
            double[] compressed = new double[accumulateStats.length];
            for(int i = 0; i < accumulateStats.length; i++) {
                double numValues = 0;
                double sum = 0;
                for(double value : accumulateStats[i]) {
                    if(value != -1) {
                        numValues++;
                        sum += value;
                    }
                }
                compressed[i] = sum / numValues;
            }
            return compressed;
        };
    }

    public MCTSBehavior(double exploreFactor, int numTrees, int numIterations, IBehaviour rolloutBehavior, IArrayCompressor<double[]> statCompressor)
    {
        super();
        this.exploreFactor = exploreFactor;
        this.numTrees = numTrees;
        this.numIterations = numIterations;
        this.rolloutBehavior = rolloutBehavior;
        this.statCompressor = statCompressor;
    }

    @Override
    public GameAction requestAction(GameContext gameContext, Player player, List<GameAction> list)
    {
        MCTSTree[] trees = new MCTSTree[numTrees];
        double[][] accumulateStats = new double[numTrees][];

        for(int i = 0; i < numTrees; i++) {
            MCTSNode root = new MCTSNode(new SimulationContext(gameContext), null, list);
            root.getContext().setBehavior(rolloutBehavior);
            root.getContext().randomize(player.getId());

            trees[i] = new MCTSTree(exploreFactor, root);

            accumulateStats[i] = new double[list.size()];
            for(int j = 0; j < accumulateStats[i].length; j++) {
                accumulateStats[i][j] = -1;
            }
        }

        Map<Integer, Integer> hashToIndex = new HashMap<>();
        for(int i = 0; i < list.size(); i++) {
            hashToIndex.put(actionHash(list.get(i)), i);
        }

        IntStream.range(0, numTrees).parallel().forEach((int i) -> runForest(trees, accumulateStats, hashToIndex, i));

        double[] actionValues = statCompressor.compress(accumulateStats);

        int maxIndex = 0;
        for(int i = 1; i < actionValues.length; i++) {
            if(actionValues[i] > actionValues[maxIndex]) {
                maxIndex = i;
            }
        }

        return list.get(maxIndex);
    }

    private void runForest(MCTSTree[] trees, double[][] accumulateStats, Map<Integer, Integer> actionHashToIndex, int treeIndex)
    {
        trees[treeIndex].run(numIterations / numTrees);

        MCTSNode root = trees[treeIndex].getRoot();
        List<ActionValuePair> actionValues = root.getChildValues(root.getContext().getActivePlayerId());
        for(ActionValuePair actionValue : actionValues) {
            int actionIndex = actionHashToIndex.get(actionHash(actionValue.action));
            accumulateStats[treeIndex][actionIndex] = actionValue.value;
        }
    }

    private int actionHash(GameAction action)
    {
        return action.toString().hashCode();
    }

    @Override
    public String getName() { return name; }

    public void setName(String newName) { name = newName; }

    @Override
    public List<Card> mulligan(GameContext gameContext, Player player, List<Card> list) {
        return new ArrayList<Card>();
    }
}
