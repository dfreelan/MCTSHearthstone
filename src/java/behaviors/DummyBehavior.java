package behaviors;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.cards.Card;

import java.util.List;

public class DummyBehavior extends Behaviour
{
    @Override
    public DummyBehavior clone() { return this; }

    @Override
    public String getName() { return "DummyBehavior"; }

    @Override
    public GameAction requestAction(GameContext game, Player player, List<GameAction> validActions)
    {
        throw new RuntimeException("Error: using Dummy Behavior");
    }

    @Override
    public List<Card> mulligan(GameContext gameContext, Player player, List<Card> list)
    {
        throw new RuntimeException("Error: using Dummy Behavior");
    }
}
