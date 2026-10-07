package com.example.pkdv3.data.repository;

import com.example.pkdv3.data.jooq.generated.tables.records.CategoriesRecord;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import static com.example.pkdv3.data.jooq.generated.Tables.CATEGORIES;

/**
 * Repository xử lý dữ liệu bảng CATEGORIES sử dụng jOOQ Type-Safe SQL.
 *
 * Cơ chế hoạt động:
 * Vì JDBC là I/O blocking, toàn bộ phương thức truy vấn jOOQ được bao bọc bên trong
 * Single.fromCallable(...) và ủy quyền thực thi trên Bounded Scheduler (dbScheduler).
 * Điều này ngăn chặn việc luồng HTTP Servlet (Tomcat) bị block, đồng thời bảo vệ
 * HikariCP Connection Pool trước các đột biến tải.
 */
@Repository
public class CategoryRepository {

    private final DSLContext dslContext;
    private final Scheduler dbScheduler;

    public CategoryRepository(DSLContext dslContext, @Qualifier("dbScheduler") Scheduler dbScheduler) {
        this.dslContext = dslContext;
        this.dbScheduler = dbScheduler;
    }

    /**
     * Tìm kiếm danh mục theo ID (chỉ lấy các bản ghi chưa bị xóa mềm).
     *
     * @param id ID danh mục
     * @return Single chứa Optional của CategoriesRecord
     */
    public Single<Optional<CategoriesRecord>> findById(Long id) {
        return Single.fromCallable(() ->
                Optional.ofNullable(
                        dslContext.selectFrom(CATEGORIES)
                                .where(CATEGORIES.ID.eq(id).and(CATEGORIES.IS_DELETED.isFalse()))
                                .fetchOne()
                )
        ).subscribeOn(dbScheduler);
    }

    /**
     * Tìm kiếm danh mục theo mã code duy nhất (chỉ lấy bản ghi chưa bị xóa mềm).
     *
     * @param code Mã danh mục
     * @return Single chứa Optional của CategoriesRecord
     */
    public Single<Optional<CategoriesRecord>> findByCode(String code) {
        return Single.fromCallable(() ->
                Optional.ofNullable(
                        dslContext.selectFrom(CATEGORIES)
                                .where(CATEGORIES.CODE.eq(code).and(CATEGORIES.IS_DELETED.isFalse()))
                                .fetchOne()
                )
        ).subscribeOn(dbScheduler);
    }

    /**
     * Kiểm tra sự tồn tại của mã danh mục (dùng cho validation trùng lặp).
     *
     * @param code Mã danh mục
     * @return Single chứa Boolean true nếu đã tồn tại
     */
    public Single<Boolean> existsByCode(String code) {
        return Single.fromCallable(() ->
                dslContext.fetchExists(CATEGORIES, CATEGORIES.CODE.eq(code))
        ).subscribeOn(dbScheduler);
    }

    /**
     * Kiểm tra sự tồn tại và còn hiệu lực (chưa xóa mềm) của danh mục theo ID.
     *
     * @param id ID danh mục
     * @return Single chứa Boolean true nếu danh mục tồn tại và active
     */
    public Single<Boolean> existsById(Long id) {
        return Single.fromCallable(() ->
                dslContext.fetchExists(CATEGORIES, CATEGORIES.ID.eq(id).and(CATEGORIES.IS_DELETED.isFalse()))
        ).subscribeOn(dbScheduler);
    }

    /**
     * Thêm mới một bản ghi danh mục vào cơ sở dữ liệu và trả về bản ghi vừa tạo.
     *
     * @param record Bản ghi CategoriesRecord cần thêm mới
     * @return Single chứa CategoriesRecord đã được gán ID và default value
     */
    public Single<CategoriesRecord> insert(CategoriesRecord record) {
        return Single.fromCallable(() -> {
            CategoriesRecord created = dslContext.insertInto(CATEGORIES)
                    .set(record)
                    .returning()
                    .fetchOne();
            return created != null ? created : record;
        }).subscribeOn(dbScheduler);
    }

    /**
     * Lấy toàn bộ danh mục đang hoạt động (chưa bị xóa mềm).
     *
     * @return Single chứa danh sách CategoriesRecord
     */
    public Single<List<CategoriesRecord>> findAllActive() {
        return Single.fromCallable(() -> {
            List<CategoriesRecord> records = dslContext.selectFrom(CATEGORIES)
                    .where(CATEGORIES.IS_DELETED.isFalse())
                    .orderBy(CATEGORIES.ID.asc())
                    .fetch();
            return records;
        }).subscribeOn(dbScheduler);
    }
}
