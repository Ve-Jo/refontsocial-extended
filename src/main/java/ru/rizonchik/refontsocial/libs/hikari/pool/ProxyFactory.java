/*
 * Decompiled with CFR 0.152.
 */
package ru.rizonchik.refontsocial.libs.hikari.pool;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import ru.rizonchik.refontsocial.libs.hikari.pool.HikariProxyCallableStatement;
import ru.rizonchik.refontsocial.libs.hikari.pool.HikariProxyConnection;
import ru.rizonchik.refontsocial.libs.hikari.pool.HikariProxyDatabaseMetaData;
import ru.rizonchik.refontsocial.libs.hikari.pool.HikariProxyPreparedStatement;
import ru.rizonchik.refontsocial.libs.hikari.pool.HikariProxyResultSet;
import ru.rizonchik.refontsocial.libs.hikari.pool.HikariProxyStatement;
import ru.rizonchik.refontsocial.libs.hikari.pool.PoolEntry;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyConnection;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyLeakTask;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyStatement;
import ru.rizonchik.refontsocial.libs.hikari.util.FastList;

public final class ProxyFactory {
    private ProxyFactory() {
    }

    static ProxyConnection getProxyConnection(PoolEntry poolEntry, Connection connection, FastList<Statement> fastList, ProxyLeakTask proxyLeakTask, long l, boolean bl, boolean bl2) {
        return new HikariProxyConnection(poolEntry, connection, (FastList)fastList, proxyLeakTask, l, bl, bl2);
    }

    static Statement getProxyStatement(ProxyConnection proxyConnection, Statement statement) {
        return new HikariProxyStatement(proxyConnection, statement);
    }

    static CallableStatement getProxyCallableStatement(ProxyConnection proxyConnection, CallableStatement callableStatement) {
        return new HikariProxyCallableStatement(proxyConnection, callableStatement);
    }

    static PreparedStatement getProxyPreparedStatement(ProxyConnection proxyConnection, PreparedStatement preparedStatement) {
        return new HikariProxyPreparedStatement(proxyConnection, preparedStatement);
    }

    static ResultSet getProxyResultSet(ProxyConnection proxyConnection, ProxyStatement proxyStatement, ResultSet resultSet) {
        return new HikariProxyResultSet(proxyConnection, proxyStatement, resultSet);
    }

    static DatabaseMetaData getProxyDatabaseMetaData(ProxyConnection proxyConnection, DatabaseMetaData databaseMetaData) {
        return new HikariProxyDatabaseMetaData(proxyConnection, databaseMetaData);
    }
}

