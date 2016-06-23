package behaviors.critic;

import behaviors.simulation.SimulationContext;
import behaviors.util.FeatureCollector;
import behaviors.util.Logger;
import behaviors.util.StateCollector;
import behaviors.util.StateJudge;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.SpecifiedIndex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class NNCritic implements Critic
{
    MultiLayerNetwork network;

    private NNCritic(){}
    public NNCritic(Path loadLocation)
    {
        network = loadNetwork(loadLocation);
    }

    public NNCritic(MultiLayerConfiguration networkConfig, TrainConfig trainConfig, Path saveLocation)
    {
        network = new MultiLayerNetwork(networkConfig);
        network.init();

        List<SimulationContext> states = trainConfig.collector.collectStates(trainConfig.numStates, trainConfig.initialState, trainConfig.parallel);
        double[][] inputsArr = new double[trainConfig.numStates][];
        double[][] labelsArr = new double[trainConfig.numStates][];

        GameContext context = trainConfig.initialState.getGameContext();
        FeatureCollector fCollector = new FeatureCollector(context, context.getPlayer1());

        int index = 0;
        while(!states.isEmpty()) {
            SimulationContext state = states.remove(states.size() - 1);

            inputsArr[index] = fCollector.getFeatures(true, state.getGameContext(), state.getActivePlayer());

            labelsArr[index] = new double[1];
            labelsArr[index][0] = trainConfig.judge.evaluate(state, state.getActivePlayer())*2.0-1.0;

            index++;
        }

        INDArray inputs = Nd4j.create(inputsArr);
        INDArray labels = Nd4j.create(labelsArr);

        inputsArr = null;
        labelsArr = null;

        network.setListeners(new TrainingIterationListener(10, true, null));
        network.fit(inputs, labels);

        saveNetwork(network, saveLocation);
    }

    @Override
    public double getCritique(SimulationContext context, Player pov)
    {
        FeatureCollector fCollector = new FeatureCollector(context.getGameContext(), pov);
        INDArray features = Nd4j.create(fCollector.getFeatures(true, context.getGameContext(), pov));
        return (network.output(features, Layer.TrainingMode.TEST).get(new SpecifiedIndex(0)).getDouble(0)+1.0)/(2.0);
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
    @Override
    public Critic clone(){
        NNCritic clone = new NNCritic();
        clone.network = this.network.clone();

        return clone;
    }
}
