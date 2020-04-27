package com.jd.wangyu.限流.令牌桶;


import com.google.common.math.LongMath;

import java.util.concurrent.TimeUnit;

public abstract class WangyuSmoothRateLimiter extends WangyuRateLimiter {

    /**
     * 下一次请求被批准的时间，初始化为当前时间，在批准一个请求之后，它将会被更新的越来越远，请求越大推动的越明显
     */
    private long nextFreeTicketMicros = 0L;

    /**
     * 两次请求之间的间隔，例如 5次/s  为200ms
     */
    double stableIntervalMicros;

    /**
     * 当前存储的permits数量
     */
    double storedPermits;

    /**
     * 最大存储的permits数量
     */
    double maxPermits;

    protected WangyuSmoothRateLimiter(SleepingStopwatch stopwatch) {
        super(stopwatch);
    }

    @Override
    void doSetRate(double permitsPerSecond, long nowMicros) {
        resync(nowMicros);
        double stableIntervalMicros = TimeUnit.SECONDS.toMicros(1L) / permitsPerSecond;
        this.stableIntervalMicros = stableIntervalMicros;
        doSetRate(permitsPerSecond, stableIntervalMicros);
    }

    abstract void doSetRate(double permitsPerSecond, double stableIntervalMicros);

    /**
     * 返回的是时间间隔
     * @return
     */
    abstract double coolDownIntervalMicros();

    /**
     * 如果当前时间大于 下一次请求被批准的时间，则更新下一次请求被批准的时间，并且计算可以存储的permit数量
     * @param nowMicros
     */
    void resync(long nowMicros) {
        if (nowMicros > nextFreeTicketMicros) {
            //在nowMicros - nextFreeTicketMicros的时间间隔内，可以生成的permit数量
            double newPermits = (nowMicros - nextFreeTicketMicros) /coolDownIntervalMicros();
            storedPermits = Math.min(maxPermits, storedPermits + newPermits);
            //TODO 更新下一次请求被批准的时间，为什么要更新呢, 每个请求只需要计算从
            // nowMicros - nextFreeTicketMicros的间隔生成的permit，延迟计算，节省资源
            nextFreeTicketMicros = nowMicros;
        }
    }

    @Override
    long reserveEarliestAvailable(int requiredPermits, long nowMicros) {
        //每获取一次，就更新一次storedPermits 和 nextFreeTicketMicros
        resync(nowMicros);
        //返回值为当前的，而不是计算以后的，本次超支了 下一个请求为上次请求背锅。下一个请求去等待
        long returnValue = nextFreeTicketMicros;
        //最大也不能超过storedPermits
        double storedPermitsToSpend = Math.min(requiredPermits, this.storedPermits);
        //如果本次超支了，则计算超支的permit
        double freshPermits = requiredPermits - storedPermitsToSpend;
        //计算等待的micros豪微秒
        long waitMicros = (long) (freshPermits * stableIntervalMicros);
        //更新nextFreeTicketMicros，反正第一次超支的已经执行了，下一个就得等待我超支的时间才能执行
        this.nextFreeTicketMicros = LongMath.saturatedAdd(nextFreeTicketMicros, waitMicros);
        //如果没超支，则还有剩余，如果超支就为0
        this.storedPermits -= storedPermitsToSpend;
        return returnValue;
    }

    @Override
    long queryEarliestAvailable(long nowMicros) {
        return nextFreeTicketMicros;
    }
}
