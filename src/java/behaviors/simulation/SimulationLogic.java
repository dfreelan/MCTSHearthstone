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
import net.demilich.metastone.game.events.SummonEvent;
import net.demilich.metastone.game.logic.GameLogic;
import net.demilich.metastone.game.targeting.EntityReference;
import net.demilich.metastone.game.targeting.TargetSelection;

import java.util.ArrayList;
import java.util.List;

public class SimulationLogic extends GameLogic
{
    private int playerId;
    public Minion minion;
    public Card source;
    private int index;
    public boolean resolveBattlecry;
    public boolean simulationActive = false;
    public ArrayList<GameAction> battlecries = null;
    public boolean battlecryRequest = false;
    public boolean rolloutActive;

    public SimulationLogic(GameLogic parent)
    {
        super(parent.getIdFactory().clone());
        super.setLoggingEnabled(false);

        this.setContext((parent.getGameContext()));
    }

    public void afterBattlecryLate()
    {
        handleEnrage(minion);
        this.getGameContext().getSummonReferenceStack().pop();
        this.getGameContext().fireGameEvent(new BoardChangedEvent(this.getGameContext()));
    }

    public void afterBattlecry()
    {
        Player player = this.getGameContext().getPlayer(playerId);

        if (getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE) != null) {
            minion = (Minion) getGameContext().resolveSingleTarget((EntityReference) getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE));
            minion.setBattlecry(null);
            getGameContext().getEnvironment().remove(Environment.TRANSFORM_REFERENCE);
        }

        getGameContext().fireGameEvent(new BoardChangedEvent(getGameContext()));

        player.getStatistics().minionSummoned(minion);
        SummonEvent summonEvent = new SummonEvent(getGameContext(), minion, source);
        getGameContext().fireGameEvent(summonEvent);

        applyAttribute(minion, Attribute.SUMMONING_SICKNESS);
        refreshAttacksPerRound(minion);

        if (minion.hasSpellTrigger()) {
            addGameEventListener(player, minion.getSpellTrigger(), minion);
        }

        if (minion.getCardCostModifier() != null) {
            addManaModifier(player, minion.getCardCostModifier(), minion);
        }

        handleEnrage(minion);

        this.getGameContext().getSummonReferenceStack().pop();
        this.getGameContext().fireGameEvent(new BoardChangedEvent(this.getGameContext()));
    }

    @Override
    public boolean summon(int playerId, Minion minion, Card source, int index, boolean resolveBattlecry)
    {
		Player player = getGameContext().getPlayer(playerId);
		if (!canSummonMoreMinions(player)) {
			//log("{} cannot summon any more minions, {} is destroyed", player.getName(), minion);
			return false;
		}
		minion.setId(getIdFactory().generateId());
		minion.setOwner(player.getId());

		getGameContext().getSummonReferenceStack().push(minion.getReference());

		//log("{} summons {}", player.getName(), minion);

		if (index < 0 || index >= player.getMinions().size()) {
			player.getMinions().add(minion);
		} else {
			player.getMinions().add(index, minion);
		}

		if (resolveBattlecry && minion.getBattlecry() != null && !minion.getBattlecry().isResolvedLate()) {
            this.resolveBattlecry = resolveBattlecry;
            this.playerId = playerId;
            this.minion = minion.clone();

            this.source = source.clone();
            this.index = index;
            resolveBattlecry(player.getId(), minion);

            if (this.battlecryRequest) {
                getGameContext().setIsInBattleCry(true);
                return true;
            }
        }

		if (getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE) != null) {
			minion = (Minion) getGameContext().resolveSingleTarget((EntityReference) getGameContext().getEnvironment().get(Environment.TRANSFORM_REFERENCE));
			minion.setBattlecry(null);
            getGameContext().getEnvironment().remove(Environment.TRANSFORM_REFERENCE);
		}

        getGameContext().fireGameEvent(new BoardChangedEvent(getGameContext()));

		player.getStatistics().minionSummoned(minion);
		SummonEvent summonEvent = new SummonEvent(getGameContext(), minion, source);
        getGameContext().fireGameEvent(summonEvent);

		applyAttribute(minion, Attribute.SUMMONING_SICKNESS);
		refreshAttacksPerRound(minion);

		if (minion.hasSpellTrigger()) {
			addGameEventListener(player, minion.getSpellTrigger(), minion);
		}

		if (minion.getCardCostModifier() != null) {
			addManaModifier(player, minion.getCardCostModifier(), minion);
		}

        if (resolveBattlecry && minion.getBattlecry() != null && minion.getBattlecry().isResolvedLate()) {
            this.resolveBattlecry = resolveBattlecry;
            this.playerId = playerId;

            this.minion = minion.clone();
            this.source = source.clone();
            this.index = index;

            resolveBattlecry(player.getId(), minion);
            if (this.battlecryRequest) {
                getGameContext().setIsInBattleCry(true);
                return true;
            }
        }

		handleEnrage(minion);

        getGameContext().getSummonReferenceStack().pop();
        getGameContext().fireGameEvent(new BoardChangedEvent(getGameContext()));
		return true;
	}

    @Override
    public void performGameAction(int playerId, GameAction action)
    {
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
    protected void resolveBattlecry(int playerId, Actor actor)
    {
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
            }else{
               // System.err.println("it is false sometimes");
            }
            battlecryAction = player.getBehaviour().requestAction(getGameContext(), player, battlecryActions);
        } else {
            battlecryAction = battlecry;
        }
        if (hasAttribute(player, Attribute.DOUBLE_BATTLECRIES) && actor.getSourceCard().hasAttribute(Attribute.BATTLECRY)) {
            // You need DOUBLE_BATTLECRIES before your battlecry action, not after.
            performGameAction(playerId, battlecryAction);
            performGameAction(playerId, battlecryAction);
        } else {
            performGameAction(playerId, battlecryAction);
        }
        checkForDeadEntities();
    }

    @Override
    public SimulationLogic clone()
    {
        //System.err.println("we are calling the correct clone");
        SimulationLogic clone = new SimulationLogic(this);
        clone.playerId = this.playerId;
        if (this.minion != null) {
            clone.minion = this.minion.clone();

        }
        if (this.source != null) {
            clone.source = this.source.clone();
        }
        clone.index = this.index;
        clone.resolveBattlecry = this.resolveBattlecry;
        //clone.debugHistory = new LinkedList<>(debugHistory);
        return clone;
    }
}

/*if (simulationActive) {

                this.battlecries = (ArrayList<GameAction>) battlecryActions;
                this.battlecryRequest = true;
                return;
            }*/