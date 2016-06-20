package main;

import java.util.stream.IntStream;

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
import behaviors.standardMCTS.MCTSBehavior;

public class MetastoneTester
{
    private static double[] stats; //0: ties, 1: wins, 2: losses, 3: time elapsed, 4: avg time per game
    public static void main(String[] args) throws Exception
    {
        long beginTime = System.nanoTime();
        stats = new double[5];

        int simulations = 1;
        String deckName = "nobattlecry";
        int numTrees = 1;//20;
        int numIterations = 10000;
        double exploreFactor = 1.4;
        boolean parallel = true;
        IBehaviour behavior2 = new PlayRandomBehaviour();
        int numTrees2 = numTrees;
        int numIterations2 = numIterations;
        double exploreFactor2 = exploreFactor;

        if(keyExists("-simulations", args)) {
            simulations = Integer.parseInt(argumentForKey("-simulations", args));
            if(simulations < 1) {
                throw new RuntimeException("Error: there must be at least one simulation run.");
            }
        }
        if(keyExists("-deck", args)) {
            deckName = argumentForKey("-deck", args);
        }
        if(keyExists("-trees", args)) {
            numTrees = Integer.parseInt(argumentForKey("-trees", args));
            if(numTrees < 1) {
                throw new RuntimeException("Error: there must be at least one tree.");
            }
        }
        if(keyExists("-iterations", args)) {
            numIterations = Integer.parseInt(argumentForKey("-iterations", args));
            if(numIterations < 1) {
                throw new RuntimeException("Error: there must be at least one iteration.");
            }
        }
        if(keyExists("-explore", args)) {
            exploreFactor = Integer.parseInt(argumentForKey("-explore", args));
        }
        if(keyExists("-parallel", args)) {
            String parallelArg = argumentForKey("-parallel", args);
            if("false".startsWith(parallelArg.toLowerCase())) {
                parallel = false;
            } else if(!"true".startsWith(parallelArg.toLowerCase())) {
                throw new RuntimeException("Error: neither true nor false starts with " + parallelArg + ".");
            }
        }
        if(keyExists("-behavior2", args)) {
            String behavior2Arg = argumentForKey("-behavior2", args);
            switch(behavior2Arg.toLowerCase()) {
                case "random": break;
                case "gamestate": behavior2 = new GameStateValueBehaviour(); break;
                case "mcts": behavior2 = null; break;
                default: throw new RuntimeException("Error: " + behavior2Arg + " behavior does not exist.");
            }
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

        if(behavior2 == null) {
            behavior2 = new MCTSBehavior(exploreFactor2, numTrees2, numIterations2, new PlayRandomBehaviour());
        }

        new CardProxy();

        DeckProxy dp = new DeckProxy();
        dp.loadDecks();

        new DeckFormatProxy();

        Deck deck1 = loadDeck(deckName);
        IBehaviour behavior1 = new MCTSBehavior(exploreFactor, numTrees, numIterations, new PlayRandomBehaviour());
        PlayerConfig p1Config = new PlayerConfig(deck1, behavior1);

        Deck deck2 = loadDeck(deckName);
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


        SimulationContext game = new SimulationContext(p1, p2, new GameLogic(), allCards);

        if(parallel) {
            IntStream.range(0, simulations).parallel().forEach((int i) -> runSimulation(game.clone()));
        } else {
            IntStream.range(0, simulations).sequential().forEach((int i) -> runSimulation(game.clone()));
        }

        stats[3] = System.nanoTime() - beginTime;
        stats[4] = stats[3] / simulations;

        System.out.println("Wins: " + stats[1]);
        System.out.println("Losses: " + stats[2]);
        System.out.println("Ties: " + stats[0]);
        System.out.println("Time Elapsed: " + stats[3]);
        System.out.println("Average Time Per Game: " + stats[4]);
    }

    private static void runSimulation(SimulationContext game)
    {
        game.randomize(0);
        game.randomize(1);
        game.play();
        updateStats(game.getWinningPlayerId());
    }

    private static synchronized void updateStats(int result)
    {
        stats[result + 1]++;
    }

    private static Deck loadDeck(String name)
    {
        String url = null;
        switch (name.toLowerCase()) {
            case "nobattlecry":
                url = "http://www.hearthpwn.com/decks/574146-no-battlecry";
                break;
            default:
                url = name;
        }

        Deck deck = new HearthPwnImporter().importFrom(url);
        if(deck == null) {
            throw new RuntimeException("Error: deck " + name + " doesn't exist or loaded unsuccessfully from hearthpwn.");
        }

        return deck;
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
