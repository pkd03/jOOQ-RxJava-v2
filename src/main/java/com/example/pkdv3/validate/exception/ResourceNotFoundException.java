package com.example.pkdv3.validate.exception;

/**
 * Ngoại lệ ném ra khi không tìm thấy tài nguyên yêu cầu trong cơ sở dữ liệu.
 *
 * Mục đích:
 * Được ném ra từ tầng Service hoặc Repository khi tìm kiếm theo ID, Code, SKU
 * mà không có bản ghi nào tồn tại hoặc bản ghi đã bị soft-delete (is_deleted = true).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Không tìm thấy %s với %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
