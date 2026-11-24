package com.kakuma.resourcelink.feature.services.domain

import com.kakuma.resourcelink.core.common.AppResult
import kotlinx.coroutines.flow.Flow

enum class ServiceCategory(val apiValue: String) {
    HEALTHCARE("healthcare"), HUMANITARIAN("humanitarian"), EDUCATION("education"), LEGAL("legal"),
    PROTECTION("protection"), LIVELIHOOD("livelihood"), COMMUNITY("community");

    companion object { fun from(v: String) = entries.firstOrNull { it.apiValue == v } ?: COMMUNITY }
}

data class EssentialService(
    val id: String,
    val name: String,
    val category: ServiceCategory,
    val organization: String,
    val description: String,
    val phone: String,
    val email: String,
    val website: String,
    val address: String,
    val openingHours: String,
    val isVerified: Boolean,
)

interface ServiceRepository {
    /** Offline-first: always emits the cached directory. */
    fun services(): Flow<List<EssentialService>>
    fun service(id: String): Flow<EssentialService?>
    suspend fun refresh(): AppResult<Unit>
}

/** Pure filter so it is easy to unit test. */
fun List<EssentialService>.filterBy(category: ServiceCategory?, query: String): List<EssentialService> {
    val q = query.trim().lowercase()
    return filter { s ->
        (category == null || s.category == category) &&
            (q.isEmpty() || s.name.lowercase().contains(q) || s.organization.lowercase().contains(q) || s.description.lowercase().contains(q))
    }
}
