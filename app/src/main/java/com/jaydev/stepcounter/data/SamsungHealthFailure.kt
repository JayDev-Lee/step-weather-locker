package com.jaydev.stepcounter.data

import com.samsung.android.sdk.health.data.error.ErrorCode

internal fun samsungHealthFailure(code: Int?): HealthAccessException = when (code) {
    ErrorCode.ERR_PLATFORM_NOT_INSTALLED -> HealthAccessException("삼성 헬스를 설치해주세요")
    ErrorCode.ERR_OLD_VERSION_PLATFORM -> HealthAccessException("삼성 헬스를 6.30.2 이상으로 업데이트해주세요")
    ErrorCode.ERR_PLATFORM_DISABLED -> HealthAccessException("삼성 헬스 앱을 사용 설정해주세요")
    ErrorCode.ERR_PLATFORM_NOT_INITIALIZED -> HealthAccessException("삼성 헬스를 열어 초기 설정을 완료해주세요")
    ErrorCode.ERR_NO_USER_PERMISSION -> HealthAccessException("앱에서 삼성 헬스 읽기 권한을 허용해주세요")
    ErrorCode.ERR_INVALID_CALLER, ErrorCode.ERR_ACCESS_CONTROL ->
        HealthAccessException("삼성 헬스 연동 인증이 필요합니다. 앱 등록 상태를 확인해주세요")
    ErrorCode.ERR_CHILD_ACCOUNT_ACCESS -> HealthAccessException("현재 삼성 계정에서는 건강 데이터 공유가 제한됩니다")
    ErrorCode.ERR_OUT_OF_SPACE -> HealthAccessException("기기의 저장 공간을 확보한 뒤 다시 시도해주세요")
    ErrorCode.ERR_CONNECTION_FAIL, ErrorCode.ERR_CONNECTION_TIMEOUT, ErrorCode.ERR_PLATFORM_DISCONNECTED,
    ErrorCode.ERR_INTERNAL_ERROR, ErrorCode.ERR_DB_ERROR, ErrorCode.ERR_INTERRUPTED ->
        HealthAccessException("삼성 헬스 연결이 지연됩니다. 다시 새로고침해주세요", permanent = false)
    else -> HealthAccessException("삼성 헬스 연동을 확인할 수 없습니다. 앱에서 연결을 확인해주세요")
}
