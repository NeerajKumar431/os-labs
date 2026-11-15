import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Task3 {
    public static void main(String[] args) throws Exception {
        int T = Integer.parseInt(args[0]);

        double[] data = new double[200_000_000];
        java.util.Arrays.fill(data, 1.0);

        long ms = 0;
        double total = 0;
        for (int round = 0; round < 3; round++) {
            ExecutorService pool = Executors.newFixedThreadPool(T);

            long start = System.nanoTime();
            List<SliceSum> tasks = new ArrayList<>();
            int chunk = data.length / T;
            for (int i = 0; i < T; i++) {
                int lo = i * chunk;
                int hi = (i == T - 1) ? data.length : lo + chunk;
                tasks.add(new SliceSum(data, lo, hi));
            }
            List<Future<Double>> futures = pool.invokeAll(tasks);
            total = 0;
            for (Future<Double> f : futures) total += f.get();
            long end = System.nanoTime();

            pool.shutdown();
            if (!pool.awaitTermination(1, TimeUnit.MINUTES))
                System.out.println("pool did not terminate in time");

            ms = (end - start) / 1_000_000;
        }
        System.out.println("T=" + T + "  total=" + total + "  time=" + ms + " ms");
    }
}

class SliceSum implements Callable<Double> {
    private final double[] data;
    private final int lo, hi;

    SliceSum(double[] data, int lo, int hi) {
        this.data = data; this.lo = lo; this.hi = hi;
    }

    @Override public Double call() {
        double sum = 0;
        for (int i = lo; i < hi; i++) sum += data[i];
        return sum;
    }
}