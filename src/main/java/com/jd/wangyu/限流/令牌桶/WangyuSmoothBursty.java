package com.jd.wangyu.限流.令牌桶;


/**
 * 固定速率
 */
public class WangyuSmoothBursty extends WangyuSmoothRateLimiter {

    /**
     * 能够保存多少秒的permits
     */
    private final double maxBurstSeconds;

    public WangyuSmoothBursty(WangyuRateLimiter.SleepingStopwatch stopwatch, double maxBurstSeconds) {
        super(stopwatch);
        this.maxBurstSeconds = maxBurstSeconds;
    }

    @Override
    void doSetRate(double permitsPerSecond, double stableIntervalMicros) {
        //初始化的时候，设置storedPermits为0，如果更新速率，则按照比率更新storedPermits的大小
        double oldMaxPermits = this.maxPermits;
        this.maxPermits = maxBurstSeconds * permitsPerSecond;
        if (oldMaxPermits == Double.POSITIVE_INFINITY) {
            storedPermits = maxPermits;
        } else {
            storedPermits =
                    (oldMaxPermits == 0.0) ? 0.0 : storedPermits * maxPermits / oldMaxPermits;
        }
    }

    @Override
    double coolDownIntervalMicros() {
        return stableIntervalMicros;
    }
}
