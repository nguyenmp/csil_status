package com.nguyenmp.csil.things;

/**
 * A simple class intended for use by a {@link com.nguyenmp.csil.LoadAvgRunner}.
 * This allows a LoadAvgRunner to easily sort computers by load average
 * and display the most available machines toward the top of the list.
 */
public class LoadAvg implements Comparable<LoadAvg> {
	public String hostname;
	public double avg1min, avg5min, avg15min;

	public LoadAvg() {
		this("", 0.0, 0.0, 0.0);
	}

	public LoadAvg(String hostname, double avg5min, double avg1min, double avg15min) {
		this.hostname = hostname;
		this.avg5min = avg5min;
		this.avg1min = avg1min;
		this.avg15min = avg15min;
	}


	@Override
	public String toString() {
		return String.format("%-10s: %-6s  %-6s  %-6s\n", hostname, avg1min, avg5min, avg15min);
	}

	@Override
	public int hashCode() {
		return hostname.hashCode();
	}

	@Override
	public int compareTo(LoadAvg other) {
		// Kind of arbitrary ranking measure that takes 5 minute load into account
		// slightly more so than the 1 and 15 minute averages
		double weightedLoad = 0.3 * avg1min + 0.4 * avg5min + 0.3 * avg15min;
		double otherLoad = 0.3 * other.avg1min + 0.4 * other.avg5min + 0.3 * other.avg15min;
		if (otherLoad < weightedLoad) {
			// Other system load is lower (more desirable) so ours is "greater than", and
			// will be placed further along in the collection
			return 1;
		} else if (otherLoad > weightedLoad) {
			// Other system load is higher (less desirable) so ours is "less than", and
			// will be placed closer to the beginning of the collection
			return -1;
		}
		else return 0;
	}
}
