package com.han.bi.config;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolExecutorConfig {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        NamingThreadFactory threadFactory = new NamingThreadFactory(r -> new Thread(r), "智能分析线程");

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                2, 4, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(200), threadFactory
        );
        return threadPoolExecutor;
    }

    /**
     * 线程工厂，它设置线程名称，有利于我们定位问题。
     */
    public final static class NamingThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNum = new AtomicInteger();
        private final ThreadFactory delegate;
        private final String name;

        /**
         * 创建一个带名字的线程池生产工厂
         */
        public NamingThreadFactory(ThreadFactory delegate, String name) {
            this.delegate = delegate;
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = delegate.newThread(r);
            t.setName(name + " [#" + threadNum.incrementAndGet() + "]");
            return t;
        }

    }

}
