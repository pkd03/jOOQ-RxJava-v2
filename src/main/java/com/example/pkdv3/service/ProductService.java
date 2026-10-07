package com.example.pkdv3.service;

import com.example.pkdv3.data.dto.request.CreateProductRequest;
import com.example.pkdv3.data.dto.request.UpdateProductRequest;
import com.example.pkdv3.data.dto.response.PageResponse;
import com.example.pkdv3.data.dto.response.ProductResponse;
import com.example.pkdv3.data.jooq.generated.tables.records.CategoriesRecord;
import com.example.pkdv3.data.jooq.generated.tables.records.ProductsRecord;
import com.example.pkdv3.data.repository.CategoryRepository;
import com.example.pkdv3.data.repository.ProductRepository;
import com.example.pkdv3.validate.exception.BusinessException;
import com.example.pkdv3.validate.exception.DuplicateResourceException;
import com.example.pkdv3.validate.exception.ResourceNotFoundException;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý các nghiệp vụ phức tạp của Sản phẩm (Product).
 *
 * Trọng tâm kiến trúc RxJava 3:
 * 1. Single.zip: Thực thi song song các tác vụ độc lập (ví dụ: check trùng SKU song song với check Category;
 *    hoặc đếm tổng số bản ghi COUNT song song với lấy danh sách phân trang LIMIT/OFFSET).
 * 2. Pair (Apache Commons): Kết hợp cặp kết quả đầu ra của 2 Single chạy song song một cách type-safe.
 * 3. flatMap: Chuyển đổi luồng không đồng bộ (Chaining Async Operations) từ bước kiểm tra sang bước ghi DB,
 *    hoặc từ bước tìm kiếm sang bước cập nhật/xóa mềm.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * Kịch bản tạo mới Sản phẩm:
     * Sử dụng Single.zip kết hợp Pair để kiểm tra song song:
     * - Tác vụ A: Kiểm tra SKU đã tồn tại chưa (productRepository.existsBySku).
     * - Tác vụ B: Kiểm tra Category ID có hợp lệ và tồn tại không (categoryRepository.findById).
     *
     * Sau khi 2 tác vụ trên hoàn thành đồng thời:
     * Sử dụng flatMap để xác thực kết quả:
     * - Nếu SKU đã tồn tại -> Trả về Single.error(DuplicateResourceException).
     * - Nếu Category không tồn tại -> Trả về Single.error(ResourceNotFoundException).
     * - Nếu hợp lệ -> Tiến hành INSERT bản ghi vào DB bằng jOOQ và ánh xạ sang ProductResponse.
     *
     * @param request Thông tin yêu cầu tạo sản phẩm
     * @return Single chứa ProductResponse hoàn chỉnh
     */
    public Single<ProductResponse> createProduct(CreateProductRequest request) {
        String normalizedSku = request.getSku().trim().toUpperCase();

        Single<Boolean> checkSkuSingle = productRepository.existsBySku(normalizedSku);
        Single<Optional<CategoriesRecord>> checkCategorySingle = categoryRepository.findById(request.getCategoryId());

        return Single.zip(checkSkuSingle, checkCategorySingle, Pair::of)
                .flatMap(pair -> {
                    Boolean skuExists = pair.getLeft();
                    Optional<CategoriesRecord> categoryOpt = pair.getRight();

                    // 1. Kiểm tra ràng buộc trùng lặp SKU
                    if (Boolean.TRUE.equals(skuExists)) {
                        return Single.error(new DuplicateResourceException("Product", "sku", normalizedSku));
                    }

                    // 2. Kiểm tra ràng buộc khóa ngoại Category
                    if (categoryOpt.isEmpty()) {
                        return Single.error(new ResourceNotFoundException("Category", "id", request.getCategoryId()));
                    }

                    CategoriesRecord category = categoryOpt.get();

                    // 3. Khởi tạo đối tượng ProductsRecord của jOOQ
                    ProductsRecord record = new ProductsRecord();
                    record.setCategoryId(request.getCategoryId());
                    record.setSku(normalizedSku);
                    record.setName(request.getName().trim());
                    record.setPrice(request.getPrice());
                    record.setStatus(request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "ACTIVE");
                    record.setIsDeleted(false);
                    record.setCreatedAt(LocalDateTime.now());

                    // Chuyển tiếp (flatMap) sang lệnh INSERT jOOQ
                    return productRepository.insert(record)
                            .map(savedRecord -> ProductResponse.fromRecord(savedRecord, category.getName()));
                });
    }

    /**
     * Kịch bản Tìm kiếm và Phân trang Sản phẩm:
     * Thay vì gọi tuần tự COUNT(*) rồi mới SELECT ... LIMIT/OFFSET, ta kích hoạt 2 truy vấn
     * chạy đồng thời trên Bounded Scheduler qua Single.zip:
     * - Tác vụ 1: itemsSingle thực hiện SELECT ... LIMIT/OFFSET JOIN categories.
     * - Tác vụ 2: countSingle thực hiện SELECT COUNT(*).
     *
     * Khi cả hai câu truy vấn hoàn thành, Single.zip gộp (aggregate) kết quả thành PageResponse chuẩn.
     *
     * @param keyword Từ khóa tìm kiếm theo tên hoặc SKU
     * @param categoryId ID danh mục cần lọc
     * @param page Số thứ tự trang (0-indexed)
     * @param size Số bản ghi tối đa trên một trang
     * @return Single chứa PageResponse<ProductResponse>
     */
    public Single<PageResponse<ProductResponse>> searchProducts(String keyword, Long categoryId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 10;
        int offset = safePage * safeSize;

        Single<List<ProductResponse>> itemsSingle = productRepository.findProductsWithPaging(keyword, categoryId, offset, safeSize);
        Single<Long> countSingle = productRepository.countProducts(keyword, categoryId);

        return Single.zip(itemsSingle, countSingle, (items, totalElements) ->
                PageResponse.of(items, safePage, safeSize, totalElements)
        );
    }

    /**
     * Kịch bản Xóa mềm (Soft Delete) Sản phẩm:
     * Quy trình xử lý theo chuỗi flatMap liên hoàn:
     * 1. productRepository.findRecordById: Kiểm tra sự tồn tại của sản phẩm.
     *    Nếu không tìm thấy -> throw ResourceNotFoundException.
     * 2. flatMap tiếp tục gọi productRepository.softDelete: Cập nhật is_deleted = true.
     *    Nếu không có bản ghi nào bị ảnh hưởng -> throw BusinessException.
     *    Nếu thành công -> Trả về thông điệp xác nhận hoàn tất.
     *
     * @param id ID sản phẩm cần xóa mềm
     * @return Single chứa chuỗi thông báo kết quả
     */
    public Single<String> deleteProduct(Long id) {
        return productRepository.findRecordById(id)
                .flatMap(optionalRecord -> {
                    if (optionalRecord.isEmpty()) {
                        return Single.error(new ResourceNotFoundException("Product", "id", id));
                    }

                    // Chuyển tiếp sang thao tác UPDATE is_deleted = true
                    return productRepository.softDelete(id)
                            .flatMap(affectedRows -> {
                                if (affectedRows == 0) {
                                    return Single.error(new BusinessException("Không thể thực hiện xóa mềm sản phẩm với ID: " + id));
                                }
                                return Single.just("Xóa mềm sản phẩm thành công với ID: " + id);
                            });
                });
    }

    /**
     * Lấy thông tin chi tiết của Sản phẩm theo ID (kèm thông tin tên danh mục).
     *
     * @param id ID sản phẩm
     * @return Single chứa ProductResponse
     */
    public Single<ProductResponse> getProductById(Long id) {
        return productRepository.findDetailById(id)
                .flatMap(optionalResponse -> optionalResponse
                        .map(Single::just)
                        .orElseGet(() -> Single.error(new ResourceNotFoundException("Product", "id", id)))
                );
    }

    /**
     * Cập nhật thông tin Sản phẩm:
     * Dùng Single.zip kiểm tra đồng thời sự tồn tại của Sản phẩm và Danh mục mới.
     * Sau đó dùng flatMap để thực thi lệnh UPDATE trên jOOQ.
     *
     * @param id ID sản phẩm cần sửa
     * @param request Dữ liệu cập nhật mới
     * @return Single chứa ProductResponse sau khi cập nhật
     */
    public Single<ProductResponse> updateProduct(Long id, UpdateProductRequest request) {
        Single<Optional<ProductsRecord>> findProductSingle = productRepository.findRecordById(id);
        Single<Optional<CategoriesRecord>> findCategorySingle = categoryRepository.findById(request.getCategoryId());

        return Single.zip(findProductSingle, findCategorySingle, Pair::of)
                .flatMap(pair -> {
                    Optional<ProductsRecord> productOpt = pair.getLeft();
                    Optional<CategoriesRecord> categoryOpt = pair.getRight();

                    if (productOpt.isEmpty()) {
                        return Single.error(new ResourceNotFoundException("Product", "id", id));
                    }
                    if (categoryOpt.isEmpty()) {
                        return Single.error(new ResourceNotFoundException("Category", "id", request.getCategoryId()));
                    }

                    ProductsRecord record = productOpt.get();
                    record.setCategoryId(request.getCategoryId());
                    record.setName(request.getName().trim());
                    record.setPrice(request.getPrice());
                    record.setStatus(request.getStatus().trim().toUpperCase());

                    return productRepository.update(record)
                            .map(updatedRecord -> ProductResponse.fromRecord(updatedRecord, categoryOpt.get().getName()));
                });
    }
}
