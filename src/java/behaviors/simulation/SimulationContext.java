package behaviors.simulation;

import java.util.ArrayList;
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
import net.demilich.metastone.game.targeting.EntityReference;

public class SimulationContext implements Cloneable
{
    private GameContext context;

    public SimulationContext(GameContext context)
    {
        context.getLogic().setLoggingEnabled(false);
        GameContext clonedContext = context.clone();

        clonedContext.getLogic().setLoggingEnabled(false);
        if(! (context.getLogic() instanceof SimulationLogic)) {
            clonedContext.setLogic(new SimulationLogic(clonedContext.getLogic()));
        }
        //change the decks to use deterministic versions of the decks
        clonedContext.getPlayer1().setDeck(new SimulationCardCollection(clonedContext.getPlayer1().getDeck()));
        clonedContext.getPlayer2().setDeck(new SimulationCardCollection(clonedContext.getPlayer2().getDeck()));
        this.context = clonedContext;
   }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        context = new GameContext(player1, player2, logic, deckFormat);
        context.getLogic().setLoggingEnabled(false);
        context.setLogic(new SimulationLogic(context.getLogic()));
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
        GameContext clone = this.context.clone();
        clone.setLogic(getLogic().clone());

        HashMap cloneMap = (HashMap)clone.getEnvironment();

        Stack<EntityReference> newStack = (Stack<EntityReference>) ((Stack<EntityReference>) context.getEnvironment().get(Environment.SUMMON_REFERENCE_STACK));
        if (newStack != null) {
           newStack = (Stack<EntityReference>) newStack.clone();
            for (int i = 0; i < newStack.size(); i++) {
                    newStack.set(i, (EntityReference) ((EntityReference) newStack.get(i)));
                }
            cloneMap.remove(Environment.SUMMON_REFERENCE_STACK);
           cloneMap.put(Environment.SUMMON_REFERENCE_STACK, newStack);
        }

        newStack = (Stack<EntityReference>) ((Stack<EntityReference>) context.getEnvironment().get(Environment.EVENT_TARGET_REFERENCE_STACK));
        if (newStack != null) {
            newStack = (Stack<EntityReference>) newStack.clone();
            for (int i = 0; i < newStack.size(); i++) {
                newStack.set(i, (EntityReference) ((EntityReference) newStack.get(i)));
            }
            cloneMap.remove(Environment.EVENT_TARGET_REFERENCE_STACK);
            cloneMap.put(Environment.EVENT_TARGET_REFERENCE_STACK, newStack);
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
        List<GameAction> actions = new ArrayList<GameAction>();
        if (getLogic().battlecries != null) {
            actions = getLogic().battlecries;
            getLogic().battlecries = null;
        } else {
            actions = context.getValidActions();
        }
        return actions;
    }
    public SimulationLogic getLogic(){
        return (SimulationLogic)context.getLogic();
    }
    public void applyAction(int playerID, GameAction action)
    {
        getLogic().battlecries = null;
        context.setIsInBattleCry(false);

        if(action.getActionType() == ActionType.BATTLECRY){
            performBattlecryAction(action);

        }else {
            getLogic().simulationActive = true;
            getLogic().performGameAction(context.getActivePlayerId(), action);
            getLogic().simulationActive = false;
            context.setIsInBattleCry(false);

            if (action.getActionType() == ActionType.END_TURN) {
                context.startTurn(context.getActivePlayerId());
            }
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
        context.setPendingCard(null);
        getLogic().minion = null;
        getLogic().resolveBattlecry = false;
        getLogic().battlecries = null;

    }
    public void play() { context.play(); }

    @Override
    public String toString(){
        System.err.println("SIMULATION CONTEXT:");
        return context.toString();
    }
}
