package behaviors.util;

import behaviors.simulation.SimulationContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.behaviour.IBehaviour;
import net.demilich.metastone.game.cards.CardSet;
import net.demilich.metastone.game.decks.Deck;
import net.demilich.metastone.game.decks.DeckFormat;
import net.demilich.metastone.game.entities.heroes.MetaHero;
import net.demilich.metastone.game.gameconfig.PlayerConfig;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.gui.deckbuilder.DeckFormatProxy;
import net.demilich.metastone.gui.deckbuilder.DeckProxy;
import net.demilich.metastone.gui.deckbuilder.importer.HearthPwnImporter;

public class MetastoneUtils
{
    public static SimulationContext createContext(Deck deck1, Deck deck2, IBehaviour behavior1, IBehaviour behavior2)
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

    public static Deck loadDeck(String name)
    {
        switch (name.toLowerCase()) {
            case "nobattlecryhunter":
                name = "http://www.hearthpwn.com/decks/577429-midrange-hunter-no-targeted-battlecries";
                break;
            case "controlwarrior":
                name = "http://www.hearthpwn.com/decks/81605-breebotjr-control-warrior";
                break;
            case "dragon":
                name = "Dragon Warrior";
                break;
            case "nobattlecryhunteroffline":
                name = "MidRange Hunter: NO (targeted) BATTLECRIES";
                break;
        }

        Deck deck = null;
        if(name.startsWith("http://www.hearthpwn.com/decks")) {
            deck = new HearthPwnImporter().importFrom(name);
            if (deck == null) {
                throw new RuntimeException("Error: deck " + name + " doesn't exist or loaded unsuccessfully from hearthpwn.");
            }
        } else {
            DeckProxy p = new DeckProxy();
            try {
                p.loadDecks();
            } catch(Exception e) {
                throw new RuntimeException("Error loading decks");
            }
            deck = p.getDeckByName(name);
        }

        return deck;
    }
}
