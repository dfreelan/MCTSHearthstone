package behaviors.util;

import behaviors.simulation.SimulationContext;

import java.nio.file.Path;
import java.util.List;

public interface StateCollector
{
    List<SimulationContext> collectStates(int numStates, SimulationContext initialState, boolean parallel, boolean consoleOutput, Path logfile);
    List<SimulationContext> collectStates(int numStates, SimulationContext initialState, boolean parallel);
    List<SimulationContext> collectStates(int numStates, SimulationContext initialState);
}
