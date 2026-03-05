/*
 * Decompiled with CFR 0.152.
 */
package ru.rizonchik.refontsocial.libs.hikari.pool;

import java.sql.CallableStatement;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyConnection;
import ru.rizonchik.refontsocial.libs.hikari.pool.ProxyPreparedStatement;

public abstract class ProxyCallableStatement
extends ProxyPreparedStatement
implements CallableStatement {
    protected ProxyCallableStatement(ProxyConnection connection, CallableStatement statement) {
        super(connection, statement);
    }
}

