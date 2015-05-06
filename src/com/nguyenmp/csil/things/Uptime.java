package com.nguyenmp.csil.things;

/**
 * A simple class intended for use by a {@link com.nguyenmp.csil.UptimeRunner}.
 * This allows an UptimeRunner to easily represent and sort computers by uptime.
 */
public class Uptime {
	public String hostname;
	public long seconds, minutes, hours, days, elapsed;

	public Uptime() {
		this("", 0);
	}

	public Uptime(String hostname, long seconds) {
		this.hostname = hostname;

		// Store total elapsed time (for sorting) and calculate days, hours,
		// minutes, and seconds for human use
		this.elapsed = seconds;
		this.days = seconds / (86400);
		this.hours = (seconds % 86400) / 3600;
		this.minutes = ((seconds % 86400) % 3600) / 60;
		this.seconds = ((seconds % 86400) % 3600) % 60;
	}


	@Override
	public String toString() {
		// print out a hostname and corresponding days, hours, minutes, seconds
		return String.format("%-12s %02d days, %02d:%02d:%02d \n", hostname, days, hours, minutes, seconds);
	}

	@Override
	public int hashCode() {
		return hostname.hashCode();
	}
}
