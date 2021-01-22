package dsu.pasta.utils;

import dsu.pasta.Main;

import java.util.Date;
import java.util.logging.*;

public class ZPrint {
    //[pasta] hour:minute:second level msg
    public static final String format = "[%s] %02d:%02d:%02d [%s] %s%n";
    public static String VERBOSE_STRING = "VERBOSE";
    public static boolean verboseFlag = false;
    public static Logger mainLogger = null;

    public static void info(String msg) {
        mainLogger.info(msg);
    }

    public static void verbose(String msg) {
        mainLogger.fine(msg);
    }

    public static void initLog(boolean verbose) {
        mainLogger = Logger.getLogger(Main.class.getName());
        mainLogger.setUseParentHandlers(false);
        ConsoleHandler consoleHandler = new ConsoleHandler();
        if (verbose) {
            mainLogger.setLevel(Level.ALL);
            consoleHandler.setLevel(Level.ALL);
        } else {
            mainLogger.setLevel(Level.INFO);
            consoleHandler.setLevel(Level.INFO);
        }
        consoleHandler.setFormatter(new MainFormatter());
        mainLogger.addHandler(consoleHandler);
    }

    public static void print(String msg) {
        if (verboseFlag)
            System.out.println("[PASTA] " + msg);
    }

    public static class MainFormatter extends Formatter {
        private final Date dat = new Date();

        @Override
        public synchronized String format(LogRecord record) {
            dat.setTime(record.getMillis());
            return String.format(format,
                    "PASTA",
                    dat.getHours(), dat.getMinutes(), dat.getSeconds(),
                    record.getLevel().getName(), record.getMessage());
        }
    }
}
