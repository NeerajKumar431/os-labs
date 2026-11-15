import java.util.concurrent.*;

public class Task5 {
    static final int TASKS = 1000;

    static class Sleeper implements Runnable {
        @Override public void run() {
            try { Thread.sleep(1000); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    static long time(String label, ExecutorService exec) throws Exception {
        long start = System.nanoTime();
        for (int i = 0; i < TASKS; i++) exec.submit(new Sleeper());
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.MINUTES);
        long ms = (System.nanoTime() - start) / 1_000_000;
        System.out.println(label + "  time=" + ms + " ms");
        return ms;
    }

    public static void main(String[] args) throws Exception {
        time("fixed pool, 8 threads  ", Executors.newFixedThreadPool(8));
        time("fixed pool, 200 threads", Executors.newFixedThreadPool(200));
        time("virtual thread per task", Executors.newVirtualThreadPerTaskExecutor());
    }
}