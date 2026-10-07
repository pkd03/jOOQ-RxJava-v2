package com.example.pkdv3.wio.rest.controller;

import com.example.pkdv3.data.dto.request.CreateProductRequest;
import com.example.pkdv3.data.dto.request.UpdateProductRequest;
import com.example.pkdv3.data.dto.response.ApiResponse;
import com.example.pkdv3.data.dto.response.PageResponse;
import com.example.pkdv3.data.dto.response.ProductResponse;
import com.example.pkdv3.service.ProductService;
import com.example.pkdv3.wio.rest.ProductOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller xử lý các yêu cầu HTTP liên quan đến Sản phẩm (Product).
 *
 * Vai trò kiến trúc:
 * 1. Hiện thực interface ProductOperations.
 * 2. Cầu nối RxJava 3 Reactive và Spring MVC: Tiếp nhận Single<T> từ ProductService và gọi .blockingGet()
 *    để chuyển kết quả thành đối tượng Java đồng bộ chuẩn bị trả về JSON cho Client.
 * 3. Đảm bảo toàn bộ phản hồi được bọc đồng nhất bên trong ApiResponse<T>.
 */
@RestController
@RequiredArgsConstructor
public class ProductController implements ProductOperations {

    private final ProductService productService;

    /**
     * Tạo mới sản phẩm:
     * ProductService thực thi Single.zip kiểm tra song song SKU và Category, sau đó flatMap sang INSERT.
     * Controller unwrap an toàn bằng .blockingGet() và trả về HTTP 201 CREATED.
     */
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(CreateProductRequest request) {
        ProductResponse response = productService.createProduct(request).blockingGet();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Khởi tạo sản phẩm thành công"));
    }

    /**
     * Lấy thông tin chi tiết sản phẩm theo ID.
     */
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(Long id) {
        ProductResponse response = productService.getProductById(id).blockingGet();
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin sản phẩm thành công"));
    }

    /**
     * Cập nhật thông tin sản phẩm.
     */
    @Override
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(Long id, UpdateProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request).blockingGet();
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật sản phẩm thành công"));
    }

    /**
     * Xóa mềm sản phẩm theo ID.
     */
    @Override
    public ResponseEntity<ApiResponse<String>> deleteProduct(Long id) {
        String message = productService.deleteProduct(id).blockingGet();
        return ResponseEntity.ok(ApiResponse.success(message, "Xóa mềm sản phẩm thành công"));
    }

    /**
     * Tìm kiếm và phân trang sản phẩm:
     * ProductService kích hoạt song song 2 câu lệnh COUNT(*) và SELECT LIMIT/OFFSET qua Single.zip.
     * Controller unwrap kết quả tổng hợp PageResponse bằng .blockingGet().
     */
    @Override
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            String keyword, Long categoryId, int page, int size) {
        PageResponse<ProductResponse> response = productService.searchProducts(keyword, categoryId, page, size).blockingGet();
        return ResponseEntity.ok(ApiResponse.success(response, "Tìm kiếm và phân trang sản phẩm thành công"));
    }
}
