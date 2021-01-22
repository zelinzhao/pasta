package dsu.pasta.test.command;

import dsu.pasta.utils.ZPrint;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CommandThreadPool {
    private ExecutorService pool = Executors.newFixedThreadPool(1);

    public void execute(Thread t) {
        pool.execute(t);
    }

    public void waitAll(int timeoutSecond) {
        pool.shutdown(); // Disable new tasks from being submitted
        ZPrint.verbose("pool.shutdown");
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(timeoutSecond, TimeUnit.SECONDS)) {
                ZPrint.verbose("pool.awaitTermination");
                pool.shutdownNow(); // Cancel currently executing tasks
                ZPrint.verbose("pool.shutdownNow");
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(timeoutSecond, TimeUnit.SECONDS))
                    ZPrint.verbose("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            ZPrint.verbose("pool.shutdownNow 2");
            // Preserve interrupt status
            Thread.currentThread().interrupt();
            ZPrint.verbose("Thread.currentThread().interrupt");
        }
    }
}
