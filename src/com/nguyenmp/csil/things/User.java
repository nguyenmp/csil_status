package com.nguyenmp.csil.things;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class User {
    public String name, line, time, idle, pid, comment;

    @Override
    public String toString() {
        return name;
    }

    public static List<User> fromWho(String consoleOutput) {
        List<User> users = new ArrayList<User>();

        try {
            Pattern pattern = Pattern.compile("[^\\s]+");
            Matcher matcher = pattern.matcher(consoleOutput);
            while (matcher.find()) {
                String name = matcher.group();

                matcher.find();
                String line = matcher.group();

                matcher.find();
                String time = matcher.group();
                matcher.find();
                time += " " + matcher.group();
                matcher.find();
                time += " " + matcher.group();

                matcher.find();
                String idle = matcher.group();

                matcher.find();
                String pid = matcher.group();

                matcher.find();
                String comment = matcher.group();

                User user = new User();
                user.comment = comment;
                user.idle = idle;
                user.line = line;
                user.name = name;
                user.pid = pid;
                user.time = time;

                users.add(user);
            }
        } catch (IllegalStateException e) {
            // Do nothing
        }
        return users;
    }
}
