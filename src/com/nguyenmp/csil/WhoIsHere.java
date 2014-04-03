package com.nguyenmp.csil;

import com.jcraft.jsch.JSchException;
import com.nguyenmp.csil.concurrency.CommandExecutor;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;
import com.nguyenmp.csil.things.User;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhoIsHere {
    private static final Map<String, List<User>> map = new HashMap<>();

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, JSchException {
        Class.forName("org.sqlite.JDBC");
        final Database database = new Database();
        List<Computer> activeComputers = database.computers.getActiveComputers();
        activeComputers.parallelStream().forEach((Computer computer) -> {
            new TopRunner(computer.hostname, map).run();
        });

        map.forEach((String hostname, List<User> users) -> System.out.printf("%s\t=>\t%s\n", hostname, users.toString()));
    }

    private static class TopRunner extends CommandExecutor {
        private final String hostname;
        private final Map<String, List<User>> map;

        TopRunner(String hostname, Map<String, List<User>> map) {
            super(hostname, "who -u");
            this.hostname = hostname;
            this.map = map;
        }

        @Override
        public void onSuccess(String result) {
            List<User> users = User.fromWho(result);
            map.put(hostname, users);
        }

        @Override
        public void onError(Exception e) {
            e.printStackTrace();
        }
    }
}
