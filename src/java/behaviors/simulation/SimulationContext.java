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
import net.demilich.metastone.game.actions.PlayCardAction;
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
        GameContext clonedContext = deepCloneContext(context);
        this.context = clonedContext;

        clonedContext.getLogic().setLoggingEnabled(false);
        if(!(context.getLogic() instanceof SimulationLogic)) {
            if(!context.getSummonReferenceStack().isEmpty() && context.getSummonReferenceStack().peek() !=null){
                getLogic().minion = (Minion)context.resolveSingleTarget(context.getSummonReferenceStack().peek());
                getLogic().source = getLogic().minion.getSourceCard();//(Card)context.resolveCardReference(((PlayCardAction)previousAction).getCardReference());
            }

            clonedContext.setLogic(new SimulationLogic(clonedContext.getLogic()));

            //change the decks to use deterministic versions of the decks
            clonedContext.getPlayer1().setDeck(new SimulationCardCollection(clonedContext.getPlayer1().getDeck()));
            clonedContext.getPlayer2().setDeck(new SimulationCardCollection(clonedContext.getPlayer2().getDeck()));
        }
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
        GameContext clone = deepCloneContext();
        clone.setLogic(getLogic().clone());
        clone.getPlayer1().setBehaviour(clone.getPlayer1().getBehaviour().clone());
        clone.getPlayer2().setBehaviour(clone.getPlayer2().getBehaviour().clone());
        clone.getLogic().setLoggingEnabled(false);
        return new SimulationContext(clone);
    }
    private GameContext deepCloneContext(){
        return deepCloneContext(this.context);

    }
    private GameContext deepCloneContext(GameContext context){
        GameContext clone = context.clone();
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
        return clone;
    }

    public GameContext getGameContext() { return context; }

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
        context.getPlayer1().setBehaviour(behavior.clone());
        context.getPlayer2().setBehaviour(behavior.clone());
    }

    public List<GameAction> getValidActions()
    {
        List<GameAction> actions = new ArrayList<>();
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
       if(action==null){
           System.err.println("ACTION WAS NULL");
           throw new RuntimeException("action cannot be null");
       }
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
        return "Simulation Context: " + context.toString();
    }
}
