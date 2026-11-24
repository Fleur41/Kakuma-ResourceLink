package com.kakuma.resourcelink.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.kakuma.resourcelink.core.database.dao.OfflineArticleDao
import com.kakuma.resourcelink.core.database.dao.OpportunityDao
import com.kakuma.resourcelink.core.database.dao.PendingOperationDao
import com.kakuma.resourcelink.core.database.dao.RemoteKeyDao
import com.kakuma.resourcelink.core.database.dao.ServiceDao
import com.kakuma.resourcelink.core.database.dao.StoryDao
import com.kakuma.resourcelink.core.database.dao.StoryDraftDao
import com.kakuma.resourcelink.core.database.entity.OfflineArticleEntity
import com.kakuma.resourcelink.core.database.entity.OpportunityEntity
import com.kakuma.resourcelink.core.database.entity.PendingOperationEntity
import com.kakuma.resourcelink.core.database.entity.RemoteKeyEntity
import com.kakuma.resourcelink.core.database.entity.ServiceEntity
import com.kakuma.resourcelink.core.database.entity.StoryDraftEntity
import com.kakuma.resourcelink.core.database.entity.StoryEntity

class Converters {
    // Unit separator never appears in user text, so a plain join is safe and avoids JSON overhead.
    @TypeConverter fun fromList(value: List<String>): String = value.joinToString(SEP)
    @TypeConverter fun toList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(SEP)

    private companion object { const val SEP = "\u001F" }
}

@Database(
    entities = [
        OpportunityEntity::class, RemoteKeyEntity::class, StoryEntity::class, OfflineArticleEntity::class,
        StoryDraftEntity::class, PendingOperationEntity::class, ServiceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun opportunityDao(): OpportunityDao
    abstract fun remoteKeyDao(): RemoteKeyDao
    abstract fun storyDao(): StoryDao
    abstract fun offlineArticleDao(): OfflineArticleDao
    abstract fun storyDraftDao(): StoryDraftDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun serviceDao(): ServiceDao
}
