package com.example.pkdv3.wio.rest;

import com.example.pkdv3.data.dto.request.CreateProductRequest;
import com.example.pkdv3.data.dto.request.UpdateProductRequest;
import com.example.pkdv3.data.dto.response.ApiResponse;
import com.example.pkdv3.data.dto.response.PageResponse;
import com.example.pkdv3.data.dto.response.ProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Hợp đồng giao tiếp (API Contract) cho các thao tác Sản phẩm (Product).
 *
 * Mục đích:
 * Định nghĩa chuẩn Swagger, các HTTP endpoints và tham số truy vấn phục vụ
 * quá trình giao tiếp chuẩn hóa giữa Client và tầng Controller.
 */
@Tag(name = "Product Management", description = "Các API quản lý thông tin sản phẩm và phân trang song song")
@RequestMapping("/api/v1/products")
public interface ProductOperations {

    /**
     * Tạo mới sản phẩm với kiểm tra song song SKU và Category bằng Single.zip.
     */
    @Operation(summary = "Tạo mới sản phẩm", description = "Kiểm tra SKU và Category song song bằng Single.zip trước khi INSERT jOOQ")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo sản phẩm thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu validation không hợp lệ"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy danh mục cha"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Mã SKU đã tồn tại")
    })
    @PostMapping
    ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request);

    /**
     * Lấy thông tin chi tiết một sản phẩm theo ID.
     */
    @Operation(summary = "Lấy chi tiết sản phẩm theo ID", description = "Truy vấn thông tin sản phẩm kèm tên danh mục qua JOIN")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tìm thấy sản phẩm"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @GetMapping("/{id}")
    ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "ID sản phẩm", required = true) @PathVariable("id") Long id
    );

    /**
     * Cập nhật thông tin sản phẩm.
     */
    @Operation(summary = "Cập nhật sản phẩm", description = "Kiểm tra tồn tại và cập nhật thông tin sản phẩm")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm hoặc danh mục")
    })
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "ID sản phẩm cần sửa", required = true) @PathVariable("id") Long id,
            @Valid @RequestBody UpdateProductRequest request
    );

    /**
     * Xóa mềm sản phẩm (cập nhật is_deleted = true).
     */
    @Operation(summary = "Xóa mềm sản phẩm", description = "Đánh dấu trạng thái is_deleted = true mà không xóa vật lý bản ghi")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa mềm thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy sản phẩm")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<ApiResponse<String>> deleteProduct(
            @Parameter(description = "ID sản phẩm cần xóa mềm", required = true) @PathVariable("id") Long id
    );

    /**
     * Tìm kiếm và phân trang sản phẩm với 2 truy vấn COUNT và SELECT LIMIT chạy song song qua Single.zip.
     */
    @Operation(summary = "Tìm kiếm và phân trang sản phẩm", description = "Thực thi song song 2 câu lệnh COUNT(*) và SELECT phân trang bằng Single.zip")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Truy vấn danh sách phân trang thành công")
    })
    @GetMapping
    ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @Parameter(description = "Từ khóa tìm kiếm theo tên hoặc SKU") @RequestParam(name = "keyword", required = false) String keyword,
            @Parameter(description = "Lọc theo ID danh mục") @RequestParam(name = "categoryId", required = false) Long categoryId,
            @Parameter(description = "Số thứ tự trang (0-indexed)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Số lượng bản ghi mỗi trang") @RequestParam(name = "size", defaultValue = "10") int size
    );
}
