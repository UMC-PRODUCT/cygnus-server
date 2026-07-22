package com.umc.product.support.fixture;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import com.umc.product.schedule.domain.AttendancePolicy;
import com.umc.product.schedule.domain.Schedule;
import com.umc.product.schedule.domain.ScheduleParticipant;
import com.umc.product.schedule.domain.ScheduleParticipantAttendance;
import com.umc.product.schedule.domain.enums.AttendanceStatus;
import com.umc.product.schedule.domain.enums.ScheduleTag;

public final class ScheduleUnitFixture {

    public static final Long AUTHOR_MEMBER_ID = 1L;
    public static final Long PARTICIPANT_MEMBER_ID = 2L;
    public static final Instant DEFAULT_STARTS_AT = Instant.parse("2030-01-01T10:00:00Z");
    public static final Instant DEFAULT_ENDS_AT = Instant.parse("2030-01-01T12:00:00Z");

    private ScheduleUnitFixture() {
    }

    public static Schedule schedule() {
        return schedule(DEFAULT_STARTS_AT, DEFAULT_ENDS_AT, policy(DEFAULT_STARTS_AT, DEFAULT_ENDS_AT));
    }

    public static Schedule schedule(Instant startsAt, Instant endsAt, AttendancePolicy policy) {
        return Schedule.builder()
            .name("정기 세션")
            .description("기본 일정 설명")
            .tags(Set.of(ScheduleTag.GENERAL))
            .authorMemberId(AUTHOR_MEMBER_ID)
            .startsAt(startsAt)
            .endsAt(endsAt)
            .locationName("UMC 라운지")
            .location(point(127.0, 37.0))
            .policy(policy)
            .build();
    }

    public static AttendancePolicy policy(Instant startsAt, Instant endsAt) {
        return Schedule.createAttendancePolicy(
            startsAt.minus(10, ChronoUnit.MINUTES),
            startsAt.plus(10, ChronoUnit.MINUTES),
            startsAt.plus(20, ChronoUnit.MINUTES),
            startsAt,
            endsAt
        );
    }

    public static ScheduleParticipant participant(Schedule schedule) {
        return participant(schedule, null);
    }

    public static ScheduleParticipant participant(
        Schedule schedule,
        ScheduleParticipantAttendance attendance
    ) {
        return ScheduleParticipant.builder()
            .memberId(PARTICIPANT_MEMBER_ID)
            .schedule(schedule)
            .attendance(attendance)
            .build();
    }

    public static ScheduleParticipantAttendance attendance(AttendanceStatus status) {
        return ScheduleParticipantAttendance.create(point(127.0, 37.0), true, null, status);
    }

    public static Point point(double longitude, double latitude) {
        Point point = new GeometryFactory().createPoint(new Coordinate(longitude, latitude));
        point.setSRID(4326);
        return point;
    }
}
