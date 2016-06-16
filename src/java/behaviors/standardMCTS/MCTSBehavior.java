package behaviors.standardMCTS;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.cards.Card;

import java.util.List;

/**
 * Created by dfreelan on 6/16/16.
 */
public class MCTSBehavior extends Behaviour {
    double[] accumulateStats;

    @Override
    public String getName() {
        return "MCTSBehavior";
    }

    @Override
    public List<Card> mulligan(GameContext gameContext, Player player, List<Card> list) {
        return null;
    }

    @Override
    public GameAction requestAction(GameContext gameContext, Player player, List<GameAction> list) {




        return null;
    }

    public void runABunch(int index,HashMap blah){
       // doStep();
        //
        //

    }
}
