package behaviors.standardMCTS;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.cards.Card;

import behaviors.util.ActionValuePair;
import behaviors.util.IArrayCompressor;
import behaviors.util.IFilter;
import behaviors.simulation.SimulationContext;

public class MCTSBehavior extends Behaviour
{
    private double exploreFactor;
    private int numTrees;
    private int numIterations;
    private IBehaviour rolloutBehavior;
    private IArrayCompressor<double[]> statCompressor;
    private IFilter actionPrune;
    private GameAction previousAction = null;
    private String name = "MCTSBehavior";

    public MCTSBehavior(double exploreFactor, int numTrees, int numIterations, IBehaviour rolloutBehavior)
    {
        super();
        this.exploreFactor = exploreFactor;
        this.numTrees = numTrees;
        this.numIterations = numIterations;
        this.rolloutBehavior = rolloutBehavior;
        this.statCompressor = (double[][] accumulateStats) -> {
            double[] compressed = new double[accumulateStats[0].length];
            for(int action = 0; action < accumulateStats[0].length; action++) {
                double numValues = 0;
                double sum = 0;
                for(int tree = 0; tree < accumulateStats.length; tree++) {
                    if (accumulateStats[tree][action] != -1) {
                        numValues++;
                        sum += accumulateStats[tree][action];
                    }
                }
                compressed[action] = sum / numValues;
            }
            return compressed;
        };
        this.actionPrune = (SimulationContext context, GameAction action) -> {
            return action.getActionType() == ActionType.SUMMON && action.getTargetKey() != null;
        };
    }

    public MCTSBehavior(double exploreFactor, int numTrees, int numIterations, IBehaviour rolloutBehavior, IArrayCompressor<double[]> statCompressor, IFilter actionPrune)
    {
        super();
        this.exploreFactor = exploreFactor;
        this.numTrees = numTrees;
        this.numIterations = numIterations;
        this.rolloutBehavior = rolloutBehavior;
        this.statCompressor = statCompressor;
        this.actionPrune = actionPrune;
    }

    @Override
    public GameAction requestAction(GameContext gameContext, Player player, List<GameAction> validActions)
    {
        if(validActions.size() == 1) {
            return validActions.get(0);
        }

        MCTSTree[] trees = new MCTSTree[numTrees];
        double[][] accumulateStats = new double[numTrees][];

        for(int i = 0; i < numTrees; i++) {

            MCTSNode root = new MCTSNode(new SimulationContext(gameContext,previousAction), null, validActions);
            root.getContext().setBehavior(rolloutBehavior);
            root.getContext().randomize(player.getId());

            trees[i] = new MCTSTree(exploreFactor, root, actionPrune);

            accumulateStats[i] = new double[validActions.size()];
            for(int j = 0; j < accumulateStats[i].length; j++) {
                accumulateStats[i][j] = -1;
            }
        }

        Map<Integer, Integer> hashToIndex = new HashMap<>();
        for(int i = 0; i < validActions.size(); i++) {
            hashToIndex.put(actionHash(validActions.get(i)), i);
        }

        IntStream.range(0, numTrees).parallel().forEach((int i) -> runForest(trees, accumulateStats, hashToIndex, i));

        double[] actionValues = statCompressor.compress(accumulateStats);

        int maxIndex = 0;
        for(int i = 1; i < actionValues.length; i++) {
            if(actionValues[i] > actionValues[maxIndex]) {
                maxIndex = i;
            }
        }


        previousAction = list.get(maxIndex);
        return previousAction;
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
