package com.example.pkdv3.service;

import com.example.pkdv3.data.dto.request.CreateCategoryRequest;
import com.example.pkdv3.data.dto.response.CategoryResponse;
import com.example.pkdv3.data.jooq.generated.tables.records.CategoriesRecord;
import com.example.pkdv3.data.repository.CategoryRepository;
import com.example.pkdv3.validate.exception.DuplicateResourceException;
import com.example.pkdv3.validate.exception.ResourceNotFoundException;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service quản lý logic nghiệp vụ cho Danh mục (Category).
 *
 * Cơ chế hoạt động:
 * Toàn bộ phương thức nghiệp vụ đều trả về luồng phản ứng RxJava 3 Single<T>.
 * Sử dụng toán tử flatMap để chuyển đổi bất đồng bộ giữa bước kiểm tra trùng lặp
 * và bước ghi dữ liệu vào cơ sở dữ liệu.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /**
     * Tạo mới danh mục sau khi kiểm tra tính duy nhất của mã (Code).
     *
     * Cơ chế toán tử RxJava:
     * 1. categoryRepository.existsByCode(request.getCode()): Trả về Single<Boolean>.
     * 2. flatMap: Kiểm tra nếu exists == true thì phát ra lỗi DuplicateResourceException qua Single.error.
     *    Nếu chưa tồn tại, chuẩn bị CategoriesRecord và chuyển tiếp sang categoryRepository.insert.
     * 3. map: Ánh xạ thủ công (Manual Mapping) kết quả CategoriesRecord vừa lưu sang CategoryResponse.
     *
     * @param request Dữ liệu tạo danh mục
     * @return Single chứa CategoryResponse
     */
    public Single<CategoryResponse> createCategory(CreateCategoryRequest request) {
        return categoryRepository.existsByCode(request.getCode().trim().toUpperCase())
                .flatMap(exists -> {
                    if (Boolean.TRUE.equals(exists)) {
                        return Single.error(new DuplicateResourceException(
                                "Category", "code", request.getCode().trim().toUpperCase()
                        ));
                    }

                    CategoriesRecord record = new CategoriesRecord();
                    record.setCode(request.getCode().trim().toUpperCase());
                    record.setName(request.getName().trim());
                    record.setIsDeleted(false);
                    record.setCreatedAt(LocalDateTime.now());

                    return categoryRepository.insert(record);
                })
                .map(CategoryResponse::fromRecord);
    }

    /**
     * Lấy thông tin danh mục theo ID.
     *
     * Cơ chế toán tử RxJava:
     * Dùng flatMap để kiểm tra Optional: Nếu rỗng thì throw ResourceNotFoundException,
     * ngược lại ánh xạ sang CategoryResponse.
     *
     * @param id ID danh mục
     * @return Single chứa CategoryResponse
     */
    public Single<CategoryResponse> getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .flatMap(optionalRecord -> optionalRecord
                        .map(record -> Single.just(CategoryResponse.fromRecord(record)))
                        .orElseGet(() -> Single.error(new ResourceNotFoundException("Category", "id", id)))
                );
    }

    /**
     * Lấy danh sách toàn bộ danh mục đang hoạt động.
     *
     * Cơ chế toán tử RxJava:
     * Dùng map để biến đổi danh sách CategoriesRecord sang List<CategoryResponse>.
     *
     * @return Single chứa danh sách CategoryResponse
     */
    public Single<List<CategoryResponse>> getAllActiveCategories() {
        return categoryRepository.findAllActive()
                .map(records -> records.stream()
                        .map(CategoryResponse::fromRecord)
                        .toList()
                );
    }
}
