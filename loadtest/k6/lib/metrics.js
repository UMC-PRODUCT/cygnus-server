import { Rate } from "k6/metrics";

// 시나리오별 에러율을 커스텀 메트릭으로 노출한다.
// http_req_failed 는 전체 합산이라, scenario 태그로 분리해 "어느 업무 시나리오가 깨지는지"를 Grafana 에서 본다.
export const errorRate = new Rate("umc_scenario_errors");
