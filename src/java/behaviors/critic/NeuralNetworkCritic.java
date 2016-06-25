package behaviors.critic;

import behaviors.simulation.SimulationContext;
import behaviors.util.FeatureCollector;
import net.demilich.metastone.game.GameContext;
import net.demilich.metastone.game.Player;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.SpecifiedIndex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class NeuralNetworkCritic implements Critic
{
    MultiLayerNetwork network;
    FeatureCollector fCollector;

    private NeuralNetworkCritic(){}
    public NeuralNetworkCritic(Path loadLocation, SimulationContext initialState)
    {
        fCollector = new FeatureCollector(initialState.getGameContext(), initialState.getGameContext().getPlayer1());
        network = loadNetwork(loadLocation);
    }

    public NeuralNetworkCritic(MultiLayerConfiguration networkConfig, TrainConfig trainConfig, Path saveLocation)
    {
        network = new MultiLayerNetwork(networkConfig);
        network.init();

        List<SimulationContext> states = trainConfig.collector.collectStates(trainConfig.numStates, trainConfig.initialState, trainConfig.parallel);
        double[][] inputsArr = new double[trainConfig.numStates][];
        double[][] labelsArr = new double[trainConfig.numStates][];

        GameContext context = trainConfig.initialState.getGameContext();
        System.err.println("Context is: " + context);
        fCollector = new FeatureCollector(context, context.getPlayer1());

        System.err.println("NumFeatures in NNCritic: " + fCollector.getFeatures(true, context, context.getPlayer1()).length);
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
        //labelsArr = null;

        network.setListeners(new TrainingIterationListener(1, true, Paths.get("training_log.txt")));
        System.err.println("BEFORE TRAINING");
        network.fit(inputs, labels);
        System.err.println("AFTER TRAINING: " + meanSqError(inputs, labels));



        System.err.println("error if always 0: " + getErrorIfZero(labelsArr));
        saveNetwork(network, saveLocation);
    }

    private double meanSqError(INDArray inputs, INDArray labels)
    {
        double totalSqError = 0;
        for(int i = 0; i < labels.length(); i++) {
            INDArray input = inputs.getRow(i);
            double output = network.output(input, false).getDouble(0);

            double error = labels.getDouble(i) - output;
            totalSqError += error * error;
        }
        return totalSqError / labels.length();
    }

    public double getErrorIfZero(double[][] labels){
        double sumErr =0.0;
        for(int i = 0; i<labels.length; i++){
            sumErr+=(0-labels[i][0])*(0-labels[i][0]);
        }
        return sumErr;

    }
    @Override
    public double getCritique(SimulationContext context, Player pov)
    {
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
        NeuralNetworkCritic clone = new NeuralNetworkCritic();
        clone.network = this.network.clone();
        clone.fCollector = fCollector.clone();

        return clone;
    }
}
