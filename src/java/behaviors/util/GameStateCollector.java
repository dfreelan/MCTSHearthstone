package behaviors.util;

import behaviors.simulation.SimulationContext;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.logic.GameLogic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class GameStateCollector implements StateCollector
{
    private Random rand;

    private IBehaviour behavior;

    public GameStateCollector(IBehaviour behavior)
    {
        rand = new Random();
        this.behavior = behavior;
    }

    @Override
    public List<SimulationContext> collectStates(int numStates, SimulationContext initialState, boolean parallel)
    {
        SimulationContext[] statesArr = new SimulationContext[numStates];

        initialState = initialState.clone();
        initialState.setBehavior(behavior);
        final SimulationContext initialStateFinal = initialState;
        if(parallel) {
            IntStream.range(0, numStates).parallel().forEach((int i) -> statesArr[i] = collectState(initialStateFinal.clone()));
        } else {
            IntStream.range(0, numStates).sequential().forEach((int i) -> statesArr[i] = collectState(initialStateFinal.clone()));
        }

        return new ArrayList<>(Arrays.asList(statesArr));
    }

    @Override
    public List<SimulationContext> collectStates(int numStates, SimulationContext initialState)
    {
        return collectStates(numStates, initialState, true);
    }

    private SimulationContext collectState(SimulationContext simulation)
    {
        List<SimulationContext> states = new ArrayList<>();

        simulation.getGameContext().init();
        while (!simulation.gameDecided()) {
            simulation.getGameContext().startTurn(simulation.getActivePlayerId());

            while (simulation.getGameContext().playTurn()) {
                states.add(simulation.clone());
            }

            if (simulation.getGameContext().getTurn() > GameLogic.TURN_LIMIT) {
                break;
            }
        }

        return states.get(rand.nextInt(states.size() - 1));
    }
}
