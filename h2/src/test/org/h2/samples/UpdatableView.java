/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.samples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import org.h2.tools.TriggerAdapter;

/**
 * This sample application shows how to use triggers to create updatable views.
 */
public class UpdatableView extends TriggerAdapter {

    private PreparedStatement prepDelete, prepInsert;

    /**
     * This method is called when executing this sample application from the
     * command line.
     *
     * @param args ignored
     * @throws Exception on failure
     */
    public static void main(String... args) throws Exception {
        Class.forName("org.h2.Driver");
        try (Connection conn = DriverManager.getConnection("jdbc:h2:mem:")) {
            Statement stat;
            stat = conn.createStatement();

            // Create the table TEST_TABLE and the view TEST_VIEW that simply
            // selects everything from the TEST_TABLE.
            stat.execute("CREATE TABLE TEST_TABLE"
                    + "(ID BIGINT GENERATED BY DEFAULT AS IDENTITY DEFAULT ON NULL PRIMARY KEY, NAME VARCHAR)");
            stat.execute("CREATE VIEW TEST_VIEW AS TABLE TEST_TABLE");

            // Create the INSTEAD OF trigger that is called whenever the data in
            // the view is modified. This trigger makes the view updatable.
            stat.execute(
                    "CREATE TRIGGER T_TEST_VIEW INSTEAD OF INSERT, UPDATE, DELETE ON TEST_VIEW FOR EACH ROW CALL \""
                            + UpdatableView.class.getName() + '"');

            // Test an INSERT operation and check that generated keys from the
            // source table are returned as expected.
            stat.execute("INSERT INTO TEST_VIEW(NAME) VALUES 'Hello', 'World'", new String[] { "ID" });
            try (ResultSet rs = stat.getGeneratedKeys()) {
                while (rs.next()) {
                    System.out.printf("Key %d was generated%n", rs.getLong(1));
                }
            }
            System.out.println();
            // Test UPDATE and DELETE operations.
            stat.execute("UPDATE TEST_VIEW SET NAME = 'Hallo' WHERE ID = 1");
            stat.execute("DELETE FROM TEST_VIEW WHERE ID = 2");

            // Print the contents of the table and the view, they should be the
            // same.
            System.out.println("TEST_TABLE:");
            try (ResultSet rs = stat.executeQuery("TABLE TEST_TABLE")) {
                while (rs.next()) {
                    System.out.printf("%d %s%n", rs.getLong(1), rs.getString(2));
                }
            }
            System.out.println();
            System.out.println("TEST_VIEW:");
            try (ResultSet rs = stat.executeQuery("TABLE TEST_VIEW")) {
                while (rs.next()) {
                    System.out.printf("%d %s%n", rs.getLong(1), rs.getString(2));
                }
            }
        }
    }

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before,
            int type) throws SQLException {
        prepDelete = conn.prepareStatement("DELETE FROM TEST_TABLE WHERE ID = ?");
        // INSERT and UPDATE triggers should return the FINAL values of the row.
        // Table TEST_TABLE has a generated column, so the FINAL row can be
        // different from the row that we try to insert here.
        prepInsert = conn.prepareStatement("SELECT * FROM FINAL TABLE(INSERT INTO TEST_TABLE VALUES (?, ?))");
        super.init(conn, schemaName, triggerName, tableName, before, type);
    }

    @Override
    public void fire(Connection conn, ResultSet oldRow, ResultSet newRow) throws SQLException {
        if (oldRow != null && oldRow.next()) {
            prepDelete.setLong(1, oldRow.getLong(1));
            prepDelete.execute();
        }
        if (newRow != null && newRow.next()) {
            long id = newRow.getLong(1);
            if (newRow.wasNull()) {
                prepInsert.setNull(1, Types.BIGINT);
            } else {
                prepInsert.setLong(1, id);
            }
            prepInsert.setString(2, newRow.getString(2));
            // Now we need to execute the INSERT statement and update the newRow
            // with the FINAL values.
            // It is necessary for the FINAL TABLE and getGeneratedKeys(); if we
            // don't update the newRow, the FINAL TABLE will work like the NEW
            // TABLE.
            // It is only necessary when the source table has generated columns
            // or other columns with default values, or it has a trigger that
            // can change the inserted values; without such columns the NEW
            // TABLE and the FINAL TABLE are the same.
            try (ResultSet rs = prepInsert.executeQuery()) {
                rs.next();
                newRow.updateLong(1, rs.getLong(1));
                newRow.updateString(2, rs.getString(2));
                newRow.rowUpdated();
            }
        }
    }

    @Override
    public void close() throws SQLException {
        prepInsert.close();
        prepDelete.close();
    }

}
