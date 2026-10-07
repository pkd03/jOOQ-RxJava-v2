package com.example.pkdv3.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Đối tượng bao bọc kết quả phân trang chuẩn hóa cho các API truy vấn danh sách.
 *
 * Mục đích:
 * Chứa danh sách các phần tử của trang hiện tại kèm theo các siêu dữ liệu (metadata)
 * phân trang: số trang hiện tại, kích thước trang, tổng số bản ghi và tổng số trang.
 *
 * @param <T> Kiểu dữ liệu của các phần tử trong danh sách
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /** Danh sách phần tử của trang hiện tại */
    private List<T> items;

    /** Số thứ tự trang hiện tại (bắt đầu từ 0) */
    private int page;

    /** Số lượng bản ghi tối đa trên một trang */
    private int size;

    /** Tổng số bản ghi thỏa mãn điều kiện lọc */
    private long totalElements;

    /** Tổng số trang */
    private int totalPages;

    /** Đánh dấu có phải trang đầu tiên */
    private boolean isFirst;

    /** Đánh dấu có phải trang cuối cùng */
    private boolean isLast;

    /**
     * Phương thức tiện ích (Factory Method) tính toán tự động các chỉ số phân trang.
     *
     * @param items Danh sách bản ghi
     * @param page Số trang hiện tại
     * @param size Kích thước trang
     * @param totalElements Tổng số bản ghi
     */
    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int calculatedTotalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .items(items)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(calculatedTotalPages)
                .isFirst(page == 0)
                .isLast(page >= calculatedTotalPages - 1 || calculatedTotalPages == 0)
                .build();
    }
}
