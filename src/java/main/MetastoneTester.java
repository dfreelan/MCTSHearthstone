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
import behaviors.util.FeatureCollector;
import behaviors.util.Logger;
import behaviors.util.RandomStateCollector;
import net.demilich.metastone.game.Player;
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
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.LearningRatePolicy;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MetastoneTester
{
    private static double[] stats; //0: ties, 1: wins, 2: losses, 3: time elapsed, 4: avg time per game
    private static boolean consoleOutput = true;
    private static Path logFile = null;

    public static void main(String[] args)
    {
        long beginTime = System.nanoTime();
        stats = new double[5];

        boolean parallel = true;
        int simulations = 1;

        String deckName = "";
        int numTrees = 20;
        int numIterations = 10000;
        double exploreFactor = 1.4;
        IBehaviour behavior = new MCTSBehavior(exploreFactor, numTrees, numIterations, new MCTSStandardNode(new PlayRandomBehaviour()));

        String deckName2 = deckName;
        int numTrees2 = numTrees;
        int numIterations2 = numIterations;
        double exploreFactor2 = exploreFactor;
        IBehaviour behavior2 = new PlayRandomBehaviour();

        if(keyExists("-console", args)) {
            consoleOutput = parseBoolean(argumentForKey("-console", args));
        }
        if(keyExists("-log", args)) {
            logFile = Paths.get(argumentForKey("-log", args));
        }
        if(keyExists("-parallel", args)) {
            parallel = parseBoolean(argumentForKey("-parallel", args));
        }
        if(keyExists("-parallel", args)) {
            parallel = parseBoolean(argumentForKey("-parallel", args));
        }
        if(keyExists("-simulations", args)) {
            simulations = Integer.parseInt(argumentForKey("-simulations", args));
            if(simulations < 1) {
                throw new RuntimeException("Error: there must be at least one simulation run.");
            }
        }
        if(keyExists("-deck", args)) {
            deckName = argumentForKey("-deck", args);
            deckName2 = deckName;
        }
        if(keyExists("-trees", args)) {
            numTrees = Integer.parseInt(argumentForKey("-trees", args));
            numTrees2 = numTrees;
            if(numTrees < 1) {
                throw new RuntimeException("Error: there must be at least one tree.");
            }
        }
        if(keyExists("-iterations", args)) {
            numIterations = Integer.parseInt(argumentForKey("-iterations", args));
            numIterations2 = numIterations;
            if(numIterations < 1) {
                throw new RuntimeException("Error: there must be at least one iteration.");
            }
        }
        if(keyExists("-explore", args)) {
            exploreFactor = Integer.parseInt(argumentForKey("-explore", args));
            exploreFactor2 = exploreFactor;
        }
        if(keyExists("-deck2", args)) {
            deckName2 = argumentForKey("-deck2", args);
        }
        if(keyExists("-trees2", args)) {
            numTrees2 = Integer.parseInt(argumentForKey("-trees2", args));
            if(numTrees2 < 1) {
                throw new RuntimeException("Error: there must be at least one tree.");
            }
        }
        if(keyExists("-iterations2", args)) {
            numIterations2 = Integer.parseInt(argumentForKey("-iterations2", args));
            if(numIterations < 1) {
                throw new RuntimeException("Error: there must be at least one iteration.");
            }
        }
        if(keyExists("-explore2", args)) {
            exploreFactor2 = Integer.parseInt(argumentForKey("-explore2", args));
        }


        if(logFile != null && Files.exists(logFile)) {
            try {
                Files.delete(logFile);
            } catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error deleting old log file " + logFile.toString());
            }
        }

        new CardProxy();
        Deck deck1 = loadDeck(deckName);
        Deck deck2 = loadDeck(deckName2);

        SimulationContext game = createContext(deck1, deck2, behavior, behavior2);

        if(keyExists("-behavior", args)) {
            String behaviorArg = argumentForKey("-behavior", args);
            game.getGameContext().getPlayer1().setBehaviour(getBehavior(behaviorArg, exploreFactor, numTrees, numIterations, game, game.getGameContext().getPlayer1()));
        }
        if(keyExists("-behavior2", args)) {
            String behavior2Arg = argumentForKey("-behavior2", args);
            game.getGameContext().getPlayer2().setBehaviour(getBehavior(behavior2Arg, exploreFactor2, numTrees2, numIterations2, game, game.getGameContext().getPlayer2()));
        }

        if(parallel) {
            IntStream.range(0, simulations).parallel().forEach((int i) -> runSimulation(game.clone(), i));
        } else {
            IntStream.range(0, simulations).sequential().forEach((int i) -> runSimulation(game.clone(), i));
        }

        stats[3] = (System.nanoTime() - beginTime) / 1e9;
        stats[4] = stats[3] / simulations;

        printStats(stats, true);
    }

    private static void runSimulation(SimulationContext game, int gameNum)
    {
        game.randomize(0);
        game.randomize(1);
        game.play();
        updateStats(game.getWinningPlayerId());
        Logger.log("Finished Simulation[" + gameNum + "], Result = " + resultString(game.getWinningPlayerId()), consoleOutput, logFile);
        printStats(stats, false);
    }

    private static void printStats(double[] stats, boolean includeTime)
    {
        String status = "";
        status += "Wins: " + stats[1] + "\n";
        status += "Losses: " + stats[2] + "\n";
        status += "Ties: " + stats[0] + "\n";
        if(includeTime) {
            status += "Time Elapsed: " + stats[3] + "s\n";
            status += "Average Time Per Game: " + stats[4] + "s\n";
        }
        Logger.log(status, consoleOutput, logFile);
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

    private static IBehaviour getBehavior(String name, double exploreFactor, int numTrees, int numIterations, SimulationContext game, Player player)
    {
        switch(name.toLowerCase()) {
            case "random": return new PlayRandomBehaviour();
            case "heuristic": return new HeuristicBehavior();
            case "gamestate": return new GameStateValueBehaviour();
            case "mcts": return new MCTSBehavior(exploreFactor, numTrees, numIterations, new MCTSStandardNode(new PlayRandomBehaviour()));
            case "mctsheuristic":
                MCTSBehavior behavior = new MCTSBehavior(exploreFactor, numTrees, numIterations, new MCTSStandardNode(new HeuristicBehavior()));
                behavior.setName("MCTSHeuristicBehavior");
                return behavior;
            case "mctsneural":
                FeatureCollector fCollector = new FeatureCollector(game.getGameContext(), player);
                System.err.println("THE gamecontext in the NN is" +game.toString());
                System.err.println("numfeatures: " + fCollector.getFeatures(true, game.getGameContext(), player).length);

                MultiLayerConfiguration networkConfig  = new NeuralNetConfiguration.Builder()
                        .learningRate(1e-2).learningRateDecayPolicy(LearningRatePolicy.Inverse).lrPolicyDecayRate(.0001).lrPolicyPower(.75)
                        .iterations(100)

                        .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                        .list(2)
                        .layer(0, new DenseLayer.Builder().nIn(fCollector.getFeatures(true, game.getGameContext(), player).length).nOut(80)
                                .activation("leakyrelu").dropOut(0.5)
                                .weightInit(WeightInit.XAVIER)
                                .build())
                        .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.SQUARED_LOSS)
                                .weightInit(WeightInit.XAVIER).updater(Updater.SGD)
                                .activation("tanh").weightInit(WeightInit.XAVIER)
                                .nIn(80).nOut(1).build()).backprop(true)
                        .build();

                TrainConfig trainConfig = new TrainConfig(1000, game, new RandomStateCollector(new PlayRandomBehaviour()),
                        new MCTSBehavior(exploreFactor, numTrees, numIterations, new MCTSStandardNode(new PlayRandomBehaviour())), true);

                MCTSBehavior neural = new MCTSBehavior(exploreFactor, numTrees, numIterations, new MCTSNeuralNode(new NeuralNetworkCritic(networkConfig, trainConfig, Paths.get("neural_network.dat"))));
                neural.setName("MCTSNeuralBehavior");
                return neural;
            default: throw new RuntimeException("Error: " + name + " behavior does not exist.");
        }
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
        p1Config.setName(behavior1.getName());
        p2Config.setHeroCard(MetaHero.getHeroCard(deck2.getHeroClass()));
        p2Config.setName(behavior2.getName());

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
            default:
                url = name;
        }

        Deck deck = new HearthPwnImporter().importFrom(url);
        if(deck == null) {
            throw new RuntimeException("Error: deck " + name + " doesn't exist or loaded unsuccessfully from hearthpwn.");
        }

        return deck;
    }

    private static boolean parseBoolean(String str)
    {
        str = str.toLowerCase();
        if("true".startsWith(str)) {
            return true;
        } else if("false".startsWith(str)) {
            return false;
        } else {
            throw new RuntimeException("Error: neither true nor false start with " + str + ".");
        }
    }

    private static boolean keyExists(String key, String[] args)
    {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    private static String argumentForKey(String key, String[] args)
    {
        for (int x = 0; x < args.length - 1; x++) // if a key has an argument, it can't be the last string
        {
            if (args[x].equalsIgnoreCase(key)) {
                return args[x + 1];
            }
        }
        return null;
    }
}
