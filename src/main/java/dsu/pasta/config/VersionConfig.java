package dsu.pasta.config;


import dsu.pasta.utils.ZFileUtils;

import java.io.File;

public class VersionConfig extends Config {
    public String root;
    public String dumpDir;

    public String projectSourceDir;

    public String projectByteDir;
    public String testByteDir;
    public String instrumentByteDir;

    public String[] jarDirs;
    public File[] jars;
    public String jarCp;

    public String versionString;

    public VersionConfig(String prefix) {
        versionString = prefix;
    }

    public void parseConfig() {
        root = ZFileUtils.getPurePath(config.getString(versionString + ".root"));
        dumpDir = String.join(File.separator, ProjectConfig.one().dumpRoot, versionString);
        ZFileUtils.createFullDirectory(dumpDir);

        projectSourceDir = config.getString(versionString + ".projectSourceDir");

        projectByteDir = config.getString(versionString + ".projectByteDir");
        testByteDir = config.getString(versionString + ".testByteDir");
        instrumentByteDir = String.join(File.separator, new File(projectByteDir).getParent(), "instrument");

        jarDirs = config.getStringArray(versionString + ".jarDirs.dir");

        jars = ZFileUtils.findAllJars(jarDirs);
        if (jars == null || jars.length == 0) {
            jarCp = projectByteDir;
        } else if (jars.length != 0) {
            jarCp = ZFileUtils.findAllJarToCP(jarDirs);
            jarCp = String.join(File.pathSeparator, jarCp, projectByteDir);
        }
    }
}