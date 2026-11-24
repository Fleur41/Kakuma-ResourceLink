package com.kakuma.resourcelink.feature.auth.domain

import com.kakuma.resourcelink.core.common.AppError
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.testutil.TestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthUseCasesTest {
    private val repository = mockk<AuthRepository>()

    @Test fun `login with invalid fields never calls the repository`() = runTest {
        val outcome = LoginUseCase(repository)("not-an-email", "")

        assertEquals(
            AuthOutcome.InvalidFields(mapOf("email" to FieldError.INVALID_EMAIL, "password" to FieldError.REQUIRED)),
            outcome,
        )
        coVerify(exactly = 0) { repository.login(any(), any()) }
    }

    @Test fun `login normalises the email before calling the repository`() = runTest {
        val user = TestData.user()
        coEvery { repository.login("amina@example.com", "secret") } returns AppResult.Success(user)

        val outcome = LoginUseCase(repository)("  Amina@Example.COM ", "secret")

        assertEquals(AuthOutcome.Success(user), outcome)
    }

    @Test fun `server field errors become InvalidFields`() = runTest {
        coEvery { repository.login(any(), any()) } returns
            AppResult.Failure(AppError.Validation("Invalid", mapOf("email" to "Unknown email")))

        val outcome = LoginUseCase(repository)("amina@example.com", "secret")

        assertEquals(AuthOutcome.InvalidFields(emptyMap(), mapOf("email" to "Unknown email")), outcome)
    }

    @Test fun `other failures are passed through`() = runTest {
        coEvery { repository.login(any(), any()) } returns AppResult.Failure(AppError.Network)

        assertEquals(AuthOutcome.Failed(AppError.Network), LoginUseCase(repository)("amina@example.com", "secret"))
    }

    @Test fun `register validates all fields and trims input`() = runTest {
        val invalid = RegisterUseCase(repository)(registration(fullName = "A", email = "x", password = "short"))
        assertEquals(
            AuthOutcome.InvalidFields(
                mapOf(
                    "full_name" to FieldError.NAME_TOO_SHORT,
                    "email" to FieldError.INVALID_EMAIL,
                    "password" to FieldError.PASSWORD_TOO_SHORT,
                ),
            ),
            invalid,
        )

        val user = TestData.user()
        coEvery { repository.register(any()) } returns AppResult.Success(user)
        RegisterUseCase(repository)(registration(fullName = "  Amina Hassan ", email = " AMINA@example.com"))
        coVerify { repository.register(registration(fullName = "Amina Hassan", email = "amina@example.com")) }
    }

    private fun registration(
        fullName: String = "Amina Hassan",
        email: String = "amina@example.com",
        password: String = "long-enough-pw",
    ) = Registration(fullName = fullName, email = email, password = password, role = UserRole.MEMBER, language = "en")
}
