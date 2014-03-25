package com.nguyenmp.csil;

import com.nguyenmp.csil.concurrency.CommandExecutor;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;
import com.nguyenmp.csil.things.LoadAvg;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A {@link com.nguyenmp.csil.concurrency.CommandExecutor} that gets system
 * load average and prints to the console in a tabular format, sorted from
 * lowest system load (top) to highest system load (bottom).  This will help
 * users decide which machine to use
 */
public class LoadAvgRunner extends CommandExecutor {
	public static final String COMMAND = "cat /proc/loadavg";
	public final String hostname;
	public static List<LoadAvg> results ;

	public LoadAvgRunner(String hostname) {
		super(hostname, COMMAND);
		this.hostname = hostname;

	}
	@Override
	public void onSuccess(String result) {
		// Parse the line to get load averages only
		String[] avgs = result.split("\\s+");
		double avg0 = Double.parseDouble(avgs[0]);
		double avg1 = Double.parseDouble(avgs[1]);
		double avg2 = Double.parseDouble(avgs[2]);
		// Create new LoadAvg with hostname prefix only (such as linux14, optimus, etc.) and load averages
		LoadAvg avg = new LoadAvg(hostname.split("\\.")[0], avg0, avg1, avg2);
		results.add(avg);
	}

	@Override
	public void onError(Exception e) {
		System.err.println("Error: could not connect to " + hostname);
	}

	public static void main(String[] args) throws SQLException, ClassNotFoundException {
		System.out.println("Connecting to remote machines...");
		// Create a thread safe ArrayList
		results = Collections.synchronizedList(new ArrayList<LoadAvg>());
		Database db = new Database();
		List<Computer> computerList = db.computers.getActiveComputers();

		// Create two threads for every logical processor
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(processors * 2);

		// Execute the threads
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

		// Sort the results from the various LoadAverageRunners and print them
		Collections.sort(results);
		// Fancy table heading
		System.out.println("System Load Averages:");
		System.out.println("Hostname    1       5       15");
		System.out.println("--------------------------------");
		// Print results
		for (LoadAvg a: results) {
			System.out.print(a.toString());
		}
	}
}
