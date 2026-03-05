/*
 * Decompiled with CFR 0.152.
 */
package ru.rizonchik.refontsocial.libs.hikari.pool;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyConnection;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyFactory;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyStatement;

public abstract class ProxyPreparedStatement
extends ProxyStatement
implements PreparedStatement {
    ProxyPreparedStatement(ProxyConnection connection, PreparedStatement statement) {
        super(connection, statement);
    }

    @Override
    public boolean execute() throws SQLException {
        this.connection.markCommitStateDirty();
        return ((PreparedStatement)this.delegate).execute();
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        this.connection.markCommitStateDirty();
        ResultSet resultSet = ((PreparedStatement)this.delegate).executeQuery();
        return ProxyFactory.getProxyResultSet(this.connection, this, resultSet);
    }

    @Override
    public int executeUpdate() throws SQLException {
        this.connection.markCommitStateDirty();
        return ((PreparedStatement)this.delegate).executeUpdate();
    }

    @Override
    public long executeLargeUpdate() throws SQLException {
        this.connection.markCommitStateDirty();
        return ((PreparedStatement)this.delegate).executeLargeUpdate();
    }
}

