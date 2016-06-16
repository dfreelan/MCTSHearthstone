package behaviors.util;

import behaviors.simulation.SimulationContext;
import net.demilich.metastone.game.actions.GameAction;

@FunctionalInterface
public interface IFilter
{
    boolean prune(SimulationContext current, GameAction action);
}
