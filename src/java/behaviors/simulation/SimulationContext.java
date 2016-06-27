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
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.game.targeting.EntityReference;

public class SimulationContext implements Cloneable
{
    private GameContext context;

    public SimulationContext(GameContext context)
    {
        context.getLogic().setLoggingEnabled(false);
        GameContext clonedContext = deepCloneContext(context);
        clonedContext.getLogic().setLoggingEnabled(false);
        if(!(clonedContext.getPlayer1().getDeck() instanceof SimulationCardCollection)) {
            //change the decks to use deterministic versions of the decks
            clonedContext.getPlayer1().setDeck(new SimulationCardCollection(clonedContext.getPlayer1().getDeck()));
            clonedContext.getPlayer2().setDeck(new SimulationCardCollection(clonedContext.getPlayer2().getDeck()));
        }
        this.context = clonedContext;
    }

    public SimulationContext(GameContext context, GameAction previousAction) {
        this(context);
    }

    public SimulationContext(Player player1, Player player2, GameLogic logic, DeckFormat deckFormat)
    {
        context = new GameContext(player1, player2, logic, deckFormat);
        context.getLogic().setLoggingEnabled(false);
        //context.setLogic(new SimulationLogic(context.getLogic()));
    }

    //shuffle deck and make a random hand for my opponent
    public void randomize(int playerID)
    {
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


    @Override
    public SimulationContext clone()
    {
        return new SimulationContext(context);
    }

    private GameContext deepCloneContext()
    {
        return deepCloneContext(this.context);
    }

    private GameContext deepCloneContext(GameContext context)
    {
        GameContext clone = context.clone();

        clone.getPlayer1().setBehaviour(clone.getPlayer1().getBehaviour().clone());
        clone.getPlayer2().setBehaviour(clone.getPlayer2().getBehaviour().clone());

        HashMap cloneMap = (HashMap)clone.getEnvironment();

        Stack<EntityReference> newStack = (Stack<EntityReference>) ((Stack<EntityReference>) context.getEnvironment().get(Environment.SUMMON_REFERENCE_STACK));
        if (newStack != null) {
            Stack<EntityReference> oldStack =  newStack;
            newStack = (Stack<EntityReference>) newStack.clone();
            for (int i = 0; i < newStack.size(); i++) {
                newStack.set(i, new EntityReference(oldStack.get(i).getId()));
            }
            cloneMap.remove(Environment.SUMMON_REFERENCE_STACK);
            cloneMap.put(Environment.SUMMON_REFERENCE_STACK, newStack);
        }

        newStack = (Stack<EntityReference>) ((Stack<EntityReference>) context.getEnvironment().get(Environment.EVENT_TARGET_REFERENCE_STACK));
        if (newStack != null) {
            Stack<EntityReference> oldStack =  newStack;
            newStack = (Stack<EntityReference>) newStack.clone();
            for (int i = 0; i < newStack.size(); i++) {
                newStack.set(i, new EntityReference( oldStack.get(i).getId()));
            }
            cloneMap.remove(Environment.EVENT_TARGET_REFERENCE_STACK);
            cloneMap.put(Environment.EVENT_TARGET_REFERENCE_STACK, newStack);
        }

        return clone;
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
        context.getPlayer1().setBehaviour(behavior.clone());
        context.getPlayer1().setName(behavior.getName());

        context.getPlayer2().setBehaviour(behavior.clone());
        context.getPlayer2().setName(behavior.getName());
    }

    public List<GameAction> getValidActions()
    {
        List<GameAction> actions = new ArrayList<GameAction>();
        actions = context.getValidActions();
        return actions;
    }

    public void applyAction(int playerID, GameAction action)
    {
        //getLogic().simulationActive = true;
        context.getLogic().performGameAction(context.getActivePlayerId(), action);
        if (action.getActionType() == ActionType.END_TURN) {
            context.startTurn(context.getActivePlayerId());
        }
    }

    public void playFromMiddle()
    {
        context.playFromMiddle();
    }

    public void play() { context.play(); }

    public GameContext getGameContext(){
        return this.context;
    }

    @Override
    public String toString()
    {
        return "Simulation Context: " + context.toString();
    }
}
