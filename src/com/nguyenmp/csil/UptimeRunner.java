package com.nguyenmp.csil;

import com.nguyenmp.csil.concurrency.CommandExecutor;
import com.nguyenmp.csil.daos.Database;
import com.nguyenmp.csil.things.Computer;
import com.nguyenmp.csil.things.Uptime;

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
 * uptime and prints to the console in a tabular format, sorted from highest
 * uptime to lowest uptime. This can help administrators identify machines
 * in need of updating/rebooting, or users identify machines unlikely to be
 * spontaneously restarted.
 */
public class UptimeRunner extends CommandExecutor {
	public static final String COMMAND = "cat /proc/uptime";
	public final String hostname;
	public static List<Uptime> results ;

	public UptimeRunner(String hostname) {
		super(Credentials.username(), Credentials.password(), hostname, COMMAND);
		this.hostname = hostname;

	}
	@Override
	public void onSuccess(String result) {
		// Parse the line to get load averages (first value) only
		String[] timeVals = result.split("\\s+");
		long seconds = (long)Double.parseDouble(timeVals[0]);

		// Create new Uptime with hostname prefix only (such as linux14, optimus, etc.) and uptime
		Uptime time  = new Uptime(hostname.split("\\.")[0], seconds);
		results.add(time);
	}

	@Override
	public void onError(Exception e) {
		System.err.println("Error: could not connect to " + hostname);
	}

	public static List<Uptime> getUptimes() throws SQLException, ClassNotFoundException {
		results = Collections.synchronizedList(new ArrayList<Uptime>());
		Database db = new Database();
		List<Computer> computerList = db.computers.getActiveComputers();

		// Create two threads for every logical processor
		int processors = Runtime.getRuntime().availableProcessors();
		ExecutorService service = Executors.newFixedThreadPool(processors * 2);

		// Execute the threads
		for (Computer c: computerList) {
			UptimeRunner runner = new UptimeRunner(c.hostname);
			service.execute(runner);
		}
		service.shutdown();
		try {
			service.awaitTermination(9999, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Sort the Uptimes from highest to lowest
		Collections.sort(results, UptimeComparator);
		return results;
	}

	public static void main(String[] args) throws SQLException, ClassNotFoundException {
		// Query all machines
		UptimeRunner.getUptimes();

		// Print results
		System.out.println("System Load Averages:");
		System.out.println("Hostname              HH:MM:SS");
		System.out.println("------------------------------");

		for (Uptime a: results) {
			System.out.print(a.toString());
		}
	}

	// Comparison logic used to sort individual Uptimes, largest-smallest by time
	static Comparator<Uptime> UptimeComparator = new Comparator<Uptime>() {
		// prioritize (place closer to head) the systems with higher uptime
		public int compare(Uptime up1, Uptime up2) {
			if (up1.elapsed < up2.elapsed) {
				// up2 system uptime is larger, so put that system closer to the
				// head of the collection.
				return 1;
			} else if (up1.elapsed > up2.elapsed) {
				// up1 system uptime is larger, so put that system closer to the
				// head of the collection.
				return -1;
			}
			else return 0;
		}
	};
}
