package jasper.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.sqlite.Function;
import org.sqlite.SQLiteConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Registers custom SQLite functions that emulate PostgreSQL JSONB functions
 * used in native queries. Only active when the "sqlite" profile is enabled.
 *
 * Wraps the DataSource via BeanPostProcessor so that UDFs are registered on
 * every connection obtained from the pool, surviving connection recycling.
 */
@Configuration
@Profile("sqlite")
public class SQLiteConfig implements BeanPostProcessor {
	private static final Logger logger = LoggerFactory.getLogger(SQLiteConfig.class);
	private static final ObjectMapper om = new ObjectMapper();

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof DataSource ds && !(bean instanceof SQLiteUdfDataSource)) {
			logger.info("Wrapping DataSource to register SQLite UDFs on every connection");
			return new SQLiteUdfDataSource(ds);
		}
		return bean;
	}

	/**
	 * DataSource wrapper that registers custom SQLite UDFs on every connection
	 * obtained from the pool. Function.create is idempotent, so re-registering
	 * on reused pooled connections is safe.
	 */
	private static class SQLiteUdfDataSource extends DelegatingDataSource {
		SQLiteUdfDataSource(DataSource delegate) {
			super(delegate);
		}

		@Override
		public Connection getConnection() throws SQLException {
			var conn = super.getConnection();
			registerFunctionsOnConnection(conn);
			return conn;
		}

		@Override
		public Connection getConnection(String username, String password) throws SQLException {
			var conn = super.getConnection(username, password);
			registerFunctionsOnConnection(conn);
			return conn;
		}
	}

	/**
	 * Register all custom functions on the given connection.
	 */
	static void registerFunctionsOnConnection(Connection conn) throws SQLException {
		var sqliteConn = conn.unwrap(SQLiteConnection.class);
		registerJsonbExists(sqliteConn);
	}

	/**
	 * Registers jsonb_exists(json, key) function for SQLite.
	 * For JSON arrays: returns true if the array contains the key as a value.
	 * For JSON objects: returns true if the object has the key as a field name.
	 */
	private static void registerJsonbExists(SQLiteConnection conn) throws SQLException {
		Function.create(conn, "jsonb_exists", new Function() {
			@Override
			protected void xFunc() throws SQLException {
				if (args() < 2) {
					result(0);
					return;
				}
				var json = value_text(0);
				var key = value_text(1);
				if (json == null || key == null) {
					result(0);
					return;
				}
				try {
					var node = om.readTree(json);
					if (node.isArray()) {
						for (var elem : node) {
							if (elem.isTextual() && elem.asText().equals(key)) {
								result(1);
								return;
							}
						}
					} else if (node.isObject()) {
						if (node.has(key)) {
							result(1);
							return;
						}
					}
					result(0);
				} catch (Exception e) {
					result(0);
				}
			}
		});
	}
}
