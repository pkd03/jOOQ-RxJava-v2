package com.example.pkdv3.validate.handler;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Định nghĩa bộ mã lỗi chuẩn hóa toàn diện cho toàn hệ thống.
 *
 * Mục đích:
 * Cung cấp mã định danh duy nhất (code), thông điệp mặc định tiếng Việt (defaultMessage),
 * và mã HTTP tương ứng (httpStatus) giúp client dễ dàng phân tích và hiển thị thông báo.
 */
@Getter
public enum ErrorCode {

    SUCCESS("SUCCESS", "Thao tác thành công", HttpStatus.OK),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "Không tìm thấy dữ liệu yêu cầu", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("DUPLICATE_RESOURCE", "Dữ liệu đã tồn tại trong hệ thống", HttpStatus.CONFLICT),
    BAD_REQUEST("BAD_REQUEST", "Yêu cầu không hợp lệ hoặc sai định dạng", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED("VALIDATION_FAILED", "Dữ liệu đầu vào vi phạm quy tắc validation", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "Lỗi xử lý nội bộ hệ thống", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }
}
