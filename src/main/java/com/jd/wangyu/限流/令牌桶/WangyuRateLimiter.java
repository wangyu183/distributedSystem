package com.jd.wangyu.限流.令牌桶;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public abstract class WangyuRateLimiter {

    private final SleepingStopwatch stopwatch;

    private volatile Object mutexDoNotUseDirectly;

    protected WangyuRateLimiter(SleepingStopwatch stopwatch) {
        this.stopwatch = stopwatch;
    }

    public static WangyuRateLimiter create(double permitsPersecond) {
        return create(permitsPersecond, SleepingStopwatch.createFromSystemTimer());
    }

    public static WangyuRateLimiter create(double permitsPersecond, SleepingStopwatch stopwatch) {
        WangyuRateLimiter rateLimiter = new WangyuSmoothBursty(stopwatch, 1.0);
        rateLimiter.setRate(permitsPersecond);
        return rateLimiter;
    }

    /**
     * 设置速率，如果已经在等待偿还上一个请求的费用了，那么下一个请求不受新的rate的影响，仍
     * 等待上一次计算的时间
     *
     *
     * @param permitsPerSecond
     */
    public final void setRate(double permitsPerSecond) {
        //并发情况下，获取锁去更新，避免发生竞态
        synchronized (mutex()) {
            //具体内容，交给子类去实现
            doSetRate(permitsPerSecond, stopwatch.readMicros());
        }
    }

    abstract void doSetRate(double permitsPerSecond, long nowMicros);

    /**
     * 双子锁生成新的锁对象，以免生成多个
     * @return
     */
    private Object mutex() {
        Object mutex = mutexDoNotUseDirectly;
        if (mutex == null) {
            synchronized (this) {
                mutex = mutexDoNotUseDirectly;
                if (mutex == null) {
                    mutexDoNotUseDirectly = mutex = new Object();
                }
            }
        }
        return mutex;
    }

    public boolean tryAcquire(int permits, long timeout, TimeUnit unit) {
        long timeoutMicros = Math.max(unit.toMicros(timeout), 0);
        long microsToWait;
        synchronized (mutex()) {
            long nowMicros = stopwatch.readMicros();
            if (!canAcquire(nowMicros, timeoutMicros)) {
                return false;
            }else {
                microsToWait = reserveAndGetWaitLength(permits, nowMicros);
            }
        }
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return true;
    }

    private boolean canAcquire(long nowMicros, long timeoutMicros) {
        return nowMicros + timeoutMicros >= queryEarliestAvailable(nowMicros);
    }

    abstract long queryEarliestAvailable(long nowMicros);

    public double acquire() {
        return acquire(1);
    }

    public double acquire(int permits) {
        long microsToWait = reserve(permits);
        stopwatch.sleepMicrosUninterruptibly(microsToWait);
        return 1.0 * microsToWait / SECONDS.toMicros(1L);
    }

    final long reserve(int permits) {
        synchronized (mutex()) {
            return reserveAndGetWaitLength(permits, stopwatch.readMicros());
        }
    }


    final long reserveAndGetWaitLength(int permits, long nowMicros) {
        long momentAvailable = reserveEarliestAvailable(permits, nowMicros);
        return Math.max(momentAvailable - nowMicros, 0);
    }

    /**
     * 真正的执行方法， 获取锁后执行
     * @param permits
     * @param nowMicros
     * @return
     */
    abstract long reserveEarliestAvailable(int permits, long nowMicros);

    static abstract class SleepingStopwatch {

        protected abstract long readMicros();

        protected abstract void sleepMicrosUninterruptibly(long micros);

        public static WangyuRateLimiter.SleepingStopwatch createFromSystemTimer() {
            return new WangyuRateLimiter.SleepingStopwatch() {
                Stopwatch stopWatch = Stopwatch.createStarted();

                @Override
                protected long readMicros() {
                    return stopWatch.elapsed(MICROSECONDS);
                }

                @Override
                protected void sleepMicrosUninterruptibly(long micros) {
                    if (micros > 0) {
                        Uninterruptibles.sleepUninterruptibly(micros, MICROSECONDS);
                    }
                }
            };
        }
    }
}
