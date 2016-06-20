package behaviors.simulation;

import net.demilich.metastone.game.Attribute;
import net.demilich.metastone.game.Environment;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.BattlecryAction;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.entities.Entity;
import net.demilich.metastone.game.entities.minions.Minion;
import net.demilich.metastone.game.events.BoardChangedEvent;
import net.demilich.metastone.game.events.GameEvent;
import net.demilich.metastone.game.events.SummonEvent;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.game.targeting.TargetSelection;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dfreelan on 6/16/16.
 */
public class SimulationLogic extends GameLogic {
    int playerId;
    public Minion minion;
    public Card source;
    int index;
    public boolean resolveBattlecry;
    public boolean simulationActive = false;
    public ArrayList<GameAction> battlecries = null;
    public boolean battlecryRequest = false;
    public boolean rolloutActive;

    public SimulationLogic(GameLogic parent) {
        super(parent.getIdFactory());
        super.setLoggingEnabled(false);
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
        this.getGameContext().fireGameEvent((GameEvent) summonEvent);

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

    @Override
    public boolean summon(int playerId, Minion minion, Card source, int index, boolean resolveBattlecry) {
        Player player = getGameContext().getPlayer(playerId);

        minion.setId(getIdFactory().generateId());
        minion.setOwner(player.getId());

        getGameContext().getSummonReferenceStack().push(minion.getReference());

        if (resolveBattlecry && minion.getBattlecry() != null && !minion.getBattlecry().isResolvedLate()) {
            this.resolveBattlecry = resolveBattlecry;
            this.playerId = playerId;
            this.minion = minion;
            this.source = source;
            this.index = index;
            resolveBattlecry(player.getId(), minion);
            if (this.battlecryRequest)
                return true;
        }

        if (getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE) != null) {
            minion = (Minion) getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE);
            minion.setBattlecry(null);
            getGameContext().getEnvironment().remove(Environment.TRANSFORM_REFERENCE);
        }

        if (index < 0 || index >= player.getMinions().size()) {
            player.getMinions().add(minion);
        } else {
            player.getMinions().add(index, minion);
        }

        SummonEvent summonEvent = new SummonEvent(getGameContext(), minion, source);
        getGameContext().fireGameEvent(summonEvent);

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
            this.resolveBattlecry = resolveBattlecry;
            this.playerId = playerId;
            this.minion = minion;
            this.source = source;
            this.index = index;
            resolveBattlecry(player.getId(), minion);
            if (this.battlecryRequest)
                return true;
        }

        handleEnrage(minion);

        getGameContext().getSummonReferenceStack().pop();
        getGameContext().fireGameEvent(new BoardChangedEvent(getGameContext()));
        return true;
    }

    @Override
    public void performGameAction(int playerId, GameAction action) {
        if (playerId != getGameContext().getActivePlayerId()) {
            logger.warn("Player {} tries to perform an action, but it is not his turn!", getGameContext().getPlayer(playerId).getName());
        }
        if (action.getTargetRequirement() != TargetSelection.NONE) {
            Entity target = getGameContext().resolveSingleTarget(action.getTargetKey());
            if (target != null) {
                getGameContext().getEnvironment().put(Environment.TARGET, target.getReference());
            } else {
                getGameContext().getEnvironment().put(Environment.TARGET, null);
            }
        }
        action.execute(getGameContext(), playerId);
        if (this.battlecryRequest)
            return;
        getGameContext().getEnvironment().remove(Environment.TARGET);

        if (!(action instanceof BattlecryAction)) {
            checkForDeadEntities();
        }
    }

    @Override
    protected void resolveBattlecry(int playerId, Actor actor) {
        BattlecryAction battlecry = actor.getBattlecry();
        Player player = getGameContext().getPlayer(playerId);
        if (!battlecry.canBeExecuted(getGameContext(), player)) {
            return;
        }

        GameAction battlecryAction = null;
        battlecry.setSource(actor.getReference());
        if (battlecry.getTargetRequirement() != TargetSelection.NONE) {
            List<Entity> validTargets = targetLogic.getValidTargets(getGameContext(), player, battlecry);
            if (validTargets.isEmpty()) {
                return;
            }

            List<GameAction> battlecryActions = new ArrayList<>();
            for (Entity validTarget : validTargets) {
                GameAction targetedBattlecry = battlecry.clone();
                targetedBattlecry.setTarget(validTarget);
                battlecryActions.add(targetedBattlecry);
            }

            if (simulationActive) {

                this.battlecries = (ArrayList<GameAction>) battlecryActions;
                this.battlecryRequest = true;
                return;
            }
            // System.err.println("requesting a response to a battlecry action");

            battlecryAction = player.getBehaviour().requestAction(getGameContext(), player, battlecryActions);
            //System.err.println("request received");
        } else {
            battlecryAction = battlecry;
        }
        performGameAction(playerId, battlecryAction);
        if (hasAttribute(player, Attribute.DOUBLE_BATTLECRIES) && actor.getSourceCard().hasAttribute(Attribute.BATTLECRY)) {
            performGameAction(playerId, battlecryAction);
        }
        checkForDeadEntities();
    }

}

