package com.example.pkdv3.validate.exception;

/**
 * Ngoại lệ ném ra khi vi phạm các quy tắc logic nghiệp vụ chung trong hệ thống.
 *
 * Mục đích:
 * Xử lý các tình huống như trạng thái sản phẩm không hợp lệ, thao tác không được phép
 * hoặc lỗi bất thường trong quy trình thực thi nghiệp vụ.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
