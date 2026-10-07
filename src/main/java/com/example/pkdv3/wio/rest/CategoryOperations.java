package com.example.pkdv3.wio.rest;

import com.example.pkdv3.data.dto.request.CreateCategoryRequest;
import com.example.pkdv3.data.dto.response.ApiResponse;
import com.example.pkdv3.data.dto.response.CategoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Hợp đồng giao tiếp (API Contract) cho các thao tác liên quan đến Danh mục (Category).
 *
 * Mục đích:
 * Tách biệt hoàn toàn phần khai báo định tuyến (Routing), tài liệu hóa Swagger/OpenAPI
 * và quy tắc xác thực (@Valid) ra khỏi lớp xử lý logic cụ thể (Controller).
 */
@Tag(name = "Category Management", description = "Các API quản lý danh mục sản phẩm")
@RequestMapping("/api/v1/categories")
public interface CategoryOperations {

    /**
     * Khởi tạo một danh mục mới trong hệ thống.
     *
     * @param request Dữ liệu đầu vào kèm ràng buộc kiểm tra
     * @return ResponseEntity chứa ApiResponse bọc CategoryResponse
     */
    @Operation(summary = "Tạo mới danh mục", description = "Kiểm tra mã code duy nhất và tạo danh mục mới")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo danh mục thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu đầu vào không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Mã danh mục đã tồn tại")
    })
    @PostMapping
    ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request);

    /**
     * Lấy thông tin chi tiết một danh mục theo ID.
     *
     * @param id ID định danh danh mục
     * @return ResponseEntity chứa ApiResponse bọc CategoryResponse
     */
    @Operation(summary = "Lấy danh mục theo ID", description = "Truy vấn danh mục đang hoạt động dựa vào ID")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy danh mục"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục")
    })
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @Parameter(description = "ID danh mục", required = true) @PathVariable("id") Long id
    );

    /**
     * Lấy danh sách toàn bộ danh mục đang hoạt động.
     *
     * @return ResponseEntity chứa ApiResponse bọc danh sách CategoryResponse
     */
    @Operation(summary = "Lấy danh sách tất cả danh mục", description = "Truy vấn toàn bộ danh mục chưa bị xóa mềm")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lấy danh sách thành công")
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories();
}
