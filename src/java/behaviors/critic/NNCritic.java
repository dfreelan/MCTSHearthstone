package behaviors.critic;

import behaviors.simulation.SimulationContext;
import behaviors.util.FeatureCollector;
import behaviors.util.StateCollector;
import behaviors.util.StateJudge;
import net.demilich.metastone.game.Player;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.SpecifiedIndex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NNCritic implements Critic
{
    MultiLayerNetwork network;

    public NNCritic(Path loadLocation)
    {
        network = loadNetwork(loadLocation);
    }

    public NNCritic(MultiLayerConfiguration networkConfig, TrainConfig trainConfig, Path saveLocation)
    {
        network = new MultiLayerNetwork(networkConfig);
        network.init();

        List<SimulationContext> states = trainConfig.collector.collectStates(trainConfig.numStates, trainConfig.initialState);
        
        while(!states.isEmpty()) {

        }

        saveNetwork(network, saveLocation);
    }

    @Override
    public double getCritique(SimulationContext context, Player pov)
    {
        FeatureCollector fCollector = new FeatureCollector(context.getGameContext(), pov);
        INDArray features = Nd4j.create(fCollector.getFeatures(true, context.getGameContext(), pov));
        return network.output(features).get(new SpecifiedIndex(0)).getDouble(0);
    }

    private MultiLayerNetwork loadNetwork(Path loadLocation)
    {
        if (Files.exists(loadLocation)) {
            try {
                return ModelSerializer.restoreMultiLayerNetwork(loadLocation.toString());
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error loading NN from " + loadLocation.toString());
            }
        } else {
            throw new RuntimeException("Error: " + loadLocation.toString() + " does not exist");
        }
    }

    private void saveNetwork(MultiLayerNetwork network, Path saveLocation)
    {
        if(!Files.exists(saveLocation)) {
            try {
                Files.createFile(saveLocation);
            } catch(Exception e) {
                throw new RuntimeException("Error creating " + saveLocation.toString());
            }
        }

        try {
            //no idea what saveUpdater (3rd arg) does
            ModelSerializer.writeModel(network, saveLocation.toString(), true);
        } catch(Exception e) {
            throw new RuntimeException("Error saving NN to " + saveLocation.toString());
        }
    }
}
