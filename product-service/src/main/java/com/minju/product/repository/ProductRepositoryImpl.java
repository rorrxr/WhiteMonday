package com.minju.product.repository;

import com.minju.product.dto.ProductSearchCondition;
import com.minju.product.entity.Product;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.minju.product.entity.QProduct.product;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 동적 상품 검색 (가격, 재고, 플래시세일, 키워드)
     */
    @Override
    public Page<Product> searchProducts(ProductSearchCondition condition, Pageable pageable) {
        // 카운트 쿼리
        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(
                        titleContains(condition.getKeyword()),
                        priceGoe(condition.getMinPrice()),
                        priceLoe(condition.getMaxPrice()),
                        flashSaleEq(condition.getFlashSale()),
                        inStockCondition(condition.getInStock())
                )
                .fetchOne();

        // 데이터 쿼리
        List<Product> content = queryFactory
                .selectFrom(product)
                .where(
                        titleContains(condition.getKeyword()),
                        priceGoe(condition.getMinPrice()),
                        priceLoe(condition.getMaxPrice()),
                        flashSaleEq(condition.getFlashSale()),
                        inStockCondition(condition.getInStock())
                )
                .orderBy(product.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 만료된 플래시세일 상품 조회
     * - flashSale = true
     * - flashSaleStartTime < currentTime (시작 시간이 지났지만 아직 flashSale이 true인 상품)
     */
    @Override
    public List<Product> findExpiredFlashSaleProducts(String currentTime) {
        return queryFactory
                .selectFrom(product)
                .where(
                        product.flashSale.isTrue(),
                        product.flashSaleStartTime.isNotNull(),
                        product.flashSaleStartTime.lt(currentTime)
                )
                .fetch();
    }

    /**
     * 재고 있는 상품만 조회 (페이징)
     */
    @Override
    public Page<Product> findAvailableProducts(Pageable pageable) {
        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(product.stock.gt(0))
                .fetchOne();

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(product.stock.gt(0))
                .orderBy(product.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    /**
     * 플래시세일 상품 조회 (페이징)
     */
    @Override
    public Page<Product> findFlashSaleProducts(Pageable pageable) {
        Long total = queryFactory
                .select(product.count())
                .from(product)
                .where(product.flashSale.isTrue())
                .fetchOne();

        List<Product> content = queryFactory
                .selectFrom(product)
                .where(product.flashSale.isTrue())
                .orderBy(product.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // ==================== 조건 빌더 메서드 ====================

    private BooleanExpression titleContains(String keyword) {
        return keyword != null ? product.title.containsIgnoreCase(keyword) : null;
    }

    private BooleanExpression priceGoe(Integer minPrice) {
        return minPrice != null ? product.price.goe(minPrice) : null;
    }

    private BooleanExpression priceLoe(Integer maxPrice) {
        return maxPrice != null ? product.price.loe(maxPrice) : null;
    }

    private BooleanExpression flashSaleEq(Boolean flashSale) {
        return flashSale != null ? product.flashSale.eq(flashSale) : null;
    }

    private BooleanExpression inStockCondition(Boolean inStock) {
        if (inStock == null) return null;
        return inStock ? product.stock.gt(0) : product.stock.loe(0);
    }
}
