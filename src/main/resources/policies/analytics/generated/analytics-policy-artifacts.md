# Analytics Policy Generated Review Artifact

- schemaVersion: `1.0`
- contextSchemaVersion: `analytics-1.0`
- policyVersion: `1.0.0`
- policyFingerprint: `97badc120cdffc62121cebbe50d2777044c735351c35cab00c8766f8662307e6`
- defaultEffect: `DENY`
- combiningAlgorithm: `DENY_OVERRIDES`

## Runtime surfaces

| Surface | Handler | Type | Action | Module | Gate |
|---|---|---|---|---|---|
| rest:GET /api/v1/analytics/admin/dashboard/action-queue | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getActionQueue | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/context | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getContext | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/operations | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getOperationsOverview | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/operations/attendance | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getOperationsAttendance | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/operations/points | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getOperationsPoints | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/operations/schools | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getOperationsSchools | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/operations/signups | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getOperationsSignups | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/operations/study-groups | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getOperationsStudyGroups | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/risk-challengers | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getRiskChallengers | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/dashboard/summary | com.umc.product.analytics.adapter.in.web.AdminDashboardController#getSummary | REST | analytics:read-dashboard | analytics-resource | DIRECT |
| rest:GET /api/v1/analytics/admin/schools/summary | com.umc.product.analytics.adapter.in.web.AdminSchoolAnalyticsController#getSchoolSummaries | REST | analytics:read-school | analytics-resource | DIRECT |

## Compiled statements

| Module | Statement | Effect | Actions | Condition | Outcomes |
|---|---|---|---|---|---|
| analytics-resource | analytics.read.active-central-member | ALLOW | analytics:read-dashboard, analytics:read-school | EQ(ATTRIBUTE(relation.activeCentralMember), true) |  |
| analytics-resource | analytics.read.active-chapter-president | ALLOW | analytics:read-dashboard, analytics:read-school | EQ(ATTRIBUTE(relation.activeChapterPresident), true) |  |
| analytics-resource | analytics.read.active-school-operator | ALLOW | analytics:read-dashboard, analytics:read-school | EQ(ATTRIBUTE(relation.activeSchoolOperator), true) |  |
| analytics-resource | analytics.read.active-super-admin | ALLOW | analytics:read-dashboard, analytics:read-school | EQ(ATTRIBUTE(relation.activeSuperAdmin), true) |  |
