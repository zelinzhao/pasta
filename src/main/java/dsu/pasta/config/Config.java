package dsu.pasta.config;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

public abstract class Config {
    public static String configFile;
    protected static XMLConfiguration config;

    public static void parseConfig(String path) {
        try {
            configFile = path;
            Configurations configs = new Configurations();
            config = configs.xml(configFile);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        ProjectConfig.one();
        UpdateConfig.one();
    }
}
