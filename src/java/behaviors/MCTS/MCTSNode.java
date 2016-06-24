package behaviors.MCTS;

import behaviors.simulation.SimulationContext;
import behaviors.util.ActionValuePair;
import behaviors.util.IFilter;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by dfreelan on 6/22/16.
 */
public abstract class  MCTSNode {
    

    protected double player1Value;
    protected double numVisits;


    protected SimulationContext context;
    protected GameAction action;
    protected List<MCTSNode> children;
    protected Random rand;
    protected List<GameAction> rootActions;


    protected IFilter actionPrune;
    private final double epsilon = 1e-6;

    public abstract MCTSNode nodeFactoryMethod(SimulationContext context, GameAction possibleAction,List<GameAction> rootActions);

    public MCTSNode(IFilter actionPrune){
        this.actionPrune = actionPrune;
    }
    public MCTSNode(){
        this.actionPrune = (SimulationContext context, GameAction action) -> {
            return action.getActionType() == ActionType.SUMMON && action.getTargetKey() != null;
        };
    }

    public MCTSNode(SimulationContext current, GameAction action, List<GameAction> rootActions, IFilter actionPrune)
    {
        this(current, action,actionPrune);
        this.rootActions = rootActions;

    }

    public MCTSNode(SimulationContext current, GameAction action, IFilter actionPrune)
    {
        this.context = current;
        this.action = action;
        this.actionPrune = actionPrune;
        rand = new Random();
    }

    public void step(double exploreFactor)
    {
        List<MCTSNode> visited = new LinkedList<>();

        MCTSNode cur = this;
        visited.add(this);
        while (!cur.isLeaf()) {
            cur = cur.select(exploreFactor);
            visited.add(cur);
        }

        List<GameAction> validActions = null;
        if (!cur.context.gameDecided()) {
            validActions = cur.expand();
        }

        double value = -1;
        if (!cur.context.gameDecided()) {
            value = rollOut(cur, validActions);
        } else if (cur.context.getWinningPlayerId() == 0) {
            cur.player1Value = Double.POSITIVE_INFINITY;
            value = 1;
        } else if (cur.context.getWinningPlayerId() == 1) {
            cur.player1Value = Double.NEGATIVE_INFINITY;
            value = 0;
        } else {
            value = 0.5;
        }

        for (MCTSNode node : visited) {
            node.updateStats(value);
        }
    }

    public abstract double rollOut(MCTSNode node, List<GameAction> validActions);


    public MCTSNode select(double exploreFactor)
    {
        if(isLeaf()) {
            return null;
        }

        MCTSNode bestNode = children.get(0);
        double bestValue = bestNode.getUCB(numVisits, exploreFactor);

        for(int i = 1; i < children.size(); i++) {
            double value = children.get(i).getUCB(numVisits, exploreFactor);
            if(value > bestValue) {
                bestNode = children.get(i);
                bestValue = value;
            }
        }

        return bestNode;
    }

    public List<GameAction> expand()
    {
        List<GameAction> actions = null;
        if(action != null) {
            context = context.clone();
            context.applyAction(context.getActivePlayerId(), action);
            actions = context.getValidActions();
        } else {
            actions = rootActions;
            rootActions = null;
        }

        if(!context.gameDecided()) {
            for (GameAction possibleAction : actions) {
                if (!actionPrune.prune(context, possibleAction)) {
                    MCTSNode child = nodeFactoryMethod(context, possibleAction,null);
                    if (children == null) {
                        children = new LinkedList<>();
                    }
                    children.add(child);
                }
            }
            assert actions.size() > 0;
        }

        return actions;
    }

    public void updateStats(double player1Value)
    {
        numVisits++;
        this.player1Value += player1Value;
    }

    public boolean isLeaf()
    {
        return children == null || children.size() == 0;
    }

    public double getUCB(double totalVisits, double exploreFactor)
    {
        return getExploit() + getExplore(totalVisits, exploreFactor) + epsilon * rand.nextDouble();
    }

    public double getExplore(double totalVisits, double exploreFactor)
    {
        return exploreFactor * Math.sqrt(Math.log(totalVisits + epsilon) / numVisits);
    }

    public double getExploit()
    {
        if(context.getActivePlayerId() == 0) {
            return player1Value / (numVisits + epsilon);
        } else {
            return (1 - player1Value) / (numVisits + epsilon);
        }
    }

    public List<ActionValuePair> getChildValues(int playerID)
    {
        List<ActionValuePair> childValues = new ArrayList<>();
        if(children != null) {
            for (MCTSNode child : children) {
                childValues.add(new ActionValuePair(child.action, child.getValue(playerID)));
            }
        }
        return childValues;
    }

    public double getValue(int playerID)
    {
        switch(playerID) {
            case 0: return player1Value;
            case 1: return 1 - player1Value;
            default: return -1;
        }
    }

    public double getNumVisits()
    {
        return numVisits;
    }

    public SimulationContext getContext()
    {
        return context;
    }
}
