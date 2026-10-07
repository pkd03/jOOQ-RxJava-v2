package com.example.pkdv3.data.dto.response;

import com.example.pkdv3.data.jooq.generated.tables.records.CategoriesRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO dữ liệu trả về của Danh mục (Category).
 * Áp dụng kỹ thuật Manual Mapping (không dùng MapStruct) để tối ưu hiệu năng và dễ kiểm soát.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin chi tiết của danh mục")
public class CategoryResponse {

    @Schema(description = "ID định danh danh mục", example = "1")
    private Long id;

    @Schema(description = "Mã danh mục", example = "ELECTRONICS")
    private String code;

    @Schema(description = "Tên hiển thị danh mục", example = "Đồ điện tử gia dụng")
    private String name;

    @Schema(description = "Cờ đánh dấu đã xóa mềm hay chưa", example = "false")
    private Boolean isDeleted;

    @Schema(description = "Thời gian tạo bản ghi", example = "2026-10-07T10:00:00")
    private LocalDateTime createdAt;

    /**
     * Phương thức ánh xạ thủ công (Manual Mapping) từ jOOQ CategoriesRecord sang CategoryResponse.
     * Tránh overhead phân giải reflection của MapStruct và đảm bảo tương thích 100% Type-Safe.
     */
    public static CategoryResponse fromRecord(CategoriesRecord record) {
        if (record == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(record.getId())
                .code(record.getCode())
                .name(record.getName())
                .isDeleted(record.getIsDeleted())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
