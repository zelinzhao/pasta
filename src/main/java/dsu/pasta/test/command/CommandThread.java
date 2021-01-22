package dsu.pasta.test.command;

import static dsu.pasta.utils.ZPrint.print;

public class CommandThread extends Thread {

    protected CommandExecutor ce;

    public CommandThread(CommandExecutor ce) {
        this.ce = ce;
    }

    @Override
    public void run() {
        ce.execute(false);
        if (ce.success && !ce.isTimeout())
            print("Execute successfully.");
        else {
            print("Execute failed. Remove this.");
        }
    }
}
