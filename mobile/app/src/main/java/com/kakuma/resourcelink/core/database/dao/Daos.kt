package com.kakuma.resourcelink.core.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.kakuma.resourcelink.core.database.entity.DraftSyncState
import com.kakuma.resourcelink.core.database.entity.OfflineArticleEntity
import com.kakuma.resourcelink.core.database.entity.OpportunityEntity
import com.kakuma.resourcelink.core.database.entity.PendingOperationEntity
import com.kakuma.resourcelink.core.database.entity.PendingOperationType
import com.kakuma.resourcelink.core.database.entity.RemoteKeyEntity
import com.kakuma.resourcelink.core.database.entity.ServiceEntity
import com.kakuma.resourcelink.core.database.entity.StoryDraftEntity
import com.kakuma.resourcelink.core.database.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpportunityDao {
    @Query("SELECT * FROM opportunities WHERE listKey = :listKey ORDER BY position")
    fun pagingSource(listKey: String): PagingSource<Int, OpportunityEntity>

    @Query("SELECT * FROM opportunities WHERE id = :id LIMIT 1")
    fun observe(id: String): Flow<OpportunityEntity?>

    @Query("SELECT * FROM opportunities WHERE listKey = :listKey ORDER BY position LIMIT :limit")
    fun observeList(listKey: String, limit: Int): Flow<List<OpportunityEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM opportunities WHERE listKey = :listKey")
    suspend fun maxPosition(listKey: String): Int

    @Upsert
    suspend fun upsertAll(items: List<OpportunityEntity>)

    @Query("DELETE FROM opportunities WHERE listKey = :listKey")
    suspend fun clearList(listKey: String)

    @Query("UPDATE opportunities SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
}

@Dao
interface RemoteKeyDao {
    @Query("SELECT * FROM remote_keys WHERE listKey = :listKey")
    suspend fun get(listKey: String): RemoteKeyEntity?

    @Upsert
    suspend fun upsert(key: RemoteKeyEntity)

    @Query("DELETE FROM remote_keys WHERE listKey = :listKey")
    suspend fun clear(listKey: String)
}

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories WHERE listKey = :listKey ORDER BY position")
    fun pagingSource(listKey: String): PagingSource<Int, StoryEntity>

    @Query("SELECT * FROM stories WHERE listKey = :listKey ORDER BY position LIMIT :limit")
    fun observeList(listKey: String, limit: Int): Flow<List<StoryEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM stories WHERE listKey = :listKey")
    suspend fun maxPosition(listKey: String): Int

    @Upsert
    suspend fun upsertAll(items: List<StoryEntity>)

    @Query("DELETE FROM stories WHERE listKey = :listKey")
    suspend fun clearList(listKey: String)

    @Query("UPDATE stories SET isLiked = :liked, likesCount = :likes WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean, likes: Int)

    @Query("UPDATE stories SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
}

@Dao
interface OfflineArticleDao {
    @Query("SELECT * FROM offline_articles WHERE id = :id")
    fun observe(id: String): Flow<OfflineArticleEntity?>

    @Query("SELECT * FROM offline_articles ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<OfflineArticleEntity>>

    @Upsert
    suspend fun upsert(article: OfflineArticleEntity)

    @Query("DELETE FROM offline_articles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE offline_articles SET isLiked = :liked, likesCount = :likes WHERE id = :id")
    suspend fun setLiked(id: String, liked: Boolean, likes: Int)

    @Query("UPDATE offline_articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)
}

@Dao
interface StoryDraftDao {
    @Query("SELECT * FROM story_drafts ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<StoryDraftEntity>>

    @Query("SELECT * FROM story_drafts WHERE localId = :localId")
    suspend fun get(localId: String): StoryDraftEntity?

    @Query("SELECT * FROM story_drafts WHERE syncState = :state")
    suspend fun withState(state: DraftSyncState): List<StoryDraftEntity>

    @Upsert
    suspend fun upsert(draft: StoryDraftEntity)

    @Query("DELETE FROM story_drafts WHERE localId = :localId")
    suspend fun delete(localId: String)
}

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt")
    suspend fun all(): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE type = :type AND targetId = :targetId")
    suspend fun delete(type: PendingOperationType, targetId: String)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE pending_operations SET attempts = attempts + 1 WHERE id = :id")
    suspend fun incrementAttempts(id: Long)

    /** Queue an action, cancelling its opposite (e.g. bookmark then unbookmark while offline = nothing). */
    @Transaction
    suspend fun enqueue(type: PendingOperationType, opposite: PendingOperationType, targetId: String, now: Long) {
        delete(opposite, targetId)
        insert(PendingOperationEntity(type = type, targetId = targetId, createdAt = now))
    }
}

@Dao
interface ServiceDao {
    @Query("SELECT * FROM services ORDER BY name")
    fun observeAll(): Flow<List<ServiceEntity>>

    @Query("SELECT * FROM services WHERE id = :id")
    fun observe(id: String): Flow<ServiceEntity?>

    @Transaction
    suspend fun replaceAll(items: List<ServiceEntity>) {
        clear()
        upsertAll(items)
    }

    @Upsert
    suspend fun upsertAll(items: List<ServiceEntity>)

    @Query("DELETE FROM services")
    suspend fun clear()
}
