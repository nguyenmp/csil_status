package com.nguyenmp.csil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nguyenmp.csil.concurrency.ComputerTester;
import com.nguyenmp.csil.daos.ComputersDAO;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Topper {

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, JSchException {
        Class.forName("org.sqlite.JDBC");

        Database database = new Database();

        // create a database connection
        initializeComputers(database.computers);

        testComputers(database.computers);

        List<Computer> activeComputers = database.computers.getActiveComputers();
        for (Computer computer : activeComputers) System.out.println(computer);


    }

    private static void testComputers(ComputersDAO computersDAO) throws SQLException {
        // set up multi-threadedness
        int logicalProcessors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(logicalProcessors * 3);

        List<Computer> computers = computersDAO.getAllComputers();

        for (Computer computer : computers) {
            ComputerTester tester = new ComputerTester(computer.hostname, computersDAO);
            executor.execute(tester);
        }

        try {
            executor.shutdown();
            executor.awaitTermination(99999, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void initializeComputers(ComputersDAO computersDAO) throws JSchException, SQLException, IOException {
        initializeComputers(computersDAO, "csil.cs.ucsb.edu");
		try {
			initializeComputers(computersDAO, "linux01.engr.ucsb.edu");
		} catch (JSchException ex) {
			// If you can establish an SSH connection, but can't actually log
			// on, catch this error.  For example, having a default shell not
			// installed on the ECI CentOS machines (such as Zsh).
		}
    }

    public static void initializeComputers(ComputersDAO computersDAO, String address) throws SQLException, JSchException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(Credentials.USERNAME, address);
        session.setPassword(Credentials.PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("cat  /etc/hosts");

        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));

        channel.connect();

        Collection<Computer> computers = new ArrayList<Computer>();
        String line;
        while ((line = reader.readLine()) != null) {
            String[] lines = line.split("\n");
            for (String resultRow : lines) {
                if ((resultRow.startsWith("128.111.43") || resultRow.contains("linux"))&& !resultRow.contains(" ")) {
                    String[] parts = resultRow.split("\t");
                    String ip_address = parts[0];
                    String hostname = parts[1];

                    Computer computer = new Computer();
                    computer.hostname = hostname;
                    computer.ipAddress = ip_address;
                    computers.add(computer);

                    System.out.println(ip_address);
                }
            }
        }

        computersDAO.addComputers(computers);

        System.out.println("Done Writing.");

        channel.disconnect();
    }
}
