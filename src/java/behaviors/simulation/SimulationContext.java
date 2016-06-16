package behaviors.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import net.demilich.metastone.game.Environment;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.logic.GameLogic;

public class SimulationContext implements Cloneable
{
    private GameContext state;

    public SimulationContext(GameContext state)
    {
        state.getLogic().setLoggingEnabled(false);
        this.state = state;
        state.setLogic(new SimulationLogic());
        
    }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        state = new GameContext(player1, player2, logic, deckFormat);
    }

    private void cloneEntity(GameContext state, GameContext clone, Environment e, HashMap cloneMap){
        Entity entity = (Entity) state.getEnvironment().get(e);
        if (entity != null) {
            entity = (Entity) entity.clone();
            cloneMap.put(e, entity);
        }
    }
    @Override
    public SimulationContext clone()
    {
        GameContext clone = state.clone();
        HashMap cloneMap = clone.getEnvironment();

        //this stuff is just making sure we REALLY deep clone this for battlecries.
        //TODO: only do this stuff if the action is a battlecry (if possible?)
        Stack<Minion> newStack = (Stack<Minion>) ((Stack<Minion>) state.getEnvironment().get(Environment.SUMMON_REFERENCE_STACK));
        if (newStack != null) {
            newStack = (Stack<Minion>) newStack.clone();
            for (int i = 0; i < newStack.size(); i++) {
                newStack.set(i, (Minion) ((Minion) newStack.get(i)).clone());
            }
            cloneMap.put(Environment.SUMMON_REFERENCE_STACK, newStack);
        }
        Card pending = (Card) state.getEnvironment().get(Environment.PENDING_CARD);
        if (pending != null) {
            pending = pending.clone();
            cloneMap.put(Environment.PENDING_CARD, pending);
        }
        cloneEntity(state,clone,Environment.TARGET_OVERRIDE,cloneMap);
        cloneEntity(state,clone,Environment.KILLED_MINION,cloneMap);
        cloneEntity(state,clone,Environment.ATTACKER_REFERENCE,cloneMap);
        cloneEntity(state,clone,Environment.EVENT_TARGET_REFERENCE_STACK,cloneMap);
        cloneEntity(state,clone,Environment.TARGET,cloneMap);


        Minion transform = (Minion) state.getEnvironment().get(Environment.TRANSFORM_REFERENCE);
        if (transform != null) {
            transform = transform.clone();
            cloneMap.put(Environment.TRANSFORM_REFERENCE, transform);
        }

        clone.getLogic().setLoggingEnabled(false);
        return new SimulationContext(clone);
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
