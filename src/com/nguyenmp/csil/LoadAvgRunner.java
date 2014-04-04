package com.nguyenmp.csil;

import com.nguyenmp.csil.concurrency.CommandExecutor;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;
import com.nguyenmp.csil.things.LoadAvg;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	public static final String COMMAND = "echo -n \"$(($(cat /proc/stat | grep \"^cpu.*\" | wc -l) - 1)) \"; cat /proc/loadavg";
	public final String hostname;
	public static List<LoadAvg> results ;

	public LoadAvgRunner(String hostname) {
		super(Credentials.username(), Credentials.password(), hostname, COMMAND);
		this.hostname = hostname;

	}
	@Override
	public void onSuccess(String result) {
		// Parse the line to get load averages only
		String[] avgs = result.split("\\s+");
		int cpus = Integer.parseInt(avgs[0]);
		double avg0 = Double.parseDouble(avgs[1]);
		double avg1 = Double.parseDouble(avgs[2]);
		double avg2 = Double.parseDouble(avgs[3]);
		// Create new LoadAvg with hostname prefix only (such as linux14, optimus, etc.) and load averages
		LoadAvg avg = new LoadAvg(hostname.split("\\.")[0], 100 * avg0/cpus, 100 * avg1/cpus, 100 * avg2/cpus);
		results.add(avg);
	}

	@Override
	public void onError(Exception e) {
		System.err.println("Error: could not connect to " + hostname);
	}

	public static List<LoadAvg> getLoadAverages() throws SQLException, ClassNotFoundException {
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
		Collections.sort(results, loadAvgComparator);
		return results;
	}

	public static void main(String[] args) throws SQLException, ClassNotFoundException {
		LoadAvgRunner.getLoadAverages();
		// Sort the results from the various LoadAverageRunners and print them
		// Fancy table heading
		System.out.println("System Load Averages:");
		System.out.println("Hostname        1       5       15");
		System.out.println("----------------------------------");
		// Print results
		for (LoadAvg a: results) {
			System.out.print(a.toString());
		}
	}

	// Comparison logic used to sort individual LoadAvg objects by system load
	static Comparator<LoadAvg> loadAvgComparator = new Comparator<LoadAvg>() {
		public int compare(LoadAvg avg1, LoadAvg avg2) {
			// Kind of arbitrary ranking measure that takes 5 minute load into account
			// slightly more so than the 1 and 15 minute averages.
			// If you're reading this, feel free to tweak this algorithm or replace
			// it entirely with something better
			double load1 = 0.3 * avg1.avg1min + 0.4 * avg1.avg5min + 0.3 * avg1.avg15min;
			double load2 = 0.3 * avg2.avg1min + 0.4 * avg2.avg5min + 0.3 * avg2.avg15min;
			if (load2 < load1) {
				// Other system load is lower (more desirable) so ours is "greater than", and
				// will be placed further along in the collection
				return 1;
			} else if (load2 > load1) {
				// Other system load is higher (less desirable) so ours is "less than", and
				// will be placed closer to the beginning of the collection
				return -1;
			}
			else return 0;
		}
	};
}
