/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.seata.rm.datasource.undo.postgresql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import io.seata.common.loader.LoadLevel;
import io.seata.core.compressor.CompressorType;
import io.seata.core.constants.ClientTableColumnsName;
import io.seata.rm.datasource.undo.AbstractUndoLogManager;
import io.seata.rm.datasource.undo.UndoLogParser;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@LoadLevel(name = JdbcConstants.POSTGRESQL)
public class PostgresqlUndoLogManager extends AbstractUndoLogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlUndoLogManager.class);

    private static final String INSERT_UNDO_LOG_SQL = "INSERT INTO " + UNDO_LOG_TABLE_NAME +
            " (" + ClientTableColumnsName.UNDO_LOG_ID + "," + ClientTableColumnsName.UNDO_LOG_BRANCH_XID + ", "
            + ClientTableColumnsName.UNDO_LOG_XID + ", " + ClientTableColumnsName.UNDO_LOG_CONTEXT + ", "
            + ClientTableColumnsName.UNDO_LOG_ROLLBACK_INFO + ", " + ClientTableColumnsName.UNDO_LOG_LOG_STATUS + ", "
            + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + ", " + ClientTableColumnsName.UNDO_LOG_LOG_MODIFIED + ")"
            + "VALUES (nextval('undo_log_id_seq'), ?, ?, ?, ?, ?, now(), now())";

    private static final String DELETE_UNDO_LOG_BY_CREATE_SQL = "DELETE FROM " + UNDO_LOG_TABLE_NAME + " WHERE "
            + ClientTableColumnsName.UNDO_LOG_ID + " IN ("
            + "SELECT " + ClientTableColumnsName.UNDO_LOG_ID + " FROM " + UNDO_LOG_TABLE_NAME
            + " WHERE " + ClientTableColumnsName.UNDO_LOG_LOG_CREATED + " <= ? LIMIT ?" + ")";

    @Override
    public int deleteUndoLogByLogCreated(Date logCreated, int limitRows, Connection conn) throws SQLException {
        PreparedStatement deletePST = null;
        try {
            deletePST = conn.prepareStatement(DELETE_UNDO_LOG_BY_CREATE_SQL);
            deletePST.setDate(1, new java.sql.Date(logCreated.getTime()));
            deletePST.setInt(2, limitRows);
            int deleteRows = deletePST.executeUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("batch delete undo log size " + deleteRows);
            }
            return deleteRows;
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        } finally {
            if (deletePST != null) {
                deletePST.close();
            }
        }
    }

    @Override
    protected void insertUndoLogWithNormal(String xid, long branchID, String rollbackCtx, byte[] undoLogContent,
                                           Connection conn) throws SQLException {
        insertUndoLog(xid, branchID, rollbackCtx, undoLogContent, State.Normal, conn);
    }

    @Override
    protected void insertUndoLogWithGlobalFinished(String xid, long branchId, UndoLogParser parser,
        Connection conn) throws SQLException {
        insertUndoLog(xid, branchId, buildContext(parser.getName(), CompressorType.NONE), parser.getDefaultContent(),
                State.GlobalFinished, conn);
    }

    private void insertUndoLog(String xid, long branchID, String rollbackCtx, byte[] undoLogContent,
                               State state, Connection conn) throws SQLException {
        PreparedStatement pst = null;
        try {
            pst = conn.prepareStatement(INSERT_UNDO_LOG_SQL);
            pst.setLong(1, branchID);
            pst.setString(2, xid);
            pst.setString(3, rollbackCtx);
            pst.setBytes(4, undoLogContent);
            pst.setInt(5, state.getValue());
            pst.executeUpdate();
        } catch (Exception e) {
            if (!(e instanceof SQLException)) {
                e = new SQLException(e);
            }
            throw (SQLException) e;
        } finally {
            if (pst != null) {
                pst.close();
            }
        }
    }
}
