package dsu.pasta.config;

import dsu.pasta.utils.ZFileUtils;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.io.File;

public class JarsConfig {
    public static String configFile;
    protected static XMLConfiguration config;
    private static JarsConfig one;
    //3-party libs
    public String junitJar;
    public String hamcrestJar;
    public String verifyJar;
    //public String verifyLibs;
    public String verifyCp;
    public String xstreamJar;
    public String xstreamLibs;
    public String xstreamCp;
    //for agent jar
    public String agentJar;

    public static JarsConfig one() {
        if (one == null) {
            System.err.println("Parse jars config file before using it");
            System.exit(-1);
        }
        return one;
    }

    public static void parseConfigViaEnv() {
        one = new JarsConfig();
        one.junitJar = System.getenv("JUNIT_JAR");
        if (one.junitJar == null) {
            System.err.println("Need path to Junit Jar");
            System.exit(-1);
        }
        one.hamcrestJar = System.getenv("HAMCREST_JAR");
        if (one.hamcrestJar == null) {
            System.err.println("Need path to hamcrest Jar");
            System.exit(-1);
        }

        one.xstreamJar = System.getenv("XSTREAM_JAR");
        if (one.xstreamJar == null) {
            System.err.println("Need path to xstream Jar");
            System.exit(-1);
        }
        one.xstreamLibs = System.getenv("XSTREAM_LIB");
        if (one.xstreamLibs == null) {
            System.err.println("Need path to xstream dependent libraries");
            System.exit(-1);
        }
        one.xstreamCp = one.xstreamJar + File.pathSeparator + ZFileUtils.findAllJarToCP(one.xstreamLibs);

        one.agentJar = System.getenv("AGENT_JAR");
        if (one.agentJar == null) {
            System.err.println("Need path to agent Jar");
            System.exit(-1);
        }
        one.verifyJar = System.getenv("VERIFY_JAR");
        if (one.verifyJar == null) {
            System.err.println("Need path to pasta Jar");
            System.exit(-1);
        }
        one.verifyCp = one.verifyJar;
    }

    public static void parseConfig(String path) {
        one = new JarsConfig();
        configFile = path;

        Configurations configs = new Configurations();
        try {
            config = configs.xml(configFile);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            return;
        }
        one.junitJar = config.getString("junitJar");
        one.hamcrestJar = config.getString("hamcrestJar");

        one.verifyJar = config.getString("verifyJar");
        one.verifyCp = one.verifyJar;

        one.xstreamJar = config.getString("xstreamJar");
        one.xstreamLibs = config.getString("xstreamLibs");
        one.xstreamCp = one.xstreamJar + File.pathSeparator + ZFileUtils.findAllJarToCP(one.xstreamLibs);

        one.agentJar = config.getString("agentJar");
    }

}
