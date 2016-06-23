package behaviors.util;

import behaviors.simulation.SimulationContext;
import net.demilich.metastone.game.Player;

public interface StateJudge
{
    double evaluate(SimulationContext context, Player pov);
}
