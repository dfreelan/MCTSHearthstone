package behaviors.critic;


import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.Sin;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 * Created by dfreelan on 6/20/16.
 */

public class NeuralNetworkBehavior {

    public static void main(String args[]){
        System.err.println("doin example thing");
        printExample();
    }
    public static void printExample(){
        Nd4j.ENFORCE_NUMERICAL_STABILITY = true;
        int numInputs = 400;
        int numOutputs = 1;
        int numHiddenNodes = 160;
        int nSamples = 1;
        INDArray x0 = Nd4j.linspace(-10, 10, numInputs).reshape(nSamples,numInputs);
        INDArray y0 = Nd4j.getExecutioner().execAndReturn(new Sin(x0, x0.dup())).div(x0);
        System.out.println(y0);


        INDArray data = Nd4j.create(new double[200]);

        int seed = 123;
        int iterations = 10;
        MultiLayerConfiguration conf  = new NeuralNetConfiguration.Builder()
                .seed(seed).learningRate(1e-1).momentum(.9)
                .iterations(iterations)
                .learningRateScoreBasedDecayRate(1e-1)
                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
                .list(2)
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation("leakyrelu")
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT)
                        .weightInit(WeightInit.XAVIER).updater(Updater.SGD)
                        .activation("identity").weightInit(WeightInit.XAVIER)
                        .nIn(numHiddenNodes).nOut(numOutputs).build()).backprop(true)
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();

        //network.setListeners((IterationListener)null);
        //network.setListeners(new ScoreIterationListener(100));
        network.fit(new DataSet(x0, y0));
        long timeBefore = System.currentTimeMillis();
        for(int i = 0; i<100000; i++) {
            network.output(x0);
        }
        System.err.println("total time: " + (System.currentTimeMillis()-timeBefore));

    }

}
