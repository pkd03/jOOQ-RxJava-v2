package com.example.pkdv3.wio.rest.controller;

import com.example.pkdv3.data.dto.request.CreateCategoryRequest;
import com.example.pkdv3.data.dto.response.ApiResponse;
import com.example.pkdv3.data.dto.response.CategoryResponse;
import com.example.pkdv3.service.CategoryService;
import com.example.pkdv3.wio.rest.CategoryOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller xử lý các yêu cầu HTTP liên quan đến Danh mục (Category).
 *
 * Vai trò kiến trúc:
 * 1. Triển khai interface CategoryOperations để thừa hưởng toàn bộ metadata mapping và Swagger.
 * 2. Đóng vai trò là "Cầu nối" (Bridge) giữa kiến trúc RxJava Reactive của Service và Spring MVC Blocking.
 * 3. Unwrap dữ liệu từ Single<T> an toàn bằng toán tử .blockingGet() trên luồng xử lý của Tomcat.
 *    Mọi Exception xảy ra trong quá trình thực thi bất đồng bộ sẽ được re-throw nguyên bản
 *    để GlobalExceptionHandler tiếp nhận và chuẩn hóa.
 */
@RestController
@RequiredArgsConstructor
public class CategoryController implements CategoryOperations {

    private final CategoryService categoryService;

    /**
     * Tạo mới danh mục.
     * Gọi categoryService.createCategory(...) trả về Single<CategoryResponse>,
     * sau đó unwrap bằng .blockingGet() để trả về HTTP 201 CREATED.
     */
    @Override
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(CreateCategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request).blockingGet();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Khởi tạo danh mục thành công"));
    }

    /**
     * Lấy chi tiết danh mục theo ID.
     */
    @Override
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(Long id) {
        CategoryResponse response = categoryService.getCategoryById(id).blockingGet();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin danh mục thành công"));
    }

    /**
     * Lấy danh sách toàn bộ danh mục đang hoạt động.
     */
    @Override
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> responseList = categoryService.getAllActiveCategories().blockingGet();
        return ResponseEntity.ok(ApiResponse.success(responseList, "Lấy danh sách danh mục thành công"));
    }
}
