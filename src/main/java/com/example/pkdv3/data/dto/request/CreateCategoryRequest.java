package com.example.pkdv3.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO tiếp nhận dữ liệu yêu cầu tạo mới Danh mục sản phẩm (Category).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Thông tin yêu cầu tạo danh mục mới")
public class CreateCategoryRequest {

    @Schema(description = "Mã định danh danh mục (duy nhất)", example = "ELECTRONICS")
    @NotBlank(message = "Mã danh mục (code) không được để trống")
    @Size(max = 50, message = "Mã danh mục không được vượt quá 50 ký tự")
    private String code;

    @Schema(description = "Tên hiển thị danh mục", example = "Đồ điện tử gia dụng")
    @NotBlank(message = "Tên danh mục (name) không được để trống")
    @Size(max = 255, message = "Tên danh mục không được vượt quá 255 ký tự")
    private String name;
}
