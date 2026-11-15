public class Task1 {
    static final int N = 4;

    static class Worker implements Runnable {
        private final int id;
        Worker(int id) { this.id = id; }

        @Override public void run() {
            Thread me = Thread.currentThread();
            System.out.printf("task %d  name=%s  id=%d  virtual=%b%n",
                    id, me.getName(), me.threadId(), me.isVirtual());
        }
    }

    public static void main(String[] args) throws Exception {
        Thread[] t = new Thread[N];
        for (int i = 0; i < N; i++)
            t[i] = new Thread(new Worker(i), "worker-" + i);
        for (int i = 0; i < N; i++) t[i].start();
        for (int i = 0; i < N; i++) t[i].join();
        System.out.println("all joined");
    }
}