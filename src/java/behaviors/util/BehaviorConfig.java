package behaviors.util;

import behaviors.critic.POVMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BehaviorConfig
{
    private int playerId;
    
    public int numTrees;
    public int numIterations;
    public boolean logTrees = false;
    public double exploreFactor;
    
    public Path loadNetworkFile;
    public Path saveNetworkFile;
    public Path networkConfigFile;
    public POVMode povMode;
    
    public String deckName;
    
    public BehaviorConfig(int playerId)
    {
        if(playerId != 0 && playerId != 1) {
            throw new RuntimeException("Error: " + playerId + " is not a valid player ID");
        }

        this.playerId = playerId;
        
        numTrees = 20;
        numIterations = 10000;
        exploreFactor = 1.4;
        
        loadNetworkFile = null;
        saveNetworkFile = Paths.get("neural_network.dat");
        networkConfigFile = null;
        povMode = POVMode.SELF;
        
        deckName = "nobattlecryhunter";
    }
    
    public void applyArguments(String[] args)
    {
        String playerIndicator = "";
        if(playerId == 1) {
            playerIndicator = "2";
        }

        if(ArgumentUtils.keyExists("-trees" + playerIndicator, args)) {
            numTrees = Integer.parseInt(ArgumentUtils.argumentForKey("-trees" + playerIndicator, args));
            if(numTrees < 1) {
                throw new RuntimeException("Error: there must be at least one tree.");
            }
        }
        if(ArgumentUtils.keyExists("-iterations" + playerIndicator, args)) {
            numIterations = Integer.parseInt(ArgumentUtils.argumentForKey("-iterations" + playerIndicator, args));
            if(numIterations < 1) {
                throw new RuntimeException("Error: there must be at least one iteration.");
            }
        }
        if(ArgumentUtils.keyExists("-explore" + playerIndicator, args)) {
            exploreFactor = Integer.parseInt(ArgumentUtils.argumentForKey("-explore" + playerIndicator, args));
        }
        if(ArgumentUtils.keyExists("-loadnetwork" + playerIndicator, args)) {
            loadNetworkFile = Paths.get(ArgumentUtils.argumentForKey("-loadnetwork" + playerIndicator, args));
            if(!Files.exists(loadNetworkFile)) {
                throw new RuntimeException("Error: " + loadNetworkFile.toString() + " does not exist");
            }
        }
        if(ArgumentUtils.keyExists("-savenetwork" + playerIndicator, args)) {
            saveNetworkFile = Paths.get(ArgumentUtils.argumentForKey("-savenetwork" + playerIndicator, args));
        }
        if(ArgumentUtils.keyExists("-networkconfig" + playerIndicator, args)) {
            networkConfigFile = Paths.get(ArgumentUtils.argumentForKey("-networkconfig" + playerIndicator, args));
            if(!Files.exists(networkConfigFile)) {
                throw new RuntimeException("Error: " + networkConfigFile.toString() + " does not exist");
            } else if(!networkConfigFile.toString().endsWith(".json")) {
                throw new RuntimeException("Error: network config must be a .json file");
            }
        }
        if(ArgumentUtils.keyExists("-povmode" + playerIndicator, args)) {
            String povArg = ArgumentUtils.argumentForKey("-povmode" + playerIndicator, args);
            switch(povArg.toLowerCase()) {
                case "self":
                    povMode = POVMode.SELF;
                    break;
                case "opponent":
                    povMode = POVMode.OPPONENT;
                    break;
                case "root":
                    povMode = POVMode.ROOT;
                    break;
                case "rootopponent":
                    povMode = POVMode.ROOT_OPPONENT;
                    break;
                case "average":
                    povMode = POVMode.AVERAGE;
                    break;
                default:
                    throw new RuntimeException("Error: " + povArg + " isn't a valid POV Mode");
            }
        }
        if(ArgumentUtils.keyExists("-deck" + playerIndicator, args)) {
            deckName = ArgumentUtils.argumentForKey("-deck" + playerIndicator, args);
        }
    }
    
    public void copyTo(BehaviorConfig other)
    {
        other.numTrees = numTrees;
        other.numIterations = numIterations;
        other.logTrees = logTrees;
        other.exploreFactor = exploreFactor;
    
        other.loadNetworkFile = loadNetworkFile;
        other.saveNetworkFile = saveNetworkFile;
        other.networkConfigFile = networkConfigFile;
        other.povMode = povMode;
    
        other.deckName = deckName;
    }
}
