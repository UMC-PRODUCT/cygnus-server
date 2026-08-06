package com.umc.product.test.application.port.in.command;

import com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataCommand;
import com.umc.product.test.application.port.in.command.dto.DeleteSeedProjectDataResult;

/**
 * 테스트 환경 프로젝트 관련 데이터 hard delete UseCase.
 * <p>
 * 운영 프로젝트 삭제 정책과 달리, 테스트 데이터 재시딩을 위해 지정 기수의 프로젝트·지원서·지원 폼·매칭 차수와
 * 연결된 form 데이터를 물리 삭제한다.
 */
public interface DeleteSeedProjectDataUseCase {

    DeleteSeedProjectDataResult delete(DeleteSeedProjectDataCommand command);
}
