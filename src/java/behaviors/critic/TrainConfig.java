package behaviors.critic;

import behaviors.simulation.SimulationContext;
import behaviors.util.StateCollector;
import behaviors.util.StateJudge;

public class TrainConfig
{
    public int numStates;
    public SimulationContext initialState;
    public StateCollector collector;
    public StateJudge judge;
    public int nestAmount = 1;
    public boolean parallel;

    public TrainConfig(int numStates, SimulationContext initialState, StateCollector collector, StateJudge judge, boolean parallel)
    {
        this.numStates = numStates;
        this.initialState = initialState;
        this.collector = collector;
        this.judge = judge;
        this.parallel = parallel;
    }
}
