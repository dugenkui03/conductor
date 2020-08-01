package com.netflix.conductor.sql;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * "在事务上下文中进行操作的功能接口"
 *
 * Functional interface for operations within a transactional context.
 */
@FunctionalInterface
public interface TransactionalFunction<R> {
	R apply(Connection tx) throws SQLException;
}
