package behaviors.critic;

import behaviors.simulation.SimulationContext;
import net.demilich.metastone.game.Player;

public interface Critic
{
    double getCritique(SimulationContext context, Player player);
    Critic clone();
}
