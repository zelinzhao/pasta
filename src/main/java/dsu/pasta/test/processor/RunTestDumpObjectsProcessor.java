package dsu.pasta.test.processor;

import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.config.VersionConfig;
import dsu.pasta.object.processor.ObjectApi;
import dsu.pasta.test.command.*;
import dsu.pasta.utils.ZFileUtils;
import dsu.pasta.utils.ZPrint;

import java.io.File;

public class RunTestDumpObjectsProcessor {
    public static final String NOW_OLD = "NOW_OLD";

    public static void process() {
        ZPrint.verbose("Tests size: " + ProjectConfig.one().allTests);
        ZPrint.verbose("Run tests on new version to dump objects");
        RunTests(ProjectConfig.one().newVersion, ProjectConfig.one().newVersion);

        ZPrint.verbose("Run tests on old version to dump objects");
        RunTests(ProjectConfig.one().oldVersion, ProjectConfig.one().newVersion);
    }

    public static void RunTests(VersionConfig one, VersionConfig testFrom) {
        int testSize = ProjectConfig.one().allTests.size();
        File[] existObjects = ZFileUtils.findFileRecursivelyWithPattern(ProjectConfig.one().oldVersion.dumpDir, ".xml");
        ZPrint.verbose("Tests number: " + testSize + "; existing objects number: " + existObjects);
        if (existObjects.length >= testSize) {
            ZPrint.verbose("Already exist objects. Skip dump");
            return;
        }

        ZPrint.verbose("Clear object dumps from " + one.dumpDir);
        ZFileUtils.deleteAllFilesInDir(one.dumpDir);

        CommandThreadPool pool = new CommandThreadPool();
        for (String test : ProjectConfig.one().allTests) {
            test = test.trim();
            ZPrint.verbose("Run test " + test + " on " + one.versionString + " version ...");
            Command cmd = RunTestDumpObjCommandBuilder.commandBuilder(one, testFrom, test);
            CommandExecutor ce = new CommandExecutor(cmd);
            ce.setWorkDir(testFrom.root);
            String objpath = String.join(File.separator, one.dumpDir, test + "_object.xml");
            if (one.versionString.equals("old"))
                ce.addEnvironmentVariable(NOW_OLD, "true");
            else
                ce.addEnvironmentVariable(NOW_OLD, "false");

            ce.addEnvironmentVariable(ObjectApi.DUMP_FILE, objpath);
            ce.addEnvironmentVariable(ObjectApi.TARGET_CLASS, UpdateConfig.one().targetClass);

            // remove previous obj dump
            new File(objpath).delete();
            // thread
            Thread comThread = new TestThread(ce, test, objpath);
            pool.execute(comThread);
        }
        pool.waitAll(CommandExecutor.timeoutSecond);
    }


    public static class TestThread extends CommandThread {

        String test;
        String objPath;

        public TestThread(CommandExecutor ce, String test, String objPath) {
            super(ce);
            this.test = test;
            this.objPath = objPath;
        }

        @Override
        public void run() {
            ce.execute(false);
            if (ce.success && !ce.isTimeout()) {
                ZPrint.verbose("Execute " + test + " successfully.");
            } else {
                ZPrint.verbose("Execute " + test + " failed. Exit.");
                ZPrint.verbose(ce.errorResult);
                System.exit(-1);
                new File(objPath).delete();
                return;
            }
            if (!new File(objPath).exists()) {
                ZPrint.verbose("Execute " + test + " can't dump object. Exit.");
                System.exit(-1);
            }
        }
    }

}
