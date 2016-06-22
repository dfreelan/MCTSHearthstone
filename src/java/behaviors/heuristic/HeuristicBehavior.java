package behaviors.heuristic;

import behaviors.simulation.SimulationContext;
import behaviors.util.ActionComparator;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.actions.ActionType;
import net.demilich.metastone.game.actions.GameAction;
import net.demilich.metastone.game.behaviour.Behaviour;
import net.demilich.metastone.game.behaviour.threat.FeatureVector;
import net.demilich.metastone.game.behaviour.threat.ThreatBasedHeuristic;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.entities.Actor;
import net.demilich.metastone.game.targeting.EntityReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class HeuristicBehavior extends Behaviour
{
    private Random rand;
    ActionComparator actionComparator;
    ThreatBasedHeuristic heuristic;

    private String name = "Heuristic";

    private List<GameAction> minionBlacklist;
    private List<GameAction> spellBlacklist;
    private List<GameAction> summonBlacklist;

    public void initialize()
    {
        rand = new Random();
        actionComparator = new ActionComparator();
        heuristic = new ThreatBasedHeuristic(FeatureVector.getDefault());
    }

    public HeuristicBehavior()
    {
        super();
        initialize();
        resetBlacklists();
    }

    private HeuristicBehavior(List<GameAction> minionBlacklist, List<GameAction> spellBlacklist, List<GameAction> summonBlacklist)
    {
        super();
        initialize();
        this.minionBlacklist = new ArrayList<>(minionBlacklist);
        this.spellBlacklist = new ArrayList<>(spellBlacklist);
        this.summonBlacklist = new ArrayList<>(summonBlacklist);
    }

    @Override
    public HeuristicBehavior clone()
    {
        return new HeuristicBehavior(minionBlacklist, spellBlacklist, summonBlacklist);
    }

    @Override
    public GameAction requestAction(GameContext gameContext, Player player, List<GameAction> validActions)
    {
        if (validActions.size() == 1) {
            resetBlacklists();
            return validActions.get(0);
        }

        //TODO: differentiate between attacks where minions and heroes are attackers
        TreeMap<GameAction, List<GameAction>> minionAttacks = new TreeMap<>(actionComparator);
        TreeMap<GameAction, List<GameAction>> heroAttacks = new TreeMap<>(actionComparator);
        TreeMap<GameAction, List<GameAction>> spells = new TreeMap<>(actionComparator);
        TreeMap<GameAction, List<GameAction>> summons = new TreeMap<>(actionComparator);
        List<TreeMap<GameAction, List<GameAction>>> nonemptyCategories = new ArrayList<>();

        for (GameAction action : validActions) {
            if(action==null){
                System.err.println("I was supplied a null action! don't do that!");
                throw new RuntimeException("was supplied a null action in heuristic behavior");
            }
            //check source to check for battlecry
            Actor source = (Actor) gameContext.resolveSingleTarget(action.getSource());
            TreeMap<GameAction, List<GameAction>> category = null;
            if (action.getActionType().equals(ActionType.PHYSICAL_ATTACK)) {
                if (action.getTargetKey().equals(EntityReference.ENEMY_HERO)) {
                    category = heroAttacks;
                } else if (!onBlacklist(action, minionBlacklist)) {
                    category = minionAttacks;
                }
                //vv  source being null should indicate that the action is not a battlecry vv
            } else if (action.getActionType().equals(ActionType.SUMMON) && source == null) {
                if (!onBlacklist(action, summonBlacklist)) {
                    category = summons;
                }
                //vv  everything else, battlecry, spells, hero power,discover,equip weapon,  etc but not system and end_turn, each  vv
            } else if (!action.getActionType().equals(ActionType.SYSTEM) && !action.getActionType().equals(ActionType.END_TURN)) {
                if (!onBlacklist(action, spellBlacklist)) {
                    category = spells;
                }
            }

            if (category != null) {
                if (category.isEmpty() && category != heroAttacks) {
                    nonemptyCategories.add(category);
                }

                addAction(action, category);
            }
        }
        //select an action. Will check/add things to the blacklist
        while (!nonemptyCategories.isEmpty()) {
            TreeMap<GameAction, List<GameAction>> category = nonemptyCategories.get(rand.nextInt(nonemptyCategories.size()));
            int actionGroupIndex = rand.nextInt(category.size());
            int currIndex = 0;
            GameAction actionGroup = null;
            for (GameAction key : category.keySet()) {
                if (currIndex == actionGroupIndex) {
                    actionGroup = key;
                    break;
                }
                currIndex++;
            }

            if (rand.nextBoolean()) {
                List<GameAction> actions = category.get(actionGroup);
                GameAction action = actions.get(rand.nextInt(actions.size()));

                boolean chooseAction = true;
                if(category == spells) {
                    SimulationContext simulation = new SimulationContext(gameContext);
                    if(action == null) {
                        throw new RuntimeException("action null before applyaction");
                    }
                    simulation.applyAction(player.getId(), action);
                    double currentScore = heuristic.getScore(gameContext, player.getId());
                    double afterScore = heuristic.getScore(simulation.getGameContext(), player.getId());
                    chooseAction = afterScore > currentScore;
                }
                if(chooseAction) {
                    return action;
                }
            }

            if (category == minionAttacks) {
                minionBlacklist.add(actionGroup);
                minionAttacks.remove(actionGroup);

                GameAction action = null;
                for (GameAction heroAttack : heroAttacks.keySet()) {
                    if (heroAttack.isSameActionGroup(actionGroup)) {
                        action = heroAttack;
                        break;
                    }
                }

                if (action != null) {
                    return action;
                }
            } else if (category == spells) {
                spellBlacklist.add(actionGroup);
                spells.remove(actionGroup);
            } else if (category == summons) {
                summonBlacklist.add(actionGroup);
                summons.remove(actionGroup);
            }

            if (category.isEmpty()) {
                nonemptyCategories.remove(category);
            }
        }

        resetBlacklists();
        return findEndTurn(validActions);
    }

    private void resetBlacklists()
    {
        minionBlacklist = new ArrayList<>();
        spellBlacklist = new ArrayList<>();
        summonBlacklist = new ArrayList<>();
    }

    private GameAction findEndTurn(List<GameAction> actions)
    {
        for(GameAction action : actions) {
            if(action.getActionType().equals(ActionType.END_TURN)) {
                return action;
            }
        }
        throw new RuntimeException("Error: END_TURN action not found");
    }

    private boolean onBlacklist(GameAction action, List<GameAction> blacklist)
    {
        for(GameAction discarded : blacklist) {
            if(action.isSameActionGroup(discarded)) {
                return true;
            }
        }
        return false;
    }

    private void addAction(GameAction action, TreeMap<GameAction, List<GameAction>> category)
    {
        for(GameAction key : category.keySet()) {
            if(action.isSameActionGroup(key)) {
                category.get(key).add(action);
                return;
            }
        }

        category.put(action, new ArrayList<>());
        category.get(action).add(action);
    }

    @Override
    public String getName() { return name; }

    public void setName(String newName) { name = newName; }

    @Override
    public List<Card> mulligan(GameContext gameContext, Player player, List<Card> list)
    {
        return new ArrayList<Card>();
    }
}
