package main;

import behaviors.simulation.SimulationContext;
import behaviors.util.ArgumentUtils;
import behaviors.util.GameStateCollector;
import behaviors.util.StateCollector;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;

import java.util.List;

public class SampleCreator
{
    public static void main(String[] args)
    {
        int numSamples = 10000;
        StateCollector collector = new GameStateCollector(new PlayRandomBehaviour());

        if(ArgumentUtils.keyExists("samples", args)) {
            numSamples = Integer.parseInt(ArgumentUtils.argumentForKey("samples", args));
        }
        if(ArgumentUtils.keyExists("collector", args)) {
            collector = getCollector(ArgumentUtils.argumentForKey("collector", args));
        }

        //List<SimulationContext> states = trainConfig.collector.collectStates(trainConfig.numStates, trainConfig.initialState, trainConfig.parallel);
    }

    private static StateCollector getCollector(String name)
    {
        switch(name.toLowerCase()) {
            case "random":
                return new GameStateCollector(new PlayRandomBehaviour());
            default:
                throw new RuntimeException("Error: " + name + " sample collector does not exist");
        }
    }
}
