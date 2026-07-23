# Maintenance GraphQL IDL

`MaintenanceStatus`는 공개 상태 projection이고 `MaintenanceWindow`는 SUPER_ADMIN이 관리하는
점검 resource다. GraphQL 요청 자체에 대한 maintenance 차단은 기존 HTTP filter가 담당한다.
