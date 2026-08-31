package com.example.offerhub.repository

import com.example.offerhub.data.model.admin.AuditLog
import com.example.offerhub.data.network.PagedResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAdminRepositoryTest {
    @Test
    fun `audit logs are returned in twenty item pages without duplicate ids`() = runBlocking {
        val repository = MockAdminRepository()

        val first = repository.auditPage(page = 0)
        val second = repository.auditPage(page = 1)
        val third = repository.auditPage(page = 2)
        val allItems = first.items + second.items + third.items

        assertEquals(20, first.items.size)
        assertEquals(20, second.items.size)
        assertEquals(13, third.items.size)
        assertEquals(53L, first.total)
        assertEquals(allItems.size, allItems.distinctBy(AuditLog::id).size)
    }

    @Test
    fun `filtered audit request starts from first page and reports filtered total`() = runBlocking {
        val repository = MockAdminRepository()

        val page = repository.auditPage(page = 0, result = "FAILED")

        assertEquals(0, page.page)
        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.result == "FAILED" })
        assertEquals(page.items.size.toLong(), page.total)
    }

    private suspend fun MockAdminRepository.auditPage(
        page: Int,
        result: String? = null
    ): PagedResult<AuditLog> {
        val response = getAuditLogs(
            actionQuery = null,
            action = null,
            result = result,
            fromDate = null,
            toDate = null,
            page = page,
            size = 20
        )
        return (response as AdminResult.Success).value
    }
}
