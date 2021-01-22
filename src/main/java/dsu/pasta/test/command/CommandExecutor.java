package dsu.pasta.test.command;

import dsu.pasta.utils.ZPrint;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CommandExecutor {
    public static int timeoutSecond = 60;
    /**
     * For suppress output.
     */
    private static File NULL_FILE = new File(
            (System.getProperty("os.name")
                    .startsWith("Windows") ? "NUL" : "/dev/null")
    );
    public boolean success = false;
    public String errorResult = null;
    private Command cmd;
    /**
     * Set the timeout time for running this command in second.
     */
    private long runningTime = -1;
    private HashMap<String, String> environmentVariable = new HashMap<>();
    private String workingDir = null;
    private boolean timeout;
    private int exitValue = -1;
    private File redirectOutputErrorTo = null;

    /**
     * The output and error message will be redirect to console ouput.
     *
     * @param cmd
     */
    public CommandExecutor(Command cmd) {
        this.cmd = cmd;
    }

    /**
     * The output and error message will be redirect to console ouput.
     *
     * @param cmd
     */
    public CommandExecutor(String cmd) {
        this(new Command(cmd));
    }

    /**
     * @param timeoutSecond timeout time in second. Default is 600 seconds if not
     *                      specified.
     */
    public static void setTimeoutSec(int timeoutSecond) {
        timeoutSecond = timeoutSecond;
    }

    public int getRunningTimeInSecond() {
        return (int) this.runningTime;
    }

    /**
     * Redirect output and error to one file
     *
     * @param file
     */
    public void setRedirectOutputErrorTo(String file) {
        this.redirectOutputErrorTo = new File(file);
    }

    public void setWorkDir(String dir) {
        this.workingDir = dir;
    }

    /**
     * Execute the command. Cache output to outString and error output to
     * errString. The exist value of this command is exitValue.
     *
     * @param suppressOutput true if suppress outputs
     */
    public void execute(boolean suppressOutput) {
        StringBuilder error = new StringBuilder();
        BufferedReader errorBr = null;

        Process process = null;

        long startTime = System.currentTimeMillis();
        try {
            ProcessBuilder probuilder = new ProcessBuilder(this.cmd.getCommand());
            if (suppressOutput) {
                probuilder.redirectError(NULL_FILE);
                probuilder.redirectOutput(NULL_FILE);
            } else {
                probuilder.redirectError(NULL_FILE);
                probuilder.redirectOutput(NULL_FILE);
            }
//            probuilder.redirectErrorStream(true);
//            probuilder.redirectError(NULL_FILE);
//            probuilder.redirectOutput(NULL_FILE);
//            probuilder.redirectError(Redirect.INHERIT);
//            probuilder.redirectOutput(Redirect.INHERIT);
            if (workingDir != null) {
                probuilder.directory(new File(workingDir));
            }
            if (this.environmentVariable.size() > 0) {
                Map<String, String> envs = probuilder.environment();
                envs.putAll(this.environmentVariable);
            }
            process = probuilder.start();

            errorBr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line = null;
            while ((line = errorBr.readLine()) != null) {
                error.append(line);
                error.append("\n");
            }
            // waitFor return true if NOT wimeout
            this.timeout = !(process.waitFor(this.timeoutSecond, TimeUnit.SECONDS));
            process.destroy();
            this.exitValue = process.exitValue();
            this.errorResult = error.toString();
        } catch (IOException e) {
//            e.printStackTrace();
        } catch (InterruptedException e) {
//            e.printStackTrace();
        } catch (IllegalThreadStateException e) {
//            e.printStackTrace();
        } finally {
            if (process.isAlive()) {
                ZPrint.verbose("Process still alive, kill again " + process.toString());
                process.destroyForcibly();
//                process.destroy();
            }
            if (this.timeout || this.exitValue != 0)
                this.success = false;
            else
                this.success = true;
            long endTime = System.currentTimeMillis();
            this.runningTime = 1 + (endTime - startTime) / 1000l;
            ZPrint.verbose("Command running time " + this.runningTime + ", success " + this.success + ", timeout " + this.timeout + ", exitValue " + this.exitValue);
        }
    }

    public void addEnvironmentVariable(String key, String value) {
        this.environmentVariable.put(key, value);
    }

    public void removeEnvironmentVariable(String key) {
        this.environmentVariable.remove(key);
    }

    public Command getCommand() {
        return this.cmd;
    }

    public boolean isTimeout() {
        return this.timeout;
    }

    public int getExitValue() {
        return this.exitValue;
    }
}

