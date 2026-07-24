package com.umc.product.schedule.application.authorization;

public enum SchedulePolicyAction {
    SCHEDULE_READ("schedule:read"),
    SCHEDULE_CREATE("schedule:create"),
    SCHEDULE_UPDATE("schedule:update"),
    SCHEDULE_DELETE("schedule:delete"),
    SCHEDULE_FORCE_DELETE("schedule:force-delete"),
    ATTENDANCE_SUBMIT("attendance:submit"),
    ATTENDANCE_READ("attendance:read"),
    ATTENDANCE_APPROVE("attendance:approve");

    private final String id;

    SchedulePolicyAction(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
