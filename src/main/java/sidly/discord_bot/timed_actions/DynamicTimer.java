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
        reschedule();
    }

    public void reschedule() {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false);
        }

        long delay = calculateDelay(targetSet.size());
        currentTask = scheduler.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, delay, TimeUnit.MILLISECONDS);
    }

    private long calculateDelay(int size) {
        if (size <= 0) {
            return this.targetMillisPerItemInSet; // no elements, default to slow pace
        }
        long delay = targetMillisPerItemInSet / size;
        return Math.max(3000, delay); // never faster than 3 second between runs
    }
}

