package com.dongne.product;

import com.dongne.common.CursorPage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 목록 검색은 네이티브 SQL 로 작성한다.
 * - ST_DWithin(geography) 는 JPQL 에 없다.
 * - 키셋 커서의 행 비교 (bumped_at, id) < (?, ?) 는 인덱스를 그대로 타는 Postgres 문법이다.
 * 파라미터는 전부 바인딩하므로 문자열 조립이어도 인젝션은 불가능하다.
 */
public class ProductSearchRepositoryImpl implements ProductSearchRepository {

	@PersistenceContext
	private EntityManager em;

	@Override
	public CursorPage<ProductSummary> search(ProductSearchCondition c) {
		boolean hasLocation = c.latitude() != null && c.longitude() != null && c.radiusKm() != null;
		Map<String, Object> params = new HashMap<>();
		StringBuilder sql = new StringBuilder("""
				SELECT p.id, p.title, p.price, p.category, p.status, p.region_name,
				       (SELECT i.url FROM product_images i WHERE i.product_id = p.id ORDER BY i.sort_order LIMIT 1) AS thumbnail,
				       p.like_count, p.chat_count, p.view_count, p.bumped_at,
				""");
		if (hasLocation) {
			sql.append("       ST_Distance(p.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography) AS distance\n");
			params.put("lng", c.longitude());
			params.put("lat", c.latitude());
		} else {
			sql.append("       NULL::double precision AS distance\n");
		}
		sql.append("FROM products p WHERE 1 = 1\n");

		if (hasLocation) {
			sql.append("  AND ST_DWithin(p.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :meters)\n");
			params.put("meters", c.radiusKm() * 1000.0);
		}
		if (!c.includeSold()) {
			sql.append("  AND p.status <> 'SOLD'\n");
		}
		if (c.category() != null) {
			sql.append("  AND p.category = :category\n");
			params.put("category", c.category().name());
		}
		if (c.keyword() != null && !c.keyword().isBlank()) {
			sql.append("  AND p.title ILIKE :keyword\n");
			params.put("keyword", "%" + c.keyword().trim() + "%");
		}
		if (c.cursor() != null && !c.cursor().isBlank()) {
			Cursor.Key key = Cursor.decode(c.cursor());
			sql.append("  AND (p.bumped_at, p.id) < (:cursorTs, :cursorId)\n");
			params.put("cursorTs", Timestamp.valueOf(key.bumpedAt()));
			params.put("cursorId", key.id());
		}
		sql.append("ORDER BY p.bumped_at DESC, p.id DESC\nLIMIT :limit");
		params.put("limit", c.size() + 1);   // hasNext 판단용 1건 더

		Query query = em.createNativeQuery(sql.toString(), Tuple.class);
		params.forEach(query::setParameter);

		@SuppressWarnings("unchecked")
		List<Tuple> rows = query.getResultList();
		List<ProductSummary> fetched = rows.stream().map(ProductSearchRepositoryImpl::toSummary).toList();
		return CursorPage.of(fetched, c.size(), s -> Cursor.encode(s.bumpedAt(), s.id()));
	}

	/** JDBC 드라이버/Hibernate 버전에 따라 Timestamp 또는 LocalDateTime 으로 온다 */
	private static java.time.LocalDateTime toLocalDateTime(Object v) {
		if (v instanceof java.time.LocalDateTime ldt) return ldt;
		if (v instanceof Timestamp ts) return ts.toLocalDateTime();
		throw new IllegalStateException("지원하지 않는 시간 타입: " + v.getClass());
	}

	private static ProductSummary toSummary(Tuple t) {
		Object distance = t.get("distance");
		return new ProductSummary(
				((Number) t.get("id")).longValue(),
				(String) t.get("title"),
				((Number) t.get("price")).longValue(),
				(String) t.get("category"),
				(String) t.get("status"),
				(String) t.get("region_name"),
				(String) t.get("thumbnail"),
				((Number) t.get("like_count")).intValue(),
				((Number) t.get("chat_count")).intValue(),
				((Number) t.get("view_count")).longValue(),
				distance == null ? null : ((Number) distance).doubleValue(),
				toLocalDateTime(t.get("bumped_at")));
	}
}
