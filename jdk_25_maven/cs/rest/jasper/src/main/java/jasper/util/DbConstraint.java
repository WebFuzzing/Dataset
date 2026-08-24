package jasper.util;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Utility for identifying database constraint violations across different
 * database backends (PostgreSQL and SQLite).
 *
 * PostgreSQL reports constraint names via ConstraintViolationException.
 * SQLite reports constraint violations via error messages with column names.
 */
public class DbConstraint {

	/**
	 * Check if the exception (or its cause chain) represents a primary key
	 * violation for the given table. The PK columns differ per table:
	 * ref=(url, origin), ext/plugin/template/users=(tag, origin).
	 */
	public static boolean isPkViolation(Throwable e, String table) {
		var firstCol = "ref".equals(table) ? "url" : "tag";
		return isConstraint(e, table + "_pkey",
			table + "." + firstCol + ", " + table + ".origin");
	}

	/**
	 * Check if the exception (or its cause chain) represents a unique constraint
	 * violation on (modified, origin) for the given table.
	 */
	public static boolean isUniqueModifiedOriginViolation(Throwable e, String table) {
		return isConstraint(e,
			table + "_modified_origin_key",
			table + ".modified, " + table + ".origin");
	}

	private static boolean isConstraint(Throwable e, String pgConstraintName, String sqliteColumns) {
		if (e == null) return false;
		// Walk the full cause chain
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (matchesConstraint(t, pgConstraintName, sqliteColumns)) return true;
		}
		return false;
	}

	private static boolean matchesConstraint(Throwable t, String pgConstraintName, String sqliteColumns) {
		if (t == null) return false;
		// PostgreSQL: ConstraintViolationException with named constraint
		if (t instanceof ConstraintViolationException c) {
			if (pgConstraintName.equals(c.getConstraintName())) return true;
		}
		// SQLite: error message contains "UNIQUE constraint failed: <columns>"
		String msg = t.getMessage();
		if (msg != null && msg.contains("UNIQUE constraint failed: " + sqliteColumns)) return true;
		return false;
	}
}
