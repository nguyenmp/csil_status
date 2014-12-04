package com.nguyenmp.csil.concurrency;


import com.nguyenmp.csil.Credentials;
import com.nguyenmp.csil.daos.ComputersDAO;

import java.sql.SQLException;

/**
 * A {@link com.nguyenmp.csil.concurrency.CommandExecutor} that only establishes
 * the initial SSH connection to the targeted computer and then closes that
 * connection.  The idea behind this class is that it determines if the username
 * and password are authorized to SSH into the given computer.
 */
public class ComputerTester extends CommandExecutor {
    private final ComputersDAO computersDAO;
    private final String hostname;

    /**
     * Creates a new ComputerTester instance that connects to the given hostname
     * and updates the given ComputersDAO with whether or not the
     * connection was successful.
     * @param hostname the hostname of the computer to connect to
     * @param computersDAO the ComputersDAO of the database to update on success/failure
     */
    public ComputerTester(String hostname, ComputersDAO computersDAO) {
        super(Credentials.username(), Credentials.password(), hostname, "tmux new -s foo -d && tmux send-keys -t foo 'java -jar ~/monitor/target/monitor-0.0-SNAPSHOT.jar' C-m");
        this.computersDAO = computersDAO;
        this.hostname = hostname;
    }

    @Override
    public void onSuccess(String result) {
        setActive(true);
    }

    @Override
    public void onError(Exception e) {
        setActive(false);
    }

    /** Sets the hostname's is active parameter in the database to the given param */
    private void setActive(boolean isActive) {
        System.out.println(hostname + " is " + isActive);
        try {
            computersDAO.setActiveByHostname(hostname, isActive);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
