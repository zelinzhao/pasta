package dsu.pasta;

import dsu.pasta.config.Config;
import dsu.pasta.config.JarsConfig;
import dsu.pasta.javaparser.gadget.program.PrettyCodePrinter;
import dsu.pasta.test.command.CommandExecutor;
import dsu.pasta.test.processor.RunTestDumpObjectsProcessor;
import dsu.pasta.test.processor.TestTransformerProcessor;
import dsu.pasta.utils.ZPrint;
import org.apache.commons.cli.*;

public class Verifier {
    public static String method;
    public static String tarVarName;
    public static String originMethodWithLinebreak;

    private static CommandLine cmd = null;
    private static Options options = null;

    public static void verifier(String[] args)
            throws SecurityException, IllegalArgumentException {
        parserCmd(args);
        ZPrint.verboseFlag = cmd.hasOption("v");
        ZPrint.initLog(ZPrint.verboseFlag);
        parseArguments(cmd);

        ZPrint.verbose("Running tests to dump target old and new objects");
        RunTestDumpObjectsProcessor.process();
        //read transformer method code
        //read target field name
        TestTransformerProcessor.process(method, tarVarName);
    }

    private static void parserCmd(String[] args) {
        options = new Options();

        options.addOption(Option.builder("pc")
                .hasArg()
                .required()
                .desc("XML config file of target program, containing source/byte code directory")
                .build());
        options.addOption(Option.builder("jc")
                .hasArg()
                .desc("XML config file of necessary jars, which can also be specified by environment variables")
                .build());
        options.addOption(Option.builder("v")
                .longOpt("verbose")
                .hasArg(false)
                .desc("Print verbose messages for debug.")
                .build());
        options.addOption(Option.builder("m")
                .longOpt("method")
                .hasArg()
                .required()
                .desc("The code of transformer method that should be verified. Inside the code, use "
                        + PrettyCodePrinter.STALE_OBJ_SPECIAL + " as the stale object, and " + PrettyCodePrinter.TAR_FIELD_SPECIAL + " as the target field to be tested")
                .build());
        options.addOption(Option.builder("tt")
                .longOpt("testTimeout")
                .hasArg()
                .desc("Timeout in seconds for one test case execution. Default is " + CommandExecutor.timeoutSecond)
                .build());
        options.addOption(Option.builder("h")
                .longOpt("help")
                .hasArg(false)
                .desc("Print help messge")
                .build());
        try {
            cmd = new DefaultParser().parse(options, args);
            if (cmd.hasOption("h")) {
                helpAndExit(0);
            }
        } catch (ParseException e) {
            helpAndExit(-1);
        }
    }

    private static void helpAndExit(int exitValue) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("pasta verifier", options);
        System.exit(exitValue);
    }

    /**
     * This method is for distiller main method.
     *
     * @param cmd
     */
    private static void parseArguments(CommandLine cmd) {
        if (cmd == null) {
            ZPrint.verbose("Use --help to see the help message.");
            System.exit(0);
        }
        method = cmd.getOptionValue("m");
        parseMethodCode();
        ZPrint.verbose("Target variable to be verified inside the transformer is " + tarVarName);

        String testTimeout = cmd.getOptionValue("tt");
        if (testTimeout != null) {
            CommandExecutor.timeoutSecond = Integer.valueOf(testTimeout);
        }

        String pc = cmd.getOptionValue("pc");
        Config.parseConfig(pc);
        String jc = cmd.getOptionValue("jc");
        if (jc != null) {
            JarsConfig.parseConfig(jc);
        } else {
            JarsConfig.parseConfigViaEnv();
        }
    }

    private static void parseMethodCode() {
        if (method == null || method.length() == 0) {
            ZPrint.verbose("Method code string is null or length is 0. Error");
            System.exit(-1);
        }
        method = PrettyCodePrinter.replaceSpecialForTest(method);
        tarVarName = PrettyCodePrinter.TAR_FIELD_NAME;

        originMethodWithLinebreak = method.replaceAll("%nn", "\n");
        method = method.replaceAll("%nn", "");
    }

}
