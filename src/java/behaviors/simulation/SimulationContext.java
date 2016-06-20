package behaviors.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import net.demilich.metastone.game.Environment;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.IBehaviour;
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
        GameContext clonedContext = context.clone();

        clonedContext.getLogic().setLoggingEnabled(false);
        clonedContext.setLogic(new SimulationLogic(clonedContext.getLogic()));
        //change the decks to use deterministic versions of the decks
        clonedContext.getPlayer1().setDeck(new SimulationCardCollection(clonedContext.getPlayer1().getDeck()));
        clonedContext.getPlayer2().setDeck(new SimulationCardCollection(clonedContext.getPlayer2().getDeck()));
        this.context = clonedContext;
   }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        context = new GameContext(player1, player2, logic, deckFormat);
        context.getLogic().setLoggingEnabled(false);
    }

    //shuffle deck and make a random hand for my opponent
    public void randomize(int playerID) {

        //figure out who my opponent is
        Player opponent;
        if (playerID == 0) {
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


        Stack<Entity> targetStack = (Stack<Entity>) ((Stack<Entity>) context.getEnvironment().get(Environment.SUMMON_REFERENCE_STACK));
        if (targetStack != null) {
            targetStack = (Stack<Entity>) targetStack.clone();
            for (int i = 0; i < targetStack.size(); i++) {
                targetStack.set(i, (Minion) ((Minion) targetStack.get(i)).clone());
            }
            cloneMap.put(Environment.SUMMON_REFERENCE_STACK, targetStack);
        }

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

    public void setBehavior(IBehaviour behavior)
    {
        context.getPlayer1().setBehaviour(behavior);
        context.getPlayer2().setBehaviour(behavior);
    }

    public List<GameAction> getValidActions()
    {
        return context.getValidActions();
    }
    public SimulationLogic getLogic(){
        return (SimulationLogic)context.getLogic();
    }
    public void applyAction(int playerID, GameAction action)
    {
        getLogic().simulationActive = true;
        getLogic().battlecries = null;
        getLogic().performGameAction(context.getActivePlayerId(), action);
        getLogic().simulationActive = false;

        if(action.getActionType() == ActionType.END_TURN){
            context.startTurn(context.getActivePlayerId());
        }
    }

    public void playFromMiddle()
    {
        context.playFromMiddle();
    }

    public void performBattlecryAction(GameAction battlecry) {
        boolean resolvedLate = getLogic().minion.getBattlecry().isResolvedLate();

        getLogic().performGameAction(context.getActivePlayerId(), battlecry);
        getLogic().checkForDeadEntities();

        if (resolvedLate) {
            getLogic().afterBattlecryLate();
        } else {
            getLogic().afterBattlecry();
        }

        getLogic().afterCardPlayed(context.getActivePlayerId(), getLogic().source.getCardReference());
        context.getEnvironment().remove(Environment.PENDING_CARD);

        context.getEnvironment().remove(Environment.TARGET);

        getLogic().minion = null;
        getLogic().resolveBattlecry = false;

    }
    public void play() { context.play(); }

    @Override
    public String toString(){
        System.err.println("SIMULATION CONTEXT:");
        return context.toString();
    }
}
