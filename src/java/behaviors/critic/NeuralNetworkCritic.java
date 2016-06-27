package behaviors.critic;

import behaviors.simulation.SimulationContext;
import behaviors.util.FeatureCollector;
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
import java.nio.file.Paths;
import java.util.List;

public class NeuralNetworkCritic implements Critic
{
    private final String trainingLogFileLocation = "training_log.txt";

    private MultiLayerNetwork network;
    private FeatureCollector fCollector;

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
        int trainingStates = (int)(trainConfig.numStates * 0.9);
        int testingStates = trainConfig.numStates - trainingStates;

        GameContext context = trainConfig.initialState.getGameContext();
        System.err.println("Context is: " + context);
        fCollector = new FeatureCollector(context, context.getPlayer1());
        System.err.println("NumFeatures in NNCritic: " + fCollector.getFeatures(true, context, context.getPlayer1()).length);

        Path trainingLogFile = Paths.get(trainingLogFileLocation);
        if(Files.exists(trainingLogFile)) {
            try {
                Files.delete(trainingLogFile);
            } catch(Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error deleting old log file " + trainingLogFileLocation);
            }
        }

        network.setListeners(new TrainingIterationListener(1, true, trainingLogFile));
        DataSet trainingData = getDataSet(states, trainingStates, fCollector, trainConfig.judge);
        network.fit(trainingData);
        System.err.println("Training Error: " + meanSqError(trainingData.getFeatures(), trainingData.getLabels()));

        DataSet testingData = getDataSet(states, testingStates, fCollector, trainConfig.judge);
        System.err.println("Testing Error: " + meanSqError(testingData.getFeatures(), testingData.getLabels()));

        System.err.println("Error if always 0: " + getErrorIfZero(trainingData.getLabels()));
        saveNetwork(network, saveLocation);
    }

    private DataSet getDataSet(List<SimulationContext> states, int statesToUse, FeatureCollector fCollector, StateJudge judge)
    {
        double[][] inputsArr = new double[statesToUse][];
        double[][] labelsArr = new double[statesToUse][];

        for(int i = 0; i < statesToUse; i++) {
            SimulationContext state = states.remove(states.size() - 1);

            inputsArr[i] = fCollector.getFeatures(true, state.getGameContext(), state.getActivePlayer());

            labelsArr[i] = new double[1];
            labelsArr[i][0] = judge.evaluate(state, state.getActivePlayer()) * 2.0 - 1.0;
            if(labelsArr[i][0] < -1.00001 || labelsArr[i][0] > 1.00001){
                throw new RuntimeException(("Error: invalid label " + labelsArr[i][0]));
            }
        }

        INDArray inputs = Nd4j.create(inputsArr);
        INDArray labels = Nd4j.create(labelsArr);
        return new DataSet(inputs, labels);
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

    private double getErrorIfZero(INDArray labels)
    {
        double sumErr = 0.0;
        for(int i = 0; i < labels.length(); i++){
            sumErr += (0 - labels.getDouble(i)) * (0 - labels.getDouble(i));
        }
        return sumErr / labels.length();
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
    public Critic clone()
    {
        NeuralNetworkCritic clone = new NeuralNetworkCritic();
        clone.network = this.network.clone();
        clone.fCollector = fCollector.clone();
        return clone;
    }
}
