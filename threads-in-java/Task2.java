public class Task2 {
    static final int N = 4;
    static long shared = 0;

    static class Worker implements Runnable {
        private final int id;
        Worker(int id) { this.id = id; }

        @Override public void run() {
            long local = 0;
            for (int i = 0; i < 1_000_000; i++) {
                local++;
                shared++;
            }
            System.out.println("task " + id + "  local=" + local);
        }
    }

    public static void main(String[] args) throws Exception {
        Thread[] t = new Thread[N];
        for (int i = 0; i < N; i++)
            t[i] = new Thread(new Worker(i), "worker-" + i);
        for (int i = 0; i < N; i++) t[i].start();
        for (int i = 0; i < N; i++) t[i].join();
        System.out.println("shared=" + shared);
    }
}