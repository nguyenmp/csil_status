package com.nguyenmp.csil;

import com.jcraft.jsch.JSchException;
import com.nguyenmp.csil.concurrency.CommandExecutor;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;
import com.nguyenmp.csil.things.User;

import java.io.IOException;
import java.security.cert.CRLException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WhoIsHere {
    private static final Map<String, List<User>> map = new HashMap<String, List<User>>();

    public static void main(String[] args) throws ClassNotFoundException, SQLException, IOException, JSchException, InterruptedException {
        Class.forName("org.sqlite.JDBC");
        final Database database = new Database();

        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(processors * 3);

        List<Computer> activeComputers = database.computers.getActiveComputers();

        for (Computer computer : activeComputers) {
            Runnable tester = new TopRunner(computer.hostname, map);
            executor.execute(tester);
        }

        executor.shutdown();
        executor.awaitTermination(9999, TimeUnit.DAYS);

        for (String hostname : map.keySet()) {
            System.out.printf("%s\n%s\n", hostname, map.get(hostname).toString());
        }
    }

    private static class TopRunner extends CommandExecutor {
        private final String hostname;
        private final Map<String, List<User>> map;

        TopRunner(String hostname, Map<String, List<User>> map) {
            super(Credentials.username(), Credentials.password(), hostname, "who -u");
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
            // Do Nothing
        }
    }
}
