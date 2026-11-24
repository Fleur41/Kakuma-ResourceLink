package com.kakuma.resourcelink.core.network

import androidx.paging.PagingSource
import androidx.paging.PagingState
import retrofit2.HttpException
import java.io.IOException

/** Network-only paging over the backend's `{count,next,previous,results}` pages (for data not cached on device). */
class NetworkPagingSource<Dto : Any, T : Any>(
    private val fetch: suspend (page: Int, pageSize: Int) -> PageDto<Dto>,
    private val map: (Dto) -> T,
) : PagingSource<Int, T>() {
    override fun getRefreshKey(state: PagingState<Int, T>): Int? =
        state.anchorPosition?.let { state.closestPageToPosition(it)?.prevKey?.plus(1) ?: state.closestPageToPosition(it)?.nextKey?.minus(1) }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, T> {
        val page = params.key ?: 1
        return try {
            val response = fetch(page, params.loadSize.coerceAtMost(50))
            LoadResult.Page(
                data = response.results.map(map),
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (response.next != null) page + 1 else null,
            )
        } catch (e: IOException) {
            LoadResult.Error(e)
        } catch (e: HttpException) {
            LoadResult.Error(e)
        }
    }
}
