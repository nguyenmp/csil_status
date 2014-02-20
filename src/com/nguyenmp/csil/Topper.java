package com.nguyenmp.csil;

import com.jcraft.jsch.*;
import com.nguyenmp.csil.Credentials;

import java.io.*;
import java.sql.*;

public class Topper {

    public static void main(String[] args) throws IOException, JSchException, ClassNotFoundException, SQLException {
        if (args.length < 1) improperUsage();

        String pathToFile = args[0];


        Class.forName("org.sqlite.JDBC");

        // create a database connection
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + pathToFile);

    }

    private static void improperUsage() {
        System.out.println("Usage: java Topper pathToDBFile");
        System.out.println("\tPath to file is a String that defines the file path that the initialized SQLite DB");
        System.exit(1);
    }
}