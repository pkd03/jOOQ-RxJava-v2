package com.example.pkdv3.controller;

import com.example.pkdv3.data.dto.request.CreateCategoryRequest;
import com.example.pkdv3.data.dto.request.CreateProductRequest;
import com.example.pkdv3.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jooq.DSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.example.pkdv3.data.jooq.generated.Tables.CATEGORIES;
import static com.example.pkdv3.data.jooq.generated.Tables.PRODUCTS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp tầng Web MVC REST API (ProductController và CategoryController).
 * Xác nhận cơ chế cầu nối .blockingGet() hoạt động trơn tru trên môi trường HTTP Tomcat MockMvc:
 * - Trả về mã HTTP 201 Created và ApiResponse chuẩn khi tạo thành công.
 * - Trả về mã HTTP 409 Conflict khi vi phạm trùng lặp SKU.
 * - Trả về mã HTTP 404 Not Found khi truy vấn ID không tồn tại.
 * - Trả về phân trang PageResponse hợp lệ khi GET danh sách.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DSLContext dslContext;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        dslContext.deleteFrom(PRODUCTS).execute();
        dslContext.deleteFrom(CATEGORIES).execute();

        CreateCategoryRequest categoryRequest = CreateCategoryRequest.builder()
                .code("MOBILE")
                .name("Điện thoại di động")
                .build();
        categoryId = categoryService.createCategory(categoryRequest).blockingGet().getId();
    }

    @Test
    @DisplayName("API POST /api/v1/products - Tạo sản phẩm thành công trả về HTTP 201 và ApiResponse")
    void testCreateProduct_Success() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(categoryId)
                .sku("IPHONE-15-PRO")
                .name("iPhone 15 Pro 256GB")
                .price(new BigDecimal("28990000.00"))
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.sku").value("IPHONE-15-PRO"))
                .andExpect(jsonPath("$.data.categoryName").value("Điện thoại di động"));
    }

    @Test
    @DisplayName("API POST /api/v1/products - Trùng SKU trả về HTTP 409 CONFLICT")
    void testCreateProduct_DuplicateSku() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(categoryId)
                .sku("SAMSUNG-S24-ULTRA")
                .name("Samsung Galaxy S24 Ultra")
                .price(new BigDecimal("31990000.00"))
                .status("ACTIVE")
                .build();

        // Tạo lần 1 thành công
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Tạo lần 2 cùng SKU -> 409 CONFLICT
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("API GET /api/v1/products/{id} - ID không tồn tại trả về HTTP 404 NOT_FOUND")
    void testGetProduct_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", 88888L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("API GET /api/v1/products - Phân trang danh sách thành công trả về PageResponse")
    void testSearchProducts_Success() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(categoryId)
                .sku("PIXEL-8-PRO")
                .name("Google Pixel 8 Pro")
                .price(new BigDecimal("21990000.00"))
                .status("ACTIVE")
                .build();

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "Pixel")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.items[0].sku").value("PIXEL-8-PRO"));
    }

    @Test
    @DisplayName("API DELETE /api/v1/products/{id} - Xóa mềm sản phẩm thành công")
    void testDeleteProduct_Success() throws Exception {
        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(categoryId)
                .sku("XIAOMI-14")
                .name("Xiaomi 14 Ultra")
                .price(new BigDecimal("23990000.00"))
                .status("ACTIVE")
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long newProductId = objectMapper.readTree(responseJson).get("data").get("id").asLong();

        // Xóa mềm
        mockMvc.perform(delete("/api/v1/products/{id}", newProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Kiểm tra gọi lại trả về 404 vì is_deleted = true
        mockMvc.perform(get("/api/v1/products/{id}", newProductId))
                .andExpect(status().isNotFound());
    }
}
