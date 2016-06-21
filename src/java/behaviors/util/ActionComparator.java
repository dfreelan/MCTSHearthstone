package behaviors.util;

import net.demilich.metastone.game.actions.GameAction;

import java.util.Comparator;

public class ActionComparator implements Comparator<GameAction>
{
    @Override
    public int compare(GameAction a1, GameAction a2)
    {
        return Integer.compare(a1.toString().hashCode(), a2.toString().hashCode());
    }
}
