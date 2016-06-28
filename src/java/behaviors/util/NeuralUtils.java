package behaviors.util;

import behaviors.simulation.SimulationContext;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class NeuralUtils
{
    public static DataSet getDataSet(List<SimulationContext> states, int statesToUse, FeatureCollector fCollector, StateJudge judge)
    {
        return getDataSet(states, statesToUse, fCollector, judge, false, null, null);
    }

    public static DataSet getDataSet(List<SimulationContext> states, int statesToUse, FeatureCollector fCollector, StateJudge judge, boolean consoleOutput, Path logFile, Path saveLocation)
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

            if(i > 0 && i % 1000 == 0 && saveLocation != null) {
                INDArray inputs = Nd4j.create(Arrays.copyOfRange(inputsArr, 0, i));
                INDArray labels = Nd4j.create(Arrays.copyOfRange(labelsArr, 0, i));
                DataSet samples = new DataSet(inputs, labels);
                try {
                    Files.deleteIfExists(saveLocation);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Error deleting old sample backup");
                }
                samples.save(saveLocation.toFile());
            }

            if((i + 1) % 10 == 0) {
                Logger.log((i + 1) + "/" + statesToUse + " states labeled", consoleOutput, logFile);
            }
        }

        INDArray inputs = Nd4j.create(inputsArr);
        INDArray labels = Nd4j.create(labelsArr);
        return new DataSet(inputs, labels);
    }

    public static double meanSqError(MultiLayerNetwork network, INDArray inputs, INDArray labels)
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

    public static double getErrorIfZero(INDArray labels)
    {
        double sumErr = 0.0;
        for(int i = 0; i < labels.length(); i++){
            sumErr += (0 - labels.getDouble(i)) * (0 - labels.getDouble(i));
        }
        return sumErr / labels.length();
    }

    public static MultiLayerNetwork loadNetwork(Path loadLocation)
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

    public static void saveNetwork(MultiLayerNetwork network, Path saveLocation)
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
