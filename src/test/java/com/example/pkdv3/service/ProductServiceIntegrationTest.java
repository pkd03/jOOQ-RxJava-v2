package com.example.pkdv3.service;

import com.example.pkdv3.data.dto.request.CreateCategoryRequest;
import com.example.pkdv3.data.dto.request.CreateProductRequest;
import com.example.pkdv3.data.dto.response.CategoryResponse;
import com.example.pkdv3.data.dto.response.PageResponse;
import com.example.pkdv3.data.dto.response.ProductResponse;
import com.example.pkdv3.validate.exception.DuplicateResourceException;
import com.example.pkdv3.validate.exception.ResourceNotFoundException;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static com.example.pkdv3.data.jooq.generated.Tables.CATEGORIES;
import static com.example.pkdv3.data.jooq.generated.Tables.PRODUCTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Bài kiểm thử tích hợp cho ProductService và CategoryService.
 * Xác nhận hoạt động chuẩn xác của toàn bộ luồng RxJava 3:
 * - Tạo sản phẩm với Single.zip (SKU check + Category check song song)
 * - Xử lý trùng lặp SKU (DuplicateResourceException)
 * - Xử lý không tìm thấy Category (ResourceNotFoundException)
 * - Tìm kiếm phân trang song song qua Single.zip
 * - Xóa mềm sản phẩm (Soft Delete) qua flatMap
 */
@SpringBootTest
public class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DSLContext dslContext;

    private Long testCategoryId;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(PRODUCTS).execute();
        dslContext.deleteFrom(CATEGORIES).execute();

        CreateCategoryRequest categoryRequest = CreateCategoryRequest.builder()
                .code("TECH")
                .name("Công nghệ thông tin")
                .build();

        CategoryResponse categoryResponse = categoryService.createCategory(categoryRequest).blockingGet();
        testCategoryId = categoryResponse.getId();
    }

    @Test
    @DisplayName("Tạo Product thành công bằng Single.zip + Pair + flatMap")
    void testCreateProduct_Success() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(testCategoryId)
                .sku("LAPTOP-MACBOOK-M3")
                .name("MacBook Pro M3")
                .price(new BigDecimal("45000000.00"))
                .status("ACTIVE")
                .build();

        ProductResponse response = productService.createProduct(request).blockingGet();

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("LAPTOP-MACBOOK-M3", response.getSku());
        assertEquals("MacBook Pro M3", response.getName());
        assertEquals("Công nghệ thông tin", response.getCategoryName());
        assertFalse(response.getIsDeleted());
    }

    @Test
    @DisplayName("Tạo Product thất bại khi SKU đã tồn tại (throw DuplicateResourceException)")
    void testCreateProduct_DuplicateSku() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(testCategoryId)
                .sku("SKU-DUPLICATE-001")
                .name("Màn hình LG 27 inch")
                .price(new BigDecimal("5000000.00"))
                .status("ACTIVE")
                .build();

        productService.createProduct(request).blockingGet();

        // Cố tình tạo thêm sản phẩm cùng SKU
        assertThrows(DuplicateResourceException.class, () -> {
            productService.createProduct(request).blockingGet();
        });
    }

    @Test
    @DisplayName("Tạo Product thất bại khi Category không tồn tại (throw ResourceNotFoundException)")
    void testCreateProduct_CategoryNotFound() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(999999L) // Category ID không tồn tại
                .sku("SKU-NO-CAT")
                .name("Bàn phím cơ Keychron")
                .price(new BigDecimal("2000000.00"))
                .status("ACTIVE")
                .build();

        assertThrows(ResourceNotFoundException.class, () -> {
            productService.createProduct(request).blockingGet();
        });
    }

    @Test
    @DisplayName("Tìm kiếm và phân trang kết hợp song song qua Single.zip")
    void testSearchProducts_Pagination() {
        // Tạo 3 sản phẩm mẫu
        for (int i = 1; i <= 3; i++) {
            CreateProductRequest req = CreateProductRequest.builder()
                    .categoryId(testCategoryId)
                    .sku("PHONE-IPHONE-" + i)
                    .name("iPhone 15 Pro Max " + i)
                    .price(new BigDecimal("30000000.00"))
                    .status("ACTIVE")
                    .build();
            productService.createProduct(req).blockingGet();
        }

        PageResponse<ProductResponse> pageResponse = productService.searchProducts("iPhone", testCategoryId, 0, 2).blockingGet();

        assertNotNull(pageResponse);
        assertEquals(2, pageResponse.getItems().size());
        assertEquals(3L, pageResponse.getTotalElements());
        assertEquals(2, pageResponse.getTotalPages());
        assertEquals(0, pageResponse.getPage());
    }

    @Test
    @DisplayName("Xóa mềm (Soft Delete) sản phẩm qua flatMap")
    void testDeleteProduct_SoftDelete() {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(testCategoryId)
                .sku("MOUSE-LOGITECH-MX")
                .name("Chuột Logitech MX Master 3S")
                .price(new BigDecimal("2500000.00"))
                .status("ACTIVE")
                .build();

        ProductResponse created = productService.createProduct(request).blockingGet();

        // Thực hiện xóa mềm
        String deleteMessage = productService.deleteProduct(created.getId()).blockingGet();
        assertNotNull(deleteMessage);

        // Kiểm tra sản phẩm không còn tìm thấy sau khi đã xóa mềm
        assertThrows(ResourceNotFoundException.class, () -> {
            productService.getProductById(created.getId()).blockingGet();
        });
    }
}
