package com.example.bpscnotes.presentation.notesreader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────
data class NotePage(
    val pageNumber: Int,
    val heading: String,
    val content: String,
    val subSections: List<NoteSection> = emptyList(),
)

data class NoteSection(
    val title: String,
    val content: String,
    val isImportant: Boolean = false,
)

data class HighlightedText(
    val pageNumber: Int,
    val text: String,
    val color: Color,
)

enum class ReaderTheme(
    val label: String,
    val bgColor: Color,
    val textColor: Color,
    val lineColor: Color
) {
    Notebook("Notebook", Color(0xFFFFFDE7), Color(0xFF1A1A2E), Color(0xFFBBDEFB)),
    White("White", Color.White, Color(0xFF1A1A2E), Color(0xFFE3F2FD)),
    Sepia("Sepia", Color(0xFFF5E6C8), Color(0xFF3E2723), Color(0xFFD7CCC8)),
    Dark("Dark", Color(0xFF1A1A2E), Color(0xFFE8EAED), Color(0xFF2C3E50)),
    Green("Green", Color(0xFFE8F5E9), Color(0xFF1B5E20), Color(0xFFA5D6A7)),
}

val mockNotePages = listOf(
    NotePage(
        1, "Fundamental Rights — Article 12 to 35",
        "Part III of the Indian Constitution (Articles 12-35) deals with Fundamental Rights. These are justiciable rights guaranteed to every citizen of India. They are enforceable in courts of law.",
        listOf(
            NoteSection(
                "Definition of State (Article 12)",
                "Article 12 defines 'State' to include the Government and Parliament of India, Government and Legislature of each State, and all local or other authorities within the territory of India. This definition is important to understand against whom Fundamental Rights can be enforced.",
                isImportant = true
            ),
            NoteSection(
                "Doctrine of Waiver",
                "A person can waive his Fundamental Rights as these are personal rights. However, a person cannot waive a right which has been conferred not only in individual interest but also as a matter of public policy."
            ),
            NoteSection(
                "Article 13 — Laws Inconsistent with Rights",
                "Article 13 declares that any law that takes away or abridges Fundamental Rights shall be void to the extent of the inconsistency. This is the basis of judicial review in India.",
                isImportant = true
            ),
        )
    ),
    NotePage(
        2, "Right to Equality — Articles 14-18",
        "The Right to Equality is guaranteed under Articles 14 to 18. It ensures every person is treated equally before law.",
        listOf(
            NoteSection(
                "Article 14 — Equality Before Law",
                "The State shall not deny to any person equality before the law or the equal protection of the laws within the territory of India. Borrowed from British Constitution. Equality before law = negative concept. Equal protection of laws = positive concept.",
                isImportant = true
            ),
            NoteSection(
                "Article 15 — Prohibition of Discrimination",
                "The State shall not discriminate against any citizen on grounds only of religion, race, caste, sex, place of birth or any of them. This applies only to citizens, not foreigners."
            ),
            NoteSection(
                "Article 16 — Equal Opportunity in Public Employment",
                "There shall be equality of opportunity for all citizens in matters relating to employment or appointment to any office under the State.",
                isImportant = true
            ),
            NoteSection(
                "Article 17 — Abolition of Untouchability",
                "Untouchability is abolished and its practice in any form is forbidden. Enforcement of any disability arising out of Untouchability shall be a punishable offence.",
                isImportant = true
            ),
            NoteSection(
                "Article 18 — Abolition of Titles",
                "No title, not being a military or academic distinction, shall be conferred by the State. No citizen shall accept any title from any foreign State."
            ),
        )
    ),
    NotePage(
        3, "Right to Freedom — Articles 19-22",
        "Article 19 guarantees six fundamental freedoms to the citizens of India. These freedoms are not absolute and can be restricted on reasonable grounds.",
        listOf(
            NoteSection(
                "Six Freedoms under Article 19",
                "1. Freedom of speech and expression\n" +
                        "2. Freedom to assemble peaceably and without arms\n" +
                        "3. Freedom to form associations or unions\n" +
                        "4. Freedom to move freely throughout India\n" +
                        "5. Freedom to reside and settle in any part of India\n" +
                        "6. Freedom to practice any profession or carry on any occupation, trade or business",
                isImportant = true
            ),
            NoteSection(
                "Article 20 — Protection in Conviction",
                "No person shall be convicted of any offence except for violation of a law in force. No person shall be prosecuted for the same offence more than once (Double Jeopardy). No person shall be compelled to be a witness against himself (Self-incrimination)."
            ),
            NoteSection(
                "Article 21 — Right to Life",
                "No person shall be deprived of his life or personal liberty except according to procedure established by law. This is the most important Fundamental Right, given widest interpretation by Supreme Court.",
                isImportant = true
            ),
            NoteSection(
                "Article 21A — Right to Education",
                "The State shall provide free and compulsory education to all children of the age of 6 to 14 years. Added by 86th Constitutional Amendment Act, 2002.",
                isImportant = true
            ),
            NoteSection(
                "Article 22 — Protection Against Arrest",
                "No person who is arrested shall be detained in custody without being informed of the grounds of arrest. Every arrested person shall have the right to consult a lawyer of his choice."
            ),
        )
    ),
    NotePage(
        4, "Right Against Exploitation — Articles 23-24",
        "Articles 23 and 24 protect citizens against exploitation in various forms including forced labour and child labour.",
        listOf(
            NoteSection(
                "Article 23 — Prohibition of Trafficking",
                "Traffic in human beings and begar and other similar forms of forced labour are prohibited. Any contravention shall be an offence punishable in accordance with law.",
                isImportant = true
            ),
            NoteSection(
                "Article 24 — Prohibition of Child Labour",
                "No child below the age of 14 years shall be employed to work in any factory or mine or engaged in any other hazardous employment. This is an absolute prohibition.",
                isImportant = true
            ),
            NoteSection(
                "Important Cases",
                "People's Union for Democratic Rights v. Union of India (1982) — Supreme Court held that payment of wages below minimum wage amounts to forced labour under Article 23."
            ),
        )
    ),
    NotePage(
        5, "Right to Freedom of Religion — Articles 25-28",
        "Articles 25 to 28 guarantee the freedom of conscience and the right to freely profess, practice and propagate religion.",
        listOf(
            NoteSection(
                "Article 25 — Freedom of Conscience",
                "Subject to public order, morality and health, all persons are equally entitled to freedom of conscience and the right to freely profess, practise and propagate religion.",
                isImportant = true
            ),
            NoteSection(
                "Article 26 — Freedom to Manage Religious Affairs",
                "Every religious denomination has the right to establish and maintain institutions for religious and charitable purposes, manage its own affairs in matters of religion."
            ),
            NoteSection(
                "Article 27 — Freedom from Tax for Religion",
                "No person shall be compelled to pay any taxes, the proceeds of which are specifically appropriated in payment of expenses for the promotion or maintenance of any particular religion."
            ),
            NoteSection(
                "Article 28 — Freedom from Religious Instruction",
                "No religious instruction shall be provided in any educational institution wholly maintained out of State funds.",
                isImportant = true
            ),
        )
    ),
    NotePage(
        6, "Cultural and Educational Rights — Articles 29-30",
        "Articles 29 and 30 protect the interests of minorities and their right to conserve their culture and establish educational institutions.",
        listOf(
            NoteSection(
                "Article 29 — Protection of Interests of Minorities",
                "Any section of citizens residing in the territory of India having a distinct language, script or culture shall have the right to conserve the same. No citizen shall be denied admission into any State-maintained institution on grounds of religion, race, caste or language.",
                isImportant = true
            ),
            NoteSection(
                "Article 30 — Right of Minorities to Establish Institutions",
                "All minorities, whether based on religion or language, shall have the right to establish and administer educational institutions of their choice. This is called the Minorities' Magna Carta.",
                isImportant = true
            ),
            NoteSection(
                "T.M.A. Pai Foundation Case",
                "The Supreme Court in 2002 held that the right to establish educational institutions is not limited to linguistic or religious minorities but extends to all citizens under Article 19(1)(g)."
            ),
        )
    ),
    NotePage(
        7, "Right to Constitutional Remedies — Article 32",
        "Article 32 is called the 'Heart and Soul' of the Constitution by Dr. B.R. Ambedkar. It provides the right to move the Supreme Court for enforcement of Fundamental Rights.",
        listOf(
            NoteSection(
                "Five Writs Under Article 32",
                "1. Habeas Corpus — 'You may have the body' — to release a person from unlawful detention\n" +
                        "2. Mandamus — 'We command' — to perform a public duty\n" +
                        "3. Prohibition — issued by superior court to inferior court to stop exceeding jurisdiction\n" +
                        "4. Certiorari — to quash the order of inferior court/tribunal\n" +
                        "5. Quo Warranto — to enquire by what authority a person holds a public office",
                isImportant = true
            ),
            NoteSection(
                "Article 32 vs Article 226",
                "Article 32 — Only Supreme Court, only for Fundamental Rights enforcement.\nArticle 226 — High Court, for both Fundamental Rights AND other legal rights. High Court has wider jurisdiction.",
                isImportant = true
            ),
            NoteSection(
                "Suspension of Article 32",
                "Article 32 can be suspended only during National Emergency under Article 359. Article 21 (Right to Life) cannot be suspended even during emergency after 44th Amendment, 1978."
            ),
        )
    ),
    NotePage(
        8, "Directive Principles of State Policy — Articles 36-51",
        "Part IV of the Constitution (Articles 36-51) contains Directive Principles of State Policy. These are non-justiciable in nature but are fundamental in the governance of the country.",
        listOf(
            NoteSection(
                "Nature of DPSPs",
                "DPSPs are borrowed from the Irish Constitution. They are not enforceable by any court but are fundamental in governance. They aim to establish a welfare state.",
                isImportant = true
            ),
            NoteSection(
                "Classification of DPSPs",
                "Socialistic Principles:\n— Article 38: State to secure social order for welfare\n— Article 39: Equal pay for equal work\n— Article 41: Right to work, education and public assistance\n\n" +
                        "Gandhian Principles:\n— Article 40: Organize village panchayats\n— Article 43: Cottage industries\n— Article 46: Promote educational interests of SCs/STs\n\n" +
                        "Liberal-Intellectual Principles:\n— Article 44: Uniform Civil Code\n— Article 45: Early childhood care\n— Article 48: Prohibition of cow slaughter",
                isImportant = true
            ),
            NoteSection(
                "Conflict Between FR and DPSP",
                "Originally Fundamental Rights prevailed over DPSPs. But after 42nd Amendment (1976), certain DPSPs were given precedence over FRs. The Minerva Mills case (1980) restored the balance."
            ),
        )
    ),
    NotePage(
        9, "Parliament of India — Lok Sabha & Rajya Sabha",
        "The Parliament of India consists of the President and two Houses — Rajya Sabha (Council of States) and Lok Sabha (House of the People). It is the supreme legislative body.",
        listOf(
            NoteSection(
                "Lok Sabha — Key Facts",
                "• Maximum strength: 552 (530 states + 20 UTs + 2 Anglo-Indians — now abolished)\n" +
                        "• Current strength: 543 elected members\n" +
                        "• Term: 5 years (can be dissolved earlier)\n" +
                        "• Minimum age: 25 years\n" +
                        "• Presided by: Speaker\n" +
                        "• Money Bills originate only in Lok Sabha",
                isImportant = true
            ),
            NoteSection(
                "Rajya Sabha — Key Facts",
                "• Maximum strength: 250 (238 elected + 12 nominated by President)\n" +
                        "• Current strength: 245 members\n" +
                        "• Permanent House: Cannot be dissolved\n" +
                        "• 1/3rd members retire every 2 years\n" +
                        "• Term of each member: 6 years\n" +
                        "• Minimum age: 30 years\n" +
                        "• Presided by: Vice President (ex-officio Chairman)",
                isImportant = true
            ),
            NoteSection(
                "Special Powers of Rajya Sabha",
                "Article 249 — Can authorize Parliament to make law on State List subject (2/3rd majority). Article 312 — Can create new All India Services (2/3rd majority). These are exclusive powers of Rajya Sabha.",
                isImportant = true
            ),
        )
    ),
    NotePage(
        10, "Preamble of Indian Constitution",
        "The Preamble is the introduction to the Constitution. It was amended once by the 42nd Constitutional Amendment Act, 1976 which added the words 'Socialist', 'Secular' and 'Integrity'.",
        listOf(
            NoteSection(
                "Complete Preamble Text",
                "\"WE, THE PEOPLE OF INDIA, having solemnly resolved to constitute India into a SOVEREIGN SOCIALIST SECULAR DEMOCRATIC REPUBLIC and to secure to all its citizens:\n\n" +
                        "JUSTICE, social, economic and political;\n" +
                        "LIBERTY of thought, expression, belief, faith and worship;\n" +
                        "EQUALITY of status and of opportunity;\n" +
                        "and to promote among them all\n" +
                        "FRATERNITY assuring the dignity of the individual and the unity and integrity of the Nation;\n\n" +
                        "IN OUR CONSTITUENT ASSEMBLY this twenty-sixth day of November, 1949, do HEREBY ADOPT, ENACT AND GIVE TO OURSELVES THIS CONSTITUTION.\"",
                isImportant = true
            ),
            NoteSection(
                "Key Words Explained",
                "• Sovereign — India is not dependent on or dominated by any external power\n" +
                        "• Socialist — Social and economic equality (added in 1976)\n" +
                        "• Secular — No state religion, equal respect for all religions (added in 1976)\n" +
                        "• Democratic — Government by the people, for the people, of the people\n" +
                        "• Republic — Elected head of state (President), not hereditary monarchy",
                isImportant = true
            ),
            NoteSection(
                "Is Preamble Part of Constitution?",
                "Berubari Union Case (1960) — SC said Preamble is not part of Constitution.\nKesavananda Bharati Case (1973) — SC overruled and held Preamble IS part of Constitution.\nLIC of India Case (1995) — SC reaffirmed Preamble is part of Constitution.",
                isImportant = true
            ),
        )
    ),
)

val tableOfContents = listOf(
    "Fundamental Rights — Article 12 to 35",
    "Right to Equality — Articles 14-18",
    "Right to Freedom — Articles 19-22",
    "Right Against Exploitation — Articles 23-24",
    "Right to Freedom of Religion — Articles 25-28",
    "Cultural and Educational Rights — Articles 29-30",
    "Right to Constitutional Remedies — Article 32",
    "Directive Principles of State Policy",
    "Parliament — Lok Sabha & Rajya Sabha",
    "Preamble of Indian Constitution",
)

// ─────────────────────────────────────────────────────────────
// MAIN SCREEN
// ─────────────────────────────────────────────────────────────
@Composable
fun NotesReaderScreen(
    navController: NavHostController,
    noteId: String,
) {
    var currentTheme by remember { mutableStateOf(ReaderTheme.Notebook) }
    var fontSize by remember { mutableFloatStateOf(16f) }
    var showControls by remember { mutableStateOf(true) }
    var showTOC by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var showFontPicker by remember { mutableStateOf(false) }
    val bookmarked = remember { mutableStateListOf<Int>() }
    val highlights = remember { mutableStateListOf<HighlightedText>() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-hide controls after tap
    LaunchedEffect(showControls) {
        if (showControls) {
            kotlinx.coroutines.delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bgColor)
            .clickable { showControls = !showControls }
    ) {
        // Notebook ruled lines background (only for Notebook theme)
        if (currentTheme == ReaderTheme.Notebook) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val lineSpacing = 32.dp.toPx()
                var y = lineSpacing * 3
                while (y < size.height) {
                    drawLine(
                        color = currentTheme.lineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.8f
                    )
                    y += lineSpacing
                }
                // Left margin line (red)
                drawLine(
                    color = Color(0xFFFFCDD2),
                    start = Offset(52.dp.toPx(), 0f),
                    end = Offset(52.dp.toPx(), size.height),
                    strokeWidth = 1.5f
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    currentTheme.bgColor,
                                    currentTheme.bgColor.copy(alpha = 0.95f),
                                    Color.Transparent
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(currentTheme.textColor.copy(0.08f))
                                .clickable { navController.popBackStack() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.ArrowBack,
                                null,
                                tint = currentTheme.textColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        // Title
                        Text(
                            "Polity Notes",
                            style = MaterialTheme.typography.titleMedium,
                            color = currentTheme.textColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        )
                        // Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                Icons.Rounded.FormatListBulleted to { showTOC = true },
                                Icons.Rounded.Bookmark to {
                                    val page = listState.firstVisibleItemIndex + 1
                                    if (bookmarked.contains(page)) bookmarked.remove(page) else bookmarked.add(
                                        page
                                    )
                                    Unit
                                },
                                Icons.Rounded.FormatSize to { showFontPicker = !showFontPicker },
                                Icons.Rounded.Palette to { showThemePicker = !showThemePicker },
                            ).forEach { (icon, action) ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(currentTheme.textColor.copy(0.08f))
                                        .clickable(onClick = action),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        icon,
                                        null,
                                        tint = currentTheme.textColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Pages ─────────────────────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = if (showControls) 0.dp else 60.dp,
                    bottom = 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(mockNotePages) { page ->
                    val isBookmarked = bookmarked.contains(page.pageNumber)

                    Box(modifier = Modifier.fillMaxWidth()) {

                        PageWatermark(theme = currentTheme)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            // Page heading
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    page.heading,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = (fontSize + 4).sp),
                                    color = when (currentTheme) {
                                        ReaderTheme.Notebook -> Color(0xFF1A237E)
                                        ReaderTheme.Sepia -> Color(0xFF4E342E)
                                        ReaderTheme.Dark -> Color(0xFF82B1FF)
                                        ReaderTheme.Green -> Color(0xFF1B5E20)
                                        else -> BpscColors.Primary
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isBookmarked) Icon(
                                    Icons.Rounded.Bookmark,
                                    null,
                                    tint = Color(0xFFE74C3C),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // Intro paragraph
                            Text(
                                page.content,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
                                color = currentTheme.textColor,
                                lineHeight = (fontSize * 1.8f).sp
                            )

                            Spacer(Modifier.height(12.dp))

                            // Sub-sections
                            page.subSections.forEach { section ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .then(
                                            if (section.isImportant) Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (currentTheme) {
                                                        ReaderTheme.Notebook -> Color(0xFFFFF9C4)
                                                        ReaderTheme.Dark -> Color(0xFF263238)
                                                        ReaderTheme.Sepia -> Color(0xFFFFF8E1)
                                                        else -> BpscColors.PrimaryLight
                                                    }
                                                )
                                                .border(
                                                    1.dp,
                                                    when (currentTheme) {
                                                        ReaderTheme.Notebook -> Color(0xFFFDD835)
                                                        ReaderTheme.Dark -> Color(0xFF37474F)
                                                        else -> BpscColors.Primary.copy(0.2f)
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(10.dp)
                                            else Modifier
                                        ),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (section.isImportant) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFE74C3C))
                                            )
                                        }
                                        Text(
                                            section.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontSize = (fontSize + 1).sp
                                            ),
                                            color = when (currentTheme) {
                                                ReaderTheme.Notebook -> Color(0xFF283593)
                                                ReaderTheme.Dark -> Color(0xFF64B5F6)
                                                ReaderTheme.Sepia -> Color(0xFF4E342E)
                                                ReaderTheme.Green -> Color(0xFF2E7D32)
                                                else -> BpscColors.Primary
                                            },
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Text(
                                        section.content,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
                                        color = currentTheme.textColor,
                                        lineHeight = (fontSize * 1.8f).sp
                                    )
                                }
                            }

                            // Page divider
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = currentTheme.textColor.copy(0.1f)
                                )
                                Text(
                                    "Page ${page.pageNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = currentTheme.textColor.copy(0.4f),
                                    fontSize = 10.sp
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = currentTheme.textColor.copy(0.1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Bottom bar ───────────────────────────────────────
        AnimatedVisibility(
            visible = showControls,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                currentTheme.bgColor
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomAction(
                        Icons.Rounded.FormatColorText,
                        "Highlight",
                        currentTheme.textColor
                    ) { /* highlight */ }
                    BottomAction(Icons.Rounded.Share, "Share", currentTheme.textColor) { }
                    BottomAction(Icons.Rounded.Search, "Search", currentTheme.textColor) { }
                    BottomAction(Icons.Rounded.Download, "Save", currentTheme.textColor) { }
                    BottomAction(
                        Icons.Rounded.FullscreenExit,
                        "Focus",
                        currentTheme.textColor
                    ) { showControls = false }
                }
            }
        }

        // ── Font size picker ─────────────────────────────────
        AnimatedVisibility(
            visible = showFontPicker,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Font Size",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${fontSize.toInt()}px",
                            style = MaterialTheme.typography.titleMedium,
                            color = BpscColors.Primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BpscColors.PrimaryLight)
                                .clickable { if (fontSize > 12f) fontSize -= 2f },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Remove,
                                null,
                                tint = BpscColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Slider(
                            value = fontSize,
                            onValueChange = { fontSize = it },
                            valueRange = 12f..24f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = BpscColors.Primary,
                                activeTrackColor = BpscColors.Primary
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BpscColors.PrimaryLight)
                                .clickable { if (fontSize < 24f) fontSize += 2f },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                null,
                                tint = BpscColors.Primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    // Preview
                    Text(
                        "Preview text at ${fontSize.toInt()}px size",
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp),
                        color = BpscColors.TextPrimary
                    )
                }
            }
        }

        // ── Theme picker ─────────────────────────────────────
        AnimatedVisibility(
            visible = showThemePicker,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Reading Theme",
                        style = MaterialTheme.typography.titleMedium,
                        color = BpscColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReaderTheme.values().forEach { theme ->
                            val isSelected = currentTheme == theme
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(theme.bgColor)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) BpscColors.Primary else Color.LightGray,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { currentTheme = theme; showThemePicker = false }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Ruled line preview
                                repeat(3) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.5.dp)
                                            .background(theme.lineColor)
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                Text(
                                    theme.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.textColor,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) Icon(
                                    Icons.Rounded.CheckCircle,
                                    null,
                                    tint = BpscColors.Primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Table of contents ────────────────────────────────
        if (showTOC) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.5f))
                    .clickable { showTOC = false }, contentAlignment = Alignment.CenterStart
            ) {
                Card(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight()
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .statusBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Contents",
                                style = MaterialTheme.typography.titleLarge,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(
                                Icons.Rounded.Close,
                                null,
                                tint = BpscColors.TextHint,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { showTOC = false })
                        }
                        Spacer(Modifier.height(16.dp))
                        tableOfContents.forEachIndexed { index, title ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (listState.firstVisibleItemIndex == index) BpscColors.PrimaryLight else Color.Transparent)
                                    .clickable {
                                        scope.launch {
                                            listState.animateScrollToItem(index)
                                            showTOC = false
                                        }
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (listState.firstVisibleItemIndex == index) BpscColors.Primary else BpscColors.Surface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (listState.firstVisibleItemIndex == index) Color.White else BpscColors.TextSecondary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }
                                Text(
                                    title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (listState.firstVisibleItemIndex == index) BpscColors.Primary else BpscColors.TextPrimary,
                                    fontWeight = if (listState.firstVisibleItemIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    lineHeight = 18.sp
                                )
                            }
                            HorizontalDivider(color = BpscColors.Divider, thickness = 0.5.dp)
                        }
                        Spacer(Modifier.weight(1f))
                        // Bookmarks list
                        if (bookmarked.isNotEmpty()) {
                            Text(
                                "🔖 Bookmarked",
                                style = MaterialTheme.typography.titleMedium,
                                color = BpscColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            bookmarked.forEach { page ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFFF8E1))
                                        .padding(10.dp)
                                        .clickable {
                                            scope.launch {
                                                listState.animateScrollToItem(page - 1); showTOC =
                                                false
                                            }
                                            Unit
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Bookmark,
                                        null,
                                        tint = Color(0xFFE74C3C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        "Page $page",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BpscColors.TextPrimary
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint.copy(0.7f),
            fontSize = 9.sp
        )
    }
}


@Composable
private fun PageWatermark(theme: ReaderTheme) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle ring
        androidx.compose.foundation.Canvas(modifier = Modifier.size(160.dp)) {
            drawCircle(
                color = if (theme == ReaderTheme.Dark) Color.White.copy(0.04f) else Color(0xFF1565C0).copy(
                    0.04f
                ),
                radius = size.minDimension / 2,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = if (theme == ReaderTheme.Dark) Color.White.copy(0.025f) else Color(
                    0xFF1565C0
                ).copy(0.025f),
                radius = size.minDimension / 2.3f,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
        // Logo + app name stacked
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.alpha(0.07f)   // ← very light watermark
        ) {
            // App icon placeholder — replace with your actual Image(painterResource) when logo is ready
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (theme == ReaderTheme.Dark) Color.White.copy(0.08f) else Color(
                            0xFF1565C0
                        ).copy(0.08f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "B", style = MaterialTheme.typography.displaySmall,
                    color = if (theme == ReaderTheme.Dark) Color.White else Color(0xFF1565C0),
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Text(
                "BPSCNotes",
                style = MaterialTheme.typography.titleLarge,
                color = if (theme == ReaderTheme.Dark) Color.White else Color(0xFF1565C0),
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp
            )
            Text(
                "Only What Matters",
                style = MaterialTheme.typography.bodyMedium,
                color = if (theme == ReaderTheme.Dark) Color.White else Color(0xFF1565C0),
                letterSpacing = 1.sp
            )
        }
    }
}