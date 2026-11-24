package com.kakuma.resourcelink.feature.projects.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.network.NetworkPagingSource
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.projects.domain.CommunityProject
import com.kakuma.resourcelink.feature.projects.domain.Contribution
import com.kakuma.resourcelink.feature.projects.domain.ContributionSummary
import com.kakuma.resourcelink.feature.projects.domain.ContributionType
import com.kakuma.resourcelink.feature.projects.domain.ProjectCategory
import com.kakuma.resourcelink.feature.projects.domain.ProjectRepository
import com.kakuma.resourcelink.feature.stories.data.AuthorDto
import com.kakuma.resourcelink.feature.stories.data.toDomain
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

// Money is sent as decimal strings by DRF; keep them as strings on the wire to avoid float rounding.
@JsonClass(generateAdapter = true)
data class SummaryDto(
    @Json(name = "total_contributions") val totalContributions: Int = 0,
    @Json(name = "pledged_amount") val pledgedAmount: String = "0",
    val currency: String = "USD",
    @Json(name = "by_type") val byType: Map<String, Int> = emptyMap(),
    val note: String = "",
)

@JsonClass(generateAdapter = true)
data class ProjectDto(
    val id: String,
    val owner: AuthorDto,
    val title: String,
    val summary: String = "",
    val description: String = "",
    val category: String,
    val location: String = "",
    @Json(name = "cover_image") val coverImage: String? = null,
    val status: String,
    @Json(name = "accepted_contribution_types") val acceptedTypes: List<String> = emptyList(),
    @Json(name = "funding_goal") val fundingGoal: String? = null,
    val currency: String = "USD",
    @Json(name = "contribution_summary") val summaryStats: SummaryDto = SummaryDto(),
)

@JsonClass(generateAdapter = true)
data class ContributionDto(
    val id: String,
    val type: String,
    val description: String,
    val amount: String? = null,
    val currency: String = "",
    val status: String,
)

@JsonClass(generateAdapter = true)
data class PledgeRequest(val type: String, val description: String, val amount: String? = null)

interface ProjectApi {
    @GET("projects/")
    suspend fun list(@Query("page") page: Int, @Query("page_size") size: Int, @Query("category") category: String?): PageDto<ProjectDto>

    @GET("projects/{id}/")
    suspend fun get(@Path("id") id: String): ProjectDto

    @GET("projects/{id}/contributions/")
    suspend fun contributions(@Path("id") id: String): PageDto<ContributionDto>

    @POST("projects/{id}/contributions/")
    suspend fun pledge(@Path("id") id: String, @Body body: PledgeRequest): ContributionDto
}

fun ProjectDto.toDomain() = CommunityProject(
    id = id, owner = owner.toDomain(), title = title, summary = summary, description = description,
    category = ProjectCategory.from(category), location = location, coverImage = coverImage, status = status,
    acceptedTypes = acceptedTypes.mapNotNull(ContributionType::from), fundingGoal = fundingGoal?.toBigDecimalOrNull(),
    currency = currency,
    summaryStats = ContributionSummary(
        summaryStats.totalContributions, summaryStats.pledgedAmount.toBigDecimalOrNull() ?: BigDecimal.ZERO,
        summaryStats.currency, summaryStats.byType.mapNotNull { (k, v) -> ContributionType.from(k)?.let { it to v } }.toMap(),
        summaryStats.note,
    ),
)

fun ContributionDto.toDomain() = ContributionType.from(type)?.let {
    Contribution(id, it, description, amount?.toBigDecimalOrNull(), currency, status)
}

@Singleton
class ProjectRepositoryImpl @Inject constructor(private val api: ProjectApi, private val moshi: Moshi) : ProjectRepository {
    override fun projects(category: ProjectCategory?): Flow<PagingData<CommunityProject>> = Pager(PagingConfig(pageSize = 20)) {
        NetworkPagingSource({ page, size -> api.list(page, size, category?.apiValue) }, { it.toDomain() })
    }.flow

    override suspend fun get(id: String) = safeApiCall(moshi) { api.get(id) }.map { it.toDomain() }

    override suspend fun myContributions(projectId: String) =
        safeApiCall(moshi) { api.contributions(projectId) }.map { page -> page.results.mapNotNull { it.toDomain() } }

    override suspend fun pledge(projectId: String, type: ContributionType, description: String, amount: BigDecimal?): AppResult<Contribution> =
        safeApiCall(moshi) {
            api.pledge(projectId, PledgeRequest(type.apiValue, description.trim(), amount?.setScale(2, java.math.RoundingMode.HALF_UP)?.toPlainString()))
        }.map { checkNotNull(it.toDomain()) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectModule {
    @Binds @Singleton abstract fun repository(impl: ProjectRepositoryImpl): ProjectRepository

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): ProjectApi = retrofit.create(ProjectApi::class.java)
    }
}
