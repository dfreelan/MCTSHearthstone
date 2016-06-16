package behaviors.standardMCTS;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.cards.Card;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Created by dfreelan on 6/16/16.
 */
public class MCTSBehavior extends Behaviour
{
    double exploreFactor;
    int numTrees;
    int numIterations;
    double[][] accumulateStats;

    public MCTSBehavior(double exploreFactor, int numTrees, int numIterations)
    {
        super();
        this.exploreFactor = exploreFactor;
        this.numTrees = numTrees;
        this.numIterations = numIterations;

        accumulateStats = new double[numTrees][];
        for(int i = 0; i < accumulateStats.length; i++) {
            accumulateStats[i] = new double[0];
        }
    }

    public void runABunch(int index)
    {
        // doStep();

        // accumlateStast[index] = root.getValue;
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

    @Override
    public GameAction requestAction(GameContext gameContext, Player player, List<GameAction> list)
    {
       // IntStream.range(0, numTrees)
        return null;
    }
}
