package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorite_channels")
data class FavoriteChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String,
    val category: String,
    val country: String,
    val url: String,
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_channels")
data class RecentChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val logo: String,
    val category: String,
    val country: String,
    val url: String,
    val watchedAt: Long = System.currentTimeMillis()
)

@Dao
interface IptvDao {
    // Favorites
    @Query("SELECT * FROM favorite_channels ORDER BY savedAt DESC")
    fun getFavorites(): Flow<List<FavoriteChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(channel: FavoriteChannelEntity)

    @Query("DELETE FROM favorite_channels WHERE id = :id")
    suspend fun removeFavorite(id: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE id = :id LIMIT 1)")
    fun isFavorite(id: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_channels WHERE id = :id LIMIT 1)")
    suspend fun isFavoriteDirect(id: String): Boolean

    // Recents
    @Query("SELECT * FROM recent_channels ORDER BY watchedAt DESC LIMIT 30")
    fun getRecents(): Flow<List<RecentChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addRecent(channel: RecentChannelEntity)

    @Query("DELETE FROM recent_channels WHERE id = :id")
    suspend fun removeRecent(id: String)

    @Query("DELETE FROM recent_channels")
    suspend fun clearHistory()
}

@Database(entities = [FavoriteChannelEntity::class, RecentChannelEntity::class], version = 1, exportSchema = false)
abstract class IptvDatabase : RoomDatabase() {
    abstract fun iptvDao(): IptvDao
}

class IptvRepository(private val dao: IptvDao) {
    val favorites: Flow<List<FavoriteChannelEntity>> = dao.getFavorites()
    val recents: Flow<List<RecentChannelEntity>> = dao.getRecents()

    suspend fun addFavorite(channel: FavoriteChannelEntity) {
        dao.addFavorite(channel)
    }

    suspend fun removeFavorite(id: String) {
        dao.removeFavorite(id)
    }

    suspend fun isFavoriteDirect(id: String): Boolean {
        return dao.isFavoriteDirect(id)
    }

    suspend fun addRecent(channel: RecentChannelEntity) {
        dao.addRecent(channel)
    }

    suspend fun removeRecent(id: String) {
        dao.removeRecent(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }
}
