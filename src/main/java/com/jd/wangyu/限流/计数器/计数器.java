package com.jd.wangyu.限流.计数器;

import java.util.concurrent.atomic.LongAdder;

/**
 * 借鉴于dubbo的TpsLimiter实现
 * 优势：简单直接
 * 劣势：边界情况下，会有瞬时流量突发
 * 举例：0-60s 在59s有流量60进来，在下一个0-60的时候，在1s的时候有流量60进来，代表着在2秒内有120进来，引起流量突增
 */
public class 计数器 {


    public static void main(String[] args) {
        //代表着60s内允许通过的次数为60
        StatItem statItem = new StatItem("testinterface", 60, 60000L);

    }


    static class StatItem{
        //限流的每个接口名字
        private String name;
        //上一次设置的时间，用于计算当前时间是否超过 lastResetTime + interval，如果超过，则重新设置token
        private long lastResetTime;
        //设置的时间间隔
        private long interval;
        //当前周期内存在的token数量
        private LongAdder token;
        //限制的次数
        private int rate;


        public StatItem(String name, int rate, long interval) {
            this.name = name;
            this.rate = rate;
            this.interval = interval;
            this.lastResetTime = System.currentTimeMillis();
            this.token = this.buildLongAdder(rate);
        }

        private LongAdder buildLongAdder(int rate) {
            LongAdder adder = new LongAdder();
            adder.add((long) rate);
            return adder;
        }

        public boolean isAllowable() {
            long now = System.currentTimeMillis();
            if (now > this.lastResetTime + this.interval) {
                this.token = this.buildLongAdder(this.rate);
                this.lastResetTime = now;
            }
            if (this.token.sum() < 0L) {
                return false;
            }
            this.token.decrement();
            return true;
        }
    }
}
