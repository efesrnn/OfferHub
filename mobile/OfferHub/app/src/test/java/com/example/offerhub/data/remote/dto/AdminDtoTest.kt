package com.example.offerhub.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AdminDtoTest {
    @Test
    fun `staff dto maps backend response to domain`() {
        val staff = StaffDto(
            id = "staff-1",
            firstName = "Ada",
            lastName = "Lovelace",
            email = "ada@offerhub.com",
            role = "EXPERT",
            specialties = listOf("CHURN_ONLEME"),
            regions = listOf("ISTANBUL")
        ).toDomain()

        requireNotNull(staff)
        assertEquals("staff-1", staff.id)
        assertEquals("Ada", staff.firstName)
        assertEquals("EXPERT", staff.role)
        assertEquals(listOf("CHURN_ONLEME"), staff.specialties)
    }

    @Test
    fun `staff dto rejects response without identity`() {
        val staff = StaffDto(
            id = null,
            firstName = "Ada",
            lastName = "Lovelace",
            email = "ada@offerhub.com",
            role = "EXPERT",
            specialties = emptyList(),
            regions = emptyList()
        ).toDomain()

        assertNull(staff)
    }

    @Test
    fun `audit page preserves backend pagination and skips malformed rows`() {
        val page = AdminPagedResponseDto(
            items = listOf(
                AuditLogDto(
                    id = "audit-1",
                    userId = "staff-1",
                    action = "STAFF_CREATED",
                    timestamp = "2026-09-07T12:00:00Z",
                    ip = "127.0.0.1",
                    result = "SUCCESS",
                    detail = null
                ),
                AuditLogDto(
                    id = null,
                    userId = "staff-2",
                    action = "ROLE_UPDATED",
                    timestamp = "2026-09-07T12:01:00Z",
                    ip = "127.0.0.1",
                    result = "SUCCESS",
                    detail = null
                )
            ),
            total = 42,
            page = 1,
            size = 20
        ).toDomain()

        requireNotNull(page)
        assertEquals(1, page.items.size)
        assertEquals(42L, page.total)
        assertEquals(1, page.page)
        assertEquals(20, page.size)
    }
}
