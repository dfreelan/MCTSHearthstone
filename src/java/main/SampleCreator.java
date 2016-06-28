package main;

import behaviors.DummyBehavior;
import behaviors.MCTS.MCTSBehavior;
import behaviors.simulation.SimulationContext;
import behaviors.standardMCTS.MCTSStandardNode;
import behaviors.util.ArgumentUtils;
import behaviors.util.BehaviorConfig;
import behaviors.util.FeatureCollector;
import behaviors.util.GameStateCollector;
import behaviors.util.GlobalConfig;
import behaviors.util.Logger;
import behaviors.util.MetastoneUtils;
import behaviors.util.NeuralUtils;
import behaviors.util.StateCollector;
import behaviors.util.StateJudge;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.gui.cards.CardProxy;
import org.nd4j.linalg.dataset.DataSet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SampleCreator
{
    public static void main(String[] args)
    {
        double startTime = System.nanoTime();

        Path saveLocation = null;
        boolean overwriteSave = false;
        int numSamples = 10000;
        StateCollector collector = new GameStateCollector(new PlayRandomBehaviour());
        String deckName1 = null;
        String deckName2 = null;

        //used for -trees, -iterations, and -explore arguments
        BehaviorConfig judgeConfig = new BehaviorConfig(0);
        judgeConfig.applyArguments(args);

        //used for -console and -log arguments
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.applyArguments(args);

        if(ArgumentUtils.keyExists("-overwrite", args)) {
            overwriteSave = ArgumentUtils.parseBoolean(ArgumentUtils.argumentForKey("-overwrite", args));
        }
        if(ArgumentUtils.keyExists("-save", args)) {
            saveLocation = Paths.get(ArgumentUtils.argumentForKey("-save", args));
        } else {
            throw new RuntimeException("Error: save location not specified");
        }
        if(ArgumentUtils.keyExists("-samples", args)) {
            numSamples = Integer.parseInt(ArgumentUtils.argumentForKey("-samples", args));
        }
        if(ArgumentUtils.keyExists("-collector", args)) {
            collector = getCollector(ArgumentUtils.argumentForKey("-collector", args));
        }
        if(ArgumentUtils.keyExists("-deck", args)) {
            deckName1 = ArgumentUtils.argumentForKey("-deck", args);
            deckName2 = deckName1;
        } else {
            throw new RuntimeException("Error: no deck specified");
        }
        if(ArgumentUtils.keyExists("-deck2", args)) {
            deckName2 = ArgumentUtils.argumentForKey("-deck2", args);
        }

        if(Files.exists(saveLocation)) {
            if(overwriteSave) {
                try {
                    Files.delete(saveLocation);
                } catch(Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error deleting old save " + saveLocation.toString());
                }
            } else {
                throw new RuntimeException("Error: " + saveLocation.toString() + " already exists and would be overwritten");
            }
        }

        new CardProxy();
        Deck deck1 = MetastoneUtils.loadDeck(deckName1);
        Deck deck2 = MetastoneUtils.loadDeck(deckName2);
        IBehaviour behavior1 = new DummyBehavior();
        IBehaviour behavior2 = new DummyBehavior();
        SimulationContext initialState = MetastoneUtils.createContext(deck1, deck2, behavior1, behavior2);

        List<SimulationContext> states = collector.collectStates(numSamples, initialState, true, globalConfig.consoleOutput, globalConfig.logFile);
        Logger.log(numSamples + "/" + numSamples + " states collected", globalConfig.consoleOutput, globalConfig.logFile);
        FeatureCollector fCollector = new FeatureCollector(initialState.getGameContext(), initialState.getGameContext().getPlayer1());
        StateJudge judge = new MCTSBehavior(judgeConfig, new MCTSStandardNode(new PlayRandomBehaviour()));

        DataSet samples = NeuralUtils.getDataSet(states, numSamples, fCollector, judge, globalConfig.consoleOutput, globalConfig.logFile, saveLocation);
        Logger.log(numSamples + "/" + numSamples + " states labeled", globalConfig.consoleOutput, globalConfig.logFile);
        try {
            Files.deleteIfExists(saveLocation);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting old sample backup");
        }
        samples.save(saveLocation.toFile());

        double duration = (System.nanoTime() - startTime) / 1e9;
        String timeUnit = "s";
        if(duration > 60) {
            timeUnit = "min";
            duration /= 60;
        }
        if(duration > 60) {
            timeUnit = "hr";
            duration /= 60;
        }
        Logger.log("Completed without incident in " + duration + timeUnit, globalConfig.consoleOutput, globalConfig.logFile);
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
