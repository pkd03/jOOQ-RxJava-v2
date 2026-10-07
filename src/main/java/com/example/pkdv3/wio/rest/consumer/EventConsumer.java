package com.example.pkdv3.wio.rest.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Consumer xử lý các sự kiện bất đồng bộ / Message queue (Kafka, RabbitMQ hoặc In-memory events).
 *
 * Mục đích:
 * Đặt tại package wio.rest.consumer theo quy chuẩn kiến trúc để sẵn sàng mở rộng cho các bài toán
 * Event-Driven Architecture, tiêu thụ message từ các hệ thống bên ngoài và kích hoạt
 * RxJava pipeline xử lý dữ liệu.
 */
@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    /**
     * Phương thức xử lý mẫu cho sự kiện nhận được từ hệ thống bên ngoài.
     *
     * @param eventType Loại sự kiện (ví dụ: PRODUCT_SYNC_EVENT)
     * @param payload Dữ liệu sự kiện
     */
    public void onEventReceived(String eventType, Object payload) {
        log.info("[EVENT_CONSUMER] Tiếp nhận sự kiện: type={}, payload={}", eventType, payload);
    }
}
