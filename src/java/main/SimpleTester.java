package main;

import behaviors.simulation.SimulationContext;
import behaviors.standardMCTS.MCTSBehavior;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.behaviour.PlayRandomBehaviour;
import net.demilich.metastone.game.behaviour.threat.GameStateValueBehaviour;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.heroes.MetaHero;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.gui.cards.CardProxy;
import net.demilich.metastone.gui.deckbuilder.DeckFormatProxy;
import net.demilich.metastone.gui.deckbuilder.DeckProxy;
import net.demilich.metastone.gui.deckbuilder.importer.HearthPwnImporter;

import java.util.stream.IntStream;

public class SimpleTester
{
    public static void main(String[] args) throws Exception
    {
        new CardProxy();

        DeckProxy dp = new DeckProxy();
        dp.loadDecks();

        DeckFormat allCards = new DeckFormat();
        for(CardSet set : CardSet.values()) {
            allCards.addSet(set);
        }

        new DeckFormatProxy();

        Deck deck =  new HearthPwnImporter().importFrom("http://www.hearthpwn.com/decks/574146-no-battlecry-rogue");
    }
}
