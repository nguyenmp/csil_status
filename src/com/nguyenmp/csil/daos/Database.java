package com.nguyenmp.csil.daos;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A wrapper class that automatically initializes the database and
 * the connection to the database.  It contains member variables
 * that are the Data Access Objects (DAOs) that abstract all
 * interactions to the database.
 *
 * Each DAO is passed the database connection and each DAO is
 * responsible for properly closing their statements and
 * leaving the connection open.  No DAO should close the connection
 * their own.
 *
 * An instance of this Database object is an instance of a connection
 * to the database and all the DAOs accessible from this instance
 * connect to the same database over the same connection.
 *
 * These DAOs are accessible as public member variables of the
 * Database class and are final parameters.
 */
public class Database {

    /** The Data Access Object (DAO) that provides an interface
     * to the computers listed in the database. This instance
     * is bound to the connection created at construction by
     * {@link com.nguyenmp.csil.daos.Database}.*/
    public final ComputersDAO computers;

    /**
     * Just like calling the {@link #Database(java.io.File)} constructor
     * with a null parameter.
     * @throws SQLException if a database access error occurs or a DAO
     * encountered an error during construction.
     * @throws ClassNotFoundException if the class "org.sqlite.JDBC" cannot
     * be located.  This is most likely an issue with not including the
     * required libraries that handle the Java Database Connection.
     */
    public Database() throws SQLException, ClassNotFoundException {
        this(null);
    }

    /**
     * Creates a new instance of the {@link com.nguyenmp.csil.daos.Database}
     * class.  Internally, this creates a single {@link java.sql.Connection}
     * to the given database and initializes all of its respective Data Access
     * Objects (DAOs) with this single connection.
     * @param databasePath The path to the SQLite database file to either create
     *                     or read from.  If the file does not exist at the given
     *                     path, a new file will be created and initialized.  The
     *                     DAOs should have initialization code to create tables
     *                     and insert default values.
     * @throws ClassNotFoundException if the class "org.sqlite.JDBC" cannot
     * be located.  This is most likely an issue with not including the
     * required libraries that handle the Java Database Connection.
     * @throws SQLException if a database access error occurs or a DAO
     * encountered an error during construction.
     */
    public Database(File databasePath)
            throws ClassNotFoundException, SQLException {

        // Default the path to "./csil_top.db" if it is null
        if (databasePath == null) databasePath = new File("./csil_top.db");

        // Loads the java database connector
        Class.forName("org.sqlite.JDBC");

        // Create a database connection
        String connectionURL = "jdbc:sqlite:" + databasePath.getPath();
        Connection conn = DriverManager.getConnection(connectionURL);

        // Initialize DAOs
        computers = new ComputersDAO(conn);
    }
}
