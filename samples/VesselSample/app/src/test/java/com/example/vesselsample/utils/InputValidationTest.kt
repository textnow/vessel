package com.example.vesselsample.utils

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class InputValidationTest {
    @Test
    fun `validate non-numeric ID returns false`() {
        assert(!InputValidation.validateID("test"))
    }

    @Test
    fun `validate numeric ID out of bounds returns false`() {
        assert(!InputValidation.validateID("13"))
    }

    @Test
    fun `validate valid ID returns true`() {
        assert(InputValidation.validateID("5"))
    }
}