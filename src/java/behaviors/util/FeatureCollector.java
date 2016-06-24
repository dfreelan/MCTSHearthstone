package behaviors.util;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import gnu.trove.map.hash.TIntIntHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import net.demilich.metastone.game.cards.Card;
import net.demilich.metastone.game.cards.CardType;
import net.demilich.metastone.game.entities.minions.Minion;

/**
 *
 * @author dfreelan
 */
public final class FeatureCollector implements Cloneable {

    Random random = new Random();
    //contains an array that holds the features
    //can query furfillOrder to populate the data
    //need to be initialized with # minions and spells in each deck
    TIntIntHashMap enemyCardMap = new TIntIntHashMap(60,1.01f,-1,-1);
    TIntIntHashMap myCardMap = new TIntIntHashMap(60,1.01f,-1,-1);

    double[] featureData; //feature array, will be resued;
    ArrayList<Integer> cardCount = new ArrayList<Integer>();
    ArrayList<String> cardNames = new ArrayList<String>();

    ArrayList<Integer> enemyCardCount = new ArrayList<Integer>();
    ArrayList<String> enemyCardNames = new ArrayList<String>();

    int lastSelfCard ;
    int lastEnemyCard ;
    int featureCount = 0;
    int myBoardStart;
    int enemyBoardStart;
    public synchronized FeatureCollector clone() {
        FeatureCollector clone = new FeatureCollector();
        clone.enemyCardMap = this.enemyCardMap;
        clone.myCardMap = this.myCardMap;
        clone.cardCount = this.cardCount;
        clone.cardNames = this.cardNames;
        clone.enemyCardCount = this.enemyCardCount;
        clone.enemyCardNames = this.enemyCardNames;

        clone.lastEnemyCard = this.lastEnemyCard;
        clone.lastSelfCard = this.lastSelfCard;
        clone.featureCount = this.featureCount;
        clone.myBoardStart = this.myBoardStart;
        clone.enemyBoardStart = this.enemyBoardStart;



        clone.featureData = this.featureData.clone();
        return clone;
    }
    public FeatureCollector(GameContext context, Player player) {
        context = context.clone();
        //System.err.println(" no entry means : "  + myCardMap.getNoEntryValue());
        player = context.getPlayer(player.getId());
        Player opponent = context.getOpponent(player);


        opponent.getDeck().addAll(opponent.getHand());
        player.getDeck().addAll(player.getHand());
        //opponent.getDeck().
        opponent.getDeck().sortByName();
        player.getDeck().sortByName();
        //self is hand,deck,played
        for (Card card : player.getHand()) {
            int hash = card.getName().hashCode();
            if(!card.getName().contains("The Coin"))
            if(!myCardMap.containsKey(hash)){
                myCardMap.put(hash, featureCount);

                this.cardNames.add(card.getName());
                cardCount.add(1);
                featureCount+=3;
            }else{
                cardCount.set(myCardMap.get(hash)/3,2);
            }
        }
        for (Card card : player.getDeck()){
            int hash = card.getName().hashCode();
            if(!card.getName().contains("The Coin"))
            if(!myCardMap.containsKey(hash)){
                myCardMap.put(hash, featureCount);

                this.cardNames.add(card.getName());
                cardCount.add(1);
                featureCount+=3;
            }else{
                cardCount.set(myCardMap.get(hash)/3,2);
            }
        }

        lastSelfCard = featureCount-3;

        opponent = context.getOpponent(player);
        //data is deck,played
        for (Card card : opponent.getHand()) {
            int hash = card.getName().hashCode();
            if(!card.getName().contains("The Coin"))
            if(!enemyCardMap.containsKey(hash)){
                enemyCardMap.put(hash, featureCount);


                this.enemyCardNames.add(card.getName());
                enemyCardCount.add(1);
                featureCount+=2;
            }else{
                enemyCardCount.set((enemyCardMap.get(hash)-lastSelfCard-3)/2,2);
            }
        }

        for (Card card : opponent.getDeck()){
            int hash = card.getName().hashCode();
            if(!card.getName().contains("The Coin"))
            if(!enemyCardMap.containsKey(hash)){
                enemyCardMap.put(hash, featureCount);

                this.enemyCardNames.add(card.getName());
                enemyCardCount.add(1);
                featureCount+=2;
            }else{
                enemyCardCount.set((enemyCardMap.get(hash)-lastSelfCard-3)/2,2);
            }
        }
        lastEnemyCard = featureCount-2;
        //feature data will be features count, my health/armor, opponent health/armor, myMana/maxMana, enemyMana/maxMana, my board(7*3), opponent board (7*3)
        //also weapon attack and health and turn
        featureData = new double[featureCount + 1 + 1 +1 + 1 + 1 +2 +2 +2 + 2 +1 +1 + 1 + 2+7*3 + 7*3];
        this.myBoardStart = featureCount+18;
        this.enemyBoardStart = myBoardStart + 7*3;

    }

    public double[] getFeatures(boolean includeAction, GameContext context,Player player){
        //hand,deck,played
        context = context.clone();
        player = context.getPlayer(player.getId());
        featureData = new double[featureData.length];
        //System.err.println("feature count + " + featureCount + " featuredata size" + featureData.length );
        populateFeatureData(this.myCardMap,0,player.getHand().toList());
        populateFeatureData(this.myCardMap,1,player.getDeck().toList());


        populateFeatureData(this.enemyCardMap,0,context.getOpponent(player).getDeck().toList());
        populateFeatureData(this.enemyCardMap,0,context.getOpponent(player).getHand().toList());

        calculatePlayedCards();
        featureData[featureCount+0] = context.getActivePlayerId()== player.getId() ? 0.0 : 1.0;

        featureData[featureCount+1] = player.getHero().getArmor()/10.0;
        featureData[featureCount+2] = player.getHero().getHp()/33.0;
        featureData[featureCount+3] = player.getMana()/10.0;
        featureData[featureCount+4] = player.getMaxMana()/10.0;
        featureData[featureCount+5] = player.getHero().getAttack();
        featureData[featureCount+6] = player.getHero().getWeapon()==null ? 0.0 : player.getHero().getWeapon().getDurability()/4.0;
        featureData[featureCount+7] = player.getHero().canAttackThisTurn() ? 1.0 : 0.0;
        featureData[featureCount+8] = player.getHand().getCount();

        Player opponent = context.getOpponent(player);

        featureData[featureCount+9] = opponent.getHero().getArmor()/10.0;
        featureData[featureCount+10] = opponent.getHero().getHp()/33.0;
        featureData[featureCount+11] = opponent.getMana()/10.0;
        featureData[featureCount+12] = opponent.getMaxMana()/10.0;
        featureData[featureCount+13] = opponent.getHero().getAttack();
        featureData[featureCount+14] = opponent.getHero().getWeapon()==null ? 0.0 : opponent.getHero().getWeapon().getDurability()/4.0;
        featureData[featureCount+15] = opponent.getHero().canAttackThisTurn() ? 1.0 : 0.0;
        featureData[featureCount+16] = opponent.getHand().getCount();

        featureData[featureCount+17] = context.getTurn()/100.0;

        addMinions(player.getMinions(), this.myBoardStart);
        addMinions(opponent.getMinions(), this.enemyBoardStart);

        return featureData;
    }
    public void addMinions(List<Minion> minions, int indexStart){
        int currentIndex = indexStart;
        for(Minion minion : minions){
            featureData[currentIndex] = minion.getAttack()/12.0;
            featureData[currentIndex+1] = minion.getHp()/12.0;
            double canAttack = .1;

            if(minion.canAttackThisTurn())
                canAttack = .9;
            featureData[currentIndex+2] = canAttack;

            currentIndex+=3;
        }

    }
    public void calculatePlayedCards(){
        for(int i = 0; i<=lastSelfCard; i+=3){
            int cardNum = i/3;
            double total = this.cardCount.get(cardNum);
            double cardsPlayed = (double) (total - (featureData[i]*2 + featureData[i+1]*2));
            featureData[i+2] = cardsPlayed/2.0;
            if(cardsPlayed<0){
                System.err.println("bad thing " + this.cardNames.get(cardNum));

                System.err.println("cards played: " + cardsPlayed);
                System.err.println("feature i, i+1" + featureData[i] + " " + featureData[i+1]);
                System.err.println("total in my deck is " + total);
                System.exit(0);
            }
        }
        for(int i = lastSelfCard+3; i<=lastEnemyCard; i+=2){
            int cardNum = (i-(lastSelfCard+3))/2;
            double total = this.enemyCardCount.get(cardNum);
            double cardsPlayed = (double)(total - (featureData[i]*2));
            featureData[i+1] = cardsPlayed/2.0;
            if(cardsPlayed<0){
                System.err.println("bad thing " + this.cardNames.get(cardNum));

                System.err.println("cards played: " + cardsPlayed);
                System.err.println("feature i, i+1" + featureData[i] + " " + featureData[i+1]);
                System.err.println("total in my deck is " + total);
                System.exit(0);
            }
        }
    }
    public void populateFeatureData(TIntIntHashMap myMap, int offset, List<Card> cards){

        for(Card card : cards){
            int index = myMap.get(card.getName().hashCode());
            if(index != -1){
                if(card.getName().contains("Aco") && myMap == this.myCardMap){
                    // System.err.println("found another  " + card.getName() + " in my offset " + offset);

                }
                featureData[index+offset] += (1.0/2.0);

            }
        }

    }
    private FeatureCollector() {}

    public void printFeatures(GameContext context,Player player){
        double[] features = this.getFeatures(false, context, player);
        System.err.println("my card data:");
        for(int i = 0; i<this.cardCount.size(); i++){
            String numbers = this.featureData[i*3] + " " + this.featureData[i*3+1] + " " + this.featureData[i*3+2];
            System.err.println(this.cardNames.get(i) + ": " + numbers);
        }

        System.err.println("enemy card data:");
        for(int i = 0; i<this.enemyCardCount.size(); i++){
            String numbers = this.featureData[this.lastSelfCard+3 + i*2] + " " + this.featureData[this.lastSelfCard+3 + i*2+1];
            System.err.println(this.enemyCardNames.get(i) + ": " + numbers);
        }
       /*        featureData[featureCount+0] = player.getHero().getArmor()/10.0;
        featureData[featureCount+1] = player.getHero().getHp()/33.0;
         featureData[featureCount+2] = player.getMana();
        featureData[featureCount+3] = player.getMaxMana();

        Player opponent = context.getOpponent(player);

        featureData[featureCount+4] = opponent.getHero().getArmor()/10.0;
        featureData[featureCount+5] = opponent.getHero().getHp()/33.0;
        featureData[featureCount+6] = opponent.getMana();
        featureData[featureCount+7] = opponent.getMaxMana();*/
        System.err.println("my armor:" + featureData[featureCount]);
        System.err.println("my hp : " + featureData[featureCount+1]);

        System.err.println("enemy armor:" + featureData[featureCount+4]);
        System.err.println("enemy hp : " + featureData[featureCount+5]);


        System.err.println("my board minions:");
        for(int i = this.myBoardStart; i<this.enemyBoardStart; i+=3){
            System.err.println("Minion: " + featureData[i]  + " " + featureData[i+1] + " " + featureData[i+2]);
        }


        System.err.println("enemy board minions:");
        for(int i = this.enemyBoardStart; i<featureData.length; i+=3){
            System.err.println("eneMinion: " + featureData[i]  + " " + featureData[i+1] + " " + featureData[i+2]);
        }


    }
}



