package behaviors.standardMCTS;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

import behaviors.MCTS.MCTSNode;
import net.demilich.metastone.game.actions.GameAction;


import behaviors.simulation.SimulationContext;
import behaviors.util.IFilter;
import behaviors.util.ActionValuePair;

public class MCTSStandardNode implements MCTSNode
{
    private List<GameAction> rootActions;

    private double player1Value;
    private double numVisits;
    private SimulationContext context;
    private GameAction action;
    private List<MCTSStandardNode> children;

    private Random rand;
    private final double epsilon = 1e-6;

    public MCTSStandardNode(SimulationContext current, GameAction action, List<GameAction> rootActions)
    {
        this(current, action);
        this.rootActions = rootActions;
    }

    public MCTSStandardNode(SimulationContext current, GameAction action)
    {
        this.context = current;
        this.action = action;

        rand = new Random();
    }

    public void step(double exploreFactor, IFilter actionPrune)
    {
        List<MCTSStandardNode> visited = new LinkedList<>();

        MCTSStandardNode cur = this;
        visited.add(this);
        while (!cur.isLeaf()) {
            cur = cur.select(exploreFactor);
            visited.add(cur);
        }

        List<GameAction> validActions = null;
        if (!cur.context.gameDecided()) {
            validActions = cur.expand(actionPrune);
        }

        double value = -1;
        if (!cur.context.gameDecided()) {
            value = rollOut(cur, validActions);
        } else if (cur.context.getWinningPlayerId() == 0) {
            player1Value = Double.POSITIVE_INFINITY;
            value = 1;
        } else if (cur.context.getWinningPlayerId() == 1) {
            player1Value = Double.NEGATIVE_INFINITY;
            value = 0;
        } else {
            player1Value = 0.5;
            value = -1;
        }

        for (MCTSStandardNode node : visited) {
            node.updateStats(value);
        }
    }

    public double rollOut(MCTSStandardNode node, List<GameAction> validActions)
    {
        SimulationContext simulation = node.context.clone();
        simulation.playFromMiddle();
        return 1 - simulation.getWinningPlayerId();
    }

    public MCTSStandardNode select(double exploreFactor)
    {
        if(isLeaf()) {
            return null;
        }

        MCTSStandardNode bestNode = children.get(0);
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

    public List<GameAction> expand(IFilter actionPrune)
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
                    MCTSStandardNode child = new MCTSStandardNode(context, possibleAction);
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
            for (MCTSStandardNode child : children) {
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