package behaviors.util;

public class ArgumentUtils
{
    public static boolean parseBoolean(String str)
    {
        str = str.toLowerCase();
        if(str.equals("true") || str.equals("t")) {
            return true;
        } else if(str.equals("false") || str.equals("f")) {
            return false;
        } else {
            throw new RuntimeException("Error: neither true nor false start with " + str + ".");
        }
    }

    public static boolean keyExists(String key, String[] args)
    {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    public static String argumentForKey(String key, String[] args)
    {
        for (int x = 0; x < args.length - 1; x++) // if a key has an argument, it can't be the last string
        {
            if (args[x].equalsIgnoreCase(key)) {
                return args[x + 1];
            }
        }
        throw new RuntimeException("Error: key " + key + " does not exist");
    }
}
