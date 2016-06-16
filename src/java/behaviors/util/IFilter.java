package behaviors.util;

import net.demilich.metastone.game.actions.GameAction;

import behaviors.simulation.SimulationContext;

@FunctionalInterface
public interface IFilter
{
    boolean prune(SimulationContext current, GameAction action);
}
