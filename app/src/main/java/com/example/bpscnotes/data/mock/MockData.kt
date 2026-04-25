package com.example.bpscnotes.data.mock

import com.example.bpscnotes.data.remote.dto.UserDto
import com.example.bpscnotes.domain.model.*

object MockData {

    // ─── User ───────────────────────────────────────────
    val currentUser = UserDto(
        id = "u001",
        name = "Rahul Kumar",
        mobile = "9876543210",
        email = "rahul@example.com",
        isSubscribed = true,
        createdAt = "2024-01-15"
    )

    // ─── Weekly Consistency ─────────────────────────────
    val weeklyConsistency = listOf(
        DayProgress("Mon", 20),
        DayProgress("Tue", 45),
        DayProgress("Wed", 30),
        DayProgress("Thu", 60),
        DayProgress("Fri", 55),
        DayProgress("Sat", 75),
        DayProgress("Sun", 85),
    )

    // ─── Daily Targets ──────────────────────────────────
    val dailyTargets = listOf(
        DailyTarget(id = "t1", title = "Polity - Chapter 5: Fundamental Rights", subject = "Polity", isCompleted = true,  totalQuestions = 10, attemptedQuestions = 10),
        DailyTarget(id = "t2", title = "History - Mughal Empire Overview",        subject = "History", isCompleted = true,  totalQuestions = 10, attemptedQuestions = 7),
        DailyTarget(id = "t3", title = "Geography - River Systems of India",      subject = "Geography", isCompleted = false, totalQuestions = 10, attemptedQuestions = 0),
        DailyTarget(id = "t4", title = "Economy - Budget & Fiscal Policy",        subject = "Economy", isCompleted = false, totalQuestions = 10, attemptedQuestions = 0),
        DailyTarget(id = "t5", title = "Science - Environment & Ecology",         subject = "Science", isCompleted = false, totalQuestions = 10, attemptedQuestions = 0),
    )

    // ─── Current Affairs ────────────────────────────────
    val prelmsCurrentAffairs = listOf(
        CurrentAffairItem(id = "ca1", date = "13 Mar 2025", headline = "India launches GSAT-20 satellite successfully", category = "Science & Tech", isBookmarked = false),
        CurrentAffairItem(id = "ca2", date = "13 Mar 2025", headline = "RBI keeps repo rate unchanged at 6.5%", category = "Economy", isBookmarked = true),
        CurrentAffairItem(id = "ca3", date = "13 Mar 2025", headline = "India ranks 132nd in Human Development Index 2024", category = "International", isBookmarked = false),
        CurrentAffairItem(id = "ca4", date = "13 Mar 2025", headline = "Bihar government launches new skill development scheme", category = "Bihar", isBookmarked = false),
        CurrentAffairItem(id = "ca5", date = "13 Mar 2025", headline = "PM inaugurates National Highway projects worth ₹12,000 Cr", category = "Infrastructure", isBookmarked = true),
        CurrentAffairItem(id = "ca6", date = "12 Mar 2025", headline = "India's forex reserves hit all-time high of \$645 billion", category = "Economy", isBookmarked = false),
        CurrentAffairItem(id = "ca7", date = "12 Mar 2025", headline = "SC verdict on electoral bonds scheme", category = "Polity", isBookmarked = false),
        CurrentAffairItem(id = "ca8", date = "12 Mar 2025", headline = "New species of frog discovered in Western Ghats", category = "Environment", isBookmarked = false),
    )

    val mainsCurrentAffairs = listOf(
        CurrentAffairItem(id = "m1", date = "13 Mar 2025", headline = "Critically analyse India's fiscal deficit management in 2024-25", category = "Economy", isBookmarked = false,
            detail = "The Union Budget 2024-25 set a fiscal deficit target of 5.1% of GDP. The government has been walking a tightrope between growth stimulus and fiscal consolidation. Key concerns include rising subsidy burden, capital expenditure quality, and revenue buoyancy assumptions. Analysts argue that while the headline number looks manageable, off-balance sheet liabilities remain a concern for long-term fiscal health."),
        CurrentAffairItem(id = "m2", date = "13 Mar 2025", headline = "Discuss the implications of India's demographic dividend for economic growth", category = "Society", isBookmarked = true,
            detail = "India is set to become the world's most populous nation with a median age of 28 years. The demographic dividend, if harnessed properly through education and skill development, could add 1-2% to GDP growth annually. However, challenges like jobless growth, skill mismatch, and regional disparities in human development indices must be addressed urgently."),
        CurrentAffairItem(id = "m3", date = "12 Mar 2025", headline = "Evaluate the role of cooperative federalism in India's development", category = "Polity", isBookmarked = false,
            detail = "Cooperative federalism has gained prominence with initiatives like GST Council, NITI Aayog, and various centrally sponsored schemes. The 15th Finance Commission's recommendations emphasize greater state autonomy. However, tensions between Centre and states over revenue sharing, Governor's role, and concurrent list subjects continue to test the federal fabric."),
    )

    // ─── Quiz Questions ──────────────────────────────────
    val quizQuestions = listOf(
        QuizQuestion(id = "q1", question = "Which article of the Indian Constitution deals with the Right to Equality?", options = listOf("Article 12", "Article 14", "Article 19", "Article 21"), correctIndex = 1, explanation = "Article 14 guarantees equality before law and equal protection of laws to all persons within the territory of India."),
        QuizQuestion(id = "q2", question = "The term 'Secular' was added to the Preamble by which Constitutional Amendment?", options = listOf("42nd Amendment", "44th Amendment", "46th Amendment", "52nd Amendment"), correctIndex = 0, explanation = "The 42nd Constitutional Amendment Act of 1976 added the words 'Socialist', 'Secular', and 'Integrity' to the Preamble."),
        QuizQuestion(id = "q3", question = "Which river is known as the 'Sorrow of Bihar'?", options = listOf("Gandak", "Kosi", "Bagmati", "Mahananda"), correctIndex = 1, explanation = "Kosi river is known as the 'Sorrow of Bihar' due to its frequent floods that cause massive destruction in the state."),
        QuizQuestion(id = "q4", question = "Who was the first Chief Minister of Bihar after independence?", options = listOf("Sri Krishna Sinha", "Binodanand Jha", "K.B. Sahay", "Bhola Paswan"), correctIndex = 0, explanation = "Sri Krishna Sinha (Shri Babu) was the first Chief Minister of Bihar, serving from 1946 to 1961."),
        QuizQuestion(id = "q5", question = "The Champaran Satyagraha of 1917 was related to which issue?", options = listOf("Salt tax", "Indigo cultivation", "Land revenue", "Forest rights"), correctIndex = 1, explanation = "The Champaran Satyagraha was Gandhi's first civil disobedience movement in India, fighting against the forced cultivation of indigo by British planters."),
        QuizQuestion(id = "q6", question = "Which BPSC exam is conducted for Class 1 officers?", options = listOf("30th BPSC", "56th BPSC", "67th BPSC", "69th BPSC"), correctIndex = 2, explanation = "The 67th BPSC Combined Competitive Examination is conducted for recruitment to Class 1 and Class 2 services in Bihar."),
        QuizQuestion(id = "q7", question = "The concept of 'Basic Structure' of the Constitution was propounded in which case?", options = listOf("Golaknath Case", "Kesavananda Bharati Case", "Minerva Mills Case", "Maneka Gandhi Case"), correctIndex = 1, explanation = "The Kesavananda Bharati v. State of Kerala (1973) case established the Basic Structure doctrine, limiting Parliament's power to amend the Constitution."),
        QuizQuestion(id = "q8", question = "Which Five Year Plan introduced the concept of 'Inclusive Growth' in India?", options = listOf("9th Plan", "10th Plan", "11th Plan", "12th Plan"), correctIndex = 2, explanation = "The 11th Five Year Plan (2007-2012) had 'Inclusive Growth' as its central theme, focusing on reducing inequality and poverty."),
        QuizQuestion(id = "q9", question = "Under which schedule of the Constitution are the Panchayati Raj institutions governed?", options = listOf("9th Schedule", "10th Schedule", "11th Schedule", "12th Schedule"), correctIndex = 2, explanation = "The 11th Schedule (added by 73rd Amendment) contains 29 subjects over which Panchayats have jurisdiction."),
        QuizQuestion(id = "q10", question = "What is the minimum age for membership in the Rajya Sabha?", options = listOf("21 years", "25 years", "30 years", "35 years"), correctIndex = 2, explanation = "As per Article 84, the minimum age for membership in the Rajya Sabha is 30 years."),
    )

    // ─── Flashcards ───────────────────────────────────────
    val flashcards = listOf(
        Flashcard(id = "f1", subject = "Polity",    front = "What is Article 21?",                           back = "Protection of Life and Personal Liberty — No person shall be deprived of his life or personal liberty except according to procedure established by law."),
        Flashcard(id = "f2", subject = "History",   front = "When was the Battle of Plassey fought?",        back = "1757 — Robert Clive of the British East India Company defeated Siraj ud-Daulah, the last independent Nawab of Bengal."),
        Flashcard(id = "f3", subject = "Geography", front = "What is the Tropic of Cancer latitude?",        back = "23.5° North — It passes through 8 Indian states: Gujarat, Rajasthan, MP, Chhattisgarh, Jharkhand, West Bengal, Tripura, and Mizoram."),
        Flashcard(id = "f4", subject = "Economy",   front = "What is GDP?",                                  back = "Gross Domestic Product — the total monetary value of all goods and services produced within a country's borders in a specific period."),
        Flashcard(id = "f5", subject = "Polity",    front = "Who appoints the CAG of India?",               back = "The President of India appoints the Comptroller and Auditor General (CAG) under Article 148 of the Constitution."),
        Flashcard(id = "f6", subject = "Science",   front = "What is the SI unit of electric current?",     back = "Ampere (A) — named after French physicist André-Marie Ampère, it measures the rate of flow of electric charge."),
        Flashcard(id = "f7", subject = "History",   front = "Who founded the Indian National Congress?",     back = "A.O. Hume, along with Dadabhai Naoroji and Dinshaw Wacha, founded the INC in 1885 in Bombay."),
        Flashcard(id = "f8", subject = "Bihar GK",  front = "What is the capital of Bihar?",                back = "Patna — historically known as Pataliputra, it was the capital of the Maurya and Gupta empires."),
    )

    // ─── Mock Tests ───────────────────────────────────────
    val mockTests = listOf(
        MockTest(id = "mt1", title = "BPSC 70th Prelims Full Mock #1",   subject = "All Subjects", totalQuestions = 150, durationMinutes = 120, attemptedBy = 12450, avgScore = 67, isPaid = false),
        MockTest(id = "mt2", title = "BPSC 70th Prelims Full Mock #2",   subject = "All Subjects", totalQuestions = 150, durationMinutes = 120, attemptedBy = 9800,  avgScore = 71, isPaid = false),
        MockTest(id = "mt3", title = "Bihar GK Sectional Test",          subject = "Bihar GK",    totalQuestions = 50,  durationMinutes = 40,  attemptedBy = 18200, avgScore = 58, isPaid = false),
        MockTest(id = "mt4", title = "Polity & Governance Master Test",  subject = "Polity",      totalQuestions = 75,  durationMinutes = 60,  attemptedBy = 7600,  avgScore = 62, isPaid = true),
        MockTest(id = "mt5", title = "BPSC Mains GS Paper 1 Mock",      subject = "GS Paper 1",  totalQuestions = 10,  durationMinutes = 180, attemptedBy = 4300,  avgScore = 55, isPaid = true),
        MockTest(id = "mt6", title = "Economy & Budget Special Test",    subject = "Economy",     totalQuestions = 60,  durationMinutes = 50,  attemptedBy = 5100,  avgScore = 60, isPaid = false),
    )

    // ─── Courses ──────────────────────────────────────────
    val courses = listOf(
        Course(id = "c1", title = "Complete BPSC 70th Preparation",     subject = "All Subjects", instructor = "Dr. Amit Verma",   totalLessons = 120, completedLessons = 45, thumbnail = null, isPaid = false, price = 0),
        Course(id = "c2", title = "Bihar Special GK Crash Course",      subject = "Bihar GK",    instructor = "Prof. Seema Singh", totalLessons = 35,  completedLessons = 35, thumbnail = null, isPaid = false, price = 0),
        Course(id = "c3", title = "Polity from Scratch to Advanced",    subject = "Polity",      instructor = "Dr. Amit Verma",   totalLessons = 60,  completedLessons = 12, thumbnail = null, isPaid = true,  price = 499),
        Course(id = "c4", title = "Indian Economy Master Class",        subject = "Economy",     instructor = "CA Rohit Gupta",   totalLessons = 45,  completedLessons = 0,  thumbnail = null, isPaid = true,  price = 399),
        Course(id = "c5", title = "Geography of India — Complete",      subject = "Geography",   instructor = "Prof. Seema Singh", totalLessons = 50,  completedLessons = 0,  thumbnail = null, isPaid = true,  price = 349),
        Course(id = "c6", title = "Modern History for BPSC",            subject = "History",     instructor = "Dr. Priya Kumari", totalLessons = 40,  completedLessons = 0,  thumbnail = null, isPaid = false, price = 0),
    )

    // ─── Reading Rooms ────────────────────────────────────
    val readingRooms = listOf(
        ReadingRoom(id = "r1", name = "Morning Focus Room",     activeUsers = 142, totalCapacity = 200, studyStreakHours = 3, isJoined = true),
        ReadingRoom(id = "r2", name = "BPSC 70th War Room",     activeUsers = 89,  totalCapacity = 150, studyStreakHours = 0, isJoined = false),
        ReadingRoom(id = "r3", name = "Polity Masters Room",    activeUsers = 56,  totalCapacity = 100, studyStreakHours = 0, isJoined = false),
        ReadingRoom(id = "r4", name = "Night Owls Study Hall",  activeUsers = 201, totalCapacity = 250, studyStreakHours = 0, isJoined = false),
        ReadingRoom(id = "r5", name = "Bihar GK Special Room",  activeUsers = 34,  totalCapacity = 75,  studyStreakHours = 0, isJoined = false),
    )

    val leaderboard = listOf(
        LeaderboardEntry(rank = 1,  name = "Priya S.",    studyHours = 8.5f, coins = 8,  avatarInitials = "PS"),
        LeaderboardEntry(rank = 2,  name = "Amit K.",     studyHours = 7.2f, coins = 7,  avatarInitials = "AK"),
        LeaderboardEntry(rank = 3,  name = "Rahul Kumar", studyHours = 6.8f, coins = 6,  avatarInitials = "RK", isCurrentUser = true),
        LeaderboardEntry(rank = 4,  name = "Neha R.",     studyHours = 6.1f, coins = 6,  avatarInitials = "NR"),
        LeaderboardEntry(rank = 5,  name = "Vikash M.",   studyHours = 5.9f, coins = 5,  avatarInitials = "VM"),
        LeaderboardEntry(rank = 6,  name = "Suman P.",    studyHours = 5.4f, coins = 5,  avatarInitials = "SP"),
        LeaderboardEntry(rank = 7,  name = "Rajan T.",    studyHours = 4.8f, coins = 4,  avatarInitials = "RT"),
        LeaderboardEntry(rank = 8,  name = "Anjali D.",   studyHours = 4.2f, coins = 4,  avatarInitials = "AD"),
    )

    // ─── Job Vacancies ────────────────────────────────────
    val jobVacancies = listOf(
        JobVacancy(id = "j1", title = "BPSC 70th Combined Competitive Exam",       organization = "BPSC",         posts = 1957, lastDate = "15 Apr 2025", category = "State PSC",    isNotified = true),
        JobVacancy(id = "j2", title = "Bihar Police Sub Inspector Recruitment",    organization = "Bihar Police", posts = 1275, lastDate = "30 Mar 2025", category = "Police",       isNotified = false),
        JobVacancy(id = "j3", title = "BPSC Teacher Recruitment (TRE 4.0)",        organization = "BPSC",         posts = 87,   lastDate = "20 Apr 2025", category = "Teaching",     isNotified = true),
        JobVacancy(id = "j4", title = "Bihar SSC Graduate Level Exam",             organization = "BSSC",         posts = 3521, lastDate = "10 Apr 2025", category = "SSC",          isNotified = false),
        JobVacancy(id = "j5", title = "UPSC Civil Services Examination 2025",      organization = "UPSC",         posts = 1129, lastDate = "18 Apr 2025", category = "Central PSC",  isNotified = false),
        JobVacancy(id = "j6", title = "Bihar Judiciary Civil Judge Recruitment",   organization = "Patna HC",     posts = 138,  lastDate = "05 May 2025", category = "Judiciary",    isNotified = false),
        JobVacancy(id = "j7", title = "Railway NTPC Bihar Zone Recruitment",       organization = "RRB",          posts = 2460, lastDate = "25 Apr 2025", category = "Railway",      isNotified = false),
    )

    // ─── Downloads ────────────────────────────────────────
    val downloads = listOf(
        DownloadItem(id = "d1", title = "BPSC Syllabus 2024 (Official)",      subject = "General",    sizeMb = 2.1f,  isDownloaded = true),
        DownloadItem(id = "d2", title = "Polity Notes — Complete",            subject = "Polity",     sizeMb = 8.4f,  isDownloaded = true),
        DownloadItem(id = "d3", title = "Bihar GK Compiled Notes",            subject = "Bihar GK",   sizeMb = 5.7f,  isDownloaded = false),
        DownloadItem(id = "d4", title = "Previous Year Papers 2019-2023",     subject = "General",    sizeMb = 12.3f, isDownloaded = true),
        DownloadItem(id = "d5", title = "Economy Quick Revision Notes",       subject = "Economy",    sizeMb = 3.8f,  isDownloaded = false),
        DownloadItem(id = "d6", title = "March 2025 Current Affairs Magazine",subject = "Current Affairs", sizeMb = 6.2f, isDownloaded = false),
    )

    // ─── Subscription Plans ───────────────────────────────
    val subscriptionPlans = listOf(
        SubscriptionPlan(id = "sp1", name = "Monthly",  durationMonths = 1,  price = 199,  originalPrice = 299,  features = listOf("All paid courses", "Full mock tests", "Monthly magazine", "Priority support")),
        SubscriptionPlan(id = "sp2", name = "Quarterly",durationMonths = 3,  price = 499,  originalPrice = 897,  features = listOf("All paid courses", "Full mock tests", "Monthly magazine", "Priority support", "Offline downloads"), isPopular = true),
        SubscriptionPlan(id = "sp3", name = "Annual",   durationMonths = 12, price = 1499, originalPrice = 3588, features = listOf("All paid courses", "Full mock tests", "Monthly magazine", "Priority support", "Offline downloads", "1-on-1 doubt sessions")),
    )
}