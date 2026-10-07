package com.example.pkdv3.performance;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.tuple.Pair;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static com.example.pkdv3.data.jooq.generated.Tables.CATEGORIES;
import static com.example.pkdv3.data.jooq.generated.Tables.PRODUCTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Bài kiểm thử tích hợp (Integration Test) đối chiếu hiệu năng:
 * Bất đồng bộ song song (Parallel Reactive Single.zip) vs Tuần tự truyền thống (Sequential Blocking)
 * trên môi trường H2 Database mô phỏng PostgreSQL.
 *
 * Mục tiêu:
 * Chứng minh về mặt thực nghiệm:
 * - Trường hợp tuần tự: Thời gian thực thi T_seq ≈ T1 + T2.
 * - Trường hợp song song: Thời gian thực thi T_par ≈ max(T1, T2).
 */
@SpringBootTest
public class ReactiveVsSequentialPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(ReactiveVsSequentialPerformanceTest.class);

    @Autowired
    private DSLContext dslContext;

    // Giả lập độ trễ I/O mạng hoặc xử lý DB phức tạp cho 2 truy vấn độc lập
    private static final long QUERY_A_LATENCY_MS = 250L;
    private static final long QUERY_B_LATENCY_MS = 250L;

    @BeforeEach
    void setUp() {
        // Dọn dẹp và chuẩn bị dữ liệu mẫu trong DB H2
        dslContext.deleteFrom(PRODUCTS).execute();
        dslContext.deleteFrom(CATEGORIES).execute();

        dslContext.insertInto(CATEGORIES)
                .set(CATEGORIES.CODE, "PERF_CAT")
                .set(CATEGORIES.NAME, "Performance Category")
                .set(CATEGORIES.IS_DELETED, false)
                .execute();
    }

    /**
     * Giả lập truy vấn đếm Category kèm độ trễ I/O (ví dụ network round-trip).
     */
    private Long executeQueryCategoryCountWithLatency() {
        try {
            Thread.sleep(QUERY_A_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return dslContext.selectCount().from(CATEGORIES).fetchOne(0, Long.class);
    }

    /**
     * Giả lập truy vấn đếm Product kèm độ trễ I/O (ví dụ network round-trip).
     */
    private Long executeQueryProductCountWithLatency() {
        try {
            Thread.sleep(QUERY_B_LATENCY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return dslContext.selectCount().from(PRODUCTS).fetchOne(0, Long.class);
    }

    /**
     * Phương thức khởi động (Warm-up) JVM để loại bỏ độ trễ ClassLoader và JIT Compiler ban đầu.
     */
    private void warmupJvm() {
        Single.zip(
                Single.fromCallable(() -> 1L).subscribeOn(Schedulers.io()),
                Single.fromCallable(() -> 2L).subscribeOn(Schedulers.io()),
                Pair::of
        ).blockingGet();
    }

    @Test
    @DisplayName("Kiểm thử đối chiếu: Single.zip song song vs Blocking tuần tự")
    void testPerformanceComparison_ParallelVsSequential() {
        // 1. Warm-up JIT
        warmupJvm();

        log.info("================================================================================");
        log.info(" BẮT ĐẦU BÀI TEST ĐỐI CHIẾU HIỆU NĂNG: PARALLEL REACTIVE VS SEQUENTIAL BLOCKING");
        log.info(" Cấu hình giả lập: Truy vấn A trễ {}ms | Truy vấn B trễ {}ms", QUERY_A_LATENCY_MS, QUERY_B_LATENCY_MS);
        log.info("================================================================================");

        // 2. KỊCH BẢN 1: Chạy tuần tự truyền thống (Sequential Blocking) trên cùng 1 thread
        long startSequential = System.currentTimeMillis();

        Long countASequential = executeQueryCategoryCountWithLatency();
        Long countBSequential = executeQueryProductCountWithLatency();

        long endSequential = System.currentTimeMillis();
        long sequentialDuration = endSequential - startSequential;

        log.info("[SEQUENTIAL BLOCKING] Luồng thực thi: {}", Thread.currentThread().getName());
        log.info("[SEQUENTIAL BLOCKING] Kết quả Count A: {}, Count B: {}", countASequential, countBSequential);
        log.info("[SEQUENTIAL BLOCKING] Tổng thời gian thực thi (T1 + T2): {} ms", sequentialDuration);

        // 3. KỊCH BẢN 2: Chạy bất đồng bộ song song (Parallel Reactive) với Single.zip trên Schedulers.io()
        long startParallel = System.currentTimeMillis();

        Single<Long> singleA = Single.fromCallable(this::executeQueryCategoryCountWithLatency)
                .subscribeOn(Schedulers.io());

        Single<Long> singleB = Single.fromCallable(this::executeQueryProductCountWithLatency)
                .subscribeOn(Schedulers.io());

        Pair<Long, Long> parallelResult = Single.zip(
                singleA,
                singleB,
                Pair::of
        ).blockingGet();

        long endParallel = System.currentTimeMillis();
        long parallelDuration = endParallel - startParallel;

        log.info("[PARALLEL REACTIVE] Kết quả Count A: {}, Count B: {}", parallelResult.getLeft(), parallelResult.getRight());
        log.info("[PARALLEL REACTIVE] Tổng thời gian thực thi max(T1, T2): {} ms", parallelDuration);

        // 4. TÍNH TOÁN HIỆU SUẤT CẢI THIỆN
        double speedupPercentage = ((double) (sequentialDuration - parallelDuration) / sequentialDuration) * 100.0;
        log.info("--------------------------------------------------------------------------------");
        log.info(" KẾT QUẢ ĐỐI CHỨNG:");
        log.info(" - Tuần tự (Sequential):   {} ms (Lý thuyết: ~{} ms)", sequentialDuration, QUERY_A_LATENCY_MS + QUERY_B_LATENCY_MS);
        log.info(" - Song song (Single.zip): {} ms (Lý thuyết: ~{} ms)", parallelDuration, Math.max(QUERY_A_LATENCY_MS, QUERY_B_LATENCY_MS));
        log.info(" - Tốc độ cải thiện:       {:.2f}%", speedupPercentage);
        log.info("================================================================================");

        // 5. KIỂM CHỨNG TÍNH ĐÚNG ĐẮN CỦA DỮ LIỆU
        assertEquals(countASequential, parallelResult.getLeft(), "Dữ liệu truy vấn A phải giống nhau");
        assertEquals(countBSequential, parallelResult.getRight(), "Dữ liệu truy vấn B phải giống nhau");

        // 6. KIỂM CHỨNG HIỆU NĂNG
        // - Thời gian chạy song song phải nhanh hơn đáng kể so với chạy tuần tự
        assertTrue(parallelDuration < sequentialDuration,
                String.format("Single.zip (%d ms) phải nhanh hơn tuần tự (%d ms)", parallelDuration, sequentialDuration));

        // - Thời gian tuần tự phải >= T1 + T2 (cho phép sai số nhỏ -30ms)
        assertTrue(sequentialDuration >= (QUERY_A_LATENCY_MS + QUERY_B_LATENCY_MS - 30),
                "Thời gian tuần tự phải xấp xỉ hoặc lớn hơn T1 + T2");

        // - Thời gian song song phải nhỏ hơn ngưỡng tuần tự tối thiểu (chứng minh chạy đồng thời)
        assertTrue(parallelDuration < (QUERY_A_LATENCY_MS + QUERY_B_LATENCY_MS - 80),
                "Thời gian song song phải nhỏ hơn đáng kể so với tổng thời gian hai truy vấn");
    }
}
