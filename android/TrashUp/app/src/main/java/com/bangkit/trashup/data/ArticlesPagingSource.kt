package com.bangkit.trashup.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bangkit.trashup.data.remote.response.DatasItem
import com.bangkit.trashup.data.remote.retrofit.ApiService

class ArticlesPagingSource(private val apiService: ApiService) : PagingSource<Int, DatasItem>() {
    private companion object {
        const val INITIAL_PAGE_INDEX = 1
    }

    override fun getRefreshKey(state: PagingState<Int, DatasItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, DatasItem> {
        return try {
            val position = params.key ?: INITIAL_PAGE_INDEX
            val response = apiService.getArticles(position, params.loadSize)

            if (response.isSuccessful) {
                val responseData = response.body()?.payload?.datas.orEmpty().filterNotNull()
                LoadResult.Page(
                    data = responseData,
                    prevKey = if (position == INITIAL_PAGE_INDEX) null else position - 1,
                    nextKey = if (responseData.isEmpty()) null else position + 1
                )
            } else {
                LoadResult.Error(Exception("Error: ${response.message()}"))
            }
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }
}