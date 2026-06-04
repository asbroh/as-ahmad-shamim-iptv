package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.Channel
import com.example.network.IptvNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class IPTVViewModel(application: Application) : AndroidViewModel(application) {
    // Database initialization
    private val database = androidx.room.Room.databaseBuilder(
        application,
        IptvDatabase::class.java,
        "iptv_database"
    ).fallbackToDestructiveMigration().build()
    private val iptvDao = database.iptvDao()
    private val repository = IptvRepository(iptvDao)

    // UI States
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCountry = MutableStateFlow("All")
    val selectedCountry: StateFlow<String> = _selectedCountry.asStateFlow()

    // Currently playing channel
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    // Real-time Clock String
    private val _currentTime = MutableStateFlow("")
    val currentTime: StateFlow<String> = _currentTime.asStateFlow()

    // Favorites & Recents from DB
    val favoriteChannels: StateFlow<List<Channel>> = repository.favorites
        .map { list ->
            list.map { entity ->
                Channel(entity.id, entity.name, entity.logo, entity.category, entity.country, entity.url)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentChannels: StateFlow<List<Channel>> = repository.recents
        .map { list ->
            list.map { entity ->
                Channel(entity.id, entity.name, entity.logo, entity.category, entity.country, entity.url)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All distinct categories
    val categories: StateFlow<List<String>> = _channels.map { list ->
        val cats = list.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + cats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // All distinct countries
    val countries: StateFlow<List<String>> = _channels.map { list ->
        val counts = list.map { it.country }.filter { it.isNotBlank() }.distinct().sorted()
        listOf("All") + counts
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    // Filtered lists
    val filteredChannels: StateFlow<List<Channel>> = combine(
        listOf(_channels, _searchQuery, _selectedCategory, _selectedCountry, favoriteChannels, recentChannels)
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val channels = array[0] as List<Channel>
        val query = array[1] as String
        val category = array[2] as String
        val country = array[3] as String
        @Suppress("UNCHECKED_CAST")
        val favs = array[4] as List<Channel>
        @Suppress("UNCHECKED_CAST")
        val recents = array[5] as List<Channel>

        var result = when (category) {
            "Favorites" -> favs
            "Recents" -> recents
            "All" -> channels
            else -> channels.filter { it.category.equals(category, ignoreCase = true) }
        }

        // Country filtering
        if (country != "All") {
            result = result.filter { it.country.equals(country, ignoreCase = true) }
        }

        // Search query filtering
        if (query.isNotBlank()) {
            result = result.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.country.contains(query, ignoreCase = true)
            }
        }
        
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchChannels()
        startClock()
    }

    private fun startClock() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                _currentTime.value = sdf.format(Date())
                delay(1000)
            }
        }
    }

    fun fetchChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // Fetch from remote channels.json
                val response = IptvNetworkClient.apiService.getChannels(
                    "https://raw.githubusercontent.com/foridul422/IPTV-/main/channels.json"
                )
                val mapped = response.map { it.toChannel() }
                if (mapped.isNotEmpty()) {
                    _channels.value = mapped
                    // Auto-select first channel as featured hero channel
                    if (_currentChannel.value == null) {
                        _currentChannel.value = mapped.firstOrNull()
                    }
                } else {
                    loadFallbackChannels()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadFallbackChannels()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadFallbackChannels() {
        // High quality fallback channels if link fails or offline
        val fallbacks = listOf(
            Channel(
                id = "france24",
                name = "France 24 English",
                logo = "https://www.france24.com/favicon.ico",
                category = "News",
                country = "France",
                url = "https://static.france24.com/live/F24_EN_LO_HLS/live_tv.m3u8"
            ),
            Channel(
                id = "nasa",
                name = "NASA TV Broadcast",
                logo = "https://www.nasa.gov/wp-content/themes/nasa-gutenberg/assets/images/nasa-logo.svg",
                category = "Science",
                country = "USA",
                url = "https://ntv1.akamaized.net/hls/live/2014027/NASA-NTV1-HLS/master.m3u8"
            ),
            Channel(
                id = "redbull",
                name = "Red Bull TV",
                logo = "https://epg.redbull.tv/logo.png",
                category = "Sports",
                country = "Global",
                url = "https://rdliveextra-i.akamaihd.net/hls/live/243645/extra/master.m3u8"
            ),
            Channel(
                id = "euronews",
                name = "Euronews English Live",
                logo = "https://www.euronews.com/favicon.ico",
                category = "News",
                country = "Europe",
                url = "https://euronews-eng.live.hexaglobe.net/live/eng/index.m3u8"
            )
        )
        _channels.value = fallbacks
        if (_currentChannel.value == null) {
            _currentChannel.value = fallbacks.firstOrNull()
        }
    }

    fun selectChannel(channel: Channel) {
        _currentChannel.value = channel
        // Add to recents database
        viewModelScope.launch(Dispatchers.IO) {
            repository.addRecent(
                RecentChannelEntity(
                    id = channel.id,
                    name = channel.name,
                    logo = channel.logo,
                    category = channel.category,
                    country = channel.country,
                    url = channel.url,
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun toggleFavorite(channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            val isFav = repository.isFavoriteDirect(channel.id)
            if (isFav) {
                repository.removeFavorite(channel.id)
            } else {
                repository.addFavorite(
                    FavoriteChannelEntity(
                        id = channel.id,
                        name = channel.name,
                        logo = channel.logo,
                        category = channel.category,
                        country = channel.country,
                        url = channel.url
                    )
                )
            }
        }
    }

    fun isFavorite(channelId: String): Flow<Boolean> {
        return iptvDao.isFavorite(channelId)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setCountry(country: String) {
        _selectedCountry.value = country
    }
}
