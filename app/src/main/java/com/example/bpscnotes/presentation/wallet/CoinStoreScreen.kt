package com.example.bpscnotes.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.AppLoader
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.data.remote.api.CoinStoreItemDto

@Composable
fun CoinStoreScreen(
    navController: NavHostController,
    viewModel: CoinStoreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    // Show success/error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.fillMaxSize().background(BpscColors.Surface).padding(padding)) {
            // Header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Color(0xFF0A2472), Color(0xFF1565C0))))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(Color.White.copy(0.15f))
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
                        Text("Coin Store", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(0.12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🪙", fontSize = 22.sp)
                        Column {
                            Text("Your Balance", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                            Text("${state.balance} coins", style = MaterialTheme.typography.titleMedium, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { AppLoader() }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🏪", fontSize = 48.sp)
                        Text("Store coming soon!", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items) { item ->
                            StoreItemCard(
                                item        = item,
                                balance     = state.balance,
                                isRedeeming = state.redeeming == item.id,
                                onRedeem    = { viewModel.redeem(item.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreItemCard(
    item: CoinStoreItemDto,
    balance: Int,
    isRedeeming: Boolean,
    onRedeem: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val canAfford = balance >= item.coinCost
    val inStock = item.stock == null || item.stock > 0

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(
                    when (item.itemType) {
                        "discount"      -> Color(0xFFE8F5E9)
                        "flashcard_pack"-> Color(0xFFE3F2FD)
                        "mock_unlock"   -> Color(0xFFFFF3E0)
                        else            -> Color(0xFFF3E5F5)
                    }
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (item.itemType) {
                        "discount"       -> "💸"
                        "flashcard_pack" -> "📚"
                        "mock_unlock"    -> "🔓"
                        "badge"          -> "🏅"
                        else             -> "🎁"
                    },
                    fontSize = 24.sp
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, fontWeight = FontWeight.Bold)
                if (!item.description.isNullOrBlank()) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🪙", fontSize = 12.sp)
                    Text("${item.coinCost} coins", style = MaterialTheme.typography.labelSmall, color = if (canAfford) BpscColors.Primary else Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                    if (item.stock != null && item.stock <= 5) {
                        Text("• ${item.stock} left", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE65100))
                    }
                }
            }

            Button(
                onClick   = onRedeem,
                enabled   = canAfford && inStock && !isRedeeming,
                shape     = RoundedCornerShape(10.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary),
                modifier  = Modifier.defaultMinSize(minWidth = 80.dp)
            ) {
                if (isRedeeming) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(if (!canAfford) "Need more" else "Redeem", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
