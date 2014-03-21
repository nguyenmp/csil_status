package com.nguyenmp.csil.things;


public class Computer {
    public int id;
    public String hostname, ipAddress;
    public boolean isActive;

    @Override
    public String toString() {
        return String.format("{id=%d, hostname=%s, ipAddress=%s, isActive=%b}", id, hostname, ipAddress, isActive);
    }
}
