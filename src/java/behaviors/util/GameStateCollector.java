package behaviors.util;

import behaviors.simulation.SimulationContext;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.logic.GameLogic;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class GameStateCollector implements StateCollector
{
    private Random rand;
    private int collected;

    private IBehaviour behavior;

    public GameStateCollector(IBehaviour behavior)
    {
        rand = new Random();
        collected = 0;
        this.behavior = behavior;
    }

    @Override
    public List<SimulationContext> collectStates(int numStates, SimulationContext initialState, boolean parallel, boolean consoleOutput, Path logfile)
    {
        SimulationContext[] statesArr = new SimulationContext[numStates];

        initialState = initialState.clone();
        initialState.setBehavior(behavior);
        final SimulationContext initialStateFinal = initialState;
        if(parallel) {
            IntStream.range(0, numStates).parallel().forEach((int i) -> {
                statesArr[i] = collectState(initialStateFinal.clone());
                updateCollected();
                if (collected % 1000 == 0) {
                    Logger.log(collected + "/" + numStates + " states collected", consoleOutput, logfile);
                }
            });
        } else {
            IntStream.range(0, numStates).sequential().forEach((int i) -> {
                statesArr[i] = collectState(initialStateFinal.clone());
                if ((i + 1) % 1000 == 0) {
                    Logger.log((i + 1) + "/" + numStates + " states collected", consoleOutput, logfile);
                }
            });
        }

        return new ArrayList<>(Arrays.asList(statesArr));
    }

    private synchronized void updateCollected() { collected++; }

    @Override
    public List<SimulationContext> collectStates(int numStates, SimulationContext initialState, boolean parallel)
    {
        return collectStates(numStates, initialState, parallel, false, null);
    }

    @Override
    public List<SimulationContext> collectStates(int numStates, SimulationContext initialState)
    {
        return collectStates(numStates, initialState, true, false, null);
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
