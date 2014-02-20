package com.nguyenmp.csil;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;

public class DBInitializer {

    public static void main(String[] args) throws IOException, JSchException, ClassNotFoundException, SQLException {
        if (args.length < 1) improperUsage();
        String pathToFile = args[0];
        String initialAddress = args.length < 2 ? "csil.cs.ucsb.edu" : args[2];


        Class.forName("org.sqlite.JDBC");

        // create a database connection
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + pathToFile);
        initializeComputers(connection, initialAddress);
        testComputers(connection);
    }

    private static void improperUsage() {
        System.out.println("Usage: java DBInitializer pathToDBFile [initialConnection]");
        System.out.println("\tPath to file is a String that defines the file path that the SQLite DB to initialize");
        System.out.println("\tinitialConnection is an optional field that takes in a " +
                "hostname or IP Address to the first computer to harvest the /etc/hosts file form.");
        System.exit(1);
    }

    public static void testComputers(Connection connection) throws SQLException, JSchException, IOException {
        Statement statement = connection.createStatement();
        ResultSet computers = statement.executeQuery("SELECT * FROM Computer WHERE is_active = \'\'");

        int i = 0;
        while (i++ < 341) computers.next();
        PreparedStatement preparedStatement = connection.prepareStatement("UPDATE Computer SET is_active=? WHERE ip_address=?");
        while (computers.next()) {

            String hostname = computers.getString("hostname");
            String ip_address = computers.getString("ip_address");
            String is_active = computers.getString("is_active");

            if (is_active != null && !is_active.equals("")) continue;

            preparedStatement.setString(2, ip_address);

            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(Credentials.USERNAME, hostname);
                session.setPassword(Credentials.PASSWORD);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(5000);
                session.connect();
                session.disconnect();
                System.out.println("Connected to " + hostname + " at " + ip_address);
                preparedStatement.setString(1, "true");
            } catch (JSchException e) {
                System.out.println("Failed with " + hostname + " at " + ip_address);
                //e.printStackTrace();
                preparedStatement.setString(1, "false");
            }

            boolean result = preparedStatement.execute();
        }
    }

    public static void initializeComputers(Connection connection) throws JSchException, SQLException, IOException {
        initializeComputers(connection, "csil.cs.ucsb.edu");
    }

    public static void initializeComputers(Connection connection, String address) throws SQLException, JSchException, IOException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30);

        statement.executeUpdate("DROP TABLE IF EXISTS Computer");
        statement.executeUpdate("DROP TABLE IF EXISTS Usage");
        statement.executeUpdate("CREATE TABLE Computer (id INTEGER PRIMARY KEY, hostname string, ip_address string, is_active string)");
        statement.executeUpdate("CREATE TABLE Usage(id INTEGER, computer integer, timestamp integer, PRIMARY KEY (id), FOREIGN KEY (computer) REFERENCES Computer(id))");


        JSch jsch = new JSch();
        Session session = jsch.getSession(Credentials.USERNAME, address);
        session.setPassword(Credentials.PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand("cat  /etc/hosts");

        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));

        channel.connect();

        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Computer (hostname, ip_address, is_active) VALUES (?, ?, \'\')");
        boolean isAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        String line;
        while ((line = reader.readLine()) != null) {

            String[] lines = line.split("\n");
            for (String resultRow : lines) {
                if (resultRow.startsWith("128.111") && !resultRow.contains(" ")) {
                    String[] parts = resultRow.split("\t");
                    String ip_address = parts[0];
                    String hostname = parts[1];
                    preparedStatement.setString(1, hostname);
                    preparedStatement.setString(2, ip_address);
                    preparedStatement.execute();

                    System.out.println(ip_address);
                }
            }
        }
        connection.commit();
        connection.setAutoCommit(isAutoCommit);

        System.out.println("Done Writing.");

        channel.disconnect();
    }
}
