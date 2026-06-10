package com.example.bpscnotes.presentation.marketplace

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.language.LocalStrings
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.dto.ApiResponse
import com.example.bpscnotes.presentation.navigation.popBackStackSafe
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.http.*
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────
// DTOs
// ─────────────────────────────────────────────────────────────

data class MarketplaceItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val subject: String,
    @SerializedName("exam_tags")      val examTags: List<String> = emptyList(),
    val price: Int = 0,
    @SerializedName("original_price") val originalPrice: Int? = null,
    @SerializedName("thumbnail_url")  val thumbnailUrl: String? = null,
    @SerializedName("preview_url")    val previewUrl: String? = null,
    @SerializedName("total_pages")    val totalPages: Int = 0,
    val downloads: Int = 0,
    val rating: Float = 0f,
    @SerializedName("review_count")   val reviewCount: Int = 0,
    @SerializedName("seller_name")    val sellerName: String = "Student",
    @SerializedName("is_purchased")   val isPurchased: Boolean = false,
    @SerializedName("is_featured")    val isFeatured: Boolean = false,
    @SerializedName("created_at")     val createdAt: String? = null
) {
    val isFree get() = price == 0
    val displayPrice get() = if (isFree) "Free" else "🪙 $price"
}

data class MarketplaceListData(val items: List<MarketplaceItem> = emptyList())
data class MarketplaceDetailData(val item: MarketplaceItem? = null)
data class PurchaseData(val purchased: Boolean = false, val alreadyPurchased: Boolean = false)
data class FileAccessData(@SerializedName("file_url") val fileUrl: String? = null)


// ─────────────────────────────────────────────────────────────
// UI STATE + VIEW MODEL
// ─────────────────────────────────────────────────────────────

data class MarketplaceUiState(
    val items:           List<MarketplaceItem> = emptyList(),
    val isLoading:       Boolean               = true,
    val error:           String?               = null,
    val isComingSoon:    Boolean               = false,   // backend not live yet
    val searchQuery:     String                = "",
    val selectedSubject: String                = "",
    val selectedSort:    String                = "popular",
    val purchasing:      String?               = null,
    val purchaseSuccess: String?               = null,
    val purchaseError:   String?               = null,
)



// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────

@Composable
fun MarketplaceScreen(
    nav: NavHostController,
    viewModel: MarketplaceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val str = LocalStrings.current
    val cs = MaterialTheme.colorScheme
    LaunchedEffect(Unit) { com.example.bpscnotes.core.analytics.Event.screenView("marketplace") }
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.purchaseSuccess) {
        state.purchaseSuccess?.let { snackbarHost.showSnackbar(it); viewModel.clearMessages() }
    }
    LaunchedEffect(state.purchaseError) {
        state.purchaseError?.let { snackbarHost.showSnackbar("❌ $it"); viewModel.clearMessages() }
    }

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHost) },
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0)
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {

            // ── Coming Soon banner (while marketplace feature is not live) ──
            if (state.isComingSoon) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("🛍️", style = MaterialTheme.typography.displayMedium)
                        Text(
                            "Marketplace Coming Soon",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = cs.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            "Buy and sell study materials created by fellow BPSC aspirants.\nLaunching soon!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                return@Scaffold
            }

            // ── Header ────────────────────────────────────────────
            Box(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(
                        listOf(Color(0xFF1A237E), Color(0xFF1565C0), Color(0xFF1E88E5)),
                        Offset.Zero, Offset(400f, 200f)
                    ))
                    .statusBarsPadding()
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(Color.White.copy(0.05f), 140.dp.toPx(), Offset(size.width + 10f, -40f))
                    drawCircle(Color.White.copy(0.04f), 70.dp.toPx(),  Offset(-10f, size.height))
                }
                Column(Modifier.padding(20.dp, 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).clickable { nav.popBackStackSafe() }, Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text(str.marketTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(str.marketSubtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.7f))
                            }
                        }
                        // Upload button
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.15f))
                                .clickable { /* TODO: navigate to upload screen */ }.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Upload, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(str.marketSell, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Creator economy info strip
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.1f)).padding(12.dp, 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MarketStat("🪙", "Earn 70%", "Of each sale")
                        Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.2f)))
                        MarketStat("📚", "${state.items.size}+", "Notes available")
                        Box(Modifier.width(1.dp).height(24.dp).background(Color.White.copy(0.2f)))
                        MarketStat("🔒", "100%", "Secure payment")
                    }
                }
            }

            // ── Search + Sort ─────────────────────────────────────
            Column(Modifier.fillMaxWidth().background(cs.surface).padding(16.dp, 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Search bar
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(cs.background).padding(12.dp, 0.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Search, null, tint = BpscColors.TextHint, modifier = Modifier.size(18.dp))
                    var searchText by remember { mutableStateOf(state.searchQuery) }
                    androidx.compose.foundation.text.BasicTextField(
                        value         = searchText,
                        onValueChange = { searchText = it; viewModel.setSearch(it) },
                        modifier      = Modifier.weight(1f).padding(vertical = 12.dp),
                        textStyle     = MaterialTheme.typography.bodyMedium.copy(color = cs.onSurface),
                        singleLine    = true,
                        decorationBox = { inner ->
                            if (searchText.isEmpty()) Text(str.marketSearchHint, color = BpscColors.TextHint)
                            inner()
                        }
                    )
                }

                // Subject + Sort filter row
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val subjects = listOf("" to "All", "Polity" to "⚖️ Polity", "History" to "🏛️ History",
                        "Economy" to "💰 Economy", "Geography" to "🗺️ Geography", "Bihar GK" to "🏔️ Bihar GK",
                        "Science" to "🔬 Science")
                    items(subjects) { (value, label) ->
                        val selected = state.selectedSubject == value
                        Box(
                            Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (selected) BpscColors.Primary else BpscColors.Surface)
                                .clickable { viewModel.setSubject(value) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) Color.White else BpscColors.TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = BpscColors.Primary)
                }
                state.error != null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text(state.error!!, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                        Button(onClick = { viewModel.load() }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                            Text(str.retry)
                        }
                    }
                }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("📚", fontSize = 48.sp)
                        Text(str.marketNoNotes, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text(str.marketBeFirst, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                        Button(onClick = { /* upload */ }, colors = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)) {
                            Icon(Icons.Rounded.Upload, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(str.marketUpload)
                        }
                    }
                }
                else -> LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier            = Modifier.fillMaxSize()
                ) {
                    // Featured items (horizontal row)
                    val featured = state.items.filter { it.isFeatured }
                    if (featured.isNotEmpty()) {
                        item {
                            Text(str.marketFeatured, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(featured) { item -> FeaturedItemCard(item, purchasing = state.purchasing == item.id) { viewModel.purchase(item.id) } }
                            }
                        }
                        item { Spacer(Modifier.height(4.dp)) }
                    }

                    item {
                        Text(
                            if (state.selectedSubject.isEmpty()) "All Notes (${state.items.size})" else "${state.selectedSubject} Notes",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = cs.onSurface
                        )
                    }

                    items(state.items.filter { !it.isFeatured }, key = { it.id }) { item ->
                        MarketplaceItemCard(
                            item      = item,
                            purchasing = state.purchasing == item.id,
                            onBuy     = { viewModel.purchase(item.id) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ITEM CARD
// ─────────────────────────────────────────────────────────────

@Composable
private fun MarketplaceItemCard(item: MarketplaceItem, purchasing: Boolean, onBuy: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val subjectColor = when (item.subject) {
        "Polity"    -> Color(0xFF9B59B6)
        "History"   -> Color(0xFFE74C3C)
        "Economy"   -> Color(0xFFE67E22)
        "Geography" -> Color(0xFF1ABC9C)
        "Bihar GK"  -> Color(0xFFF39C12)
        "Science"   -> Color(0xFF2ECC71)
        else        -> BpscColors.Primary
    }
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            // Thumbnail / placeholder
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(subjectColor.copy(0.1f)),
                Alignment.Center
            ) {
                if (!item.thumbnailUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = item.thumbnailUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Text(
                        when (item.subject) { "Polity"->"⚖️"; "History"->"🏛️"; "Economy"->"💰"; "Geography"->"🗺️"; "Bihar GK"->"🏔️"; "Science"->"🔬"; else->"📄" },
                        fontSize = 28.sp
                    )
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                // Subject chip
                Text(item.subject, style = MaterialTheme.typography.labelSmall, color = subjectColor, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(subjectColor.copy(0.1f)).padding(horizontal = 7.dp, vertical = 2.dp))

                Text(item.title, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (item.rating > 0f) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                            Text("%.1f".format(item.rating), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (item.totalPages > 0) Text("${item.totalPages}p", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                    Text("by ${item.sellerName}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, maxLines = 1)
                }

                // Price + Buy button
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    if (item.originalPrice != null && item.originalPrice > item.price && !item.isFree) {
                        Column {
                            Text("🪙 ${item.price}", style = MaterialTheme.typography.titleMedium, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                            Text("🪙 ${item.originalPrice}", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint,
                                textDecoration = TextDecoration.LineThrough)
                        }
                    } else {
                        Text(item.displayPrice, style = MaterialTheme.typography.titleMedium,
                            color = if (item.isFree) BpscColors.Success else BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }

                    if (item.isPurchased) {
                        Row(
                            Modifier.clip(RoundedCornerShape(10.dp)).background(BpscColors.Success.copy(0.1f)).padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(14.dp))
                            Text(str.marketOwned, style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick   = onBuy,
                            enabled   = !purchasing,
                            shape     = RoundedCornerShape(10.dp),
                            colors    = ButtonDefaults.buttonColors(containerColor = if (item.isFree) BpscColors.Success else BpscColors.Primary),
                            modifier  = Modifier.height(34.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp)
                        ) {
                            if (purchasing) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (item.isFree) str.marketGetFree else "Buy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedItemCard(item: MarketplaceItem, purchasing: Boolean, onBuy: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val str = LocalStrings.current
    val accent = when (item.subject) {
        "Polity" -> Color(0xFF9B59B6); "History" -> Color(0xFFE74C3C); "Economy" -> Color(0xFFE67E22)
        "Geography" -> Color(0xFF1ABC9C); "Bihar GK" -> Color(0xFFF39C12); else -> BpscColors.Primary
    }
    Card(
        modifier  = Modifier.width(220.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(10.dp)).background(accent.copy(0.1f)), Alignment.Center) {
                Text(when (item.subject) { "Polity"->"⚖️"; "History"->"🏛️"; "Economy"->"💰"; "Geography"->"🗺️"; "Bihar GK"->"🏔️"; else->"📄" }, fontSize = 32.sp)
                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFFFD700)).padding(horizontal = 5.dp, vertical = 2.dp)) {
                    Text(str.marketFeatured, style = MaterialTheme.typography.labelSmall, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(item.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, color = cs.onSurface)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(item.displayPrice, style = MaterialTheme.typography.titleSmall, color = if (item.isFree) BpscColors.Success else BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                if (!item.isPurchased) {
                    Button(onClick = onBuy, enabled = !purchasing, shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 10.dp)) {
                        if (purchasing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                        else Text(if (item.isFree) "Get" else "Buy", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MarketStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(icon, fontSize = 11.sp)
            Text(value, style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}
