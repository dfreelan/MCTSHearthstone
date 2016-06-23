package behaviors.critic;

import behaviors.util.Logger;
import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.optimize.api.IterationListener;

import java.nio.file.Path;

public class TrainingIterationListener implements IterationListener
{
    private boolean invoked;
    private int logInterval;
    private boolean consoleOutput;
    private Path logFile;

    public TrainingIterationListener(int logInterval, boolean consoleOutput, Path logFile)
    {
        this.invoked = false;
        this.logInterval = Math.min(1, logInterval);
        this.consoleOutput = consoleOutput;
        this.logFile = logFile;
    }

    @Override
    public void invoke() { invoked = false; }

    @Override
    public boolean invoked() { return invoked; }

    @Override
    public void iterationDone(Model model, int iteration)
    {
        if(iteration % logInterval == 0) {
            invoke();
            Logger.log("Iteration[ + " + iteration + "] Score: " + model.score(), consoleOutput, logFile);
        }
    }
}
