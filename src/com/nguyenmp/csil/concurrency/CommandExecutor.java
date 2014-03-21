package com.nguyenmp.csil.concurrency;


import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nguyenmp.csil.Credentials;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * An abstract CommandExecutor defines the remote SSH execution of
 * a command and calls it's own {@link #onSuccess(String)} method
 * if a result was received (or null if no command was executed).
 * Alternatively, {@link #onError(Exception)} is called when an
 * exception is encountered during execution (usually due to an
 * authentication error or a networking error).
 */
public abstract class CommandExecutor implements Runnable {
    private final String hostname, command;

    /**
     * <p>Creates a new {@link com.nguyenmp.csil.concurrency.CommandExecutor} that
     * connects to the given hostname and runs remote SSH execution of the given
     * command.</p>
     *
     * <p>You can either create a thread with this object as a runnable and
     * start that thread like so:</p>
     *
     * <p><code>
     *     Runnable executor = new CommandExecutor(hostname, command);<br />
     *     Thread thread = new Thread(executor);<br />
     *     thread.start();<br />
     * </code></p>
     *
     * <p>Or you can run it through an executor.</p>
     * @param hostname
     * @param command
     */
    CommandExecutor(String hostname, String command) {
        this.hostname = hostname;
        this.command = command;
    }

    @Override
    public void run() {
        try {
            // Initialization of the JSch connection
            JSch jsch = new JSch();
            Session session = jsch.getSession(Credentials.USERNAME, hostname);
            session.setPassword(Credentials.PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(5000);
            session.connect();

            // If the command is specified, execute it
            String result = null;
            if (command != null) {

                // Execute command
                ChannelExec channel = (ChannelExec) session.openChannel("exec");
                channel.setCommand(command);
                channel.connect();

                // Read result
                BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
                StringBuilder builder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append('\n');
                }

                // Set result to be the read output
                result = builder.toString();
            }

            // Clean up!
            session.disconnect();

            // Trigger callback
            onSuccess(result);
        } catch (JSchException e) {
            onError(e);
        } catch (IOException e) {
            onError(e);
        }
    }

    /**
     * Called when the SSH remote execution was successful.
     * @param result The string result of the remote execution or <code>null</code>
     *               if there was no command executed.
     */
    public abstract void onSuccess(String result);

    /**
     * Called when an exception was encountered
     * @param e either {@link java.io.IOException} or {@link com.jcraft.jsch.JSchException}
     */
    public abstract void onError(Exception e);
}