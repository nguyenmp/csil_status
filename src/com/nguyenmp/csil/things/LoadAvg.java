package com.nguyenmp.csil.things;

/**
 * A simple class intended for use by a {@link com.nguyenmp.csil.LoadAvgRunner}.
 * This allows a LoadAvgRunner to easily sort computers by load average
 * and display the most available machines toward the top of the list.
 */
public class LoadAvg {
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
}
