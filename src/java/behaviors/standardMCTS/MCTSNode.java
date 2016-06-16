package behaviors.standardMCTS;

import java.util.List;
import java.util.ArrayList;

import net.demilich.metastone.game.actions.GameAction;

import behaviors.simulation.SimulationContext;
import behaviors.util.IFilter;
import behaviors.util.ActionValuePair;

public class MCTSNode
{
    private double player1Value;
    private double numVisits;
    private SimulationContext current;
    private GameAction action;
    private List<MCTSNode> children;

    public MCTSNode(SimulationContext current, GameAction action)
    {
        this.current = current;
        this.action = action;
    }

    public void step()
    {

    }

    public void expand(IFilter filter)
    {
        if(action != null) {
            current = current.clone();
            current.applyAction(current.getActivePlayerId(), action);
            action = null;
        }

        List<GameAction> actions = current.getValidActions();
        for(GameAction possibleAction : actions) {
            if(!filter.prune(current, possibleAction)) {
                MCTSNode child = new MCTSNode(current, possibleAction);
                children.add(child);
            }
        }
    }

    public List<ActionValuePair> getChildValues(int playerID)
    {
        List<ActionValuePair> childValues = new ArrayList<>();
        for(MCTSNode child : children) {
            childValues.add(new ActionValuePair(child.action, child.getValue(playerID)));
        }
        return childValues;
    }

    public double getValue(int playerID)
    {
        if(playerID == 0) {
            return player1Value;
        } else if(playerID == 1) {
            return 1 - player1Value;
        } else {
            return -1;
        }
    }

    public double getNumVisits()
    {
        return numVisits;
    }
}