package com.nguyenmp.csil;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nguyenmp.csil.Credentials;

import java.io.*;
import java.sql.*;

public class Topper {

    public static void main(String[] args) throws IOException, JSchException, ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        // create a database connection
        Connection connection = DriverManager.getConnection("jdbc:sqlite:./csil_top.db");
        initializeComputers(connection);
    }

    public static void testComputers(Connection connection) throws SQLException, JSchException, IOException {
        Statement statement = connection.createStatement();
        ResultSet computers = statement.executeQuery("SELECT * FROM Computer WHERE is_active = \'\'");

        int i = 0;
        while (i++ < 341) computers.next();
        PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Computer (hostname, ip_address, is_active) VALUES (?, ?, ?)");
        while (computers.next()) {

            String hostname = computers.getString("hostname");
            String ip_address = computers.getString("ip_address");
            preparedStatement.setString(1, hostname);
            preparedStatement.setString(2, ip_address);

            try {
                JSch jsch = new JSch();
                Session session = jsch.getSession(Credentials.USERNAME, hostname);
                session.setPassword(Credentials.PASSWORD);
                session.setConfig("StrictHostKeyChecking", "no");
                session.setTimeout(20000);
                session.connect();
                session.disconnect();
                System.out.println("Connected to " + hostname + " at " + ip_address);
                preparedStatement.setString(3, "true");
            } catch (JSchException e) {
                System.out.println("Failed with " + hostname + " at " + ip_address);
                e.printStackTrace();
                preparedStatement.setString(3, "false");
            }

            preparedStatement.execute();
        }
    }

    private static class AuthTester extends Thread {
        private final String hostname, ip_address;

        AuthTester(String hostname, String ip_address) {
            this.hostname = hostname;
            this.ip_address = ip_address;
        }

        public void run() {
        }
    }

    public static void initializeComputers(Connection connection) throws SQLException, JSchException, IOException {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30);

        statement.executeUpdate("DROP TABLE IF EXISTS Computer");
        statement.executeUpdate("DROP TABLE IF EXISTS Usage");
        statement.executeUpdate("CREATE TABLE Computer (id integer AUTO_INCREMENT, hostname string, ip_address string, is_active string, PRIMARY KEY (id))");
        statement.executeUpdate("CREATE TABLE Usage(id integer AUTO_INCREMENT, computer integer, timestamp integer, PRIMARY KEY (id), FOREIGN KEY (computer) REFERENCES Computer(id))");


        JSch jsch = new JSch();
        Session session = jsch.getSession(Credentials.USERNAME, "dot.cs.ucsb.edu");
        session.setPassword(Credentials.PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        Channel channel = session.openChannel("shell");

        BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));

        channel.connect();

        PrintWriter writr = new PrintWriter(channel.getOutputStream());
        writr.println();
        writr.flush();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("-bash-4.2")) {
                writr.println("cat /etc/hosts");
                writr.println();
                writr.flush();

                PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO Computer (hostname, ip_address, is_active) VALUES (?, ?, \'\')");

                reader.read();
                StringBuilder string = new StringBuilder();
                while ((line = reader.readLine()) != null && !line.startsWith("-bash-4.2")) {
                    string.append(line);
                    string.append('\n');
                }

                connection.setAutoCommit(false);
                String[] lines = string.toString().split("\n");
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
                connection.commit();
                System.out.println("Done Writing.");

                channel.disconnect();
            }
        }
    }
}
