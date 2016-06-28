package behaviors.critic;

import behaviors.simulation.SimulationContext;
import behaviors.util.BehaviorConfig;
import behaviors.util.FeatureCollector;
import behaviors.util.NeuralUtils;
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
        network = NeuralUtils.loadNetwork(loadLocation);
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
        DataSet trainingData = NeuralUtils.getDataSet(states, trainingStates, fCollector, trainConfig.judge);
        network.fit(trainingData);
        System.err.println("Training Error: " + NeuralUtils.meanSqError(network, trainingData.getFeatures(), trainingData.getLabels()));

        DataSet testingData = NeuralUtils.getDataSet(states, testingStates, fCollector, trainConfig.judge);
        System.err.println("Testing Error: " + NeuralUtils.meanSqError(network, testingData.getFeatures(), testingData.getLabels()));

        System.err.println("Error if always 0: " + NeuralUtils.getErrorIfZero(trainingData.getLabels()));
        NeuralUtils.saveNetwork(network, saveLocation);
    }

    public NeuralNetworkCritic(MultiLayerConfiguration networkConfig, TrainConfig trainConfig, BehaviorConfig saveAndSampleFiles)
    {
        network = new MultiLayerNetwork(networkConfig);
        network.init();

        GameContext context = trainConfig.initialState.getGameContext();
        fCollector = new FeatureCollector(context, context.getPlayer1());

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
        DataSet trainingData = new DataSet();
        trainingData.load(saveAndSampleFiles.trainingSamplesFile.toFile());
        network.fit(trainingData);
        System.err.println("Training Error: " + NeuralUtils.meanSqError(network, trainingData.getFeatures(), trainingData.getLabels()));

        DataSet testingData = new DataSet();
        testingData.load(saveAndSampleFiles.testingSamplesFile.toFile());
        System.err.println("Testing Error: " + NeuralUtils.meanSqError(network, testingData.getFeatures(), testingData.getLabels()));

        System.err.println("Error if always 0: " + NeuralUtils.getErrorIfZero(trainingData.getLabels()));
        NeuralUtils.saveNetwork(network, saveAndSampleFiles.saveNetworkFile);
    }

    @Override
    public double getCritique(SimulationContext context, Player pov)
    {
        INDArray features = Nd4j.create(fCollector.getFeatures(true, context.getGameContext(), pov));
        return (network.output(features, Layer.TrainingMode.TEST).get(new SpecifiedIndex(0)).getDouble(0)+1.0)/(2.0);
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
