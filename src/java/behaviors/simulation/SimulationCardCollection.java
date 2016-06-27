package behaviors.simulation;

import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardCollection;

public class SimulationCardCollection extends CardCollection
{
    private boolean shuffled = true;

    public SimulationCardCollection(CardCollection cards)
    {
        this.addAll(cards.clone());
        this.shuffle();
    }

    //the list of shuffled now, so getting the 0th element is "random"
    //this is so that when i clone it, it stays shuffled in the same order
    //again, necesary for determinization in MCTS
    @Override
    public Card getRandom() {
        return this.get(0);
    }

    @Override
    public void add(Card card){
        this.addRandomly(card);
    }
}
