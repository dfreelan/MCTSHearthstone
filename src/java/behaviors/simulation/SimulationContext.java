package behaviors.simulation;

import java.util.List;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.logic.GameLogic;

public class SimulationContext implements Cloneable
{
    private GameContext context;

    public SimulationContext(GameContext context)
    {
        this.context = context;
    }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        context = new GameContext(player1, player2, logic, deckFormat);
    }

    @Override
    public SimulationContext clone()
    {
        return new SimulationContext(context.clone());
    }

    public boolean gameDecided()
    {
        return context.gameDecided();
    }

    public Player getActivePlayer()
    {
        return context.getActivePlayer();
    }

    public int getActivePlayerId()
    {
        return context.getActivePlayerId();
    }

    public int getWinningPlayerId()
    {
        return context.getWinningPlayerId();
    }

    public List<GameAction> getValidActions()
    {
        return context.getValidActions();
    }

    public void applyAction(int playerID, GameAction action)
    {
        context.getLogic().performGameAction(context.getActivePlayerId(), action);
    }

    public void playFromMiddle()
    {
        context.playFromMiddle();
    }
}
