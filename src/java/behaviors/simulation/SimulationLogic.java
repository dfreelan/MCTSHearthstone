package behaviors.simulation;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.Environment;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.events.BoardChangedEvent;
import net.demilich.metastone.game.events.GameEvent;
import net.demilich.metastone.game.events.SummonEvent;
import net.demilich.metastone.game.logic.GameLogic;

import java.util.ArrayList;

/**
 * Created by dfreelan on 6/16/16.
 */
public class SimulationLogic extends GameLogic
{
    int playerId;
    public Minion minion;
    public Card source;
    int index;
    public boolean resolveBattlecry;
    public boolean simulationActive = false;
    public ArrayList<GameAction> battlecries = null;
    public boolean battlecryRequest = false;
    public boolean rolloutActive;

    public SimulationLogic(GameLogic parent){
        super(parent.getIdFactory());
        this.setContext((parent.getGameContext()));
    }

    public void afterBattlecryLate() {
        handleEnrage(minion);
        this.getGameContext().getSummonReferenceStack().pop();
        this.getGameContext().fireGameEvent(new BoardChangedEvent(this.getGameContext()));
    }

    public void afterBattlecry() {
        Player player = this.getGameContext().getPlayer(playerId);
        if (this.getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE) != null) {
            minion = (Minion) this.getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE);
            minion.setBattlecry(null);
            this.getGameContext().getEnvironment().remove(Environment.TRANSFORM_REFERENCE);
        }

        if (index < 0 || index >= player.getMinions().size()) {
            player.getMinions().add(minion);
        } else {
            player.getMinions().add(index, minion);
        }

        SummonEvent summonEvent = new SummonEvent(this.getGameContext(), minion, source);
        this.getGameContext().fireGameEvent((GameEvent)summonEvent);

        applyAttribute(minion, Attribute.SUMMONING_SICKNESS);
        refreshAttacksPerRound(minion);
        if (player.getHero().hasAttribute(Attribute.CANNOT_REDUCE_HP_BELOW_1)) {
            minion.setAttribute(Attribute.CANNOT_REDUCE_HP_BELOW_1);
        }

        if (minion.hasSpellTrigger()) {
            addGameEventListener(player, minion.getSpellTrigger(), minion);
        }

        if (minion.getCardCostModifier() != null) {
            addManaModifier(player, minion.getCardCostModifier(), minion);
        }

        if (resolveBattlecry && minion.getBattlecry() != null && minion.getBattlecry().isResolvedLate()) {
            resolveBattlecry(player.getId(), minion);
        }

        handleEnrage(minion);

        this.getGameContext().getSummonReferenceStack().pop();
        this.getGameContext().fireGameEvent(new BoardChangedEvent(this.getGameContext()));
    }

}
