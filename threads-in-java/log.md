# Threads in Java

Name: Neeraj Kumar  
Seat no: B23110106054

Measured on: Linux sandbox, 2 cores, 8 GB RAM, OpenJDK 21. Numbers will differ on other computers (see the note at the bottom).

---

## Task 1: Start and join

Output (one run):

```
task 0  name=worker-0  id=19  virtual=false
task 1  name=worker-1  id=20  virtual=false
task 2  name=worker-2  id=21  virtual=false
task 3  name=worker-3  id=22  virtual=false
all joined
```

1. **Same order every time?** No. In 3 runs, the third printed task 3 before task 2. After `start()`, the OS scheduler decides which thread prints first, and the program does not control that. Only the order of the `start()` calls is fixed.
2. **Can `all joined` print before a worker line?** No. `t[i].join()` blocks `main` until that thread has finished. All four joins must return before `main` reaches the last `println`, so every worker line is already printed.
3. **Different `worker-N` in each line?** Yes: worker-0, worker-1, worker-2, worker-3, and each has its own thread id.

### Break it, first way (`run()` instead of `start()`)

```
task 0  name=main  id=1  virtual=false
task 1  name=main  id=1  virtual=false
task 2  name=main  id=1  virtual=false
task 3  name=main  id=1  virtual=false
all joined
```

1. The `name=` column shows `main` for every line, not worker-0..3.
2. **Zero** new threads ran. `run()` is an ordinary method call, so the work executes on the calling thread (`main`). Only `start()` asks the kernel for a new OS thread. The order is now always 0,1,2,3 because it is one thread running in sequence.

### Break it, second way (shared `Counter`)

```
task 4  name=worker-0  id=19  virtual=false
task 4  name=worker-1  id=20  virtual=false
task 4  name=worker-3  id=22  virtual=false
task 4  name=worker-2  id=21  virtual=false
all joined
```

1. No, it prints `4` on every line, not 0, 1, 2, 3.
2. All four threads hold a reference to the **same** `Counter`. They read `counter.value` only when they run. The skeleton creates all four threads first and starts them afterwards, so `main` has already incremented the counter to 4 by then. Each thread reads whatever the value is *at that moment*, not what it was when the thread was created.
3. The `int` version stores a **copy** of the number inside each `Worker` at construction time. Nobody else can change that copy, so each thread sees its own value.

---

## Task 2: Shared and private

Five runs:

| Run | each thread's `local` | `shared` |
|---|---|---|
| 1 | 1000000 (all 4) | 3535709 |
| 2 | 1000000 (all 4) | 2418428 |
| 3 | 1000000 (all 4) | 3944703 |
| 4 | 1000000 (all 4) | 3771836 |
| 5 | 1000000 (all 4) | 3971811 |

1. `local` is always 1000000. `shared` is **never** 4000000 and changes every run.
2. `local` is the correct one every time. `shared` is wrong.
3. `local` lives on each thread's own stack, so each thread has a private copy that nobody else touches. `shared` is a static field on the heap, and all four threads update the same memory location. `shared++` is really three steps (read, add 1, write back). Two threads can read the same old value, and both write back old+1, so one update is lost. Lost updates make the total smaller than 4000000, by a different amount each run.

---

## Task 3: Partition and time

Each T was measured 3 times inside one run, keeping the last (JIT warm-up). Total was 2.0E8 every time, which is correct.

| T | Time (ms) | Speedup = time(1) / time(T) |
|---|---|---|
| 1 | 242 | 1.00 |
| 2 | 121 | 2.00 |
| 4 | 139 | 1.74 |
| 8 | 122 | 1.98 |
| 16 (cores x 4 = 8 here, so 16 is not it) | 125 | 1.94 |

Note: this machine has 2 cores, so "cores x 4" is **8**. The T=8 row is that row. T=16 is extra.

1. Speedup first falls short at **T = 4** (1.74 instead of 4.00). Even T=2 only just reaches 2.00, because this machine has only 2 cores. More threads than cores cannot run at the same time.
2. Amdahl: speedup = 1 / ((1 - f) + f/T). With T=4 and speedup 1.74:
   1/1.74 = 0.575 = 1 - f + f/4 = 1 - 0.75f, so 0.75f = 0.425 and **f ≈ 0.57**.
   (This f is not trustworthy: the real limit here is 2 cores, not a serial fraction, and the timings are noisy.)
3. The cores x 4 row (T=8) is about **the same** as the best row, not faster. Only 2 threads can run at once, so extra threads just wait for a core and add scheduling overhead. This loop is CPU-bound: it never waits, so more threads than cores do not help.

---

## Task 4: Where platform threads run out

| Mode | n | Wall time | Outcome |
|---|---|---|---|
| platform | 10,000 | 3411 ms | finished |
| virtual | 10,000 | 1161 ms | finished |
| virtual | 1,000,000 | 12294 ms | finished |

Extra runs for platform threads: 20,000 took 6214 ms, 40,000 took 13453 ms, 100,000 took 28173 ms. **No `OutOfMemoryError` up to 100,000 on this machine.**

1. **Closest to one second:** virtual with 10,000 tasks (1161 ms).
2. The platform run **finished** here. It did not crash even at 100,000 threads, because this sandbox has a lot of headroom and the stack is reserved as address space, not all touched. Time grew linearly (about 0.28 ms per thread, so thread creation itself is the cost). Each platform thread reserves about 1 MB of stack, so 10,000 threads reserve about 10 GB of address space and 1,000,000 would need about 1 TB. On a laptop with less memory or a lower thread limit it will fail much earlier, so **find your own threshold** by trying smaller n.
3. When a virtual thread calls `sleep`, the JVM **unmounts** it. Its small continuation (its stack) is saved on the heap and the carrier OS thread is released to run something else. After 1 second the virtual thread is remounted on any free carrier. A sleeping virtual thread therefore uses no OS thread, only a few hundred bytes to KB of heap, so a million of them fit on a handful of carrier threads.
   (The 1M run took 12 s, not 1 s: creating and scheduling a million tasks has its own cost on only 2 cores. It is still far better than platform threads.)

---

## Task 5: Pool starvation

| Executor | Predicted time | Wall time |
|---|---|---|
| Fixed pool, 8 threads | 125 s | 125032 ms |
| Fixed pool, 200 threads | 5 s | 5027 ms |
| Virtual thread per task | about 1 s | 1031 ms |

1. Predictions were close. Pool of 8: 1,000 tasks / 8 at a time = 125 rounds x 1 second = **125 s**. Measured 125.03 s.
2. A pool of 200 runs 200 sleepers at once, so the queue drains in 1000 / 200 = 5 rounds x 1 s = 5 s. The pool of 8 has 992 tasks waiting in the queue and only 8 sleeping at any time. Every waiting task is stuck behind sleepers that are doing no work: the pool is starved while nothing computes.
3. To match the virtual version, the pool would need **1,000 threads**, one per task, and in general as many as the number of blocking tasks. Growing the pool is a bad fix because each platform thread costs about 1 MB of stack plus OS resources, and the number of tasks is not known in advance. Virtual threads solve it by giving every task its own cheap thread, and unmounting it while it waits, so waiting costs almost nothing.

---

## Note on the measurements

All timings in this log were measured on one Linux machine (2 cores, 8 GB RAM, OpenJDK 21). Timings depend on the number of cores and the machine's load, so absolute numbers will differ on another computer, but the patterns and explanations above (non-deterministic order, lost updates without synchronization, speedup limited by cores and serial work, pool starvation versus virtual threads) do not depend on the machine.
