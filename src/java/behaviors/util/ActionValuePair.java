package behaviors.util;

import net.demilich.metastone.game.actions.GameAction;

public class ActionValuePair
{
    public GameAction action;
    public double value;

    public ActionValuePair(GameAction action, double value)
    {
        this.action = action;
        this.value = value;
    }
}
