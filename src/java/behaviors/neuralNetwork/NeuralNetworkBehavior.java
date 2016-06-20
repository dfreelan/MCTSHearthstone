package behaviors.neuralNetwork;

/*import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.Sin;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
*/
/**
 * Created by dfreelan on 6/20/16.
 */

public class NeuralNetworkBehavior {


    public void printExample(){
       /* Nd4j.ENFORCE_NUMERICAL_STABILITY = true;
        int numInputs = 1;
        int numOutputs = 1;
        int numHiddenNodes = 20;
        int nSamples = 500;
        INDArray x0 = Nd4j.linspace(-10, 10, 500).reshape(nSamples,1);
        INDArray y0 = Nd4j.getExecutioner().execAndReturn(new Sin(x0, x0.dup())).div(x0);
        System.out.println(y0);

        int seed = 123;
        int iterations = 100;
        MultiLayerConfiguration conf  = new NeuralNetConfiguration.Builder()
                .seed(seed).constrainGradientToUnitNorm(true).learningRate(1e-1)
                .iterations(iterations).constrainGradientToUnitNorm(true).l1(1e-1)
                .l2(1e-3).regularization(true).miniBatch(false)
                .optimizationAlgo(OptimizationAlgorithm.LINE_GRADIENT_DESCENT)
                .list(2)
                .layer(0, new DenseLayer.Builder().nIn(numInputs).nOut(numHiddenNodes)
                        .activation("relu")
                        .weightInit(WeightInit.XAVIER)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT)
                        .weightInit(WeightInit.XAVIER).updater(Updater.SGD)
                        .activation("identity").weightInit(WeightInit.XAVIER)
                        .nIn(numHiddenNodes).nOut(numOutputs).build()).backprop(true)
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(conf);
        network.init();
        network.setListeners(new ScoreIterationListener(1));
        network.fit(new DataSet(x0, y0));
        System.out.println(network.output(x0));*/

    }

}
