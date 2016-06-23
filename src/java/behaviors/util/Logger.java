package behaviors.util;

import java.nio.file.Files;
import java.nio.file.Path;

public class Logger
{
    private Logger() {}

    public static synchronized void log(String text, boolean consoleOutput, Path logFile)
    {
        if(consoleOutput) {
            System.out.println(text);
        }

        if(logFile != null) {
            writeToFile(logFile, text);
        }
    }

    private static synchronized void writeToFile(Path path, String text)
    {
        if(!Files.exists(path)) {
            try {
                Files.createFile(path);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Error creating + " + path.toString());
            }
        }

        try {
            Files.write(path, text.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error writing to + " + path.toString());
        }
    }
}