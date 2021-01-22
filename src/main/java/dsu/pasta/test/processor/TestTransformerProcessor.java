package dsu.pasta.test.processor;

import dsu.pasta.Verifier;
import dsu.pasta.config.ProjectConfig;
import dsu.pasta.config.UpdateConfig;
import dsu.pasta.object.processor.CompareObjects;
import dsu.pasta.object.processor.ObjectComparator;
import dsu.pasta.test.command.*;
import dsu.pasta.utils.ZFileUtils;
import dsu.pasta.utils.ZPrint;

import java.io.File;
import java.text.DecimalFormat;

import static dsu.pasta.utils.ZPrint.print;

public class TestTransformerProcessor {
    private static DecimalFormat df = new DecimalFormat("0.0000");

    public static void process(String transMethod, String targetFieldName) {
        ZFileUtils.deleteAllFilesInDir(ProjectConfig.one().transObjRoot);
        ZPrint.verbose("Dump transformed objects to " + ProjectConfig.one().transObjRoot);

        SucFail sf = new SucFail(0, 0, targetFieldName);
        transMethod += "dsu.pasta.object.processor.ObjectApi.processObject(" + targetFieldName + ",-1);";
        CommandThreadPool pool = new CommandThreadPool();
        OneTransThread.sf = sf;
        OneTransThread.result = "";
        OneTransThread.tryTimes = 0;
        OneTransThread.errorResult = "";

        long pre = System.currentTimeMillis();
        for (File oldObject : ZFileUtils.findFileRecursivelyWithPattern(ProjectConfig.one().oldVersion.dumpDir, ".xml")) {
            String fileName = oldObject.getName();
            String testName = fileName.replace("_object.xml", "");
            ZPrint.verbose("Old object path: " + oldObject.getAbsolutePath());
            ZPrint.verbose("Test name: " + testName);
            String newObject = String.join(File.separator, ProjectConfig.one().newVersion.dumpDir, fileName);
            String newCleanObject = CompareObjects.cleanXmlFile(newObject, false);
            Command ttcmd = TestTransformerCommandBuilder.commandBuilder(ProjectConfig.one().oldVersion,
                    ProjectConfig.one().newVersion, testName);
            CommandExecutor ttexe = new CommandExecutor(ttcmd);
            ttexe.addEnvironmentVariable(ZPrint.VERBOSE_STRING, String.valueOf(ZPrint.verboseFlag));
            ttexe.addEnvironmentVariable("METHOD_BODY", transMethod);
            ttexe.addEnvironmentVariable("TARGET_CLASS", UpdateConfig.one().targetClass);
            ttexe.addEnvironmentVariable("TRANS_OBJECT", "true");
            ttexe.addEnvironmentVariable(RunTestDumpObjectsProcessor.NOW_OLD, "true");

            ttexe.setWorkDir(ProjectConfig.one().newVersion.root);

            if (!new File(newObject).exists()) {
                ZPrint.verbose("No corresponding new object. Continue ...");
                continue;
            }
            ZPrint.verbose("New object path: " + newObject);
            String transformedObject = String.join(File.separator, ProjectConfig.one().transObjRoot, fileName);
            ZPrint.verbose("Transformed object path: " + transformedObject);
            new File(transformedObject).delete();
            ttexe.addEnvironmentVariable("DUMP_FILE", transformedObject);
            // for dumping transformed field
            ZPrint.verbose("Testing transformer: Old object --> transformed object ...");
            OneTransThread ott = new OneTransThread(ttexe, transformedObject, newCleanObject, fileName, testName);
            pool.execute(ott);
        }
        pool.waitAll(CommandExecutor.timeoutSecond);
        ZPrint.print(Verifier.originMethodWithLinebreak);
        if (OneTransThread.tryTimes > 0 && OneTransThread.tryTimes == OneTransThread.sf.successTimes) {
            System.out.println("[SuccessRate]\t" + 1.0);
            System.out.println("[CompareTimes]\t" + OneTransThread.tryTimes + "\t[IdenticalTimes]\t" + OneTransThread.sf.successTimes
                    + "\t[NotIdenticalTimes]\t" + OneTransThread.sf.failTimes);
            System.out.print(OneTransThread.result);
        } else {
            double rate = OneTransThread.tryTimes == 0 ? 0 : (double) OneTransThread.sf.successTimes / (double) OneTransThread.tryTimes;
            System.out.println("[SuccessRate]\t" + df.format(rate));
            System.out.println("[CompareTimes]\t" + OneTransThread.tryTimes + "\t[IdenticalTimes]\t" + OneTransThread.sf.successTimes
                    + "\t[NotIdenticalTimes]\t" + OneTransThread.sf.failTimes);
            System.out.print(OneTransThread.result);
        }
    }

    private static class SucFail {
        public int successTimes;
        public int failTimes;
        public String targetName;

        public SucFail(int suc, int fail, String tar) {
            this.successTimes = suc;
            this.failTimes = fail;
            this.targetName = tar;
        }
    }

    private static class OneTransThread extends CommandThread {
        static SucFail sf;
        static String result = "";
        static int tryTimes;
        static String errorResult = "";

        static Object lock = new Object();

        String transformedObject;
        String newCleanObject;
        String fileName;
        String testName;

        public OneTransThread(CommandExecutor ce, String transformedObject, String newCleanObject, String fileName, String testName) {
            super(ce);
            this.transformedObject = transformedObject;
            this.newCleanObject = newCleanObject;
            this.fileName = fileName;
            this.testName = testName;
        }

        @Override
        public void run() {
            ce.execute(ZPrint.verboseFlag);
            errorResult += ce.errorResult;
            // deserialize success
            if (ce.success) {
                ZPrint.verbose("Run test successfully! Compare transformed object with new object ...");
            } else {
                ZPrint.verbose("Run test failed! Don't compare transformed object with new object.");
                synchronized (lock) {
                    result += "[FailToRun]\t" + testName + "\n";
                }
                return;
            }
            String transCleanObject = CompareObjects.cleanXmlFile(transformedObject, true);

            print("Compare new object with transformed object");
            //TODO we assume new fields' length is 1 in many places. take care of this
            ObjectComparator oc = new ObjectComparator(false, transCleanObject, newCleanObject,
                    UpdateConfig.one().newField.name, ProjectConfig.one().onlyCompareTag, false);
            synchronized (lock) {
                if (oc.isSame()) {
                    sf.successTimes++;
                    print("Transformed object is identical to new object.");
                    result += "[IdenticalStates]\t" + testName + "\n";
                } else {
                    sf.failTimes++;
                    print("Transformed object is not identical to new object.");
                    result += "[NotIdenticalStates]\t" + testName + "\n";
                }
                tryTimes++;
            }
        }
    }
}
