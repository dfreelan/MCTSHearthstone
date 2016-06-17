package behaviors.standardMCTS;

import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;

import behaviors.simulation.SimulationContext;
import behaviors.util.IFilter;
import behaviors.util.ActionValuePair;

public class MCTSNode
{
    private List<GameAction> rootActions;

    private double player1Value;
    private double numVisits;
    private SimulationContext context;
    private GameAction action;
    private List<MCTSNode> children;

    private Random rand;
    private final double epsilon = 1e-6;

    public MCTSNode(SimulationContext current, GameAction action, List<GameAction> rootActions)
    {
        this(current, action);
        this.rootActions = rootActions;
    }

    public MCTSNode(SimulationContext current, GameAction action)
    {
        this.context = current;
        this.action = action;

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
            validActions = cur.expand((context, gameAction) -> false);
        }

        double value = -1;
        if (!cur.context.gameDecided()) {
            value = rollOut(cur, validActions);
        } else if (cur.context.getWinningPlayerId() == 0) {
            player1Value = Double.POSITIVE_INFINITY;
            value = 0;
        } else if (cur.context.getWinningPlayerId() == 1) {
            player1Value = Double.NEGATIVE_INFINITY;
            value = 1;
        } else {
            player1Value = 0.5;
            value = -1;
        }

        for (MCTSNode node : visited) {
            node.updateStats(value);
        }
    }

    public double rollOut(MCTSNode node, List<GameAction> validActions)
    {
        System.err.println("node is : " + node);
        System.err.println("context is : " + node.context);
        SimulationContext simulation = node.context.clone();

        GameAction randAction = validActions.get(rand.nextInt(validActions.size()));
        simulation.applyAction(simulation.getActivePlayerId(), randAction);

        simulation.playFromMiddle();
        return 1 - simulation.getWinningPlayerId();
    }

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

    public List<GameAction> expand(IFilter filter)
    {
        List<GameAction> actions = null;
        if(action != null) {
            context = context.clone();
            context.applyAction(context.getActivePlayerId(), action);
            actions = context.getValidActions();
            action = null;
        } else {
            actions = rootActions;
            rootActions = null;
        }

        for(GameAction possibleAction : actions) {
            if(!filter.prune(context, possibleAction)) {
                MCTSNode child = new MCTSNode(context, possibleAction);
                if(children == null) {
                    children = new LinkedList<>();
                }
                children.add(child);
            }
        }
        if(actions.size() < 1){
            System.err.println("was unable to come with actions for the state:" + context.toString());
            System.err.println("action was: " + action);
            throw new RuntimeException();
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