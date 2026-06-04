package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.example.model.Channel
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.*
import com.example.viewmodel.IPTVViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPTVHomeScreen(
    viewModel: IPTVViewModel,
    modifier: Modifier = Modifier
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val channels by viewModel.channels.collectAsStateWithLifecycle()
    val filteredChannels by viewModel.filteredChannels.collectAsStateWithLifecycle()
    val favoriteList by viewModel.favoriteChannels.collectAsStateWithLifecycle()
    val recentsList by viewModel.recentChannels.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val countries by viewModel.countries.collectAsStateWithLifecycle()

    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCountry by viewModel.selectedCountry.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SlateBg)
    ) {
        val isWideScreen = maxWidth >= 768.dp

        if (isWideScreen) {
            // Tablet-Desktop Layout (Sidebar + Dashboard Main Pane)
            Row(modifier = Modifier.fillMaxSize()) {
                // Sidebar
                IPTVDesktopSidebar(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { viewModel.setCategory(it) },
                    favoriteCount = favoriteList.size,
                    recentCount = recentsList.size,
                    modifier = Modifier
                        .width(260.dp)
                        .fillMaxHeight()
                )

                VerticalDivider(color = BorderColor)

                // Main Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Header Bar
                    IPTVHeaderBar(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        currentTime = currentTime,
                        liveCount = channels.size,
                        countries = countries,
                        selectedCountry = selectedCountry,
                        onCountrySelect = { viewModel.setCountry(it) }
                    )

                    // Hero and Grid Pane
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Pane: Active Media Stream + Hero Detail
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (currentChannel != null) {
                                IPTVActivePlayerCard(
                                    channel = currentChannel!!,
                                    isFavoriteFlow = viewModel.isFavorite(currentChannel!!.id),
                                    onFavoriteToggle = { viewModel.toggleFavorite(currentChannel!!) }
                                )
                            } else {
                                IPTVNoSelectionPlayerCard()
                            }
                            
                            // Live TV counter stats banner
                            IPTVStatsCard(
                                totalCount = channels.size,
                                favoritesCount = favoriteList.size,
                                recentsCount = recentsList.size
                            )
                        }

                        // Right Pane: Grid list representing filtered stream feeds
                        Column(
                            modifier = Modifier
                                .weight(1.5f)
                                .fillMaxHeight()
                        ) {
                            Text(
                                text = "Channels ($selectedCategory - $selectedCountry)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (isLoading) {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(8) {
                                        ChannelSkeletonCard()
                                    }
                                }
                            } else if (filteredChannels.isEmpty()) {
                                IPTVEmptyState(
                                    selectedCategory = selectedCategory,
                                    onResetFilters = {
                                        viewModel.setCategory("All")
                                        viewModel.setCountry("All")
                                        viewModel.setSearchQuery("")
                                    }
                                )
                            } else {
                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 160.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(filteredChannels, key = { it.id }) { channel ->
                                        IPTVChannelCard(
                                            channel = channel,
                                            isPlaying = currentChannel?.id == channel.id,
                                            onSelect = { viewModel.selectChannel(channel) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Desktop Footer
                    IPTVFooterSection(modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            // Compact Mobile Layout
            Scaffold(
                bottomBar = {
                    IPTVMobileNavigation(
                        selectedCategory = selectedCategory,
                        onCategorySelect = { viewModel.setCategory(it) }
                    )
                },
                containerColor = SlateBg
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(bottom = 8.dp)
                ) {
                    // Mobile Header with clocks and statistics
                    IPTVHeaderBar(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        currentTime = currentTime,
                        liveCount = channels.size,
                        countries = countries,
                        selectedCountry = selectedCountry,
                        onCountrySelect = { viewModel.setCountry(it) }
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Channel Video Player Frame (Hero Spot)
                        item {
                            if (currentChannel != null) {
                                IPTVActivePlayerCard(
                                    channel = currentChannel!!,
                                    isFavoriteFlow = viewModel.isFavorite(currentChannel!!.id),
                                    onFavoriteToggle = { viewModel.toggleFavorite(currentChannel!!) }
                                )
                            } else {
                                IPTVNoSelectionPlayerCard()
                            }
                        }

                        // Categories Slider Row (Quick selector)
                        item {
                            Text(
                                text = "Categories",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                items(categories) { cat ->
                                    val isSelected = cat == selectedCategory
                                    IPTVCategoryTabPill(
                                        title = cat,
                                        isSelected = isSelected,
                                        onClick = { viewModel.setCategory(cat) }
                                    )
                                }
                            }
                        }

                        // Header representing actual results
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$selectedCategory Live TV ($selectedCountry)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${filteredChannels.size} Chs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(
                                            Brush.linearGradient(listOf(Color(0x223b82f6), Color(0x2206b6d4))),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(0.5.dp, BorderColor, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Grid lists or Skeleton loads
                        if (isLoading) {
                            items(4) {
                                ChannelSkeletonCard()
                            }
                        } else if (filteredChannels.isEmpty()) {
                            item {
                                IPTVEmptyState(
                                    selectedCategory = selectedCategory,
                                    onResetFilters = {
                                        viewModel.setCategory("All")
                                        viewModel.setCountry("All")
                                        viewModel.setSearchQuery("")
                                    }
                                )
                            }
                        } else {
                            items(filteredChannels.chunked(2)) { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { channel ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            IPTVChannelCard(
                                                channel = channel,
                                                isPlaying = currentChannel?.id == channel.id,
                                                onSelect = { viewModel.selectChannel(channel) }
                                            )
                                        }
                                    }
                                    // Empty weight space if odd number
                                    if (pair.size < 2) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Mobile Footer
                    IPTVFooterSection(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
fun IPTVDesktopSidebar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    favoriteCount: Int,
    recentCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(SlateNav)
            .padding(24.dp)
    ) {
        // App Identity Label Block
        IPTVAppLogoHeader()

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation Group Lists
        Text(
            text = "LIBRARY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )

        IPTVDesktopSidebarItem(
            label = "All Channels",
            icon = Icons.Default.Tv,
            isSelected = selectedCategory == "All",
            onClick = { onCategorySelect("All") },
            trailingBadge = null
        )

        IPTVDesktopSidebarItem(
            label = "Favorites",
            icon = Icons.Default.Favorite,
            isSelected = selectedCategory == "Favorites",
            onClick = { onCategorySelect("Favorites") },
            trailingBadge = if (favoriteCount > 0) favoriteCount.toString() else null,
            badgeBg = Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFF43F5E)))
        )

        IPTVDesktopSidebarItem(
            label = "Recently Watched",
            icon = Icons.Default.History,
            isSelected = selectedCategory == "Recents",
            onClick = { onCategorySelect("Recents") },
            trailingBadge = if (recentCount > 0) recentCount.toString() else null
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "CATEGORIES",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )

        // Filtering custom library categories scroll
        val listCategories = categories.filter { it != "All" && it != "Favorites" && it != "Recents" }
        
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(listCategories) { cat ->
                val isSelected = cat == selectedCategory
                val catIcon = when {
                    cat.contains("sport", ignoreCase = true) -> Icons.Default.SportsBasketball
                    cat.contains("movie", ignoreCase = true) -> Icons.Default.Movie
                    cat.contains("news", ignoreCase = true) -> Icons.Default.Announcement
                    cat.contains("kid", ignoreCase = true) -> Icons.Default.ChildCare
                    cat.contains("music", ignoreCase = true) -> Icons.Default.MusicNote
                    else -> Icons.Default.Category
                }
                IPTVDesktopSidebarItem(
                    label = cat,
                    icon = catIcon,
                    isSelected = isSelected,
                    onClick = { onCategorySelect(cat) }
                )
            }
        }
    }
}

@Composable
fun IPTVDesktopSidebarItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    trailingBadge: String? = null,
    badgeBg: Brush = Brush.horizontalGradient(listOf(NeonBlue, NeonCyan))
) {
    val bgModifier = if (isSelected) {
        Modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFF1D4ED8).copy(alpha = 0.4f), Color.Transparent)),
            RoundedCornerShape(10.dp)
        )
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(bgModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) NeonCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (trailingBadge != null) {
            Box(
                modifier = Modifier
                    .background(badgeBg, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = trailingBadge,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun IPTVAppLogoHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(NeonBlue, NeonCyan))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = "AS AHMAD SHAMIM",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
            Text(
                text = "IPTV STREAMING",
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IPTVHeaderBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    currentTime: String,
    liveCount: Int,
    countries: List<String>,
    selectedCountry: String,
    onCountrySelect: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateNav)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: Logo (on mobile), Info Badge counters, clock
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Clock & Logo Info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        IPTVAppLogoHeader()
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Stats live counter pill
                    Surface(
                        color = Color(0x333b82f6),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(0xFF22C55E), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Content: $liveCount",
                                fontSize = 11.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Real-time Clock display
                Surface(
                    color = SlateCard.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderColor)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Active Clock",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentTime.ifBlank { "Live Sync" },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search panel and Country dropdown selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Search Input with Glassmorphism border
                TextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search channel, category, country...", color = TextSecondary, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = TextSecondary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SlateBg,
                        unfocusedContainerColor = SlateBg,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                        .testTag("search_channels_input")
                )

                // Country Filter Dropdown
                var showCountryDropdown by remember { mutableStateOf(false) }

                Box {
                    Surface(
                        onClick = { showCountryDropdown = true },
                        color = SlateBg,
                        border = BorderStroke(1.dp, BorderColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(46.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (selectedCountry == "All") "All Regions" else selectedCountry,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 100.dp)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                        }
                    }

                    DropdownMenu(
                        expanded = showCountryDropdown,
                        onDismissRequest = { showCountryDropdown = false },
                        modifier = Modifier
                            .width(180.dp)
                            .background(SlateCard)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Regions", color = TextPrimary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                onCountrySelect("All")
                                showCountryDropdown = false
                            }
                        )
                        countries.filter { it != "All" }.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country, color = TextPrimary) },
                                onClick = {
                                    onCountrySelect(country)
                                    showCountryDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = BorderColor)
    }
}

@Composable
fun IPTVActivePlayerCard(
    channel: Channel,
    isFavoriteFlow: kotlinx.coroutines.flow.Flow<Boolean>,
    onFavoriteToggle: () -> Unit
) {
    val isFavorite by isFavoriteFlow.collectAsState(initial = false)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .testTag("active_player_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Media3 player view screen
            VideoPlayer(
                url = channel.url,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Streaming control details bar (Title, toggle active favorites, labels)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo loaded lazily
                    SubcomposeAsyncImage(
                        model = channel.logo,
                        contentDescription = "Active channel Logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp),
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.linearGradient(listOf(NeonBlue, NeonCyan))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = channel.name.take(2).uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.Red,
                                shape = RoundedCornerShape(3.dp),
                                modifier = Modifier.padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "LIVE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = channel.name,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = channel.category,
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(modifier = Modifier.size(4.dp).background(TextSecondary, CircleShape))
                            Text(
                                text = channel.country,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Add to Favorite Button
                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .border(1.dp, BorderColor, CircleShape)
                        .testTag("toggle_favorite_button")
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle stream favorite",
                        tint = if (isFavorite) Color.Red else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun IPTVNoSelectionPlayerCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.03f), CircleShape)
                        .border(1.dp, BorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TvOff,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Channel Active",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please choose a channel below to initiate live IPTV stream decoding",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun IPTVCategoryTabPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillModifier = if (isSelected) {
        Modifier
            .background(Brush.linearGradient(listOf(NeonBlue, NeonCyan)), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
    } else {
        Modifier
            .background(SlateCard, RoundedCornerShape(20.dp))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
    }

    Box(
        modifier = Modifier
            .then(pillModifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (title == "Favorites") {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = if (isSelected) Color.White else Color.Red, modifier = Modifier.size(12.dp))
            } else if (title == "Recents") {
                Icon(Icons.Default.History, contentDescription = null, tint = if (isSelected) Color.White else NeonCyan, modifier = Modifier.size(12.dp))
            }
            Text(
                text = title,
                color = if (isSelected) Color.White else TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun IPTVChannelCard(
    channel: Channel,
    isPlaying: Boolean,
    onSelect: () -> Unit
) {
    val cardBackground = if (isPlaying) {
        Brush.radialGradient(
            colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.5f), SlateCard),
            radius = 350f
        )
    } else {
        Brush.linearGradient(listOf(SlateCard, SlateCard))
    }

    val cardBorder = if (isPlaying) {
        BorderStroke(2.dp, Brush.horizontalGradient(listOf(NeonBlue, NeonCyan)))
    } else {
        BorderStroke(1.dp, GlassBorder)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        border = cardBorder,
        modifier = Modifier
            .fillMaxWidth()
            .height(134.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelect() }
            .testTag("channel_item_${channel.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackground)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Logo & active player visualiser
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = channel.logo,
                            contentDescription = "Channel logo",
                            contentScale = ContentScale.Fit,
                            loading = { CircularProgressIndicator(strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp)) },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(listOf(NeonBlue, NeonCyan))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = channel.name.take(2).uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                        )
                    }

                    // Live badge active feed
                    if (isPlaying) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "PLAYING",
                                color = Color(0xFF34D399),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color.Red.copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color.Red),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color.Red,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Bottom area: Channel title labels
                Column {
                    Text(
                        text = channel.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${channel.category} • ${channel.country}",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ChannelSkeletonCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(134.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SlateCard)
            .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
            }
            Column {
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                )
            }
        }
    }
}

@Composable
fun IPTVStatsCard(
    totalCount: Int,
    favoritesCount: Int,
    recentsCount: Int
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IPTVStatColumn(metric = totalCount.toString(), label = "Streams")
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderColor))
            IPTVStatColumn(metric = favoritesCount.toString(), label = "Favorites")
            Box(modifier = Modifier.width(1.dp).height(30.dp).background(BorderColor))
            IPTVStatColumn(metric = recentsCount.toString(), label = "Recent")
        }
    }
}

@Composable
fun IPTVStatColumn(metric: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = metric,
            fontSize = 18.sp,
            color = NeonCyan,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IPTVEmptyState(
    selectedCategory: String,
    onResetFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.SentimentDissatisfied,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No channels matched selection",
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try searching for a different region, channel, or clear filters.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onResetFilters,
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceAccent)
            ) {
                Text("Reset Filters")
            }
        }
    }
}

@Composable
fun IPTVMobileNavigation(
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    NavigationBar(
        containerColor = SlateNav,
        tonalElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Tv, contentDescription = "Streams") },
            label = { Text("Streams", fontSize = 10.sp) },
            selected = selectedCategory != "Favorites" && selectedCategory != "Recents",
            onClick = { onCategorySelect("All") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonCyan,
                selectedTextColor = NeonCyan,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color(0x333b82f6)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
            label = { Text("Favorites", fontSize = 10.sp) },
            selected = selectedCategory == "Favorites",
            onClick = { onCategorySelect("Favorites") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Red,
                selectedTextColor = Color.Red,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color(0x33ef4444)
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = "Recent History") },
            label = { Text("Recents", fontSize = 10.sp) },
            selected = selectedCategory == "Recents",
            onClick = { onCategorySelect("Recents") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NeonBlue,
                selectedTextColor = NeonBlue,
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary,
                indicatorColor = Color(0x333b82f6)
            )
        )
    }
}

@Composable
fun IPTVFooterSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        HorizontalDivider(color = BorderColor)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateNav)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "AS AHMAD SHAMIM - IPTV • Premium Streaming Platform © 2026",
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}
