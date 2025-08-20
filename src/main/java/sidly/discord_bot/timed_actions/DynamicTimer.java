package sidly.discord_bot.timed_actions;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DynamicTimer {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> currentTask;
    private final List<?> targetSet;
    private final Runnable task; // the function to run
    private final long targetMillisPerItemInSet;
    private final long minMillis;

    public DynamicTimer(List<?> targetSet, Runnable task, long targetMillisPerItemInSet, long minMillis) {
        this.targetSet = targetSet;
        this.task = task;
        this.targetMillisPerItemInSet = targetMillisPerItemInSet;
        this.minMillis = minMillis;
    }

    public void start() {
        scheduleNext();
    }

    int lastSize;
    private long lastDelay;
    private int runs = 0;
    private void scheduleNext() {
        long delay;
        if (runs++ > lastSize / 4) {
            runs = 0;
            lastSize = targetSet.size();
            delay = calculateDelay(lastSize);
        } else {
            delay = lastDelay; // reuse previous delay
        }
        lastDelay = delay;

        currentTask = scheduler.schedule(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
            scheduleNext();
        }, delay, TimeUnit.MILLISECONDS);
    }


    private long calculateDelay(int size) {
        if (size <= 0) {
            return this.minMillis * 4;
        }
        long delay = targetMillisPerItemInSet / size;
        return Math.max(minMillis, delay);
    }

    public void cancel() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            currentTask = null;
        }
    }
}

