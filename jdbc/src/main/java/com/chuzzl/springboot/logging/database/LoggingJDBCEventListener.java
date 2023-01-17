package com.chuzzl.springboot.logging.database;

import com.chuzzl.springboot.logging.api.MessageFormatter;
import com.p6spy.engine.common.ConnectionInformation;
import com.p6spy.engine.common.ResultSetInformation;
import com.p6spy.engine.common.StatementInformation;
import com.p6spy.engine.logging.LoggingEventListener;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LoggingJDBCEventListener extends LoggingEventListener {
    private final Logger log = Logger.getLogger(LoggingJDBCEventListener.class.getName());
    private ThreadLocal<List<String>> resultList = new ThreadLocal<>();
    private MessageFormatter messageFormatter;

    public LoggingJDBCEventListener(MessageFormatter messageFormatter) {
        this.messageFormatter = messageFormatter;
    }

    @Override
    public void onAfterAnyExecute(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
        log.info(messageFormatter.format(new DatabaseMessage(statementInformation.getSqlWithValues())));
    }

    @Override
    public void onAfterExecuteBatch(StatementInformation statementInformation, long timeElapsedNanos, int[] updateCounts, SQLException e) {
    }

    @Override
    public void onAfterCommit(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onAfterRollback(ConnectionInformation connectionInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onAfterAnyAddBatch(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onAfterGetResultSet(StatementInformation statementInformation, long timeElapsedNanos, SQLException e) {
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, int columnIndex, Object value, SQLException e) {
        super.onAfterResultSetGet(resultSetInformation,columnIndex,value,e);
    }

    @Override
    public void onAfterResultSetGet(ResultSetInformation resultSetInformation, String columnLabel, Object value, SQLException e) {
        super.onAfterResultSetGet(resultSetInformation,columnLabel,value,e);
    }

    @Override
    public void onBeforeResultSetNext(ResultSetInformation resultSetInformation) {
        if (resultSetInformation.getCurrRow() > -1) {
            resultList.get().add(resultSetInformation.getSqlWithValues());
        } else {
            resultList.set(new ArrayList<>());
        }
    }

    @Override
    public void onAfterResultSetNext(ResultSetInformation resultSetInformation, long timeElapsedNanos, boolean hasNext, SQLException e) {
    }

    @Override
    public void onAfterResultSetClose(ResultSetInformation resultSetInformation, SQLException e) {
        log.info(messageFormatter.format(new DatabaseMessage(resultList.get().toString(),resultList.get().size())));
    }
}
