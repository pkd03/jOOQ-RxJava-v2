package com.example.pkdv3.data.repository;

import com.example.pkdv3.data.dto.response.ProductResponse;
import com.example.pkdv3.data.jooq.generated.tables.records.ProductsRecord;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.pkdv3.data.jooq.generated.Tables.CATEGORIES;
import static com.example.pkdv3.data.jooq.generated.Tables.PRODUCTS;

/**
 * Repository xử lý dữ liệu bảng PRODUCTS sử dụng jOOQ Type-Safe SQL kết hợp RxJava 3.
 *
 * Cơ chế hoạt động:
 * Mỗi hàm truy vấn SQL được bao bọc trong Single.fromCallable(...) và chạy trên Bounded
 * Scheduler (dbScheduler). Tầng Repository chịu trách nhiệm tương tác cơ sở dữ liệu
 * nguyên tử (Atomic Operations) chuẩn bị sẵn sàng cho việc kết hợp (zip, flatMap) ở Service.
 */
@Repository
public class ProductRepository {

    private final DSLContext dslContext;
    private final Scheduler dbScheduler;

    public ProductRepository(DSLContext dslContext, @Qualifier("dbScheduler") Scheduler dbScheduler) {
        this.dslContext = dslContext;
        this.dbScheduler = dbScheduler;
    }

    /**
     * Lấy bản ghi ProductsRecord thuần túy theo ID (chỉ lấy các bản ghi chưa bị xóa mềm).
     *
     * @param id ID sản phẩm
     * @return Single chứa Optional của ProductsRecord
     */
    public Single<Optional<ProductsRecord>> findRecordById(Long id) {
        return Single.fromCallable(() ->
                Optional.ofNullable(
                        dslContext.selectFrom(PRODUCTS)
                                .where(PRODUCTS.ID.eq(id).and(PRODUCTS.IS_DELETED.isFalse()))
                                .fetchOne()
                )
        ).subscribeOn(dbScheduler);
    }

    /**
     * Lấy thông tin chi tiết sản phẩm kèm tên danh mục (JOIN categories) theo ID.
     *
     * @param id ID sản phẩm
     * @return Single chứa Optional của ProductResponse
     */
    public Single<Optional<ProductResponse>> findDetailById(Long id) {
        return Single.fromCallable(() -> {
            var record = dslContext.select(PRODUCTS.fields())
                    .select(CATEGORIES.NAME.as("category_name"))
                    .from(PRODUCTS)
                    .leftJoin(CATEGORIES).on(PRODUCTS.CATEGORY_ID.eq(CATEGORIES.ID))
                    .where(PRODUCTS.ID.eq(id).and(PRODUCTS.IS_DELETED.isFalse()))
                    .fetchOne();

            if (record == null) {
                return Optional.<ProductResponse>empty();
            }

            ProductsRecord pr = record.into(PRODUCTS);
            String categoryName = record.get("category_name", String.class);
            return Optional.of(ProductResponse.fromRecord(pr, categoryName));
        }).subscribeOn(dbScheduler);
    }

    /**
     * Kiểm tra sự tồn tại của SKU để phục vụ xác thực trùng lặp.
     *
     * @param sku Mã SKU cần kiểm tra
     * @return Single chứa Boolean true nếu đã tồn tại SKU
     */
    public Single<Boolean> existsBySku(String sku) {
        return Single.fromCallable(() ->
                dslContext.fetchExists(PRODUCTS, PRODUCTS.SKU.eq(sku))
        ).subscribeOn(dbScheduler);
    }

    /**
     * Thêm mới một sản phẩm vào cơ sở dữ liệu và trả về bản ghi hoàn chỉnh.
     *
     * @param record Đối tượng ProductsRecord cần lưu
     * @return Single chứa ProductsRecord với ID sinh tự động
     */
    public Single<ProductsRecord> insert(ProductsRecord record) {
        return Single.fromCallable(() -> {
            ProductsRecord created = dslContext.insertInto(PRODUCTS)
                    .set(record)
                    .returning()
                    .fetchOne();
            return created != null ? created : record;
        }).subscribeOn(dbScheduler);
    }

    /**
     * Cập nhật thông tin bản ghi sản phẩm.
     *
     * @param record Bản ghi ProductsRecord đã thay đổi
     * @return Single chứa ProductsRecord sau khi cập nhật
     */
    public Single<ProductsRecord> update(ProductsRecord record) {
        return Single.fromCallable(() -> {
            record.attach(dslContext.configuration());
            record.update();
            return record;
        }).subscribeOn(dbScheduler);
    }

    /**
     * Xóa mềm sản phẩm bằng cách cập nhật cờ is_deleted = true.
     *
     * @param id ID sản phẩm cần xóa mềm
     * @return Single chứa số lượng bản ghi bị tác động (1 nếu thành công, 0 nếu không tìm thấy)
     */
    public Single<Integer> softDelete(Long id) {
        return Single.fromCallable(() ->
                dslContext.update(PRODUCTS)
                        .set(PRODUCTS.IS_DELETED, true)
                        .where(PRODUCTS.ID.eq(id).and(PRODUCTS.IS_DELETED.isFalse()))
                        .execute()
        ).subscribeOn(dbScheduler);
    }

    /**
     * Đếm tổng số sản phẩm thỏa mãn điều kiện tìm kiếm và danh mục.
     *
     * @param keyword Từ khóa tìm kiếm theo tên hoặc SKU
     * @param categoryId ID danh mục lọc
     * @return Single chứa tổng số lượng bản ghi
     */
    public Single<Long> countProducts(String keyword, Long categoryId) {
        return Single.fromCallable(() -> {
            Condition condition = buildSearchCondition(keyword, categoryId);
            Long count = dslContext.selectCount()
                    .from(PRODUCTS)
                    .where(condition)
                    .fetchOne(0, Long.class);
            return count != null ? count : 0L;
        }).subscribeOn(dbScheduler);
    }

    /**
     * Truy vấn danh sách sản phẩm phân trang kết hợp JOIN bảng categories.
     *
     * @param keyword Từ khóa tìm kiếm
     * @param categoryId ID danh mục
     * @param offset Vị trí bắt đầu lấy dữ liệu
     * @param limit Số lượng bản ghi tối đa
     * @return Single chứa danh sách ProductResponse
     */
    public Single<List<ProductResponse>> findProductsWithPaging(String keyword, Long categoryId, int offset, int limit) {
        return Single.fromCallable(() -> {
            Condition condition = buildSearchCondition(keyword, categoryId);
            return dslContext.select(PRODUCTS.fields())
                    .select(CATEGORIES.NAME.as("category_name"))
                    .from(PRODUCTS)
                    .leftJoin(CATEGORIES).on(PRODUCTS.CATEGORY_ID.eq(CATEGORIES.ID))
                    .where(condition)
                    .orderBy(PRODUCTS.CREATED_AT.desc())
                    .limit(limit)
                    .offset(offset)
                    .fetch(record -> {
                        ProductsRecord pr = record.into(PRODUCTS);
                        String categoryName = record.get("category_name", String.class);
                        return ProductResponse.fromRecord(pr, categoryName);
                    });
        }).subscribeOn(dbScheduler);
    }

    /**
     * Phương thức tiện ích xây dựng điều kiện truy vấn động dựa trên tham số lọc.
     */
    private Condition buildSearchCondition(String keyword, Long categoryId) {
        Condition condition = PRODUCTS.IS_DELETED.isFalse();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String pattern = "%" + keyword.trim() + "%";
            condition = condition.and(
                    PRODUCTS.NAME.likeIgnoreCase(pattern)
                            .or(PRODUCTS.SKU.likeIgnoreCase(pattern))
            );
        }

        if (categoryId != null) {
            condition = condition.and(PRODUCTS.CATEGORY_ID.eq(categoryId));
        }

        return condition;
    }
}
