package com.example.pkdv3.data.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Đối tượng bao bọc dữ liệu phản hồi (Response Envelope) chuẩn cho toàn bộ REST API.
 *
 * Mục đích:
 * Chuẩn hóa cấu trúc JSON đầu ra cho mọi endpoint gồm trạng thái thành công/thất bại,
 * mã phản hồi, thông điệp chi tiết, dữ liệu trả về (payload) và thời điểm phát sinh.
 *
 * @param <T> Kiểu dữ liệu payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** Đánh dấu trạng thái nghiệp vụ thành công hay thất bại */
    private boolean success;

    /** Mã định danh kết quả (SUCCESS, RESOURCE_NOT_FOUND, VALIDATION_FAILED,...) */
    private String code;

    /** Thông điệp mô tả chi tiết bằng tiếng Việt */
    private String message;

    /** Payload dữ liệu thực tế trả về cho client */
    private T data;

    /** Thời điểm server sinh phản hồi */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * Tạo đối tượng ApiResponse thành công kèm dữ liệu và thông điệp tùy chỉnh.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Tạo đối tượng ApiResponse thành công kèm dữ liệu và thông điệp mặc định.
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Thao tác thành công");
    }

    /**
     * Tạo đối tượng ApiResponse báo lỗi với mã lỗi và thông điệp chi tiết.
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Tạo đối tượng ApiResponse báo lỗi kèm payload bổ sung (như danh sách lỗi validation chi tiết).
     */
    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
