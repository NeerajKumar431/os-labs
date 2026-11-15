import java.util.concurrent.*;

public class Task4 {
    static class Sleeper implements Runnable {
        @Override public void run() {
            try { Thread.sleep(1000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        int n = Integer.parseInt(args[1]);

        long start = System.nanoTime();
        if (mode.equals("platform")) {
            Thread[] t = new Thread[n];
            for (int i = 0; i < n; i++) t[i] = new Thread(new Sleeper());
            for (int i = 0; i < n; i++) t[i].start();
            for (int i = 0; i < n; i++) t[i].join();
        } else if (mode.equals("virtual")) {
            ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();
            for (int i = 0; i < n; i++) exec.submit(new Sleeper());
            exec.shutdown();
            exec.awaitTermination(1, TimeUnit.MINUTES);
        } else {
            System.out.println("mode must be platform or virtual");
            return;
        }
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println(mode + "  n=" + n + "  time=" + ms + " ms");
    }
}