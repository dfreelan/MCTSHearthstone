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
    private GameContext context;

    public SimulationContext(GameContext context)
    {
        context.getLogic().setLoggingEnabled(false);
        this.context = context;
        context.setLogic(new SimulationLogic());
        //change the decks to use deterministic versions of the decks
        context.getPlayer1().setDeck(new SimulationCardCollection(context.getPlayer1().getDeck()));
        context.getPlayer2().setDeck(new SimulationCardCollection(context.getPlayer2().getDeck()));
   }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        context = new GameContext(player1, player2, logic, deckFormat);
    }

    //shuffle deck and make a random hand for my opponent
    public void Randomize(int myId) {

        //figure out who my opponent is
        Player opponent;
        if (myId == 0) {
            opponent = context.getPlayer2();
        } else {
            opponent = context.getPlayer1();
        }
        //discard his entire hand into his deck
        opponent.getDeck().addAll(opponent.getHand());
        int handSize = opponent.getHand().getCount();
        for (int k = 0; k < handSize; k++) {
            Card card = opponent.getHand().get(0);
            context.getLogic().removeCard(opponent.getId(), card);
        }
        //shuffle both decks
        context.getPlayer2().getDeck().shuffle();
        context.getPlayer1().getDeck().shuffle();

        //refill opponents hand
        for (int a = 0; a < handSize; a++) {
            context.getLogic().receiveCard(opponent.getId(), opponent.getDeck().removeFirst());
        }
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
        GameContext clone = context.clone();
        HashMap cloneMap = clone.getEnvironment();

        //this stuff is just making sure we REALLY deep clone this for battlecries.
        //TODO: only do this stuff if the action is a battlecry (if possible?)
        Stack<Minion> newStack = (Stack<Minion>) ((Stack<Minion>) context.getEnvironment().get(Environment.SUMMON_REFERENCE_STACK));
        if (newStack != null) {
            newStack = (Stack<Minion>) newStack.clone();
            for (int i = 0; i < newStack.size(); i++) {
                newStack.set(i, (Minion) ((Minion) newStack.get(i)).clone());
            }
            cloneMap.put(Environment.SUMMON_REFERENCE_STACK, newStack);
        }
        Card pending = (Card) context.getEnvironment().get(Environment.PENDING_CARD);
        if (pending != null) {
            pending = pending.clone();
            cloneMap.put(Environment.PENDING_CARD, pending);
        }
        cloneEntity(context,clone,Environment.TARGET_OVERRIDE,cloneMap);
        cloneEntity(context,clone,Environment.KILLED_MINION,cloneMap);
        cloneEntity(context,clone,Environment.ATTACKER_REFERENCE,cloneMap);
        cloneEntity(context,clone,Environment.EVENT_TARGET_REFERENCE_STACK,cloneMap);
        cloneEntity(context,clone,Environment.TARGET,cloneMap);


        Minion transform = (Minion) context.getEnvironment().get(Environment.TRANSFORM_REFERENCE);
        if (transform != null) {
            transform = transform.clone();
            cloneMap.put(Environment.TRANSFORM_REFERENCE, transform);
        }

        clone.getLogic().setLoggingEnabled(false);
        return new SimulationContext(clone);
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
