package main;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.stream.IntStream;

import behaviors.MCTSCritic.MCTSNeuralNode;
import behaviors.critic.NeuralNetworkCritic;
import behaviors.critic.TrainConfig;
import behaviors.heuristic.HeuristicBehavior;
import behaviors.standardMCTS.MCTSStandardNode;
import behaviors.util.ArgumentUtils;
import behaviors.util.BehaviorConfig;
import behaviors.util.FeatureCollector;
import behaviors.util.GlobalConfig;
import behaviors.util.Logger;
import behaviors.util.RandomStateCollector;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.heroes.MetaHero;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.gui.cards.CardProxy;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.gui.deckbuilder.DeckFormatProxy;
import net.demilich.metastone.gui.deckbuilder.DeckProxy;
import net.demilich.metastone.gui.deckbuilder.importer.HearthPwnImporter;

import behaviors.simulation.SimulationContext;
import behaviors.MCTS.MCTSBehavior;
import org.apache.commons.configuration.Configuration;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.stepfunctions.DefaultStepFunction;
import org.deeplearning4j.nn.conf.stepfunctions.NegativeDefaultStepFunction;
import org.deeplearning4j.nn.conf.stepfunctions.StepFunction;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.SerializationUtils;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MetastoneTester
{
    private static final String defaultNetworkConfigLocation = "default_network_config.json";

    private static double[] stats; //0: ties, 1: wins, 2: losses, 3: time elapsed, 4: avg time per game

    public static void main(String[] args)
    {
        long beginTime = System.nanoTime();
        stats = new double[5];

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.applyArguments(args);

        BehaviorConfig behaviorConfig1 = new BehaviorConfig(0);
        BehaviorConfig behaviorConfig2 = new BehaviorConfig(1);

        behaviorConfig1.applyArguments(args);
        behaviorConfig1.copyTo(behaviorConfig1);

        behaviorConfig2.applyArguments(args);

        if(globalConfig.logFile != null && Files.exists(globalConfig.logFile)) {
            try {
                Files.delete(globalConfig.logFile);
            } catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error deleting old log file " + globalConfig.logFile.toString());
            }
        }

        new CardProxy();
        Deck deck1 = loadDeck(behaviorConfig1.deckName);
        Deck deck2 = loadDeck(behaviorConfig2.deckName);
        IBehaviour behavior = new MCTSBehavior(behaviorConfig1, new MCTSStandardNode(new PlayRandomBehaviour()));
        IBehaviour behavior2 = new PlayRandomBehaviour();
        SimulationContext game = createContext(deck1, deck2, behavior, behavior2);

        if(ArgumentUtils.keyExists("-behavior", args)) {
            String behaviorArg = ArgumentUtils.argumentForKey("-behavior", args);
            behavior = getBehavior(behaviorArg, behaviorConfig1, game, game.getGameContext().getPlayer1());
            game.getGameContext().getPlayer1().setBehaviour(behavior);
        }
        if(ArgumentUtils.keyExists("-behavior2", args)) {
            String behavior2Arg = ArgumentUtils.argumentForKey("-behavior2", args);
            behavior2 = getBehavior(behavior2Arg, behaviorConfig2, game, game.getGameContext().getPlayer2());
            game.getGameContext().getPlayer2().setBehaviour(behavior2);
        }

        game.getGameContext().getPlayer1().setName(behavior.getName());
        game.getGameContext().getPlayer2().setName(behavior2.getName());

        long beginGamesTime = System.nanoTime();
        if(globalConfig.parallel) {
            IntStream.range(0, globalConfig.simulations).parallel().forEach((int i) -> runSimulation(game.clone(), i, globalConfig));
        } else {
            IntStream.range(0, globalConfig.simulations).sequential().forEach((int i) -> runSimulation(game.clone(), i, globalConfig));
        }

        stats[3] = (System.nanoTime() - beginTime) / 1e9;
        stats[4] = (System.nanoTime() - beginGamesTime) / 1e9 / globalConfig.simulations;
        printStats(stats, globalConfig, true);
    }

    private static void runSimulation(SimulationContext game, int gameNum, GlobalConfig globalConfig)
    {
        game.randomize(0);
        game.randomize(1);
        game.play();
        updateStats(game.getWinningPlayerId());
        Logger.log("Finished Simulation[" + gameNum + "], Result = " + resultString(game.getWinningPlayerId()), globalConfig.consoleOutput, globalConfig.logFile);
        printStats(stats, globalConfig, false);
    }

    private static void printStats(double[] stats, GlobalConfig globalConfig, boolean includeTime)
    {
        String status = "";
        status += "Wins: " + stats[1] + "\n";
        status += "Losses: " + stats[2] + "\n";
        status += "Ties: " + stats[0] + "\n";
        if(includeTime) {
            status += "Time Elapsed: " + stats[3] + "s\n";
            status += "Average Time Per Game: " + stats[4] + "s\n";
        }
        Logger.log(status, globalConfig.consoleOutput, globalConfig.logFile);
    }

    private static String resultString(int result)
    {
        switch(result) {
            case -1: return "Tie";
            case 0: return "Win";
            case 1: return "Loss";
            default: return "UNDEFINED";
        }
    }

    private static synchronized void updateStats(int result)
    {
        stats[result + 1]++;
    }

    private static IBehaviour getBehavior(String name, BehaviorConfig behaviorConfig, SimulationContext game, Player player)
    {
        switch(name.toLowerCase()) {
            case "random": return new PlayRandomBehaviour();
            case "heuristic": return new HeuristicBehavior();
            case "gamestate": return new GameStateValueBehaviour();
            case "mcts": return new MCTSBehavior(behaviorConfig, new MCTSStandardNode(new PlayRandomBehaviour()));
            case "mctsheuristic":
                MCTSBehavior behavior = new MCTSBehavior(behaviorConfig, new MCTSStandardNode(new HeuristicBehavior()));
                behavior.setName("MCTSHeuristicBehavior");
                return behavior;
            case "mctsneural":
                System.err.println("THE gamecontext in the NN is" + game.toString());

                MultiLayerConfiguration networkConfig;
                if(behaviorConfig.networkConfigFile == null) {
                    networkConfig = defaultNetworkConfig(game, player);
                } else {
                    //TODO: load config from .json file
                    networkConfig = null;
                }

                MCTSBehavior neural = null;
                if(behaviorConfig.loadNetworkFile == null) {
                    TrainConfig trainConfig = new TrainConfig(5000, game, new RandomStateCollector(new PlayRandomBehaviour()),
                            new MCTSBehavior(behaviorConfig.exploreFactor, 10, 1000, new MCTSStandardNode(new PlayRandomBehaviour())), true);

                    neural = new MCTSBehavior(behaviorConfig, new MCTSNeuralNode(new NeuralNetworkCritic(networkConfig, trainConfig, Paths.get("neural_network.dat"))));
                } else {
                    neural = new MCTSBehavior(behaviorConfig, new MCTSNeuralNode(new NeuralNetworkCritic(behaviorConfig.loadNetworkFile, game)));
                }

                neural.setName("MCTSNeuralBehavior");
                return neural;
            default: throw new RuntimeException("Error: " + name + " behavior does not exist.");
        }
    }

    private static MultiLayerConfiguration defaultNetworkConfig(SimulationContext game, Player player)
    {
        FeatureCollector fCollector = new FeatureCollector(game.getGameContext(), player);
        MultiLayerConfiguration networkConfig = new NeuralNetConfiguration.Builder()
                .learningRate(1e-1).learningRateDecayPolicy(LearningRatePolicy.Inverse).lrPolicyDecayRate(1e-4).lrPolicyPower(0.75)
                .iterations(1000).stepFunction(new NegativeDefaultStepFunction())

                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .list(3)
                .layer(0, new DenseLayer.Builder().nIn(fCollector.getFeatures(true, game.getGameContext(), player).length).nOut(80)
                        .activation("leakyrelu").momentum(0.9)
                        .weightInit(WeightInit.XAVIER)
                        .updater(Updater.NESTEROVS)
                        .build())
                .layer(1, new DenseLayer.Builder().nIn(80).nOut(80)
                        .activation("leakyrelu").dropOut(0.5).momentum(0.9)
                        .weightInit(WeightInit.XAVIER)
                        .updater(Updater.NESTEROVS)
                        .build())
                .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .weightInit(WeightInit.XAVIER).updater(Updater.SGD).momentum(0.9)
                        .updater(Updater.NESTEROVS)
                        .activation("tanh").weightInit(WeightInit.XAVIER)
                        .nIn(80).nOut(1).build())//.backprop(true)
                .build();

        return networkConfig;
    }

    private static SimulationContext createContext(Deck deck1, Deck deck2, IBehaviour behavior1, IBehaviour behavior2)
    {
        DeckProxy dp = new DeckProxy();
        try {
            dp.loadDecks();
        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading decks");
        }

        new DeckFormatProxy();

        PlayerConfig p1Config = new PlayerConfig(deck1, behavior1);
        PlayerConfig p2Config = new PlayerConfig(deck2, behavior2);

        p1Config.build();
        p2Config.build();

        p1Config.setHeroCard(MetaHero.getHeroCard(deck1.getHeroClass()));
        p2Config.setHeroCard(MetaHero.getHeroCard(deck2.getHeroClass()));

        Player p1 = new Player(p1Config);
        Player p2 = new Player(p2Config);

        DeckFormat allCards = new DeckFormat();
        for(CardSet set : CardSet.values()) {
            allCards.addSet(set);
        }

        return new SimulationContext(p1, p2, new GameLogic(), allCards);
    }

    private static Deck loadDeck(String name)
    {
        String url = null;
        switch (name.toLowerCase()) {
            case "nobattlecryhunter":
                url = "http://www.hearthpwn.com/decks/577429-midrange-hunter-no-targeted-battlecries";
                break;
            case "controlwarrior":
                url = "http://www.hearthpwn.com/decks/81605-breebotjr-control-warrior";
                break;
            case "dragon":
                DeckProxy p = new DeckProxy();
                try{p.loadDecks();}catch(Exception e){System.exit(123);}
                return  p.getDeckByName("Dragon Warrior");
            case "nobattlecryhunteroffline":
                p = new DeckProxy();
                try{p.loadDecks();}catch(Exception e){System.exit(123);}
                return  p.getDeckByName("MidRange Hunter: NO (targeted) BATTLECRIES");
            default:
                url = name;
        }

        Deck deck = new HearthPwnImporter().importFrom(url);
        if(deck == null) {
            throw new RuntimeException("Error: deck " + name + " doesn't exist or loaded unsuccessfully from hearthpwn.");
        }

        return deck;
    }
}
