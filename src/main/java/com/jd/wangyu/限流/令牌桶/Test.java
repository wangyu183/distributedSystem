package com.jd.wangyu.限流.令牌桶;

import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.TimeUnit;

public class Test {

    public static void main(String[] args) {
//        RateLimiter rateLimiter = RateLimiter.create(0.1);
//        System.out.println(System.currentTimeMillis());
//        rateLimiter.acquire();
//        System.out.println(System.currentTimeMillis());
//        rateLimiter.acquire();
//        System.out.println(System.currentTimeMillis());
//        rateLimiter.acquire();
//        System.out.println(System.currentTimeMillis());

//        WangyuRateLimiter wangyuRateLimiter = WangyuRateLimiter.create(0.1);
//        System.out.println(System.currentTimeMillis());
//        wangyuRateLimiter.acquire();
//        System.out.println(System.currentTimeMillis());
//        wangyuRateLimiter.acquire();
//        System.out.println(System.currentTimeMillis());
//        wangyuRateLimiter.acquire();
//        System.out.println(System.currentTimeMillis());


        WangyuRateLimiter wangyuRateLimiter = WangyuRateLimiter.create(0.1);
        System.out.println(System.currentTimeMillis());
        System.out.println(wangyuRateLimiter.tryAcquire(1, 0, TimeUnit.MICROSECONDS));
        System.out.println(System.currentTimeMillis());
        System.out.println(wangyuRateLimiter.tryAcquire(1, 0, TimeUnit.MICROSECONDS));
        System.out.println(System.currentTimeMillis());
        System.out.println(wangyuRateLimiter.tryAcquire(1, 0, TimeUnit.MICROSECONDS));
        System.out.println(System.currentTimeMillis());
    }
}
