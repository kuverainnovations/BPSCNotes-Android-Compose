package com.example.bpscnotes.presentation.subscription

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
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class SubPlan(
    val id: String,
    val name: String,
    val price: Int,
    val originalPrice: Int,
    val duration: String,
    val billingCycle: String,
    val perMonthPrice: Int,
    val coins: Int,
    val isPopular: Boolean = false,
    val isLimited: Boolean = false,
    val offerHours: Int = 0,
    val savings: Int = 0,
)

data class ExamPack(
    val examName: String,
    val emoji: String,
    val color: Color,
    val relevantFeatures: List<String>,
)

val subPlans = listOf(
    SubPlan("p1", "Monthly",   199,  299,  "1 Month",   "Billed monthly",     199, 20,  savings = 100),
    SubPlan("p2", "Quarterly", 499,  899,  "3 Months",  "₹166/month",         166, 60,  isPopular = true, isLimited = true, offerHours = 36, savings = 400),
    SubPlan("p3", "Annual",    1499, 2999, "12 Months", "₹125/month",         125, 200, isLimited = true, offerHours = 12, savings = 1500),
)

val examPacks = mapOf(
    "BPSC 70th CCE"   to ExamPack("BPSC 70th CCE",    "🎯", Color(0xFF1565C0), listOf("BPSC 70th Complete Course", "Bihar GK Intensive", "10 Full Mock Tests", "BPSC Previous Year Papers (2018-2024)", "Current Affairs — BPSC Focused")),
    "BPSC 71st CCE"   to ExamPack("BPSC 71st CCE",    "🎯", Color(0xFF1565C0), listOf("BPSC 71st Complete Course", "Bihar GK Intensive", "10 Full Mock Tests", "BPSC Previous Year Papers", "Current Affairs — BPSC Focused")),
    "Bihar Police SI" to ExamPack("Bihar Police SI",  "👮", Color(0xFF2ECC71), listOf("Bihar Police SI Complete Course", "Reasoning Master Class", "Physical Standards Guide", "Bihar GK Crash Course", "SI Previous Year Papers")),
    "SSC CGL"         to ExamPack("SSC CGL",          "🇮🇳", Color(0xFF2980B9), listOf("SSC CGL Complete Course", "Quantitative Aptitude Master", "English Excellence Course", "Reasoning Booster", "SSC CGL Previous Year Papers")),
    "SSC CHSL"        to ExamPack("SSC CHSL",         "📝", Color(0xFF3498DB), listOf("SSC CHSL Complete Course", "Typing & Computer Module", "English Grammar Master", "GK & Current Affairs", "CHSL Previous Year Papers")),
    "Railway NTPC"    to ExamPack("Railway NTPC",     "🚂", Color(0xFFE74C3C), listOf("RRB NTPC Complete Course", "Maths Speed Tricks", "GK & Current Affairs", "Reasoning Shortcut Methods", "NTPC Previous Year Papers")),
    "UPSC CSE"        to ExamPack("UPSC CSE",         "🏆", Color(0xFFF39C12), listOf("UPSC Prelims Complete Course", "GS Paper 1-4 Coverage", "CSAT Master Class", "Essay Writing Module", "UPSC Previous Year Papers")),
)

val commonFeatures = listOf(
    Triple(Icons.Rounded.PlayLesson,      "Unlimited Course Access",          true),
    Triple(Icons.Rounded.Assignment,      "Unlimited Mock Tests",             true),
    Triple(Icons.Rounded.LibraryBooks,    "E-Library Full Access",            true),
    Triple(Icons.Rounded.LiveTv,          "Live Classes",                     true),
    Triple(Icons.Rounded.Download,        "Offline Downloads",                true),
    Triple(Icons.Rounded.Block,           "Ad-free Experience",               true),
    Triple(Icons.Rounded.Groups,          "Group Study Rooms",                true),
    Triple(Icons.Rounded.Psychology,      "Active Recall Flashcards",         true),
    Triple(Icons.Rounded.EmojiEvents,     "Monthly Coin Bonus",               true),
    Triple(Icons.Rounded.SupportAgent,    "Priority Support",                 true),
    Triple(Icons.Rounded.Cancel,          "Limited Free Quiz Access",         false),
    Triple(Icons.Rounded.Cancel,          "Ads in Free Content",              false),
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun SubscriptionScreen(
    navController: NavHostController,
    primaryExam: String = "BPSC 70th CCE",  // passed from ExamSelection or Profile
) {
    var selectedPlan     by remember { mutableStateOf(subPlans[1]) }  // quarterly default
    var autoRenew        by remember { mutableStateOf(true) }
    var coinsToUse       by remember { mutableIntStateOf(0) }
    var showPayment      by remember { mutableStateOf(false) }
    var expandFeatures   by remember { mutableStateOf(false) }
    val userCoins        = 142
    val isSubscribed     = false

    val examPack         = examPacks[primaryExam] ?: examPacks["BPSC 70th CCE"]!!
    val maxCoins         = minOf(userCoins, (selectedPlan.price * 0.30).toInt())
    val coinDiscount     = (coinsToUse * 0.10).toInt()
    val finalPrice       = maxOf(0, selectedPlan.price - coinDiscount)
    val animProgress     by animateFloatAsState((selectedPlan.savings.toFloat() / selectedPlan.originalPrice), tween(800), label = "sav")

    Box(modifier = Modifier.fillMaxSize().background(BpscColors.Surface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 130.dp)
        ) {

            // ── Hero Header ──────────────────────────────────
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.linearGradient(
                            listOf(Color(0xFF0A2472), Color(0xFF1565C0), Color(0xFF1E88E5)),
                            Offset(0f, 0f), Offset(400f, 500f)
                        ))
                        .statusBarsPadding()
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(Color.White.copy(0.05f), 180.dp.toPx(), Offset(size.width + 30.dp.toPx(), -60.dp.toPx()))
                        drawCircle(Color.White.copy(0.04f), 100.dp.toPx(), Offset(-30.dp.toPx(), size.height * 0.7f))
                        val dotSpacing = 28.dp.toPx()
                        var x = dotSpacing
                        while (x < size.width) {
                            var y = dotSpacing
                            while (y < size.height) {
                                drawCircle(Color.White.copy(0.05f), 1.dp.toPx(), Offset(x, y))
                                y += dotSpacing
                            }
                            x += dotSpacing
                        }
                    }
                    // Shimmer line
                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(0.4f), Color.White.copy(0.7f), Color.White.copy(0.4f), Color.Transparent))
                    ))

                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Back + current plan
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(0.15f)).border(0.5.dp, Color.White.copy(0.2f), CircleShape).clickable { navController.popBackStack() }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.ArrowBack, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            if (isSubscribed) {
                                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFD700)).padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("Pro Active", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                }
                            } else {
                                Row(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White.copy(0.15f)).padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🪙", fontSize = 12.sp)
                                    Text("$userCoins coins", style = MaterialTheme.typography.labelSmall, color = BpscColors.CoinGold, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        // Crown + title
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("👑", fontSize = 52.sp)
                            Text("BPSCNotes Pro", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            // Exam-specific subtitle
                            Row(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(0.15f)).border(0.5.dp, Color.White.copy(0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(examPack.emoji, fontSize = 16.sp)
                                Text("Personalized for ${examPack.examName}", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                            Text("Everything you need to crack your exam", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.75f))
                        }

                        // Stats strip
                        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(0.1f)).padding(horizontal = 4.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly) {
                            HeroStat("🎓", "50+", "Courses")
                            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                            HeroStat("📝", "500+", "Mock Tests")
                            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                            HeroStat("📚", "1000+", "Resources")
                            Box(Modifier.width(1.dp).height(28.dp).background(Color.White.copy(0.2f)))
                            HeroStat("👥", "18K+", "Students")
                        }
                    }
                }
            }

            // ── Exam-specific content pack ───────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(examPack.emoji, fontSize = 20.sp)
                        Text("${examPack.examName} Pack", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                        Text("INCLUDED", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 9.sp,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BpscColors.Success).padding(horizontal = 7.dp, vertical = 3.dp))
                    }
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = examPack.color.copy(0.05f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, examPack.color.copy(0.2f))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            examPack.relevantFeatures.forEachIndexed { index, feature ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(examPack.color.copy(0.15f)), contentAlignment = Alignment.Center) {
                                        Text("${index + 1}", style = MaterialTheme.typography.labelSmall, color = examPack.color, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                                    }
                                    Text(feature, style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Rounded.CheckCircle, null, tint = examPack.color, modifier = Modifier.size(16.dp))
                                }
                                if (index < examPack.relevantFeatures.size - 1) HorizontalDivider(color = examPack.color.copy(0.1f), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Plans ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Text("Choose Your Plan", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
            }

            items(subPlans) { plan ->
                val isSelected = selectedPlan.id == plan.id
                val planDiscount = ((1f - plan.price.toFloat() / plan.originalPrice) * 100).toInt()

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedPlan = plan },
                        shape    = RoundedCornerShape(18.dp),
                        colors   = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFE8F0FD) else Color.White),
                        elevation = CardDefaults.cardElevation(if (isSelected) 6.dp else 2.dp),
                        border   = if (isSelected) BorderStroke(2.dp, BpscColors.Primary) else BorderStroke(1.dp, BpscColors.Divider)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(plan.name, style = MaterialTheme.typography.titleLarge, color = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                        if (plan.isPopular) Text("⭐ Most Popular", style = MaterialTheme.typography.labelSmall, color = Color.White, fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(BpscColors.Primary).padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Text(plan.duration, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                                    Text(plan.billingCycle, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint, fontSize = 10.sp)
                                }
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("₹${plan.originalPrice}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint, textDecoration = TextDecoration.LineThrough)
                                        Text("$planDiscount% OFF", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold,
                                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFFEE8E8)).padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                    Text("₹${plan.price}", style = MaterialTheme.typography.headlineSmall, color = if (isSelected) BpscColors.Primary else BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                                    Text("You save ₹${plan.savings}", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }

                            // Savings bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(BpscColors.Surface)) {
                                    Box(modifier = Modifier.fillMaxWidth(((1f - plan.price.toFloat() / plan.originalPrice))).fillMaxHeight()
                                        .background(if (isSelected) BpscColors.Primary else BpscColors.Divider, RoundedCornerShape(3.dp)))
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Coins bonus
                                Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (isSelected) BpscColors.PrimaryLight else BpscColors.Surface).padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("🪙", fontSize = 11.sp)
                                    Text("+${plan.coins} bonus coins", style = MaterialTheme.typography.labelSmall, color = if (isSelected) BpscColors.Primary else BpscColors.TextSecondary, fontWeight = FontWeight.Bold)
                                }
                                // Limit offer
                                if (plan.isLimited) {
                                    Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFEE8E8)).padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("⏰", fontSize = 11.sp)
                                        Text("Ends in ${plan.offerHours}h", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE74C3C), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    // Selected tick
                    if (isSelected) {
                        Box(modifier = Modifier.align(Alignment.TopStart).offset(x = (-6).dp, y = (-6).dp).size(22.dp).clip(CircleShape).background(BpscColors.Primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }

            // ── Coins slider ─────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("🪙 Use Coins for Discount", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("1 coin = ₹0.10 off · Max 30% discount via coins", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Using $coinsToUse coins", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            Text("= ₹$coinDiscount off", style = MaterialTheme.typography.titleMedium, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                        }
                        Slider(
                            value = coinsToUse.toFloat(), onValueChange = { coinsToUse = it.toInt() },
                            valueRange = 0f..maxCoins.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = BpscColors.CoinGold, activeTrackColor = BpscColors.CoinGold, inactiveTrackColor = BpscColors.Surface)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("0 coins", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                            Text("$userCoins available", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextHint)
                        }
                    }
                }
            }

            // ── Auto renewal ─────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Autorenew, null, tint = BpscColors.Primary, modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Auto-renewal", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Renew before plan expires automatically", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            }
                        }
                        Switch(checked = autoRenew, onCheckedChange = { autoRenew = it }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BpscColors.Primary))
                    }
                }
            }

            // ── All features ─────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("✅ All Pro Features", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                        Text(if (expandFeatures) "Show less ↑" else "Show all ↓", style = MaterialTheme.typography.bodyMedium, color = BpscColors.Primary, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { expandFeatures = !expandFeatures })
                    }
                    Spacer(Modifier.height(10.dp))
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            val toShow = if (expandFeatures) commonFeatures else commonFeatures.take(6)
                            toShow.forEachIndexed { index, (icon, feature, included) ->
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(if (included) BpscColors.Success.copy(0.1f) else Color(0xFFFEE8E8)), contentAlignment = Alignment.Center) {
                                        Icon(icon, null, tint = if (included) BpscColors.Success else Color(0xFFE74C3C), modifier = Modifier.size(16.dp))
                                    }
                                    Text(feature, style = MaterialTheme.typography.bodyLarge, color = if (included) BpscColors.TextPrimary else BpscColors.TextHint,
                                        textDecoration = if (!included) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f))
                                    if (included) Text("✓", style = MaterialTheme.typography.bodyLarge, color = BpscColors.Success, fontWeight = FontWeight.ExtraBold)
                                }
                                if (index < toShow.size - 1) HorizontalDivider(color = BpscColors.Divider, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ── Also included section ─────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📦 Also Included — All Exams", style = MaterialTheme.typography.titleLarge, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(examPacks.entries.filter { it.key != primaryExam }.take(5).toList()) { (exam, pack) ->
                            Card(modifier = Modifier.width(150.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(pack.emoji, fontSize = 22.sp)
                                    Text(exam, style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                                    Text("${pack.relevantFeatures.size} exclusive items", style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp)
                                    Text("Included ✓", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Trust badges ──────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TrustBadge("🔒", "Secure\nPayment")
                    TrustBadge("↩️", "Cancel\nAnytime")
                    TrustBadge("⚡", "Instant\nAccess")
                    TrustBadge("🎯", "18K+\nStudents")
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Sticky bottom CTA ────────────────────────────────
        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.White.copy(0f), Color.White, Color.White)))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Price breakdown row
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = BpscColors.Surface), elevation = CardDefaults.cardElevation(0.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${selectedPlan.name} Plan · ${selectedPlan.duration}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
                            if (coinDiscount > 0) Text("🪙 Coins: -₹$coinDiscount applied", style = MaterialTheme.typography.labelSmall, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            if (coinDiscount > 0) Text("₹${selectedPlan.price}", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint, textDecoration = TextDecoration.LineThrough)
                            Text("₹$finalPrice", style = MaterialTheme.typography.headlineSmall, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Button(
                    onClick  = { showPayment = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
                ) {
                    Icon(Icons.Rounded.CurrencyRupee, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Subscribe for ₹$finalPrice · Pay via UPI", style = MaterialTheme.typography.titleMedium)
                }
                Text("🔒 Secure · Cancel anytime · Instant access after payment", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }

        // UPI Payment sheet
        if (showPayment) {
            UPIPaymentSheet(
                amount    = finalPrice,
                plan      = selectedPlan,
                coins     = coinsToUse,
                coinSaved = coinDiscount,
                onDismiss = { showPayment = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// UPI PAYMENT SHEET
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UPIPaymentSheet(
    amount: Int,
    plan: SubPlan,
    coins: Int,
    coinSaved: Int,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Color.White,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Complete Payment", style = MaterialTheme.typography.headlineSmall, color = BpscColors.TextPrimary, fontWeight = FontWeight.ExtraBold)

            // Order summary
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = BpscColors.PrimaryLight), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${plan.name} Plan", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        Text("₹${plan.price}", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextPrimary)
                    }
                    if (coinSaved > 0) Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Coins Discount ($coins 🪙)", style = MaterialTheme.typography.bodyLarge, color = BpscColors.TextSecondary)
                        Text("- ₹$coinSaved", style = MaterialTheme.typography.bodyLarge, color = BpscColors.Success, fontWeight = FontWeight.SemiBold)
                    }
                    HorizontalDivider(color = BpscColors.Primary.copy(0.2f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("₹$amount", style = MaterialTheme.typography.titleLarge, color = BpscColors.Primary, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Text("Pay via UPI", style = MaterialTheme.typography.titleMedium, color = BpscColors.TextPrimary, fontWeight = FontWeight.Bold)

            // UPI apps grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    Triple("G", "GPay",    Color(0xFF4285F4)),
                    Triple("P", "PhonePe", Color(0xFF5F259F)),
                    Triple("₹", "Paytm",   Color(0xFF00BAF2)),
                    Triple("B", "BHIM",    Color(0xFF00796B)),
                ).forEach { (letter, app, color) ->
                    Column(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(color.copy(0.08f))
                            .border(1.dp, color.copy(0.2f), RoundedCornerShape(14.dp))
                            .clickable { }.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(letter, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.ExtraBold)
                        Text(app, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 10.sp)
                    }
                }
            }

            // Manual UPI
            Text("Or enter UPI ID manually:", style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextSecondary)
            var upiId by remember { mutableStateOf("") }
            OutlinedTextField(
                value = upiId, onValueChange = { upiId = it },
                placeholder = { Text("yourname@upi") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true,
                trailingIcon = {
                    if (upiId.contains("@")) Icon(Icons.Rounded.CheckCircle, null, tint = BpscColors.Success)
                }
            )

            Button(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Icon(Icons.Rounded.CurrencyRupee, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Pay ₹$amount · Get Instant Access", style = MaterialTheme.typography.titleMedium)
            }

            Text("🔒 256-bit encrypted · Powered by UPI · Cancel anytime",
                style = MaterialTheme.typography.bodyMedium, color = BpscColors.TextHint, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────
@Composable
private fun HeroStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.width(60.dp)) {
        Text(icon, fontSize = 14.sp)
        Text(value, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.6f), fontSize = 9.sp)
    }
}

@Composable
private fun TrustBadge(icon: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(BpscColors.PrimaryLight), contentAlignment = Alignment.Center) {
            Text(icon, fontSize = 18.sp)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = BpscColors.TextSecondary, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}