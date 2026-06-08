package com.medcare.clinic_backend.dto;

import java.time.LocalDateTime;

/**
 * Slot response for doctor follow-up UI.
 * Includes both new fields (time, totalSlots, available) and legacy FE aliases
 * (startTime, maxPatients, disabled, disabledReason, ...).
 */
public class AppointmentSlotResponse {

    private final String time;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final String shift;
    private final int totalSlots;
    private final long bookedSlots;
    private final long remainingSlots;
    private final boolean available;
    private final String disabledReason;

    public AppointmentSlotResponse(
            String time,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String shift,
            int totalSlots,
            long bookedSlots,
            long remainingSlots,
            boolean available,
            String disabledReason
    ) {
        this.time = time;
        this.startTime = startTime;
        this.endTime = endTime;
        this.shift = shift;
        this.totalSlots = totalSlots;
        this.bookedSlots = bookedSlots;
        this.remainingSlots = remainingSlots;
        this.available = available;
        this.disabledReason = disabledReason;
    }

    public String getTime() {
        return time;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getShift() {
        return shift;
    }

    public int getTotalSlots() {
        return totalSlots;
    }

    public long getBookedSlots() {
        return bookedSlots;
    }

    public long getRemainingSlots() {
        return remainingSlots;
    }

    public boolean isAvailable() {
        return available;
    }

    public boolean getAvailable() {
        return available;
    }

    /** Legacy alias for FE normalizeAppointmentSlot(). */
    public int getMaxPatients() {
        return totalSlots;
    }

    /** Legacy alias for FE normalizeAppointmentSlot(). */
    public long getBookedPatients() {
        return bookedSlots;
    }

    /** Legacy alias for FE normalizeAppointmentSlot(). */
    public boolean isFull() {
        return remainingSlots <= 0;
    }

    /** Legacy alias for FE normalizeAppointmentSlot(). */
    public boolean isDisabled() {
        return !available;
    }

    public boolean getDisabled() {
        return !available;
    }

    public String getDisabledReason() {
        return disabledReason;
    }
}
