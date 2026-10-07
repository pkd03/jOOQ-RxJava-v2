package com.example.pkdv3.validate.exception;

/**
 * Ngoại lệ ném ra khi tài nguyên tạo mới hoặc cập nhật vi phạm ràng buộc trùng lặp (Unique Constraint).
 *
 * Mục đích:
 * Được ném ra khi kiểm tra trùng mã Category Code hoặc Product SKU trong kịch bản RxJava.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s với %s = '%s' đã tồn tại trong hệ thống", resourceName, fieldName, fieldValue));
    }
}
