package com.jaydev.stepcounter.data

import com.samsung.android.sdk.health.data.error.ErrorCode
import org.junit.Assert.*
import org.junit.Test

class SamsungHealthFailureTest {
    @Test fun `missing platform requires installation without automatic retries`() {
        val failure = samsungHealthFailure(ErrorCode.ERR_PLATFORM_NOT_INSTALLED)
        assertTrue(failure.permanent)
        assertTrue(failure.userMessage.contains("설치"))
    }
    @Test fun `denied permission requests user action instead of background permission prompts`() {
        val failure = samsungHealthFailure(ErrorCode.ERR_NO_USER_PERMISSION)
        assertTrue(failure.permanent)
        assertTrue(failure.userMessage.contains("권한"))
    }
    @Test fun `authorization is distinguished from transient connection issues`() {
        assertTrue(samsungHealthFailure(ErrorCode.ERR_INVALID_CALLER).permanent)
        assertTrue(samsungHealthFailure(ErrorCode.ERR_ACCESS_CONTROL).permanent)
        assertFalse(samsungHealthFailure(ErrorCode.ERR_CONNECTION_FAIL).permanent)
        assertFalse(samsungHealthFailure(ErrorCode.ERR_CONNECTION_TIMEOUT).permanent)
    }
    @Test fun `unknown sdk code is safe and stops repeated background requests`() {
        val failure = samsungHealthFailure(null)
        assertTrue(failure.permanent)
        assertFalse(failure.userMessage.contains("null"))
    }
}
