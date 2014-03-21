package com.nguyenmp.csil.daos;

import com.nguyenmp.csil.things.Computer;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ComputersDAO implements DAO<Computer> {
    private final Connection mConn;

    private static final String CREATE_QUERY =
            "CREATE TABLE IF NOT EXISTS Computers ( " +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "hostname TEXT, " +
                    "ip_address TEXT, " +
                    "is_active BOOLEAN, " +
                    "UNIQUE (ip_address), " +
                    "UNIQUE (hostname) " +
            ")";

    /**
     * Creates the Computers table if it doesn't exist.
     * @param conn The SQL Connection to execute all queries over
     * @throws SQLException yeah...
     */
    public ComputersDAO(final Connection conn) throws SQLException {
        mConn = conn;

        Statement statement = mConn.createStatement();
        statement.execute(CREATE_QUERY);
        statement.close();
    }

    /**
     * Extracts the hostname, ipAddress, and isActive properties from the
     * given computer and inserts that into the database.
     * @param computer the bean to extract the information from.
     * @throws SQLException if an exception (like duplicate hostname or
     * ip address) is encountered.
     */
    public void addComputer(Computer computer) throws SQLException {
        PreparedStatement statement = mConn.prepareStatement("" +
                "INSERT INTO Computers " +
                "(hostname, ip_address, is_active) " +
                "VALUES " +
                "(?, ?, ?)");
        statement.setString(1, computer.hostname);
        statement.setString(2, computer.ipAddress);
        statement.setBoolean(3, computer.isActive);
        statement.execute();
        statement.close();
    }

    /**
     *
     * @param hostname the hostname of the computer to set the active status as.  if the given
     *                 hostname does not exist in the database, nothing happens.
     * @param isActive the value to set into the database for this hostname
     * @throws SQLException when an SQLException is encountered.
     */
    public void setActiveByHostname(String hostname, boolean isActive) throws SQLException {
        PreparedStatement statement = mConn.prepareStatement("" +
                "UPDATE Computers SET is_active=? " +
                "WHERE hostname=?");
        statement.setBoolean(1, isActive);
        statement.setString(2, hostname);
        statement.execute();
        statement.close();
    }

    /**
     * Inserts all the given computers into the database.  Internally,
     * this command batches all the insert commands together to optimize
     * SQL insertions.  Since databases tend to optimize on transactions and
     * not individual commands, inserting a large set information into the
     * database as a batch in a single transaction really speeds things up.
     * @param computers the set of the beans to extract information
     *                  from (hostname, ip_address, is_active)
     * @throws SQLException yeah...
     */
    public void addComputers(Collection<Computer> computers) throws SQLException {
        // Disable autocommit so we can run a batch insert
        // Running batch jobs are faster than individual jobs
        mConn.setAutoCommit(false);

        PreparedStatement statement = mConn.prepareStatement("" +
                "INSERT INTO Computers " +
                "(hostname, ip_address, is_active) " +
                "VALUES " +
                "(?, ?, ?)");

        for (Computer computer : computers) {
            statement.setString(1, computer.hostname);
            statement.setString(2, computer.ipAddress);
            statement.setBoolean(3, computer.isActive);
            statement.execute();
        }

        mConn.commit();

        statement.close();
        mConn.setAutoCommit(true);
    }

    /**
     * Retrieves only the computers marked as active
     * @return a list of active computers
     * @throws SQLException
     */
    public List<Computer> getActiveComputers() throws SQLException {
        return getComputers(true);
    }

    /**
     * Retrieves all computers (without discerning between active and
     * non-active.
     * @return a list of all computers in the CSIL subnet
     * @throws SQLException
     */
    public List<Computer> getAllComputers() throws SQLException {
        return getComputers(false);
    }

    /**
     * Gets a list of computers in our database
     * @param filterByActive true if you want to only get active computers,
     *                       false to return all computers including inactive ones
     * @return the list of computers specified by the filter from our database
     * @throws SQLException
     */
    public List<Computer> getComputers(boolean filterByActive) throws SQLException {
        // Query the database for the list of computers
        final Statement statement = mConn.createStatement();
        final String query = "SELECT * FROM Computers " + (filterByActive ? " WHERE is_active=1" : "");
        final ResultSet resultSet = statement.executeQuery(query);

        // This is the Plain Old Java Object (POJO) that
        // contains all the computers in our database.  We
        // need to initialize it though.
        final List<Computer> result = new ArrayList<Computer>();

        // For every row returned from the query for all Computers
        while (resultSet.next()) {
            // Parse the result as a Computer
            Computer computer = parseResultSet(resultSet);
            result.add(computer);
        }

        // Do cleanup!
        statement.close();

        // Return the list of computers we scrapped
        return result;
    }

    @Override
    public Computer parseResultSet(final ResultSet resultSet) throws SQLException {
        // Creates a new Computer bean
        final Computer computer = new Computer();

        // Initializes the bean's values from the result set
        computer.id = resultSet.getInt("id");
        computer.hostname = resultSet.getString("hostname");
        computer.ipAddress = resultSet.getString("ip_address");
        computer.isActive = resultSet.getBoolean("is_active");

        // Returns the initialized bean
        return computer;
    }
}
