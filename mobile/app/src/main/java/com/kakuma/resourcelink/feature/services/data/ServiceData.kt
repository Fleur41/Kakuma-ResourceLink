package com.kakuma.resourcelink.feature.services.data

import com.kakuma.resourcelink.core.common.AppResult
import com.kakuma.resourcelink.core.common.map
import com.kakuma.resourcelink.core.database.AppDatabase
import com.kakuma.resourcelink.core.database.entity.ServiceEntity
import com.kakuma.resourcelink.core.network.PageDto
import com.kakuma.resourcelink.core.network.safeApiCall
import com.kakuma.resourcelink.feature.services.domain.EssentialService
import com.kakuma.resourcelink.feature.services.domain.ServiceCategory
import com.kakuma.resourcelink.feature.services.domain.ServiceRepository
import com.kakuma.resourcelink.sync.SyncTask
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class ServiceDto(
    val id: String,
    val name: String,
    val category: String,
    val organization: String = "",
    val description: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    @Json(name = "opening_hours") val openingHours: String = "",
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "is_active") val isActive: Boolean = true,
)

interface ServiceApi {
    @GET("services/")
    suspend fun list(@Query("page") page: Int, @Query("page_size") pageSize: Int = 50): PageDto<ServiceDto>
}

fun ServiceDto.toEntity() = ServiceEntity(id, name, category, organization, description, phone, email, website, address, openingHours, isVerified)
fun ServiceEntity.toDomain() = EssentialService(
    id, name, ServiceCategory.from(category), organization, description, phone, email, website, address, openingHours, isVerified,
)

@Singleton
class ServiceRepositoryImpl @Inject constructor(
    private val api: ServiceApi,
    private val db: AppDatabase,
    private val moshi: Moshi,
) : ServiceRepository {
    override fun services(): Flow<List<EssentialService>> = db.serviceDao().observeAll().map { l -> l.map { it.toDomain() } }
    override fun service(id: String): Flow<EssentialService?> = db.serviceDao().observe(id).map { it?.toDomain() }

    /** The directory is small, so it is downloaded completely and replaced atomically. */
    override suspend fun refresh(): AppResult<Unit> = safeApiCall(moshi) {
        val all = mutableListOf<ServiceDto>()
        var page = 1
        while (page <= MAX_PAGES) {
            val response = api.list(page)
            all += response.results
            if (response.next == null) break
            page++
        }
        all
    }.map { list -> db.serviceDao().replaceAll(list.filter { it.isActive }.map { it.toEntity() }) }

    private companion object { const val MAX_PAGES = 20 }
}

class ServicesSyncTask @Inject constructor(private val repository: ServiceRepository) : SyncTask {
    override val name = "services"
    override suspend fun run() = repository.refresh() is AppResult.Success
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ServiceModule {
    @Binds @Singleton abstract fun repository(impl: ServiceRepositoryImpl): ServiceRepository
    @Binds @IntoSet abstract fun sync(impl: ServicesSyncTask): SyncTask

    companion object {
        @Provides @Singleton fun api(retrofit: Retrofit): ServiceApi = retrofit.create(ServiceApi::class.java)
    }
}
