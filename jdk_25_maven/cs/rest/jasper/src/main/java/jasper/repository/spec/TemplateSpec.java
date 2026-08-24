package jasper.repository.spec;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jasper.domain.Template;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;

import static jasper.repository.spec.SortSpec.createJsonbSortExpression;
import static jasper.repository.spec.SortSpec.createOriginNestingExpression;
import static jasper.repository.spec.SortSpec.createTagLevelsExpression;
import static jasper.repository.spec.SortSpec.isJsonbSortProperty;

public class TemplateSpec {

	/**
	 * Creates a Specification with sorting applied based on the PageRequest's sort orders.
	 * JSONB field sort columns are rewritten as JPA Specification orderBy clauses.
	 * Supports config->, defaults->, schema->, origin:len, and tag:len sorting.
	 * Uses COALESCE to handle nulls (0 for numeric/length, '' for string).
	 *
	 * @param spec the base specification to add sorting to
	 * @param pageable the page request containing sort orders
	 * @return a new Specification with sorting applied for all fields
	 */
	public static Specification<Template> sort(Specification<Template> spec, Pageable pageable) {
		if (pageable == null || pageable.getSort().isUnsorted()) {
			return spec;
		}
		var orders = pageable.getSort().toList();
		return spec.and((root, query, cb) -> {
			if (query.getResultType() == Long.class || query.getResultType() == long.class) {
				return null; // Don't apply ordering to count queries
			}
			var jpaOrders = new ArrayList<Order>();
			for (Sort.Order order : orders) {
				var property = order.getProperty();
				var ascending = order.isAscending();
				Expression<?> expr;
				if (isJsonbSortProperty(property, "config", "defaults", "schema")) {
					expr = createJsonbSortExpression(root, cb, property, "config", "defaults", "schema");
				} else if (property.equals("origin:len")) {
					expr = createOriginNestingExpression(root, cb);
				} else if (property.equals("tag:len")) {
					expr = createTagLevelsExpression(root, cb);
				} else {
					expr = root.get(property);
				}
				if (expr != null) jpaOrders.add(ascending ? cb.asc(expr) : cb.desc(expr));
			}
			if (!jpaOrders.isEmpty()) query.orderBy(jpaOrders);
			return null;
		});
	}
}
