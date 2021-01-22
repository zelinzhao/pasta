package dsu.pasta;

import dsu.pasta.config.Config;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.dpg.ExtractProjectUpdatedInfoProcessor;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javaparser.gadget.collect.GadgetsReader;
import dsu.pasta.javaparser.gadget.program.PenaltySynthesizer;
import dsu.pasta.javassist.JavassistSolver;
import dsu.pasta.utils.ZPrint;
import org.apache.commons.cli.*;

import java.util.Arrays;

public class Synthesizer {
    private static CommandLine cmd = null;
    private static Options options = null;

    public static void synthesizer(String[] args)
            throws SecurityException, IllegalArgumentException {
        parserCmd(args);
        ZPrint.verboseFlag = cmd.hasOption("v");
        parseArguments(cmd);
        ZPrint.initLog(ZPrint.verboseFlag);

        ZPrint.verbose("Starting PASTA");
        ZPrint.verbose("Read detailed update information");
        ExtractProjectUpdatedInfoProcessor.readUpdateInfo(UpdateConfig.javelusXml);
        /**/
        ZPrint.verbose("Building javaparser solver on old version to solve added field's type");
        JavaparserSolver.BuildSolver(ProjectConfig.one().oldVersion);
        UpdateConfig.one().solveFieldType();

        ZPrint.verbose("Building javassist solver on old version to solve removed and unchanged field's type");
        JavassistSolver.BuildSolver(ProjectConfig.one().oldVersion);
        ExtractProjectUpdatedInfoProcessor.resolveChangedFieldTypes();
        ExtractProjectUpdatedInfoProcessor.resolveSameFieldTypes();

        ZPrint.verbose("Building javassist solver on new version to solve added field's type");
        JavassistSolver.BuildSolver(ProjectConfig.one().newVersion);
        ExtractProjectUpdatedInfoProcessor.resolveChangedFieldTypes();
        GadgetsReader.process();

        ZPrint.verbose("Synthesize transformers from gadgets");
        PenaltySynthesizer.process();

        ZPrint.verbose("Pasta end.");
    }

    private static void parserCmd(String[] args) {
        options = new Options();
        options.addOption(Option.builder("pc")
                .hasArg()
                .required()
                .desc("XML config file of target program, containing source/byte code directory")
                .build());
        options.addOption(Option.builder("v")
                .longOpt("verbose")
                .hasArg(false)
                .desc("Print verbose messages for debug")
                .build());
        options.addOption(Option.builder("u")
                .hasArg()
                .required()
                .desc("The javelus.xml file that contains detailed update information")
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
        formatter.printHelp("pasta synthesizer", options);
        System.exit(exitValue);
    }

    /**
     * This method is for distiller main method.
     *
     * @param cmd
     */
    public static void parseArguments(CommandLine cmd) {
        if (cmd == null) {
            ZPrint.verbose("Use --help to see the help message.");
            System.exit(0);
        }

        String pc = cmd.getOptionValue("pc");
        Config.parseConfig(pc);
        UpdateConfig.javelusXml = cmd.getOptionValue("u");
        String apis = cmd.getOptionValue("apis");
        if (apis != null && apis.length() > 0) {
            String[] sp = apis.split(":");
            ProjectConfig.apiTypes.addAll(Arrays.asList(sp));
        }
    }

}
