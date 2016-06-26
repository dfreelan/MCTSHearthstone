package behaviors.util;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GlobalConfig
{
    public boolean consoleOutput;
    public Path logFile;

    public boolean parallel;
    public int simulations;

    public GlobalConfig()
    {
        consoleOutput = true;
        logFile = null;

        parallel = true;
        simulations = 1;
    }

    public void applyArguments(String[] args)
    {
        if(ArgumentUtils.keyExists("-console", args)) {
            consoleOutput = ArgumentUtils.parseBoolean(ArgumentUtils.argumentForKey("-console", args));
        }
        if(ArgumentUtils.keyExists("-log", args)) {
            logFile = Paths.get(ArgumentUtils.argumentForKey("-log", args));
        }
        if(ArgumentUtils.keyExists("-parallel", args)) {
            parallel = ArgumentUtils.parseBoolean(ArgumentUtils.argumentForKey("-parallel", args));
        }
        if(ArgumentUtils.keyExists("-simulations", args)) {
            simulations = Integer.parseInt(ArgumentUtils.argumentForKey("-simulations", args));
            if(simulations < 1) {
                throw new RuntimeException("Error: there must be at least one simulation run.");
            }
        }
    }
}
