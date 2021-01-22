package dsu.pasta.junit;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLClassLoader;

/**
 * Require Junit 4
 */
public class JunitTestRunner {
    public static final String SEPARATOR = "+";
    public static final String TEST_CASE_SUCCESSFUL = "TEST_CASE_SUCCESSFUL";
    public static final String TEST_CASE_FAILED = "TEST_CASE_FAILED";
    private static int exitValue = 0;
    private static ClassLoader loader = (URLClassLoader) JunitTestRunner.class.getClassLoader();

    private static void processResult(Result result) {
        if (result.wasSuccessful()) {
//            System.out.println(TEST_CASE_SUCCESSFUL);
        } else {
//            System.out.println(TEST_CASE_FAILED);
            for (Failure fail : result.getFailures()) {
                System.err.println(fail.toString());
            }
            exitValue = -1;
        }
    }

    /**
     * Run one Junit test method.
     *
     * @param classMethod a test method in a test class in the form of "testClass+testMethod".
     */
    private static void runMethod(String classMethod) {
        if (!classMethod.contains(SEPARATOR)) {
            System.out.println("Not valid class+method.");
            return;
        }
        String[] cm = classMethod.split("\\" + SEPARATOR);
        Request request = null;
        Class testClass = null;
        try {
            testClass = loader.loadClass(cm[0]);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        request = Request.method(testClass, cm[1]);
        Result result = new JUnitCore().run(request);
        processResult(result);
    }

    /**
     * Run all Junit test methods in one test class.
     *
     * @param cla
     */
    private static void runClass(String cla) {
        if (cla.contains(SEPARATOR)) {
            System.out.println("Not valid class name.");
            return;
        }
        Request request = null;
        Class testClass = null;
        try {
            testClass = loader.loadClass(cla);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        request = Request.aClass(testClass);
        Result result = new JUnitCore().run(request);
        processResult(result);
    }

    /**
     * Run a list of test cases.
     *
     * @param args a list of tests. Each test is either "testClass" (run all test methods in this class)
     *             or "testClass+testmethod" (run one test method in this class).
     */
    public static void main(String... args) throws ClassNotFoundException, IOException {
//        debug();
        for (String t : args) {
            if (t.contains(SEPARATOR))
                runMethod(t);
            else
                runClass(t);
        }
        System.exit(exitValue);
    }

    private static void debug() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("E:\\Work\\ftpcure\\output"));
            String basedir = System.getProperty("basedir");
            bw.write("basedir: " + basedir);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
