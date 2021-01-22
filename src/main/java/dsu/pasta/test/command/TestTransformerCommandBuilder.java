package dsu.pasta.test.command;

import dsu.pasta.config.JarsConfig;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.VersionConfig;
import dsu.pasta.junit.JunitTestRunner;
import dsu.pasta.utils.ZPrint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TestTransformerCommandBuilder {
    public static Command commandBuilder(VersionConfig config) {
        String classpath = ProjectConfig.one().testRoot;
        classpath += File.pathSeparator + JarsConfig.one().xstreamCp;
        classpath += File.pathSeparator + config.jarCp;
        classpath += File.pathSeparator + JarsConfig.one().verifyCp;

        List<String> cmds = new ArrayList<>();
        cmds.add("java");
        cmds.add("-javaagent:" + JarsConfig.one().agentJar);
        cmds.add("-cp");
        cmds.add(classpath);
        cmds.add("dsu.pasta.test.TransTestFramework");

        Command c = new Command(cmds);
        c.shortCmd = "java -javaagent:agent.jar dsu.pasta.test.TransTestFramework";
        return c;
    }

    public static Command commandBuilder(VersionConfig config, VersionConfig testFrom, String targetTest) {
        String classpath = config.instrumentByteDir;
        classpath += File.pathSeparator + ProjectConfig.one().testRoot;
        classpath += File.pathSeparator + testFrom.testByteDir;
        classpath += File.pathSeparator + config.jarCp;
        classpath += File.pathSeparator + JarsConfig.one().xstreamCp;
        classpath += File.pathSeparator + JarsConfig.one().junitJar;
        classpath += File.pathSeparator + JarsConfig.one().hamcrestJar;
        classpath += File.pathSeparator + JarsConfig.one().verifyCp;

        List<String> cmds = new ArrayList<>();
        cmds.add("java");
        cmds.add("-javaagent:" + JarsConfig.one().agentJar);
        cmds.add("-cp");
        cmds.add(classpath);
        cmds.add(JunitTestRunner.class.getName());
        cmds.add(targetTest);

        ZPrint.verbose(cmds.toString());

        Command c = new Command(cmds);
        return c;
    }
}
