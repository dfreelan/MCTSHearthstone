package behaviors.simulation;

import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.logic.GameLogic;

public class SimulationContext implements Cloneable
{
    private GameContext state;

    public SimulationContext(GameContext state)
    {
        this.state = state;
    }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        state = new GameContext(player1, player2, logic, deckFormat);
    }

    @Override
    public SimulationContext clone()
    {
        GameContext cloneState = state.clone();
        return new SimulationContext(cloneState);
    }

    public Player getActivePlayer() {
        return state.getActivePlayer();
    }

    public int getActivePlayerId() {
        return state.getActivePlayerId();
    }

    public List<GameAction> getValidActions()
    {
        return state.getValidActions();
    }

    public void applyAction(int playerID, GameAction action)
    {
        state.getLogic().performGameAction(state.getActivePlayerId(), action);
    }
}
