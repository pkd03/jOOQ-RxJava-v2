package com.example.pkdv3.validate.handler;

import com.example.pkdv3.data.dto.response.ApiResponse;
import com.example.pkdv3.validate.exception.BusinessException;
import com.example.pkdv3.validate.exception.DuplicateResourceException;
import com.example.pkdv3.validate.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ tập trung (Global Exception Handler) cho toàn bộ ứng dụng.
 *
 * Cơ chế hoạt động:
 * Khi controller gọi .blockingGet() trên RxJava Single, nếu pipeline phát sinh ngoại lệ,
 * ngoại lệ đó sẽ được ném ra trên luồng Servlet của Tomcat. RestControllerAdvice sẽ bắt
 * và định tuyến đến phương thức xử lý thích hợp, bọc dữ liệu thành ApiResponse chuẩn
 * và gán HTTP Status chính xác.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bắt lỗi không tìm thấy tài nguyên (ResourceNotFoundException).
     * Trả về HTTP 404 NOT_FOUND.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("[RESOURCE_NOT_FOUND] Thông báo: {}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getMessage()));
    }

    /**
     * Bắt lỗi tài nguyên đã tồn tại (DuplicateResourceException).
     * Trả về HTTP 409 CONFLICT.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateResourceException(DuplicateResourceException ex) {
        log.warn("[DUPLICATE_RESOURCE] Thông báo: {}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.DUPLICATE_RESOURCE.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.DUPLICATE_RESOURCE.getCode(), ex.getMessage()));
    }

    /**
     * Bắt lỗi nghiệp vụ chung (BusinessException).
     * Trả về HTTP 400 BAD_REQUEST.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("[BUSINESS_EXCEPTION] Thông báo: {}", ex.getMessage());
        return ResponseEntity.status(ErrorCode.BAD_REQUEST.getHttpStatus())
                .body(ApiResponse.error(ErrorCode.BAD_REQUEST.getCode(), ex.getMessage()));
    }

    /**
     * Bắt lỗi kiểm tra tính hợp lệ của dữ liệu đầu vào (@Valid MethodArgumentNotValidException).
     * Trả về HTTP 400 BAD_REQUEST kèm map chi tiết các trường bị lỗi.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("[VALIDATION_FAILED] Chi tiết vi phạm: {}", fieldErrors);

        ApiResponse<Map<String, String>> response = ApiResponse.error(
                ErrorCode.VALIDATION_FAILED.getCode(),
                "Dữ liệu đầu vào không hợp lệ",
                fieldErrors
        );
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus()).body(response);
    }

    /**
     * Bắt các ngoại lệ không mong muốn còn lại (Catch-All Exception).
     * Trả về HTTP 500 INTERNAL_SERVER_ERROR.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("[INTERNAL_SERVER_ERROR] Lỗi hệ thống chưa được kiểm soát: ", ex);
        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        "Hệ thống gặp sự cố nội bộ. Vui lòng thử lại sau: " + ex.getMessage()
                ));
    }
}
