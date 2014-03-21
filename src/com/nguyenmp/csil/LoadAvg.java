package com.nguyenmp.csil;

import com.nguyenmp.csil.concurrency.CommandExecutor;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by david on 3/20/14.
 */
public class LoadAvg {
	public static void main(String[] args) throws SQLException, ClassNotFoundException {
		Database db = new Database();
		List<Computer> computerList = db.computers.getActiveComputers();
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(processors * 2);
		for (Computer c: computerList) {
			LoadAvgRunner runner = new LoadAvgRunner(c.hostname);
			service.execute(runner);
		}
		service.shutdown();
		try {
			service.awaitTermination(9999, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	/**
	 * A {@link com.nguyenmp.csil.concurrency.CommandExecutor} that gets system
	 * load average and prints to the console in a tabular format.  This will
	 * let users choose which
	 */

	private static class LoadAvgRunner extends CommandExecutor {
		public static final String COMMAND = "cat /proc/loadavg";
		public final String hostname;

		public LoadAvgRunner(String hostname) {
			super(hostname, COMMAND);
			this.hostname = hostname;

		}
		@Override
		public void onSuccess(String result) {
			// Parse the line to get load averages only
			String[] avgs = result.split("\\s+");
			System.out.format("%-10s: %s  %s  %s\n", hostname.split("\\.")[0], avgs[0], avgs[1], avgs[2]);
		}

		@Override
		public void onError(Exception e) {
			System.err.println("Error: could not connect to " + hostname);
		}
	}
}

