package com.jd.wangyu.限流.信号量;

import java.util.concurrent.Semaphore;

public class 信号量 {

    Semaphore semaphore = new Semaphore(5);

    public String acquire() {
        try {
            semaphore.acquire(1);
            return "successed";
        } catch (InterruptedException e) {
            return "fail";
        }finally {
            semaphore.release(1);
        }
    }
}

