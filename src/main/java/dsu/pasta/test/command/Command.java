package dsu.pasta.test.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Be careful about escape spaces and quotes.
 */
public class Command {
    public String shortCmd = "";
    private List<String> commandList = new ArrayList<String>();

    public Command() {
//        if (SystemUtils.IS_OS_WINDOWS) {
//            this.commandList.add("cmd");
//            this.commandList.add("/c");
//        } else if (SystemUtils.IS_OS_LINUX) {
//            this.commandList.add("/bin/sh");
//            this.commandList.add("-c");
//        }
    }

    public Command(String cmd) {
        this();
        this.commandList.add(cmd);
    }

    public Command(List<String> cmdList) {
        this();
        this.commandList.addAll(cmdList);
    }

    public String[] getCommand() {
        return this.commandList.toArray(new String[this.commandList.size()]);
    }

    public String toString() {
        return String.join("", commandList);
    }
}
