package com.example.pkdv3.data.dto.response;

import com.example.pkdv3.data.jooq.generated.tables.records.ProductsRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO dữ liệu trả về của Sản phẩm (Product).
 * Áp dụng kỹ thuật Manual Mapping trực tiếp từ jOOQ ProductsRecord.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết của sản phẩm")
public class ProductResponse {

    @Schema(description = "ID định danh sản phẩm", example = "100")
    private Long id;

    @Schema(description = "ID danh mục cha", example = "1")
    private Long categoryId;

    @Schema(description = "Tên danh mục cha (khi có JOIN)", example = "Đồ điện tử gia dụng")
    private String categoryName;

    @Schema(description = "Mã SKU quản lý kho", example = "LAPTOP-DELL-XPS13")
    private String sku;

    @Schema(description = "Tên sản phẩm", example = "Laptop Dell XPS 13 Plus")
    private String name;

    @Schema(description = "Giá bán hiện tại", example = "29990000.00")
    private BigDecimal price;

    @Schema(description = "Trạng thái kinh doanh", example = "ACTIVE")
    private String status;

    @Schema(description = "Cờ đánh dấu xóa mềm", example = "false")
    private Boolean isDeleted;

    @Schema(description = "Thời gian tạo bản ghi", example = "2026-10-07T10:00:00")
    private LocalDateTime createdAt;

    /**
     * Ánh xạ thủ công (Manual Mapping) từ ProductsRecord độc lập không có dữ liệu JOIN Category.
     */
    public static ProductResponse fromRecord(ProductsRecord record) {
        if (record == null) {
            return null;
        }
        return ProductResponse.builder()
                .id(record.getId())
                .categoryId(record.getCategoryId())
                .categoryName(null)
                .sku(record.getSku())
                .name(record.getName())
                .price(record.getPrice())
                .status(record.getStatus())
                .isDeleted(record.getIsDeleted())
                .createdAt(record.getCreatedAt())
                .build();
    }

    /**
     * Ánh xạ thủ công từ ProductsRecord kèm categoryName được lấy từ câu lệnh JOIN jOOQ.
     */
    public static ProductResponse fromRecord(ProductsRecord record, String categoryName) {
        ProductResponse response = fromRecord(record);
        if (response != null) {
            response.setCategoryName(categoryName);
        }
        return response;
    }
}
