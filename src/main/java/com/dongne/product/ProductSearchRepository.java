package com.dongne.product;

import com.dongne.common.CursorPage;

public interface ProductSearchRepository {

	CursorPage<ProductSummary> search(ProductSearchCondition condition);
}
