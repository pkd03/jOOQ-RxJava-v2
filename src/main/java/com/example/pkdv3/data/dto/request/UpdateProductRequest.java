package com.example.pkdv3.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO tiếp nhận dữ liệu cập nhật Sản phẩm (Product).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin yêu cầu cập nhật sản phẩm")
public class UpdateProductRequest {

    @Schema(description = "ID của danh mục cha", example = "1")
    @NotNull(message = "ID danh mục (categoryId) không được để trống")
    private Long categoryId;

    @Schema(description = "Tên hiển thị sản phẩm", example = "Laptop Dell XPS 13 Plus 2026")
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 255, message = "Tên sản phẩm không được vượt quá 255 ký tự")
    private String name;

    @Schema(description = "Giá bán sản phẩm", example = "31500000.00")
    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    @Schema(description = "Trạng thái kinh doanh", example = "ACTIVE")
    @NotBlank(message = "Trạng thái không được để trống")
    private String status;
}
