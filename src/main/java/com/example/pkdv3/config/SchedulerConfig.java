package com.example.pkdv3.config;

import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cấu hình Bounded Scheduler chuyên dụng cho các tác vụ JDBC/Database qua RxJava 3.
 *
 * Mục đích:
 * Mặc định Schedulers.io() sử dụng CachedThreadPool không giới hạn số luồng (unbounded).
 * Khi hệ thống chịu tải cao, việc tạo ra hàng loạt thread cùng tranh chấp tài nguyên DB
 * sẽ nhanh chóng làm cạn kiệt Connection Pool của HikariCP hoặc gây Out-Of-Memory (OOM).
 *
 * Cơ chế hoạt động:
 * Khởi tạo một ThreadPoolExecutor có kích thước cố định (Bounded) tương thích chặt chẽ
 * với số lượng connection tối đa của HikariCP (db-pool-size). Khi hàng đợi đầy,
 * chính sách CallerRunsPolicy sẽ điều tiết tốc độ (backpressure), buộc luồng gọi phải
 * tham gia xử lý thay vì làm tràn bộ nhớ.
 */
@Configuration
public class SchedulerConfig {

    @Value("${app.scheduler.db-pool-size:10}")
    private int dbPoolSize;

    @Value("${app.scheduler.db-queue-capacity:100}")
    private int dbQueueCapacity;

    /**
     * Bean Scheduler dùng riêng cho Repository layer để thực thi các câu truy vấn jOOQ blocking.
     */
    @Bean(name = "dbScheduler")
    public Scheduler dbScheduler() {
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread worker = new Thread(runnable, "rx-db-worker-" + threadNumber.getAndIncrement());
                worker.setDaemon(true);
                return worker;
            }
        };

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                dbPoolSize,
                dbPoolSize,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(dbQueueCapacity),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        return Schedulers.from(threadPoolExecutor);
    }
}
