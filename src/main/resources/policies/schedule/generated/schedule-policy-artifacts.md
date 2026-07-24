# Schedule Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `schedule-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `8268d86534bd5f79571bcd6ebfc858510194d5faf823958c3a3066b3e91ec240`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:DELETE /api/v2/schedules/{scheduleId} | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#delete | REST | schedule:delete | schedule-resource | DIRECT |
| rest:DELETE /api/v2/schedules/{scheduleId}/force | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#forceDelete | REST | schedule:force-delete | schedule-resource | DIRECT |
| rest:GET /api/v2/schedules/attendance | com.umc.product.schedule.adapter.in.web.v2.ScheduleQueryV2Controller#getAttendanceInfoList | REST | attendance:read | schedule-resource | DIRECT |
| rest:GET /api/v2/schedules/me | com.umc.product.schedule.adapter.in.web.v2.ScheduleQueryV2Controller#mySchedules | REST | schedule:read | schedule-resource | DIRECT |
| rest:GET /api/v2/schedules/{scheduleId} | com.umc.product.schedule.adapter.in.web.v2.ScheduleQueryV2Controller#details | REST | schedule:read | schedule-resource | DIRECT |
| rest:GET /api/v2/schedules/{scheduleId}/attendance | com.umc.product.schedule.adapter.in.web.v2.ScheduleQueryV2Controller#getAttendanceInfo | REST | attendance:read | schedule-resource | DIRECT |
| rest:PATCH /api/v2/schedules/{scheduleId} | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#edit | REST | schedule:update | schedule-resource | DIRECT |
| rest:POST /api/v2/schedules | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#create | REST | schedule:create | schedule-resource | DIRECT |
| rest:POST /api/v2/schedules/{scheduleId}/attendances/decide | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#decideAttendances | REST | attendance:approve | schedule-resource | DIRECT |
| rest:POST /api/v2/schedules/{scheduleId}/attendances/excuse | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#excuseAttendance | REST | attendance:submit | schedule-resource | DIRECT |
| rest:POST /api/v2/schedules/{scheduleId}/attendances/request | com.umc.product.schedule.adapter.in.web.v2.ScheduleCommandV2Controller#requestAttendance | REST | attendance:submit | schedule-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| schedule-resource | attendance.read-approve.super-admin | ALLOW | attendance:approve, attendance:read | ALL(EQ(ATTRIBUTE(relation.superAdmin), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| schedule-resource | attendance.read-approve.target-gisu-staff | ALLOW | attendance:approve, attendance:read | ALL(EQ(ATTRIBUTE(relation.activeOperatingStaffInTargetGisu), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| schedule-resource | attendance.read-list.active-staff | ALLOW | attendance:read | ALL(EQ(ATTRIBUTE(relation.activeOperatingStaff), true), EQ(ATTRIBUTE(resource.specified), false)) |  |
| schedule-resource | attendance.read-list.super-admin | ALLOW | attendance:read | ALL(EQ(ATTRIBUTE(relation.superAdmin), true), EQ(ATTRIBUTE(resource.specified), false)) |  |
| schedule-resource | attendance.submit.participant-challenger | ALLOW | attendance:submit | ALL(EQ(ATTRIBUTE(relation.challengerHistory), true), EQ(ATTRIBUTE(relation.isParticipant), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| schedule-resource | schedule.force-delete.super-admin | ALLOW | schedule:force-delete | ALL(EQ(ATTRIBUTE(relation.superAdmin), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| schedule-resource | schedule.read-create.challenger | ALLOW | schedule:create, schedule:read | EQ(ATTRIBUTE(relation.challengerHistory), true) |  |
| schedule-resource | schedule.read-create.super-admin | ALLOW | schedule:create, schedule:read | EQ(ATTRIBUTE(relation.superAdmin), true) |  |
| schedule-resource | schedule.update-delete.author | ALLOW | schedule:delete, schedule:update | ALL(EQ(ATTRIBUTE(relation.isAuthor), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
| schedule-resource | schedule.update-delete.super-admin | ALLOW | schedule:delete, schedule:update | ALL(EQ(ATTRIBUTE(relation.superAdmin), true), EQ(ATTRIBUTE(resource.specified), true)) |  |
