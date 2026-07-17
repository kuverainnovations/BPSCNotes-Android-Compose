package com.example.bpscnotes.core.language

// ── Each data class kept under 50 fields to avoid Dalvik VerifyError ─────────

data class CommonStrings(
    val ok: String, val yes: String, val no: String, val cancel: String, val save: String,
    val close: String, val back: String, val retry: String, val loading: String,
    val error: String, val success: String, val submit: String, val next: String,
    val done: String, val skip: String, val search: String, val noData: String,
    val seeAll: String, val viewAll: String, val free: String, val premium: String,
    val coins: String, val minutes: String, val hours: String, val days: String,
    val all: String, val start: String, val goBack: String, val tryAgain: String, val version: String,
)

data class NavAuthStrings(
    val navDashboard: String, val navMyLearning: String, val navRooms: String, val navProfile: String,
    val splashTagline: String,
    val langSelectTitle: String, val langSelectSubtitle: String, val langSelectContinue: String,
    val onboardingGetStarted: String, val onboardingSkip: String,
    val onboarding1Title: String, val onboarding1Subtitle: String, val onboarding1Body: String,
    val onboarding2Title: String, val onboarding2Subtitle: String, val onboarding2Body: String,
    val onboarding3Title: String, val onboarding3Subtitle: String, val onboarding3Body: String,
    val loginSubtitle: String, val loginEnterMobile: String, val loginSendOtp: String,
    val loginMobileHint: String, val loginTermsPrivacy: String,
    val otpTitle: String, val otpSentTo: String, val otpResend: String,
    val otpVerify: String, val otpChangeNumber: String, val otpDidntReceive: String,
)

data class ProfileEditStrings(
    val editPersonalInfo: String, val editFullName: String, val editEmail: String,
    val editDistrict: String, val editExamSettings: String, val editPrepLevel: String,
    val editTargetYear: String, val editSaveChanges: String, val editSaving: String,
    val editMobile: String, val editVerified: String, val editNotVerified: String,
    val prepBeginner: String, val prepIntermediate: String, val prepAdvanced: String,
    val examSetupChoose: String, val examSetupTapHint: String,
    val examSetupSearch: String, val examSetupPrimary: String, val examSetupSecondary: String,
    val examSetupTargetYear: String, val examSetupPlan: String,
    val examSetupPersonalize: String, val examSetupNext: String,
    val thisYear: String, val nextYear: String, val longTerm: String,
)

data class DashboardStrings(
    val dashboardHello: String, val dashboardGoodMorning: String,
    val dashboardGoodAfternoon: String, val dashboardGoodEvening: String,
    val dashboardDailyTargets: String, val dashboardAddTarget: String,
    val dashboardNoTargets: String, val dashboardTodayProgress: String,
    val dashboardContinueLearning: String, val dashboardDailyQuiz: String,
    val dashboardCurrentAffairs: String, val dashboardLatestJobs: String,
    val dashboardStudyRoom: String, val dashboardCoins: String,
    val dashboardStreak: String, val dashboardAccuracy: String, val dashboardStudyTime: String,
    val dashboardTodayQuizzes: String, val dashboardTodayFocus: String,
    val dashboardNoQuizzes: String, val dashboardQuizNoQuestions: String, val dashboardDone: String,
    val drawerCourses: String, val drawerStudyMaterials: String, val drawerCurrentAffairs: String,
    val drawerMockTests: String, val drawerJobAlerts: String, val drawerStudyRooms: String,
    val drawerCoinWallet: String, val drawerAchievements: String,
    val drawerSettings: String, val drawerLanguage: String, val drawerLogout: String,
    val dashboardAspirant: String, val dashboardNoTargets2: String,
    val dashboardViewAll: String, val dashboardCreateTarget: String,
    val dashboardWeeklyConsistency: String, val dashboardWeeklySubtitle: String,
    val dashboardNoActivity: String, val dashboardStartStudying: String,
    val dashboardQuickAccess: String, val dashboardRecommended: String,
    val dashboardMySchedule: String, val dashboardUpcomingEvents: String,
    val dashboardNoClasses: String, val dashboardNoMeetingLink: String,
    val dashboardClassEnded: String, val dashboardAlreadyRegistered: String,
    val dashboardCreateCustomTarget: String, val dashboardBuildPlan: String,
    val dashboardWhatNext: String,
)

data class CourseStrings(
    val coursesTitle: String, val coursesEnrollNow: String, val coursesEnrolled: String,
    val coursesContinue: String, val coursesCompleted: String, val coursesLessons: String,
    val coursesProgress: String, val coursesFree: String, val coursesPaid: String,
    val coursesContent: String, val coursesNoCurriculum: String,
    val coursesRate: String, val coursesRateSubtitle: String, val coursesSubmitReview: String,
    val coursesThankYou: String, val coursesReviews: String,
    val coursesNoReviews: String, val coursesBeFirst: String, val coursesVerified: String,
    val lessonCompleted: String, val lessonMarkComplete: String, val lessonSaving: String,
    val lessonNoPdf: String, val lessonNoVideo: String, val lessonLoading: String,
    val lessonLiveClass: String, val lessonJoinLive: String, val lessonLiveNotReady: String,
    val lessonChapterQuiz: String, val lessonCantLoadPdf: String,
    val lessonOpenBrowser: String, val lessonLoadingPdf: String,
    val courseByAuthor: String, val courseYourProgress: String,
    val courseCertEarned: String, val courseCertTitle: String,
    val courseCertTap: String, val courseCertComplete: String,
    val courseShareCert: String, val courseShareCertBtn: String,
    val courseCourseCompleted: String, val courseContinueLearning: String,
    val courseStartLearning: String, val courseEnrollFree: String,
    val courseAbout: String, val courseFreeTrial: String,
    val courseWatch: String, val courseSyllabus: String,
    val courseShowLess: String, val courseShowAll: String,
    val courseStudentReviews: String, val coursePriceSummary: String,
    val courseCoinsDiscount: String, val courseCouponApplied: String,
    val courseTotalPayable: String, val courseUseCoins: String,
    val courseYouHaveCoins: String, val courseCouponCode: String,
    val courseCouponSaved: String, val courseSecurePayment: String,
    val courseGrabNow: String, val courseTapStar: String,
    val courseHindiEnglish: String,
    val courseInProgress: String, val courseNoCoursesYet: String,
    val courseExploreStore: String, val courseMyCertificates: String,
    val courseOverallProgress: String, val courseTapRead: String,
    val courseEnrollTitle: String,
)

data class QuizStrings(
    val quizTitle: String, val quizDaily: String, val quizTopic: String, val quizMock: String,
    val quizStart: String, val quizQuestions: String, val quizTimeLimit: String,
    val quizSubmit: String, val quizResult: String, val quizScore: String,
    val quizCorrect: String, val quizWrong: String,
    val quizCoinsEarned: String, val quizReview: String, val quizNext: String, val quizFinish: String,
    val quizSkip: String, val quizPrevious: String, val quizHint: String,
    val quizAttempted: String, val quizDuration: String, val quizRules: String,
    val quizNoQuestions: String, val quizRetake: String, val quizStartNow: String,
    val quizSubmitting: String, val quizQuitTitle: String, val quizQuitBody: String,
    val quizQuit: String, val quizKeepGoing: String,
    val quizExcellent: String, val quizGoodJob: String, val quizKeepPracticing: String,
    val quizSelectCorrect: String, val quizSkipped: String, val quizAnswered: String,
    val quizNoAvailable: String, val quizCheckLater: String,
    val quizReviewAll: String, val quizRetakeQuiz: String, val quizBackToQuizzes: String,
    val quizNavTitle: String, val quizCorrectAns: String, val quizWrongAns: String,
    val quizSaveNext: String, val quizSubmitTest: String,
    val quizStartTest: String, val quizTestOverview: String,
    val quizCanResume: String, val quizAvgScore: String,
    val quizFullMock: String, val quizMiniTest: String, val quizPrevYear: String,
    val quizAllTests: String, val quizFeatured: String,
    val quizPracticeReal: String, val quizNoTestsCategory: String,
    val quizNoTestsYet: String, val quizTestsComingSoon: String,
    val quizCoinsEarned2: String, val quizNoCoins: String,
    val quizAddedWallet: String, val quizAlreadyEarned: String,
    val quizSubjectAnalysis: String, val quizViewLeaderboard: String,
    val quizRetryTest: String, val quizBackToTests: String,
    val quizCreateCustom: String, val quizSelectSubjects: String,
    val quizNegativeMarking: String, val quizCustomTest: String,
    val quizStartCustom: String, val quizSubmitTestTitle: String,
    val quizLoadingQ: String, val quizSettingUp: String, val quizPreparingQ: String,
    val quizYourRank: String,
    // ── Answer Writing (Mains practice) — defaults are English so only
    // the Hindi instantiation needs to override them ────────────────────
    val awTitle: String = "Answer Writing",
    val awSubtitle: String = "Daily Mains answer practice",
    val awTodayBadge: String = "TODAY'S QUESTION",
    val awStartWriting: String = "Start Writing ✍️",
    val awQuestionsTab: String = "Questions",
    val awMyAnswersTab: String = "My Answers",
    val awStatusNew: String = "✍️ Write Now",
    val awStatusPending: String = "⏳ Under Review",
    val awStatusReviewed: String = "✅ Reviewed",
    val awMarks: String = "Marks",
    val awWordLimit: String = "Word Limit",
    val awWords: String = "words",
    val awTips: String = "Writing Tips",
    val awYourAnswer: String = "Your Answer",
    val awModelAnswer: String = "Model Answer",
    val awFeedback: String = "Examiner's Feedback",
    val awScore: String = "Score",
    val awWriteHint: String = "Start writing your answer here…",
    val awSubmit: String = "Submit Answer",
    val awSubmitting: String = "Submitting…",
    val awConfirmTitle: String = "Submit your answer?",
    val awConfirmBody: String = "You get one attempt per question. After submitting, the model answer unlocks and a mentor will review your answer with feedback.",
    val awOverLimit: String = "over the limit",
    val awEmpty: String = "No questions yet",
    val awEmptyBody: String = "New answer-writing questions are posted regularly — check back soon!",
    val awNoSubmissions: String = "You haven't written any answers yet",
    val awNoSubmissionsBody: String = "Pick a question and write your first Mains answer!",
    val awPendingNote: String = "Your answer is with the examiner — you'll get a notification once it's reviewed.",
    val awDashSubtitle: String = "Mains practice · Model answers · Expert review",
    // ── Handwritten photo answers ────────────────────────────────────
    val awTypeMode: String = "⌨️ Type",
    val awPhotoMode: String = "📷 Photos",
    val awPhotoHint: String = "Write your answer in your notebook, then photograph each page in order (up to 5 photos).",
    val awTakePhoto: String = "Camera",
    val awFromGallery: String = "Gallery",
    val awPhotos: String = "photos",
    // ── Peer review ──────────────────────────────────────────────────
    val awPeerReview: String = "Peer Review",
    val awPeerReviewSub: String = "Review answers written by other aspirants and earn review credits.",
    val awReviewNow: String = "Review Now",
    val awReviewsGiven: String = "Reviews Given",
    val awPendingReviews: String = "To Review",
    val awReviewCredits: String = "Review Credits",
    val awReviewLockedNoSub: String = "Submit your own answer first to unlock peer reviewing.",
    val awReviewLockedNotReviewed: String = "Reviewing unlocks once your own answer gets its first review.",
    val awHelpFellow: String = "Help a fellow aspirant. Earn a review credit.",
    val awReviewBannerTitle: String = "Give an honest, constructive review.",
    val awReviewBannerBody: String = "Your review will help another aspirant improve.",
    val awAnonymous: String = "Anonymous",
    val awStudentAnswer: String = "Student's Answer",
    val awYourReview: String = "Your Review",
    val awReviewQ1: String = "1. Did the answer address the question demand?",
    val awReviewQ2: String = "2. Overall Answer Rating",
    val awReviewQ3: String = "3. What needs the most improvement?",
    val awReviewQ4: String = "4. One suggestion to improve the answer",
    val awPartly: String = "Partly",
    val awSuggestionHint: String = "E.g., Add more recent examples and a stronger conclusion with way forward.",
    val awSubmitReview: String = "Submit Review & Earn 1 Credit",
    val awNoMoreReviews: String = "All caught up!",
    val awNoMoreReviewsBody: String = "No answers waiting for your review right now — check back later.",
    val awPeerReviewsReceived: String = "Peer Reviews on Your Answer",
    val awUnderPeerReview: String = "Your answer has been submitted and is being reviewed by peers.",
    val awAreaContent: String = "Content",
    val awAreaStructure: String = "Structure",
    val awAreaAnalysis: String = "Analysis",
    val awAreaBihar: String = "Bihar Angle",
    val awAreaPresentation: String = "Presentation",
    val awAreaConclusion: String = "Conclusion",
    // ── Insights tab ─────────────────────────────────────────────────
    val awInsightsTab: String = "Insights",
    val awInsightsTitle: String = "Your Activity",
    val awInsightsSub: String = "Track your progress and contribution.",
    val awAnswersWritten: String = "Answers Written",
    val awReviewsReceived: String = "Reviews Received",
    val awAvgRating: String = "Average Rating",
    val awWritingStreak: String = "Writing Streak",
    val awDays: String = "Days",
    val awThisMonth: String = "this month",
    val awTotalWords: String = "Total Words",
    val awMentorScore: String = "Avg Mentor Score",
    val awGoalTitle: String = "Your Goal",
    val awGoalBody: String = "Write 10 quality answers this month",
    val awGoalDone: String = "Completed! 🎉",
    val awReviewerLevel: String = "Reviewer Level",
    val awLevelBeginner: String = "Beginner",
    val awLevelActive: String = "Active Reviewer",
    val awLevelAdvanced: String = "Advanced Reviewer",
    val awLevelExpert: String = "Expert Reviewer",
    val awKeepItUp: String = "Keep it up!",
    val awNoInsights: String = "Write your first answer to see your stats here!",
    val awChars: String = "chars",
    // ── v2 (client notes 16 Jul) ─────────────────────────────────────
    val awPyq: String = "PYQ",
    val awModelAnswerTomorrow: String = "The model answer unlocks tomorrow — come back to compare!",
    val awAreaIntro: String = "Introduction",
    val awAreaValueAdd: String = "Value Addition",
    val awUpTo3: String = "(up to 3)",
    val awTopWeaknesses: String = "Top 3 Weaknesses",
    val awTopReviewers: String = "Top Reviewers",
    val awTopWriters: String = "Top Writers",
    val awAnswersLower: String = "answers",
    val awGiveToGet: String = "Tip: give reviews to get your own answer reviewed faster 🤝",
)

data class ContentStrings(
    val caTitle: String, val caBookmark: String, val caBookmarked: String,
    val caShare: String, val caMcqPractice: String, val caImportant: String,
    val caSubtitle: String, val caSearchHint: String, val caNoSaved: String,
    val caNoArticles: String, val caBookmarkHint: String, val caTryFilter: String,
    val caReadMore: String, val caLoadingQ: String, val caNoMcqs: String,
    val caGoBack: String, val caBackToArticle: String, val caNoMcqsBody: String,
    val caNxtQuestion: String, val caSeeResults: String,
    val caExcellent: String, val caWellDone: String, val caGoodEffort: String, val caKeepPracticing: String,
    val materialsTitle: String, val materialsSubtitle: String,
    val materialsDownload: String, val materialsDownloaded: String,
    val materialsUpload: String, val materialsView: String, val materialsSearchHint: String,
    val materialsPopular: String, val materialsNewest: String, val materialsTopRated: String,
    val materialsPinned: String, val materialsTrending: String, val materialsRecent: String,
    val materialsAll: String, val materialsFilterSubject: String,
    val materialsFilterLanguage: String, val materialsFilters: String,
    val materialsExplore: String, val materialsMyUploads: String,
)

data class ContentStrings2(
    val materialsSave: String, val materialsRemoveSaved: String,
    val materialsTapOpen: String, val materialsNoPreview: String,
    val materialsUploadTitle: String,
    val materialsUploading: String, val materialsSubmitReview: String,
    val materialsReviewNote: String, val materialsNoSaved: String,
    val materialsNoResources: String, val materialsBookmarkHint: String,
    val materialsNoUploads: String, val materialsUploadHint: String,
    val materialsPublished: String, val materialsRejected: String,
    val materialsNoDownloads: String, val materialsDownloadHint: String,
    val materialsPremiumContent: String, val materialsChargeCoins: String,
    val materialsDownloadedDone: String, val materialsUnlockPro: String, val materialsDownloadFree: String,
    val caLoadingQuestions: String, val caSavedDone: String,
    val caGateWaiting: String, val caGateReady: String, val caGateSubtitle: String,
    val caGateContinue: String, val caGateWaitButton: String,
    val caGateReadyMcq: String, val caGateContinueMcq: String,
)

data class JobRoomStrings(
    val jobsTitle: String, val jobsApplyNow: String, val jobsLastDate: String,
    val jobsPosts: String, val jobsSave: String, val jobsSaved: String,
    val jobsOpeningsCountLabel: String,
    val jobsSearchHint: String, val jobsNoJobs: String, val jobsTryFilter: String,
    val jobsFeatured: String, val jobsAllJobs: String,
    val roomsTitle: String, val roomsChoose: String, val roomsChooseHint: String,
    val roomsJoin: String, val roomsLeave: String,
    val roomsStartSession: String, val roomsEndSession: String,
    val roomsStudying: String, val roomsOnline: String,
    val roomsTierBronze: String, val roomsTierSilver: String,
    val roomsTierGold: String, val roomsTierDiamond: String,
    val roomsEarnCoins: String, val roomsSessionActive: String,
    val roomsLive: String, val roomsLocked: String, val roomsStudied: String,
    val roomsYourRoom: String, val roomsClaimPromotion: String, val roomsMetRequirements: String,
    val roomsClaimNow: String, val roomsLater: String, val roomsRequirements: String,
    val roomsKeepStudying: String, val roomsReadyForNext: String, val roomsPromotedMidnight: String,
    val roomsGroupStudy: String, val roomsTapToStart: String,
    val jobsNew: String, val jobsApplicationClosed: String, val jobsNoLink: String,
    val jobsApplyOfficialSite: String, val jobsAboutJob: String,
    val jobsEligibility: String, val jobsImportantDates: String,
    val jobsApplyStart: String, val jobsAlerts: String, val jobsAlertsSubtitle: String,
    val roomsSearchHint: String, val roomsFeatured: String, val roomsAllRooms: String,
    val roomsLeaderboard: String, val roomsLeaderboardResets: String,
    val roomsFocusSession: String, val roomsFocusTip1: String, val roomsFocusTip2: String,
    val roomsFocusTip3: String, val roomsFocusTip4: String,
    val roomsCreate: String, val roomsTodayTopic: String, val roomsMaxMembers: String,
    val roomsRequiresCode: String, val roomsCreateBtn: String,
    val roomsEnterCode: String, val roomsJoinBtn: String,
    val roomsTodayStudyTogether: String,
)

data class FocusProfileStrings(
    val focusEndSessionTitle: String, val focusKeepStudying: String,
    val focusJoining: String, val focusSettingUp: String,
    val focusAfk: String, val focusAfkIdle: String, val focusImBack: String,
    val focusSessionComplete: String, val focusGreatWork: String,
    val focusYou: String, val focusThisSession: String, val focusTotal: String,
    val focusBackToRooms: String, val focusTapMessage: String,
    val focusFirstHere: String, val focusOthersJoin: String,
    val focusSaving: String, val focusActive: String,
    val profileTitle: String, val profileEdit: String, val profileStreak: String,
    val profileCoins: String, val profileAchievements: String, val profileBadges: String,
    val profileStudyTime: String, val profileShare: String, val profileRank: String,
    val profileSubjectProgress: String, val profileWeeklyStreak: String,
    val profileHeatmap: String, val profileLast28: String,
    val profileLess: String, val profileMore: String,
    val profileDayStreak: String, val profileShowLess: String, val profileStudy: String,
    val profileCoinWallet: String, val profileViewAll: String,
    val profileCurrentBalance: String, val profileHowToEarn: String,
    val profileEarnQuiz: String, val profileEarnStreak: String, val profileEarnReferral: String,
    val profileNoTransactions: String, val profileMyCourses: String,
    val recallKeepGoing: String, val recallSwipeRate: String, val recallGotIt: String,
    val recallMastered: String, val recallTapReveal: String, val recallRevealAnswer: String,
    val recallGotItBtn: String, val recallSessionComplete: String, val recallBackToSubjects: String,
)

data class SettingsStrings(
    val achievementsSubtitle: String, val achievementsInProgress: String,
    val challengesTitle: String, val challengesThisWeek: String,
    val challengesNone: String, val challengesCheckBack: String,
    val challengesGoal: String, val challengesClaimed: String,
    val challengesClaim: String, val challengesKeepStudying: String,
    val settingsTitle: String, val settingsAccount: String, val settingsAppearance: String,
    val settingsDarkMode: String, val settingsDarkModeSubtitle: String,
    val settingsLanguage: String, val settingsLanguageSubtitle: String,
    val settingsStudyPrefs: String, val settingsReminder: String, val settingsReminderSubtitle: String,
    val settingsAutoPlay: String, val settingsAutoPlaySubtitle: String,
    val settingsSound: String, val settingsSoundSubtitle: String,
    val settingsHaptics: String, val settingsHapticsSubtitle: String,
    val settingsStorage: String, val settingsClearCache: String, val settingsClearCacheSubtitle: String,
)

data class SettingsStrings2(
    val settingsOffline: String, val settingsOfflineSubtitle: String,
    val settingsAbout: String, val settingsVersion: String, val settingsVersionSubtitle: String,
    val settingsRate: String, val settingsRateSubtitle: String,
    val settingsShare: String, val settingsShareSubtitle: String,
    val settingsPrivacy: String, val settingsPrivacySubtitle: String,
    val settingsTerms: String, val settingsTermsSubtitle: String,
    val settingsSupport: String, val settingsSupportSubtitle: String,
    val settingsLogout: String, val settingsLogoutSubtitle: String,
    val settingsDeleteAccount: String, val settingsDeleteSubtitle: String,
    val settingsDeleteConfirmTitle: String, val settingsDeleteConfirmBody: String,
    val settingsDeleteForever: String,
    val settingsDownloadedContent: String, val settingsManageOffline: String,
    val settingsClearing: String, val settingsCacheCleared: String,
)

data class PaymentStrings(
    val paymentTitle: String, val paymentActivated: String, val paymentWelcome: String,
    val paymentGetPro: String, val paymentUnlockAll: String, val paymentWhatsIncluded: String,
    val paymentChoosePlan: String, val paymentSecure: String,
    val paymentMonthly: String, val paymentQuarterly: String, val paymentAnnual: String,
    val paymentSubscribe: String, val paymentMostPopular: String, val paymentBestValue: String,
    val paymentActivate: String, val paymentSuccess: String, val paymentFailed: String,
    val paymentCoupon: String, val paymentEnterCoupon: String,
    val paymentApply: String, val paymentApplied: String,
    val paymentBreakdown: String, val paymentBasePrice: String,
    val paymentCouponDiscount: String, val paymentTotal: String,
    val paymentCreating: String, val paymentConfirming: String,
    val paymentBenefit1: String, val paymentBenefit2: String, val paymentBenefit3: String,
    val paymentBenefit4: String, val paymentBenefit5: String,
    val paymentBenefit6: String, val paymentBenefit7: String,
    val coursePaymentTitle: String, val courseUnlocked: String, val courseUnlockedBody: String,
    val coursePurchaseBtn: String, val coursePurchaseComplete: String, val coursePrice: String,
    val courseLifetime: String, val courseOffline: String, val courseCertificate: String,
    val courseSecure: String, val courseProcessing: String,
)

data class MiscStrings(
    val targetTitle: String, val targetAdd: String, val targetComplete: String,
    val targetNoTargets: String, val targetCompleted: String, val targetCoinsEarned: String,
    val targetCreate: String, val targetCreateSheet: String, val targetPlaceholder: String,
    val targetStreak: String, val targetStreakProtect: String, val targetAllFilter: String,
    val targetMax: String, val targetCarried: String, val targetLoading: String,
    val targetFailed: String, val targetMorning: String, val targetAfternoon: String,
    val targetNight: String, val targetEasy: String, val targetMedium: String, val targetHard: String,
    val walletTitle: String, val walletBalance: String, val walletEarned: String,
    val walletSpent: String, val walletHistory: String, val walletEarnCoins: String,
    val walletDailyStreak: String, val walletCheckIn: String,
    val walletCheckedIn: String, val walletCheckingIn: String,
    val walletNoTasks: String, val walletNoTransactions: String,
    val walletInviteFriend: String, val walletWatchAd: String,
    val pdfReadFreePages: String, val pdfPurchaseUnlock: String,
    val pdfPageNum: String, val pdfPageLocked: String, val pdfPurchaseAccess: String,
    val loginIAgree: String, val editBio: String,
    val registerCreateProfile: String, val registerPersonalize: String,
    val registerContinue: String, val registerDataSecure: String,
    val registerPersonalDetails: String,
    val examStartPreparing: String, val examPrimaryTip: String, val examPrimaryTapChange: String,
    val examBiharState: String,
    val marketGetFree: String,
    val paymentStartLearning: String, val paymentOpenFailed: String,
    val studyFocusJustNow: String,
    val notifAllRead: String,
)

data class MiscStrings3(
    val notifTitle: String, val notifMarkRead: String, val notifNone: String,
    val notifToday: String, val notifYesterday: String, val notifJustNow: String,
    val chatRoomChat: String, val chatLive: String, val chatConnecting: String,
    val chatReconnecting: String, val chatLoading: String, val chatNoMessages: String,
    val chatStartConversation: String, val chatToday: String,
    val chatMessageHint: String, val chatFailedSend: String, val chatYou: String,
)

data class MiscStrings2(
    val pomodoroFocus: String, val pomodoroBreak: String, val pomodoroLongBreak: String,
    val pomodoroPause: String, val pomodoroStart: String,
    val pomodoroFocusDone: String, val pomodoroFocusDoneBody: String,
    val pomodoroBreakOver: String, val pomodoroBreakOverBody: String,
    val pomodoroLongDone: String, val pomodoroLongDoneBody: String,
    val recallTitle: String, val recallSubtitle: String, val recallOverallMastery: String,
    val recallRetryWeak: String, val recallNoCards: String, val recallAskAdmin: String,
    val recallChooseSubject: String, val recallLoading: String, val recallFailed: String,
    val recallEasy: String, val recallMedium: String, val recallHard: String,
    val pdfUnlock: String, val pdfBuyAccess: String, val pdfMaybeLater: String,
    val pdfFullAccess: String, val pdfLoadingPdf: String, val pdfCantLoad: String,
    val pdfGoBack: String, val pdfNoPages: String,
    val marketTitle: String, val marketSubtitle: String, val marketSell: String,
    val marketSearchHint: String, val marketNoNotes: String,
    val placeholdersBilledMonthly: String, val placeholdersBilledQuarterly: String,
    val placeholdersBilledAnnually: String, val placeholdersExclusiveNotes: String,
    val placeholdersUnlockAll: String, val placeholdersPrioritySupport: String,
    val placeholdersAllPremium: String, val placeholdersNoPremiumYet: String,
    val placeholdersCheckBack: String, val placeholdersNotesReader: String, val placeholdersOpenWith: String,
)

data class MiscStrings4(
    val marketBeFirst: String, val marketUpload: String, val marketFeatured: String,
    val topicQuizTitle: String, val topicQuizDetails: String, val topicQuizStart: String,
    val topicQuizPerQuestion: String, val topicQuizCoin: String,
    val topicQuizSkip: String, val topicQuizHint: String,
    val topicQuizTimer: String, val topicQuizReview: String,
    val demotionStudyNow: String, val demotionDismiss: String,
    val promotionCongrats: String, val promotionPromotedTo: String, val promotionPerks: String,
    val pipSessionActive: String, val pipReturn: String,
    val downloadsTitle: String, val downloadsSubtitle: String,
    val downloadsNone: String, val downloadsNoneHint: String, val downloadsBrowse: String,
    val filterAll: String, val filterPrelims: String, val filterMains: String, val filterSaved: String,
    val quizReviewExplanation: String,
    val dashboardActiveRecall: String,
    val mockSampleQ1: String, val mockSampleA: String, val mockSampleQ2: String,
    // ── NEW: previously hardcoded strings ─────────────────────────────────
    // StudyMaterials
    val materialsOpenPdf: String, val materialsUnlockPro2: String,
    val materialsShareHint: String, val materialsNotesTitle: String,
    val materialsAuthorName: String, val materialsContentType: String,
    val materialsTags: String, val materialsTagHint: String,
    val materialsDownloadDevice: String, val materialsLifetimeAccess: String,
    // Targets
    val targetSwipeNavigate: String, val targetAllDone: String,
    // Certificates
    val courseCertOfCompletion: String, val courseCertDownload: String,
    val courseCertShare: String, val courseCertNotAvailable: String,
    // Permissions
    val permOpenSettings: String, val permNotNow: String,
    // Misc
    val recallRelatedMcq: String, val examLoadFailed: String,
    val registerNameHint: String, val courseBpscExpert: String,
    val lessonViewerTimeout: String, val notifSettingsHint: String,
    val placeholdersSeeAll2: String,
    val placeholdersPremiumNotes: String, val placeholdersPremiumCourses: String,
    // Reading rooms
    val roomCodeHint: String, val roomCreate: String,
    val roomTabChat: String, val roomTabMembers: String,
    val roomTabLeaderboard: String, val roomTabPomodoro: String,
    // Mock tests
    val mockAttempts: String, val mockUnlock: String, val mockCustom: String,
    val mockSubjectWise: String, val mockLeaderboard: String,
    val mockQuitTitle: String, val mockQuitBody: String,
    val mcqQuitTitle: String,
)

data class MiscStrings5(
    // ActiveRecall
    val recallReviseAgain: String,
    val recallAnswer: String,
    val recallExample: String,
    val recallMastered: String,
    // Placeholders
    val placeholderFileMissing: String,
    val placeholderOnDevice: String,
    val placeholderGetPro: String,
    val placeholderPremiumPdfs: String,
    val placeholderOfflineDownloads: String,
    // Dashboard
    val dashRegistered: String,
    val dashRegister: String,
    val dashProgress: String,
    // Mock Tests
    val mockCreateCustom: String,
    val mockInstructions: String,
    val mockPercentile: String,
    val mockNegativeMarking: String,
    // Profile
    val profileMaxTier: String,
    val profileLast28: String,
    val profileBadgeEarned: String,
    val profileNotEarned: String,
    // Study Materials
    val materialAbout: String,
    val materialPriceCoins: String,
    val materialUnlockAccess: String,
    val materialPrice: String,
    // Quiz
    val quizImageQ: String,
    val quizAccuracy: String,
    val quizCorrectAnswer: String,
    val quizScore: String,
    // MyLearning
    val myLearningNotesTitle: String,
    val myLearningUpiHint: String,
    // Ads
    val adLoading: String,
    val adSponsored: String,
    val adGreatSession: String,
    // Study Focus
    val focusActive: String,
    // Daily Targets
    val targetCoins: String,
    val targetCarriedForward: String,
    // Course Detail
    val courseWhatYouLearn: String,
    val courseAboutInstructor: String,
    // Permissions
    val permOpenSettings: String,
    // Register
    val registerEmailHint: String,
    // Exam Setup
    val examBack: String,
    // Marketplace
    val marketOwned: String,
    // Reading Rooms
    val roomsReset: String,
    // Coin Wallet
    val walletCoins: String,
    // Lesson
    val lessonInAppBrowser: String,
    // App UI
    val uiTryAgain: String,
    // Daily Quiz
    val quizDailyScore: String,
    val quizDailyAccuracy: String,
    // Close generic
    val closeLabel: String,
    // Marketplace Rules popup
    val marketRulesTitle: String,
    val marketRulesSubtitle: String,
    val marketRule1: String,
    val marketRule2: String,
    val marketRule3: String,
    val marketRule4: String,
    val marketRule5: String,
    val marketRulesGotIt: String,
    val marketRulesInfoTooltip: String,
)

data class MiscStrings6(
    // Permissions
    val permStayUpdated: String,
    val permStreakWarn: String,
    val permStudentsNote: String,
    val permEnableBtn: String,
    // Ads
    val adNoLimit: String,
    val adAdvertisement: String,
    val adSkip: String,
    // Study materials
    val materialOpen: String,
    val materialPreview: String,
    val materialUnlockPro: String,
    // Rooms
    val roomCreate: String,
    val roomSubject: String,
    // App UI
    val uiSomethingWrong: String,
    // Additional static strings
    val permMaybeLater: String,
    val permBlocked: String,
    val mlShareNotes: String,
    val mlContentType: String,
    val mlAttachFile: String,
    val roomPrivate: String,
    val roomJoinPrivate: String,
)

// ── Facade — 12 fields, all call sites keep using str.xxx unchanged ───────────
data class AppStrings(
    val _c: CommonStrings, val _n: NavAuthStrings, val _pe: ProfileEditStrings,
    val _db: DashboardStrings, val _co: CourseStrings, val _qz: QuizStrings,
    val _ct: ContentStrings, val _ct2: ContentStrings2, val _jr: JobRoomStrings, val _fp: FocusProfileStrings,
    val _st: SettingsStrings, val _st2: SettingsStrings2, val _pay: PaymentStrings,
    val _m: MiscStrings, val _m3: MiscStrings3, val _m2: MiscStrings2, val _m4: MiscStrings4,
    val _m5: MiscStrings5,
    val _m6: MiscStrings6,
) {
    val ok get() = _c.ok; val yes get() = _c.yes; val no get() = _c.no
    val cancel get() = _c.cancel; val save get() = _c.save; val close get() = _c.close
    val back get() = _c.back; val retry get() = _c.retry; val loading get() = _c.loading
    val error get() = _c.error; val success get() = _c.success; val submit get() = _c.submit
    val next get() = _c.next; val done get() = _c.done; val skip get() = _c.skip
    val search get() = _c.search; val noData get() = _c.noData
    val seeAll get() = _c.seeAll; val viewAll get() = _c.viewAll
    val free get() = _c.free; val premium get() = _c.premium; val coins get() = _c.coins
    val minutes get() = _c.minutes; val hours get() = _c.hours; val days get() = _c.days
    val all get() = _c.all; val start get() = _c.start; val goBack get() = _c.goBack
    val tryAgain get() = _c.tryAgain; val version get() = _c.version

    val navDashboard get() = _n.navDashboard; val navMyLearning get() = _n.navMyLearning
    val navRooms get() = _n.navRooms; val navProfile get() = _n.navProfile
    val splashTagline get() = _n.splashTagline
    val langSelectTitle get() = _n.langSelectTitle; val langSelectSubtitle get() = _n.langSelectSubtitle
    val langSelectContinue get() = _n.langSelectContinue
    val onboardingGetStarted get() = _n.onboardingGetStarted; val onboardingSkip get() = _n.onboardingSkip
    val onboarding1Title get() = _n.onboarding1Title; val onboarding1Subtitle get() = _n.onboarding1Subtitle; val onboarding1Body get() = _n.onboarding1Body
    val onboarding2Title get() = _n.onboarding2Title; val onboarding2Subtitle get() = _n.onboarding2Subtitle; val onboarding2Body get() = _n.onboarding2Body
    val onboarding3Title get() = _n.onboarding3Title; val onboarding3Subtitle get() = _n.onboarding3Subtitle; val onboarding3Body get() = _n.onboarding3Body
    val loginSubtitle get() = _n.loginSubtitle; val loginEnterMobile get() = _n.loginEnterMobile
    val loginSendOtp get() = _n.loginSendOtp; val loginMobileHint get() = _n.loginMobileHint; val loginTermsPrivacy get() = _n.loginTermsPrivacy
    val otpTitle get() = _n.otpTitle; val otpSentTo get() = _n.otpSentTo
    val otpResend get() = _n.otpResend; val otpVerify get() = _n.otpVerify
    val otpChangeNumber get() = _n.otpChangeNumber; val otpDidntReceive get() = _n.otpDidntReceive

    val editPersonalInfo get() = _pe.editPersonalInfo; val editFullName get() = _pe.editFullName
    val editEmail get() = _pe.editEmail; val editDistrict get() = _pe.editDistrict
    val editExamSettings get() = _pe.editExamSettings; val editPrepLevel get() = _pe.editPrepLevel
    val editTargetYear get() = _pe.editTargetYear; val editSaveChanges get() = _pe.editSaveChanges
    val editSaving get() = _pe.editSaving; val editMobile get() = _pe.editMobile; val editVerified get() = _pe.editVerified
    val editNotVerified get() = _pe.editNotVerified
    val prepBeginner get() = _pe.prepBeginner; val prepIntermediate get() = _pe.prepIntermediate; val prepAdvanced get() = _pe.prepAdvanced
    val examSetupChoose get() = _pe.examSetupChoose; val examSetupTapHint get() = _pe.examSetupTapHint
    val examSetupSearch get() = _pe.examSetupSearch; val examSetupPrimary get() = _pe.examSetupPrimary; val examSetupSecondary get() = _pe.examSetupSecondary
    val examSetupTargetYear get() = _pe.examSetupTargetYear; val examSetupPlan get() = _pe.examSetupPlan
    val examSetupPersonalize get() = _pe.examSetupPersonalize; val examSetupNext get() = _pe.examSetupNext
    val thisYear get() = _pe.thisYear; val nextYear get() = _pe.nextYear; val longTerm get() = _pe.longTerm

    val dashboardHello get() = _db.dashboardHello; val dashboardGoodMorning get() = _db.dashboardGoodMorning
    val dashboardGoodAfternoon get() = _db.dashboardGoodAfternoon; val dashboardGoodEvening get() = _db.dashboardGoodEvening
    val dashboardDailyTargets get() = _db.dashboardDailyTargets; val dashboardAddTarget get() = _db.dashboardAddTarget
    val dashboardNoTargets get() = _db.dashboardNoTargets; val dashboardTodayProgress get() = _db.dashboardTodayProgress
    val dashboardContinueLearning get() = _db.dashboardContinueLearning; val dashboardDailyQuiz get() = _db.dashboardDailyQuiz
    val dashboardCurrentAffairs get() = _db.dashboardCurrentAffairs; val dashboardLatestJobs get() = _db.dashboardLatestJobs
    val dashboardStudyRoom get() = _db.dashboardStudyRoom; val dashboardCoins get() = _db.dashboardCoins
    val dashboardStreak get() = _db.dashboardStreak; val dashboardAccuracy get() = _db.dashboardAccuracy; val dashboardStudyTime get() = _db.dashboardStudyTime
    val dashboardTodayQuizzes get() = _db.dashboardTodayQuizzes; val dashboardTodayFocus get() = _db.dashboardTodayFocus
    val dashboardNoQuizzes get() = _db.dashboardNoQuizzes; val dashboardQuizNoQuestions get() = _db.dashboardQuizNoQuestions; val dashboardDone get() = _db.dashboardDone
    val drawerCourses get() = _db.drawerCourses; val drawerStudyMaterials get() = _db.drawerStudyMaterials
    val drawerCurrentAffairs get() = _db.drawerCurrentAffairs; val drawerMockTests get() = _db.drawerMockTests
    val drawerJobAlerts get() = _db.drawerJobAlerts; val drawerStudyRooms get() = _db.drawerStudyRooms
    val drawerCoinWallet get() = _db.drawerCoinWallet; val drawerAchievements get() = _db.drawerAchievements
    val drawerSettings get() = _db.drawerSettings; val drawerLanguage get() = _db.drawerLanguage; val drawerLogout get() = _db.drawerLogout

    val coursesTitle get() = _co.coursesTitle; val coursesEnrollNow get() = _co.coursesEnrollNow
    val coursesEnrolled get() = _co.coursesEnrolled; val coursesContinue get() = _co.coursesContinue
    val coursesCompleted get() = _co.coursesCompleted; val coursesLessons get() = _co.coursesLessons
    val coursesProgress get() = _co.coursesProgress; val coursesFree get() = _co.coursesFree; val coursesPaid get() = _co.coursesPaid
    val coursesContent get() = _co.coursesContent; val coursesNoCurriculum get() = _co.coursesNoCurriculum
    val coursesRate get() = _co.coursesRate; val coursesRateSubtitle get() = _co.coursesRateSubtitle; val coursesSubmitReview get() = _co.coursesSubmitReview
    val coursesThankYou get() = _co.coursesThankYou; val coursesReviews get() = _co.coursesReviews
    val coursesNoReviews get() = _co.coursesNoReviews; val coursesBeFirst get() = _co.coursesBeFirst; val coursesVerified get() = _co.coursesVerified
    val lessonCompleted get() = _co.lessonCompleted; val lessonMarkComplete get() = _co.lessonMarkComplete
    val lessonSaving get() = _co.lessonSaving; val lessonNoPdf get() = _co.lessonNoPdf; val lessonNoVideo get() = _co.lessonNoVideo
    val lessonLoading get() = _co.lessonLoading; val lessonLiveClass get() = _co.lessonLiveClass
    val lessonJoinLive get() = _co.lessonJoinLive; val lessonLiveNotReady get() = _co.lessonLiveNotReady
    val lessonChapterQuiz get() = _co.lessonChapterQuiz; val lessonCantLoadPdf get() = _co.lessonCantLoadPdf
    val lessonOpenBrowser get() = _co.lessonOpenBrowser; val lessonLoadingPdf get() = _co.lessonLoadingPdf

    val quizTitle get() = _qz.quizTitle; val quizDaily get() = _qz.quizDaily
    val quizTopic get() = _qz.quizTopic; val quizMock get() = _qz.quizMock
    val quizStart get() = _qz.quizStart; val quizQuestions get() = _qz.quizQuestions; val quizTimeLimit get() = _qz.quizTimeLimit
    val quizCorrect get() = _qz.quizCorrect; val quizWrong get() = _qz.quizWrong
    val quizCoinsEarned get() = _qz.quizCoinsEarned; val quizReview get() = _qz.quizReview
    val quizNext get() = _qz.quizNext; val quizFinish get() = _qz.quizFinish
    val quizSkip get() = _qz.quizSkip; val quizPrevious get() = _qz.quizPrevious; val quizHint get() = _qz.quizHint
    val quizAttempted get() = _qz.quizAttempted; val quizDuration get() = _qz.quizDuration; val quizRules get() = _qz.quizRules
    val quizNoQuestions get() = _qz.quizNoQuestions; val quizRetake get() = _qz.quizRetake; val quizStartNow get() = _qz.quizStartNow
    val quizSubmitting get() = _qz.quizSubmitting; val quizQuitTitle get() = _qz.quizQuitTitle; val quizQuitBody get() = _qz.quizQuitBody
    val quizQuit get() = _qz.quizQuit; val quizKeepGoing get() = _qz.quizKeepGoing
    val quizExcellent get() = _qz.quizExcellent; val quizGoodJob get() = _qz.quizGoodJob; val quizKeepPracticing get() = _qz.quizKeepPracticing
    val quizSelectCorrect get() = _qz.quizSelectCorrect
    val quizSkipped get() = _qz.quizSkipped; val quizAnswered get() = _qz.quizAnswered
    val quizNoAvailable get() = _qz.quizNoAvailable; val quizCheckLater get() = _qz.quizCheckLater

    val caTitle get() = _ct.caTitle; val caBookmark get() = _ct.caBookmark; val caBookmarked get() = _ct.caBookmarked
    val caShare get() = _ct.caShare; val caMcqPractice get() = _ct.caMcqPractice; val caImportant get() = _ct.caImportant
    val caSubtitle get() = _ct.caSubtitle; val caSearchHint get() = _ct.caSearchHint
    val caNoSaved get() = _ct.caNoSaved; val caNoArticles get() = _ct.caNoArticles
    val caBookmarkHint get() = _ct.caBookmarkHint; val caTryFilter get() = _ct.caTryFilter
    val caReadMore get() = _ct.caReadMore; val caLoadingQ get() = _ct.caLoadingQ; val caNoMcqs get() = _ct.caNoMcqs
    val caGoBack get() = _ct.caGoBack; val caBackToArticle get() = _ct.caBackToArticle; val caNoMcqsBody get() = _ct.caNoMcqsBody
    val caNxtQuestion get() = _ct.caNxtQuestion; val caSeeResults get() = _ct.caSeeResults
    val caExcellent get() = _ct.caExcellent; val caWellDone get() = _ct.caWellDone
    val caGoodEffort get() = _ct.caGoodEffort; val caKeepPracticing get() = _ct.caKeepPracticing
    val materialsTitle get() = _ct.materialsTitle; val materialsSubtitle get() = _ct.materialsSubtitle
    val materialsDownload get() = _ct.materialsDownload; val materialsDownloaded get() = _ct.materialsDownloaded
    val materialsUpload get() = _ct.materialsUpload; val materialsView get() = _ct.materialsView; val materialsSearchHint get() = _ct.materialsSearchHint
    val materialsPopular get() = _ct.materialsPopular; val materialsNewest get() = _ct.materialsNewest; val materialsTopRated get() = _ct.materialsTopRated
    val materialsPinned get() = _ct.materialsPinned; val materialsTrending get() = _ct.materialsTrending; val materialsRecent get() = _ct.materialsRecent
    val materialsAll get() = _ct.materialsAll; val materialsFilterSubject get() = _ct.materialsFilterSubject
    val materialsFilterLanguage get() = _ct.materialsFilterLanguage; val materialsFilters get() = _ct.materialsFilters
    val materialsExplore get() = _ct.materialsExplore; val materialsMyUploads get() = _ct.materialsMyUploads

    val jobsTitle get() = _jr.jobsTitle; val jobsApplyNow get() = _jr.jobsApplyNow; val jobsLastDate get() = _jr.jobsLastDate
    val jobsPosts get() = _jr.jobsPosts; val jobsSave get() = _jr.jobsSave; val jobsSaved get() = _jr.jobsSaved
    val jobsOpeningsCountLabel get() = _jr.jobsOpeningsCountLabel
    val jobsSearchHint get() = _jr.jobsSearchHint; val jobsNoJobs get() = _jr.jobsNoJobs; val jobsTryFilter get() = _jr.jobsTryFilter
    val jobsFeatured get() = _jr.jobsFeatured; val jobsAllJobs get() = _jr.jobsAllJobs
    val roomsTitle get() = _jr.roomsTitle; val roomsChoose get() = _jr.roomsChoose; val roomsChooseHint get() = _jr.roomsChooseHint
    val roomsJoin get() = _jr.roomsJoin; val roomsLeave get() = _jr.roomsLeave
    val roomsStartSession get() = _jr.roomsStartSession; val roomsEndSession get() = _jr.roomsEndSession
    val roomsStudying get() = _jr.roomsStudying; val roomsOnline get() = _jr.roomsOnline
    val roomsTierBronze get() = _jr.roomsTierBronze; val roomsTierSilver get() = _jr.roomsTierSilver
    val roomsTierGold get() = _jr.roomsTierGold; val roomsTierDiamond get() = _jr.roomsTierDiamond
    val roomsEarnCoins get() = _jr.roomsEarnCoins; val roomsSessionActive get() = _jr.roomsSessionActive
    val roomsLive get() = _jr.roomsLive; val roomsLocked get() = _jr.roomsLocked; val roomsStudied get() = _jr.roomsStudied
    val roomsYourRoom get() = _jr.roomsYourRoom; val roomsClaimPromotion get() = _jr.roomsClaimPromotion
    val roomsMetRequirements get() = _jr.roomsMetRequirements; val roomsClaimNow get() = _jr.roomsClaimNow
    val roomsLater get() = _jr.roomsLater; val roomsRequirements get() = _jr.roomsRequirements
    val roomsKeepStudying get() = _jr.roomsKeepStudying; val roomsReadyForNext get() = _jr.roomsReadyForNext
    val roomsPromotedMidnight get() = _jr.roomsPromotedMidnight
    val roomsGroupStudy get() = _jr.roomsGroupStudy; val roomsTapToStart get() = _jr.roomsTapToStart

    val focusEndSessionTitle get() = _fp.focusEndSessionTitle; val focusKeepStudying get() = _fp.focusKeepStudying
    val focusJoining get() = _fp.focusJoining; val focusSettingUp get() = _fp.focusSettingUp
    val focusAfk get() = _fp.focusAfk; val focusAfkIdle get() = _fp.focusAfkIdle; val focusImBack get() = _fp.focusImBack
    val focusSessionComplete get() = _fp.focusSessionComplete; val focusGreatWork get() = _fp.focusGreatWork
    val focusYou get() = _fp.focusYou; val focusThisSession get() = _fp.focusThisSession; val focusTotal get() = _fp.focusTotal
    val focusBackToRooms get() = _fp.focusBackToRooms; val focusTapMessage get() = _fp.focusTapMessage
    val focusFirstHere get() = _fp.focusFirstHere; val focusOthersJoin get() = _fp.focusOthersJoin
    val profileTitle get() = _fp.profileTitle; val profileEdit get() = _fp.profileEdit; val profileStreak get() = _fp.profileStreak
    val profileCoins get() = _fp.profileCoins; val profileAchievements get() = _fp.profileAchievements; val profileBadges get() = _fp.profileBadges
    val profileStudyTime get() = _fp.profileStudyTime; val profileShare get() = _fp.profileShare; val profileRank get() = _fp.profileRank
    val profileSubjectProgress get() = _fp.profileSubjectProgress; val profileWeeklyStreak get() = _fp.profileWeeklyStreak
    val profileLess get() = _fp.profileLess; val profileMore get() = _fp.profileMore
    val profileDayStreak get() = _fp.profileDayStreak; val profileShowLess get() = _fp.profileShowLess; val profileStudy get() = _fp.profileStudy

    val achievementsSubtitle get() = _st.achievementsSubtitle; val achievementsInProgress get() = _st.achievementsInProgress
    val challengesTitle get() = _st.challengesTitle; val challengesThisWeek get() = _st.challengesThisWeek
    val challengesNone get() = _st.challengesNone; val challengesCheckBack get() = _st.challengesCheckBack
    val challengesGoal get() = _st.challengesGoal; val challengesClaimed get() = _st.challengesClaimed
    val challengesClaim get() = _st.challengesClaim; val challengesKeepStudying get() = _st.challengesKeepStudying
    val settingsTitle get() = _st.settingsTitle; val settingsAccount get() = _st.settingsAccount; val settingsAppearance get() = _st.settingsAppearance
    val settingsDarkMode get() = _st.settingsDarkMode; val settingsDarkModeSubtitle get() = _st.settingsDarkModeSubtitle
    val settingsLanguage get() = _st.settingsLanguage; val settingsLanguageSubtitle get() = _st.settingsLanguageSubtitle
    val settingsStudyPrefs get() = _st.settingsStudyPrefs; val settingsReminder get() = _st.settingsReminder; val settingsReminderSubtitle get() = _st.settingsReminderSubtitle
    val settingsAutoPlay get() = _st.settingsAutoPlay; val settingsAutoPlaySubtitle get() = _st.settingsAutoPlaySubtitle
    val settingsSound get() = _st.settingsSound; val settingsSoundSubtitle get() = _st.settingsSoundSubtitle
    val settingsHaptics get() = _st.settingsHaptics; val settingsHapticsSubtitle get() = _st.settingsHapticsSubtitle
    val settingsStorage get() = _st.settingsStorage; val settingsClearCache get() = _st.settingsClearCache; val settingsClearCacheSubtitle get() = _st.settingsClearCacheSubtitle
    val settingsOffline get() = _st2.settingsOffline; val settingsOfflineSubtitle get() = _st2.settingsOfflineSubtitle
    val settingsAbout get() = _st2.settingsAbout; val settingsVersion get() = _st2.settingsVersion; val settingsVersionSubtitle get() = _st2.settingsVersionSubtitle
    val settingsRate get() = _st2.settingsRate; val settingsRateSubtitle get() = _st2.settingsRateSubtitle
    val settingsShare get() = _st2.settingsShare; val settingsShareSubtitle get() = _st2.settingsShareSubtitle
    val settingsPrivacy get() = _st2.settingsPrivacy; val settingsPrivacySubtitle get() = _st2.settingsPrivacySubtitle
    val settingsTerms get() = _st2.settingsTerms; val settingsTermsSubtitle get() = _st2.settingsTermsSubtitle
    val settingsSupport get() = _st2.settingsSupport; val settingsSupportSubtitle get() = _st2.settingsSupportSubtitle
    val settingsLogout get() = _st2.settingsLogout; val settingsLogoutSubtitle get() = _st2.settingsLogoutSubtitle
    val settingsDeleteAccount get() = _st2.settingsDeleteAccount; val settingsDeleteSubtitle get() = _st2.settingsDeleteSubtitle
    val settingsDeleteConfirmTitle get() = _st2.settingsDeleteConfirmTitle; val settingsDeleteConfirmBody get() = _st2.settingsDeleteConfirmBody
    val settingsDeleteForever get() = _st2.settingsDeleteForever
    val settingsDownloadedContent get() = _st2.settingsDownloadedContent; val settingsManageOffline get() = _st2.settingsManageOffline
    val settingsClearing get() = _st2.settingsClearing; val settingsCacheCleared get() = _st2.settingsCacheCleared

    val paymentTitle get() = _pay.paymentTitle; val paymentActivated get() = _pay.paymentActivated; val paymentWelcome get() = _pay.paymentWelcome
    val paymentGetPro get() = _pay.paymentGetPro; val paymentUnlockAll get() = _pay.paymentUnlockAll; val paymentWhatsIncluded get() = _pay.paymentWhatsIncluded
    val paymentChoosePlan get() = _pay.paymentChoosePlan; val paymentSecure get() = _pay.paymentSecure
    val paymentMonthly get() = _pay.paymentMonthly; val paymentQuarterly get() = _pay.paymentQuarterly; val paymentAnnual get() = _pay.paymentAnnual
    val paymentSubscribe get() = _pay.paymentSubscribe; val paymentMostPopular get() = _pay.paymentMostPopular; val paymentBestValue get() = _pay.paymentBestValue
    val paymentActivate get() = _pay.paymentActivate; val paymentSuccess get() = _pay.paymentSuccess; val paymentFailed get() = _pay.paymentFailed
    val paymentCoupon get() = _pay.paymentCoupon; val paymentEnterCoupon get() = _pay.paymentEnterCoupon
    val paymentApply get() = _pay.paymentApply; val paymentApplied get() = _pay.paymentApplied
    val paymentBreakdown get() = _pay.paymentBreakdown; val paymentBasePrice get() = _pay.paymentBasePrice
    val paymentCouponDiscount get() = _pay.paymentCouponDiscount; val paymentTotal get() = _pay.paymentTotal
    val paymentCreating get() = _pay.paymentCreating; val paymentConfirming get() = _pay.paymentConfirming
    val paymentBenefit1 get() = _pay.paymentBenefit1; val paymentBenefit2 get() = _pay.paymentBenefit2; val paymentBenefit3 get() = _pay.paymentBenefit3
    val paymentBenefit4 get() = _pay.paymentBenefit4; val paymentBenefit5 get() = _pay.paymentBenefit5
    val paymentBenefit6 get() = _pay.paymentBenefit6; val paymentBenefit7 get() = _pay.paymentBenefit7
    val coursePaymentTitle get() = _pay.coursePaymentTitle; val courseUnlocked get() = _pay.courseUnlocked; val courseUnlockedBody get() = _pay.courseUnlockedBody
    val coursePurchaseBtn get() = _pay.coursePurchaseBtn; val coursePurchaseComplete get() = _pay.coursePurchaseComplete; val coursePrice get() = _pay.coursePrice
    val courseLifetime get() = _pay.courseLifetime; val courseOffline get() = _pay.courseOffline; val courseCertificate get() = _pay.courseCertificate
    val courseSecure get() = _pay.courseSecure; val courseProcessing get() = _pay.courseProcessing

    val targetTitle get() = _m.targetTitle; val targetAdd get() = _m.targetAdd; val targetComplete get() = _m.targetComplete
    val targetNoTargets get() = _m.targetNoTargets; val targetCompleted get() = _m.targetCompleted; val targetCoinsEarned get() = _m.targetCoinsEarned
    val targetCreate get() = _m.targetCreate; val targetCreateSheet get() = _m.targetCreateSheet; val targetPlaceholder get() = _m.targetPlaceholder
    val targetStreak get() = _m.targetStreak; val targetStreakProtect get() = _m.targetStreakProtect; val targetAllFilter get() = _m.targetAllFilter
    val targetMax get() = _m.targetMax; val targetCarried get() = _m.targetCarried; val targetLoading get() = _m.targetLoading
    val targetFailed get() = _m.targetFailed; val targetMorning get() = _m.targetMorning; val targetAfternoon get() = _m.targetAfternoon
    val targetNight get() = _m.targetNight; val targetEasy get() = _m.targetEasy; val targetMedium get() = _m.targetMedium; val targetHard get() = _m.targetHard
    val walletTitle get() = _m.walletTitle; val walletBalance get() = _m.walletBalance; val walletEarned get() = _m.walletEarned
    val walletSpent get() = _m.walletSpent; val walletHistory get() = _m.walletHistory; val walletEarnCoins get() = _m.walletEarnCoins
    val walletDailyStreak get() = _m.walletDailyStreak; val walletCheckIn get() = _m.walletCheckIn
    val walletCheckedIn get() = _m.walletCheckedIn; val walletCheckingIn get() = _m.walletCheckingIn
    val walletNoTasks get() = _m.walletNoTasks; val walletNoTransactions get() = _m.walletNoTransactions
    val walletInviteFriend get() = _m.walletInviteFriend; val walletWatchAd get() = _m.walletWatchAd
    val notifTitle get() = _m3.notifTitle; val notifMarkRead get() = _m3.notifMarkRead; val notifNone get() = _m3.notifNone
    val notifToday get() = _m3.notifToday; val notifYesterday get() = _m3.notifYesterday; val notifJustNow get() = _m3.notifJustNow
    val chatRoomChat get() = _m3.chatRoomChat; val chatLive get() = _m3.chatLive
    val chatConnecting get() = _m3.chatConnecting; val chatReconnecting get() = _m3.chatReconnecting; val chatLoading get() = _m3.chatLoading
    val chatNoMessages get() = _m3.chatNoMessages; val chatStartConversation get() = _m3.chatStartConversation; val chatToday get() = _m3.chatToday
    val chatMessageHint get() = _m3.chatMessageHint; val chatFailedSend get() = _m3.chatFailedSend; val chatYou get() = _m3.chatYou

    val pomodoroFocus get() = _m2.pomodoroFocus; val pomodoroBreak get() = _m2.pomodoroBreak; val pomodoroLongBreak get() = _m2.pomodoroLongBreak
    val pomodoroPause get() = _m2.pomodoroPause; val pomodoroStart get() = _m2.pomodoroStart
    val pomodoroFocusDone get() = _m2.pomodoroFocusDone; val pomodoroFocusDoneBody get() = _m2.pomodoroFocusDoneBody
    val pomodoroBreakOver get() = _m2.pomodoroBreakOver; val pomodoroBreakOverBody get() = _m2.pomodoroBreakOverBody
    val pomodoroLongDone get() = _m2.pomodoroLongDone; val pomodoroLongDoneBody get() = _m2.pomodoroLongDoneBody
    val recallTitle get() = _m2.recallTitle; val recallSubtitle get() = _m2.recallSubtitle; val recallOverallMastery get() = _m2.recallOverallMastery
    val recallRetryWeak get() = _m2.recallRetryWeak; val recallNoCards get() = _m2.recallNoCards; val recallAskAdmin get() = _m2.recallAskAdmin
    val recallChooseSubject get() = _m2.recallChooseSubject; val recallLoading get() = _m2.recallLoading; val recallFailed get() = _m2.recallFailed
    val recallEasy get() = _m2.recallEasy; val recallMedium get() = _m2.recallMedium; val recallHard get() = _m2.recallHard
    val pdfUnlock get() = _m2.pdfUnlock; val pdfBuyAccess get() = _m2.pdfBuyAccess; val pdfMaybeLater get() = _m2.pdfMaybeLater
    val pdfFullAccess get() = _m2.pdfFullAccess; val pdfLoadingPdf get() = _m2.pdfLoadingPdf; val pdfCantLoad get() = _m2.pdfCantLoad
    val pdfGoBack get() = _m2.pdfGoBack; val pdfNoPages get() = _m2.pdfNoPages
    val marketTitle get() = _m2.marketTitle; val marketSubtitle get() = _m2.marketSubtitle; val marketSell get() = _m2.marketSell
    val marketSearchHint get() = _m2.marketSearchHint; val marketNoNotes get() = _m2.marketNoNotes
    val marketBeFirst get() = _m4.marketBeFirst; val marketUpload get() = _m4.marketUpload; val marketFeatured get() = _m4.marketFeatured
    val topicQuizTitle get() = _m4.topicQuizTitle; val topicQuizDetails get() = _m4.topicQuizDetails; val topicQuizStart get() = _m4.topicQuizStart
    val topicQuizPerQuestion get() = _m4.topicQuizPerQuestion; val topicQuizCoin get() = _m4.topicQuizCoin
    val topicQuizSkip get() = _m4.topicQuizSkip; val topicQuizHint get() = _m4.topicQuizHint
    val topicQuizTimer get() = _m4.topicQuizTimer; val topicQuizReview get() = _m4.topicQuizReview
    val demotionStudyNow get() = _m4.demotionStudyNow; val demotionDismiss get() = _m4.demotionDismiss
    val promotionCongrats get() = _m4.promotionCongrats; val promotionPromotedTo get() = _m4.promotionPromotedTo; val promotionPerks get() = _m4.promotionPerks
    val pipSessionActive get() = _m4.pipSessionActive; val pipReturn get() = _m4.pipReturn
    val downloadsTitle get() = _m4.downloadsTitle; val downloadsSubtitle get() = _m4.downloadsSubtitle
    val downloadsNone get() = _m4.downloadsNone; val downloadsNoneHint get() = _m4.downloadsNoneHint; val downloadsBrowse get() = _m4.downloadsBrowse
    val filterAll get() = _m4.filterAll; val filterPrelims get() = _m4.filterPrelims; val filterMains get() = _m4.filterMains; val filterSaved get() = _m4.filterSaved
    // ── Quiz extras ──────────────────────────────────────────────
    val quizReviewAll get() = _qz.quizReviewAll; val quizRetakeQuiz get() = _qz.quizRetakeQuiz
    val quizBackToQuizzes get() = _qz.quizBackToQuizzes; val quizNavTitle get() = _qz.quizNavTitle
    val quizCorrectAns get() = _qz.quizCorrectAns; val quizWrongAns get() = _qz.quizWrongAns
    val quizSaveNext get() = _qz.quizSaveNext; val quizSubmitTest get() = _qz.quizSubmitTest
    val quizStartTest get() = _qz.quizStartTest; val quizTestOverview get() = _qz.quizTestOverview
    val quizCanResume get() = _qz.quizCanResume; val quizAvgScore get() = _qz.quizAvgScore
    val quizFullMock get() = _qz.quizFullMock; val quizMiniTest get() = _qz.quizMiniTest
    val quizPrevYear get() = _qz.quizPrevYear; val quizAllTests get() = _qz.quizAllTests
    val quizFeatured get() = _qz.quizFeatured; val quizPracticeReal get() = _qz.quizPracticeReal
    val quizNoTestsCategory get() = _qz.quizNoTestsCategory; val quizNoTestsYet get() = _qz.quizNoTestsYet
    val quizTestsComingSoon get() = _qz.quizTestsComingSoon
    val quizCoinsEarned2 get() = _qz.quizCoinsEarned2; val quizNoCoins get() = _qz.quizNoCoins
    val quizAddedWallet get() = _qz.quizAddedWallet; val quizAlreadyEarned get() = _qz.quizAlreadyEarned
    val quizSubjectAnalysis get() = _qz.quizSubjectAnalysis
    val quizViewLeaderboard get() = _qz.quizViewLeaderboard; val quizRetryTest get() = _qz.quizRetryTest
    val quizBackToTests get() = _qz.quizBackToTests; val quizCreateCustom get() = _qz.quizCreateCustom
    val quizSelectSubjects get() = _qz.quizSelectSubjects; val quizNegativeMarking get() = _qz.quizNegativeMarking
    val quizCustomTest get() = _qz.quizCustomTest; val quizStartCustom get() = _qz.quizStartCustom
    val quizSubmitTestTitle get() = _qz.quizSubmitTestTitle
    val quizLoadingQ get() = _qz.quizLoadingQ; val quizSettingUp get() = _qz.quizSettingUp
    val quizPreparingQ get() = _qz.quizPreparingQ; val quizYourRank get() = _qz.quizYourRank
    // ── Answer Writing (Mains practice) ───────────────────────────
    val awTitle get() = _qz.awTitle; val awSubtitle get() = _qz.awSubtitle
    val awTodayBadge get() = _qz.awTodayBadge; val awStartWriting get() = _qz.awStartWriting
    val awQuestionsTab get() = _qz.awQuestionsTab; val awMyAnswersTab get() = _qz.awMyAnswersTab
    val awStatusNew get() = _qz.awStatusNew; val awStatusPending get() = _qz.awStatusPending; val awStatusReviewed get() = _qz.awStatusReviewed
    val awMarks get() = _qz.awMarks; val awWordLimit get() = _qz.awWordLimit; val awWords get() = _qz.awWords
    val awTips get() = _qz.awTips; val awYourAnswer get() = _qz.awYourAnswer; val awModelAnswer get() = _qz.awModelAnswer
    val awFeedback get() = _qz.awFeedback; val awScore get() = _qz.awScore; val awWriteHint get() = _qz.awWriteHint
    val awSubmit get() = _qz.awSubmit; val awSubmitting get() = _qz.awSubmitting
    val awConfirmTitle get() = _qz.awConfirmTitle; val awConfirmBody get() = _qz.awConfirmBody
    val awOverLimit get() = _qz.awOverLimit
    val awEmpty get() = _qz.awEmpty; val awEmptyBody get() = _qz.awEmptyBody
    val awNoSubmissions get() = _qz.awNoSubmissions; val awNoSubmissionsBody get() = _qz.awNoSubmissionsBody
    val awPendingNote get() = _qz.awPendingNote; val awDashSubtitle get() = _qz.awDashSubtitle
    val awTypeMode get() = _qz.awTypeMode; val awPhotoMode get() = _qz.awPhotoMode; val awPhotoHint get() = _qz.awPhotoHint
    val awTakePhoto get() = _qz.awTakePhoto; val awFromGallery get() = _qz.awFromGallery; val awPhotos get() = _qz.awPhotos
    val awPeerReview get() = _qz.awPeerReview; val awPeerReviewSub get() = _qz.awPeerReviewSub; val awReviewNow get() = _qz.awReviewNow
    val awReviewsGiven get() = _qz.awReviewsGiven; val awPendingReviews get() = _qz.awPendingReviews; val awReviewCredits get() = _qz.awReviewCredits
    val awReviewLockedNoSub get() = _qz.awReviewLockedNoSub; val awReviewLockedNotReviewed get() = _qz.awReviewLockedNotReviewed
    val awHelpFellow get() = _qz.awHelpFellow; val awReviewBannerTitle get() = _qz.awReviewBannerTitle; val awReviewBannerBody get() = _qz.awReviewBannerBody
    val awAnonymous get() = _qz.awAnonymous; val awStudentAnswer get() = _qz.awStudentAnswer; val awYourReview get() = _qz.awYourReview
    val awReviewQ1 get() = _qz.awReviewQ1; val awReviewQ2 get() = _qz.awReviewQ2; val awReviewQ3 get() = _qz.awReviewQ3; val awReviewQ4 get() = _qz.awReviewQ4
    val awPartly get() = _qz.awPartly; val awSuggestionHint get() = _qz.awSuggestionHint; val awSubmitReview get() = _qz.awSubmitReview
    val awNoMoreReviews get() = _qz.awNoMoreReviews; val awNoMoreReviewsBody get() = _qz.awNoMoreReviewsBody
    val awPeerReviewsReceived get() = _qz.awPeerReviewsReceived; val awUnderPeerReview get() = _qz.awUnderPeerReview
    val awAreaContent get() = _qz.awAreaContent; val awAreaStructure get() = _qz.awAreaStructure; val awAreaAnalysis get() = _qz.awAreaAnalysis
    val awAreaBihar get() = _qz.awAreaBihar; val awAreaPresentation get() = _qz.awAreaPresentation; val awAreaConclusion get() = _qz.awAreaConclusion
    val awInsightsTab get() = _qz.awInsightsTab; val awInsightsTitle get() = _qz.awInsightsTitle; val awInsightsSub get() = _qz.awInsightsSub
    val awAnswersWritten get() = _qz.awAnswersWritten; val awReviewsReceived get() = _qz.awReviewsReceived; val awAvgRating get() = _qz.awAvgRating
    val awWritingStreak get() = _qz.awWritingStreak; val awDays get() = _qz.awDays; val awThisMonth get() = _qz.awThisMonth
    val awTotalWords get() = _qz.awTotalWords; val awMentorScore get() = _qz.awMentorScore
    val awGoalTitle get() = _qz.awGoalTitle; val awGoalBody get() = _qz.awGoalBody; val awGoalDone get() = _qz.awGoalDone
    val awReviewerLevel get() = _qz.awReviewerLevel; val awLevelBeginner get() = _qz.awLevelBeginner; val awLevelActive get() = _qz.awLevelActive
    val awLevelAdvanced get() = _qz.awLevelAdvanced; val awLevelExpert get() = _qz.awLevelExpert
    val awKeepItUp get() = _qz.awKeepItUp; val awNoInsights get() = _qz.awNoInsights; val awChars get() = _qz.awChars
    val awPyq get() = _qz.awPyq; val awModelAnswerTomorrow get() = _qz.awModelAnswerTomorrow
    val awAreaIntro get() = _qz.awAreaIntro; val awAreaValueAdd get() = _qz.awAreaValueAdd; val awUpTo3 get() = _qz.awUpTo3
    val awTopWeaknesses get() = _qz.awTopWeaknesses; val awTopReviewers get() = _qz.awTopReviewers; val awTopWriters get() = _qz.awTopWriters
    val awAnswersLower get() = _qz.awAnswersLower; val awGiveToGet get() = _qz.awGiveToGet
    // ── Content extras ────────────────────────────────────────────
    val materialsSave get() = _ct2.materialsSave; val materialsRemoveSaved get() = _ct2.materialsRemoveSaved
    val materialsTapOpen get() = _ct2.materialsTapOpen; val materialsNoPreview get() = _ct2.materialsNoPreview
    val materialsUploadTitle get() = _ct2.materialsUploadTitle
    val materialsUploading get() = _ct2.materialsUploading; val materialsSubmitReview get() = _ct2.materialsSubmitReview
    val materialsReviewNote get() = _ct2.materialsReviewNote
    val materialsNoSaved get() = _ct2.materialsNoSaved; val materialsNoResources get() = _ct2.materialsNoResources
    val materialsBookmarkHint get() = _ct2.materialsBookmarkHint
    val materialsNoUploads get() = _ct2.materialsNoUploads; val materialsUploadHint get() = _ct2.materialsUploadHint
    val materialsPublished get() = _ct2.materialsPublished; val materialsRejected get() = _ct2.materialsRejected
    val materialsNoDownloads get() = _ct2.materialsNoDownloads; val materialsDownloadHint get() = _ct2.materialsDownloadHint
    val materialsPremiumContent get() = _ct2.materialsPremiumContent; val materialsChargeCoins get() = _ct2.materialsChargeCoins
    val materialsDownloadedDone get() = _ct2.materialsDownloadedDone; val materialsUnlockPro get() = _ct2.materialsUnlockPro
    val materialsDownloadFree get() = _ct2.materialsDownloadFree
    val caLoadingQuestions get() = _ct2.caLoadingQuestions; val caSavedDone get() = _ct2.caSavedDone
    val caGateWaiting get() = _ct2.caGateWaiting; val caGateReady get() = _ct2.caGateReady
    val caGateSubtitle get() = _ct2.caGateSubtitle; val caGateContinue get() = _ct2.caGateContinue
    val caGateWaitButton get() = _ct2.caGateWaitButton
    val caGateReadyMcq get() = _ct2.caGateReadyMcq; val caGateContinueMcq get() = _ct2.caGateContinueMcq
    // ── Job/Room extras ───────────────────────────────────────────
    val jobsNew get() = _jr.jobsNew; val jobsApplicationClosed get() = _jr.jobsApplicationClosed
    val jobsNoLink get() = _jr.jobsNoLink; val jobsApplyOfficialSite get() = _jr.jobsApplyOfficialSite
    val jobsAboutJob get() = _jr.jobsAboutJob; val jobsEligibility get() = _jr.jobsEligibility
    val jobsImportantDates get() = _jr.jobsImportantDates; val jobsApplyStart get() = _jr.jobsApplyStart
    val jobsAlerts get() = _jr.jobsAlerts; val jobsAlertsSubtitle get() = _jr.jobsAlertsSubtitle
    val roomsSearchHint get() = _jr.roomsSearchHint
    val roomsFeatured get() = _jr.roomsFeatured; val roomsAllRooms get() = _jr.roomsAllRooms
    val roomsLeaderboard get() = _jr.roomsLeaderboard; val roomsLeaderboardResets get() = _jr.roomsLeaderboardResets
    val roomsFocusSession get() = _jr.roomsFocusSession
    val roomsFocusTip1 get() = _jr.roomsFocusTip1; val roomsFocusTip2 get() = _jr.roomsFocusTip2
    val roomsFocusTip3 get() = _jr.roomsFocusTip3; val roomsFocusTip4 get() = _jr.roomsFocusTip4
    val roomsCreate get() = _jr.roomsCreate; val roomsTodayTopic get() = _jr.roomsTodayTopic
    val roomsMaxMembers get() = _jr.roomsMaxMembers; val roomsRequiresCode get() = _jr.roomsRequiresCode
    val roomsCreateBtn get() = _jr.roomsCreateBtn; val roomsEnterCode get() = _jr.roomsEnterCode
    val roomsJoinBtn get() = _jr.roomsJoinBtn; val roomsTodayStudyTogether get() = _jr.roomsTodayStudyTogether
    // ── Dashboard extras ──────────────────────────────────────────
    val dashboardAspirant get() = _db.dashboardAspirant; val dashboardNoTargets2 get() = _db.dashboardNoTargets2
    val dashboardViewAll get() = _db.dashboardViewAll; val dashboardCreateTarget get() = _db.dashboardCreateTarget
    val dashboardWeeklyConsistency get() = _db.dashboardWeeklyConsistency
    val dashboardWeeklySubtitle get() = _db.dashboardWeeklySubtitle
    val dashboardNoActivity get() = _db.dashboardNoActivity; val dashboardStartStudying get() = _db.dashboardStartStudying
    val dashboardQuickAccess get() = _db.dashboardQuickAccess; val dashboardRecommended get() = _db.dashboardRecommended
    val dashboardMySchedule get() = _db.dashboardMySchedule; val dashboardUpcomingEvents get() = _db.dashboardUpcomingEvents
    val dashboardNoClasses get() = _db.dashboardNoClasses; val dashboardNoMeetingLink get() = _db.dashboardNoMeetingLink
    val dashboardClassEnded get() = _db.dashboardClassEnded; val dashboardAlreadyRegistered get() = _db.dashboardAlreadyRegistered
    val dashboardCreateCustomTarget get() = _db.dashboardCreateCustomTarget
    val dashboardBuildPlan get() = _db.dashboardBuildPlan; val dashboardWhatNext get() = _db.dashboardWhatNext
    // ── Course extras ─────────────────────────────────────────────
    val courseByAuthor get() = _co.courseByAuthor; val courseYourProgress get() = _co.courseYourProgress
    val courseCertEarned get() = _co.courseCertEarned; val courseCertTitle get() = _co.courseCertTitle
    val courseCertTap get() = _co.courseCertTap; val courseCertComplete get() = _co.courseCertComplete
    val courseShareCert get() = _co.courseShareCert; val courseShareCertBtn get() = _co.courseShareCertBtn
    val courseCourseCompleted get() = _co.courseCourseCompleted
    val courseContinueLearning get() = _co.courseContinueLearning; val courseStartLearning get() = _co.courseStartLearning
    val courseEnrollFree get() = _co.courseEnrollFree; val courseAbout get() = _co.courseAbout
    val courseFreeTrial get() = _co.courseFreeTrial; val courseWatch get() = _co.courseWatch
    val courseSyllabus get() = _co.courseSyllabus; val courseShowLess get() = _co.courseShowLess
    val courseShowAll get() = _co.courseShowAll; val courseStudentReviews get() = _co.courseStudentReviews
    val coursePriceSummary get() = _co.coursePriceSummary; val courseCoinsDiscount get() = _co.courseCoinsDiscount
    val courseCouponApplied get() = _co.courseCouponApplied; val courseTotalPayable get() = _co.courseTotalPayable
    val courseUseCoins get() = _co.courseUseCoins; val courseYouHaveCoins get() = _co.courseYouHaveCoins
    val courseCouponCode get() = _co.courseCouponCode; val courseCouponSaved get() = _co.courseCouponSaved
    val courseSecurePayment get() = _co.courseSecurePayment; val courseGrabNow get() = _co.courseGrabNow
    val courseTapStar get() = _co.courseTapStar; val courseHindiEnglish get() = _co.courseHindiEnglish
    val courseInProgress get() = _co.courseInProgress; val courseNoCoursesYet get() = _co.courseNoCoursesYet
    val courseExploreStore get() = _co.courseExploreStore; val courseMyCertificates get() = _co.courseMyCertificates
    val courseOverallProgress get() = _co.courseOverallProgress; val courseTapRead get() = _co.courseTapRead
    val courseEnrollTitle get() = _co.courseEnrollTitle
    // ── Focus/Profile extras ──────────────────────────────────────
    val profileCoinWallet get() = _fp.profileCoinWallet; val profileViewAll get() = _fp.profileViewAll
    val profileCurrentBalance get() = _fp.profileCurrentBalance; val profileHowToEarn get() = _fp.profileHowToEarn
    val profileEarnQuiz get() = _fp.profileEarnQuiz; val profileEarnStreak get() = _fp.profileEarnStreak
    val profileEarnReferral get() = _fp.profileEarnReferral
    val profileNoTransactions get() = _fp.profileNoTransactions; val profileMyCourses get() = _fp.profileMyCourses
    val recallKeepGoing get() = _fp.recallKeepGoing; val recallSwipeRate get() = _fp.recallSwipeRate
    val recallTapReveal get() = _fp.recallTapReveal; val recallRevealAnswer get() = _fp.recallRevealAnswer
    val recallGotItBtn get() = _fp.recallGotItBtn; val recallSessionComplete get() = _fp.recallSessionComplete
    val recallBackToSubjects get() = _fp.recallBackToSubjects
    // ── Misc extras ───────────────────────────────────────────────
    val pdfReadFreePages get() = _m.pdfReadFreePages; val pdfPurchaseUnlock get() = _m.pdfPurchaseUnlock
    val pdfPageNum get() = _m.pdfPageNum; val pdfPageLocked get() = _m.pdfPageLocked
    val pdfPurchaseAccess get() = _m.pdfPurchaseAccess
    val loginIAgree get() = _m.loginIAgree; val editBio get() = _m.editBio
    val registerCreateProfile get() = _m.registerCreateProfile; val registerPersonalize get() = _m.registerPersonalize
    val registerContinue get() = _m.registerContinue; val registerDataSecure get() = _m.registerDataSecure
    val registerPersonalDetails get() = _m.registerPersonalDetails
    val examStartPreparing get() = _m.examStartPreparing; val examPrimaryTip get() = _m.examPrimaryTip
    val examPrimaryTapChange get() = _m.examPrimaryTapChange; val examBiharState get() = _m.examBiharState
    val marketGetFree get() = _m.marketGetFree
    val paymentStartLearning get() = _m.paymentStartLearning; val paymentOpenFailed get() = _m.paymentOpenFailed
    val studyFocusJustNow get() = _m.studyFocusJustNow; val notifAllRead get() = _m.notifAllRead
    // ── Placeholders extras ───────────────────────────────────────
    val placeholdersBilledMonthly get() = _m2.placeholdersBilledMonthly
    val placeholdersBilledQuarterly get() = _m2.placeholdersBilledQuarterly
    val placeholdersBilledAnnually get() = _m2.placeholdersBilledAnnually
    val placeholdersExclusiveNotes get() = _m2.placeholdersExclusiveNotes
    val placeholdersUnlockAll get() = _m2.placeholdersUnlockAll
    val placeholdersPrioritySupport get() = _m2.placeholdersPrioritySupport
    val placeholdersAllPremium get() = _m2.placeholdersAllPremium
    val placeholdersNoPremiumYet get() = _m2.placeholdersNoPremiumYet
    val placeholdersCheckBack get() = _m2.placeholdersCheckBack
    val placeholdersNotesReader get() = _m2.placeholdersNotesReader
    val placeholdersOpenWith get() = _m2.placeholdersOpenWith
    // ── MiscStrings4 extras ───────────────────────────────────────
    val quizReviewExplanation get() = _m4.quizReviewExplanation
    val dashboardActiveRecall get() = _m4.dashboardActiveRecall
    val mockSampleQ1  get() = _m4.mockSampleQ1
    val mockSampleA   get() = _m4.mockSampleA
    val mockSampleQ2  get() = _m4.mockSampleQ2

    // ── New strings (previously hardcoded) ─────────────────────────────
    val materialsOpenPdf          get() = _m4.materialsOpenPdf
    val materialsUnlockPro2       get() = _m4.materialsUnlockPro2
    val materialsShareHint        get() = _m4.materialsShareHint
    val materialsNotesTitle       get() = _m4.materialsNotesTitle
    val materialsAuthorName       get() = _m4.materialsAuthorName
    val materialsContentType      get() = _m4.materialsContentType
    val materialsTags             get() = _m4.materialsTags
    val materialsTagHint          get() = _m4.materialsTagHint
    val materialsDownloadDevice   get() = _m4.materialsDownloadDevice
    val materialsLifetimeAccess   get() = _m4.materialsLifetimeAccess
    val targetSwipeNavigate       get() = _m4.targetSwipeNavigate
    val targetAllDone             get() = _m4.targetAllDone
    val courseCertOfCompletion    get() = _m4.courseCertOfCompletion
    val courseCertDownload        get() = _m4.courseCertDownload
    val courseCertShare           get() = _m4.courseCertShare
    val courseCertNotAvailable    get() = _m4.courseCertNotAvailable
    val permNotNow                get() = _m4.permNotNow
    val recallRelatedMcq          get() = _m4.recallRelatedMcq
    val examLoadFailed            get() = _m4.examLoadFailed
    val registerNameHint          get() = _m4.registerNameHint
    val courseBpscExpert          get() = _m4.courseBpscExpert
    val lessonViewerTimeout       get() = _m4.lessonViewerTimeout
    val notifSettingsHint         get() = _m4.notifSettingsHint
    val placeholdersSeeAll2       get() = _m4.placeholdersSeeAll2
    val placeholdersPremiumNotes  get() = _m4.placeholdersPremiumNotes
    val placeholdersPremiumCourses get() = _m4.placeholdersPremiumCourses
    val roomCodeHint              get() = _m4.roomCodeHint
    val roomCreate                get() = _m4.roomCreate
    val roomTabChat               get() = _m4.roomTabChat
    val roomTabMembers            get() = _m4.roomTabMembers
    val roomTabLeaderboard        get() = _m4.roomTabLeaderboard
    val roomTabPomodoro           get() = _m4.roomTabPomodoro
    val mockAttempts              get() = _m4.mockAttempts
    val mockUnlock                get() = _m4.mockUnlock
    val mockCustom                get() = _m4.mockCustom
    val mockSubjectWise           get() = _m4.mockSubjectWise
    val mockLeaderboard           get() = _m4.mockLeaderboard
    val mockQuitTitle             get() = _m4.mockQuitTitle
    val mockQuitBody              get() = _m4.mockQuitBody
    val mcqQuitTitle              get() = _m4.mcqQuitTitle

    // ── MiscStrings5 getters ──────────────────────────────────
    val recallReviseAgain       get() = _m5.recallReviseAgain
    val recallAnswer            get() = _m5.recallAnswer
    val recallExample           get() = _m5.recallExample
    val recallMastered          get() = _m5.recallMastered
    val placeholderFileMissing  get() = _m5.placeholderFileMissing
    val placeholderOnDevice     get() = _m5.placeholderOnDevice
    val placeholderGetPro       get() = _m5.placeholderGetPro
    val placeholderPremiumPdfs  get() = _m5.placeholderPremiumPdfs
    val placeholderOfflineDownloads get() = _m5.placeholderOfflineDownloads
    val dashRegistered          get() = _m5.dashRegistered
    val dashRegister            get() = _m5.dashRegister
    val dashProgress            get() = _m5.dashProgress
    val mockCreateCustom        get() = _m5.mockCreateCustom
    val mockInstructions        get() = _m5.mockInstructions
    val mockPercentile          get() = _m5.mockPercentile
    val mockNegativeMarking     get() = _m5.mockNegativeMarking
    val profileMaxTier          get() = _m5.profileMaxTier
    val profileLast28           get() = _m5.profileLast28
    val profileBadgeEarned      get() = _m5.profileBadgeEarned
    val profileNotEarned        get() = _m5.profileNotEarned
    val materialAbout           get() = _m5.materialAbout
    val materialPriceCoins      get() = _m5.materialPriceCoins
    val materialUnlockAccess    get() = _m5.materialUnlockAccess
    val materialPrice           get() = _m5.materialPrice
    val quizImageQ              get() = _m5.quizImageQ
    val quizAccuracy            get() = _m5.quizAccuracy
    val quizCorrectAnswer       get() = _m5.quizCorrectAnswer
    val quizScore               get() = _m5.quizScore
    val myLearningNotesTitle    get() = _m5.myLearningNotesTitle
    val myLearningUpiHint       get() = _m5.myLearningUpiHint
    val adLoading               get() = _m5.adLoading
    val adSponsored             get() = _m5.adSponsored
    val adGreatSession          get() = _m5.adGreatSession
    val focusActive             get() = _m5.focusActive
    val targetCoins             get() = _m5.targetCoins
    val targetCarriedForward    get() = _m5.targetCarriedForward
    val courseWhatYouLearn      get() = _m5.courseWhatYouLearn
    val courseAboutInstructor   get() = _m5.courseAboutInstructor
    val permOpenSettings        get() = _m5.permOpenSettings
    val registerEmailHint       get() = _m5.registerEmailHint
    val examBack                get() = _m5.examBack
    val marketOwned             get() = _m5.marketOwned
    val roomsReset              get() = _m5.roomsReset
    val walletCoins             get() = _m5.walletCoins
    val lessonInAppBrowser      get() = _m5.lessonInAppBrowser
    val uiTryAgain              get() = _m5.uiTryAgain
    val quizDailyScore          get() = _m5.quizDailyScore
    val quizDailyAccuracy       get() = _m5.quizDailyAccuracy
    val closeLabel              get() = _m5.closeLabel
    val marketRulesTitle        get() = _m5.marketRulesTitle
    val marketRulesSubtitle     get() = _m5.marketRulesSubtitle
    val marketRule1             get() = _m5.marketRule1
    val marketRule2             get() = _m5.marketRule2
    val marketRule3             get() = _m5.marketRule3
    val marketRule4             get() = _m5.marketRule4
    val marketRule5             get() = _m5.marketRule5
    val marketRulesGotIt        get() = _m5.marketRulesGotIt
    val marketRulesInfoTooltip  get() = _m5.marketRulesInfoTooltip
    // ── Auto-restored getters ──────────────────────
    val quizSubmit                   get() = _qz.quizSubmit
    val quizResult                   get() = _qz.quizResult
    val focusSaving                  get() = _fp.focusSaving
    val profileHeatmap               get() = _fp.profileHeatmap
    val recallGotIt                  get() = _fp.recallGotIt
    val permStayUpdated              get() = _m6.permStayUpdated
    val permStreakWarn               get() = _m6.permStreakWarn
    val permStudentsNote             get() = _m6.permStudentsNote
    val permEnableBtn                get() = _m6.permEnableBtn
    val adNoLimit                    get() = _m6.adNoLimit
    val adAdvertisement              get() = _m6.adAdvertisement
    val adSkip                       get() = _m6.adSkip
    val materialOpen                 get() = _m6.materialOpen
    val materialPreview              get() = _m6.materialPreview
    val materialUnlockPro            get() = _m6.materialUnlockPro
    val roomSubject                  get() = _m6.roomSubject
    val uiSomethingWrong             get() = _m6.uiSomethingWrong
    val permMaybeLater               get() = _m6.permMaybeLater
    val permBlocked                  get() = _m6.permBlocked
    val mlShareNotes                 get() = _m6.mlShareNotes
    val mlContentType                get() = _m6.mlContentType
    val mlAttachFile                 get() = _m6.mlAttachFile
    val roomPrivate                  get() = _m6.roomPrivate
    val roomJoinPrivate              get() = _m6.roomJoinPrivate
}

// ── Helper to build AppStrings from grouped sub-objects ───────────────────────
private fun mkAppStrings(
    c: CommonStrings, n: NavAuthStrings, pe: ProfileEditStrings,
    db: DashboardStrings, co: CourseStrings, qz: QuizStrings,
    ct: ContentStrings, ct2: ContentStrings2, jr: JobRoomStrings, fp: FocusProfileStrings,
    st: SettingsStrings, st2: SettingsStrings2, pay: PaymentStrings,
    m: MiscStrings, m3: MiscStrings3, m2: MiscStrings2, m4: MiscStrings4, m5: MiscStrings5, m6: MiscStrings6,
) = AppStrings(_c=c,_n=n,_pe=pe,_db=db,_co=co,_qz=qz,_ct=ct,_ct2=ct2,_jr=jr,_fp=fp,
    _st=st,_st2=st2,_pay=pay,_m=m,_m3=m3,_m2=m2,_m4=m4,_m5=m5,_m6=m6)

// ── ENGLISH ───────────────────────────────────────────────────────────────────
val EnglishStrings: AppStrings = mkAppStrings(
    c = CommonStrings(
        ok="OK", yes="Yes", no="No", cancel="Cancel", save="Save", close="Close", back="Back",
        retry="Retry", loading="Loading…", error="Error", success="Success", submit="Submit",
        next="Next", done="Done", skip="Skip", search="Search", noData="No data available",
        seeAll="See All", viewAll="View All", free="Free", premium="Premium", coins="Coins",
        minutes="min", hours="hr", days="days", all="All", start="Start",
        goBack="Go Back", tryAgain="Try Again", version="v1.0.0",
    ),
    n = NavAuthStrings(
        navDashboard="Dashboard", navMyLearning="My Courses", navRooms="Study Rooms", navProfile="Profile",
        splashTagline="Study Smart. Recall Better. Rank Higher.",
        langSelectTitle="Choose Your Language", langSelectSubtitle="You can change this anytime from Settings",
        langSelectContinue="Continue",
        onboardingGetStarted="Get Started", onboardingSkip="Skip",
        onboarding1Title="Daily Targets", onboarding1Subtitle="Stay on Track",
        onboarding1Body="Create your own daily study targets with linked quizzes. You decide what to study, when to study, and we track your progress every step of the way.",
        onboarding2Title="Active Recall", onboarding2Subtitle="Retain More",
        onboarding2Body="Flashcards and spaced repetition built into every topic. Study less, remember more — proven science-backed techniques for BPSC.",
        onboarding3Title="Group Study Rooms", onboarding3Subtitle="Earn While You Learn",
        onboarding3Body="Join virtual study rooms with fellow BPSC aspirants. Compete on leaderboards, earn coins and redeem them for premium content.",
        loginSubtitle="Sign in to continue", loginEnterMobile="Enter Mobile Number",
        loginSendOtp="Send OTP", loginMobileHint="Mobile number", loginTermsPrivacy="Terms & Privacy Policy",
        otpTitle="Verify Your Number", otpSentTo="WhatsApp OTP sent to +91",
        otpResend="Resend WhatsApp OTP", otpVerify="Verify & Continue",
        otpChangeNumber="Change Mobile Number", otpDidntReceive="Didn't receive WhatsApp OTP?",
    ),
    pe = ProfileEditStrings(
        editPersonalInfo="Personal Info", editFullName="Full Name *", editEmail="Email Address",
        editDistrict="District", editExamSettings="Exam Settings", editPrepLevel="Preparation Level",
        editTargetYear="Target Year", editSaveChanges="Save Changes", editSaving="Saving…",
        editMobile="Mobile Number", editVerified="Verified ✓", editNotVerified="Not Verified",
        prepBeginner="Beginner", prepIntermediate="Intermediate", prepAdvanced="Advanced",
        examSetupChoose="Choose Your Exams",
        examSetupTapHint="Tap once to set primary · Tap again to add secondary",
        examSetupSearch="Search exam...", examSetupPrimary="PRIMARY", examSetupSecondary="SECONDARY",
        examSetupTargetYear="Review Your Plan", examSetupPlan="Your Exam Plan",
        examSetupPersonalize="What we'll personalize for you", examSetupNext="Next →",
        thisYear="This year 🔥", nextYear="Next year", longTerm="Long term",
    ),
    db = DashboardStrings(
        dashboardHello="Hello", dashboardGoodMorning="Good Morning",
        dashboardGoodAfternoon="Good Afternoon", dashboardGoodEvening="Good Evening",
        dashboardDailyTargets="Daily Targets", dashboardAddTarget="+ Add Target",
        dashboardNoTargets="No targets yet. Add one to stay on track!",
        dashboardTodayProgress="Today's Progress", dashboardContinueLearning="Continue Learning",
        dashboardDailyQuiz="Daily Quiz", dashboardCurrentAffairs="Current Affairs",
        dashboardLatestJobs="Latest Jobs", dashboardStudyRoom="Study Room",
        dashboardCoins="Coins", dashboardStreak="Streak", dashboardAccuracy="Accuracy",
        dashboardStudyTime="Study", dashboardTodayQuizzes="Today's Quizzes",
        dashboardTodayFocus="Today's Focus", dashboardNoQuizzes="No quizzes today. Check back later!",
        dashboardQuizNoQuestions="This quiz has no questions yet.", dashboardDone="✓ Done",
        drawerCourses="Courses", drawerStudyMaterials="Student Hub",
        drawerCurrentAffairs="Current Affairs", drawerMockTests="Mock Tests",
        drawerJobAlerts="Job Alerts", drawerStudyRooms="Study Rooms",
        drawerCoinWallet="Coin Wallet", drawerAchievements="Achievements",
        drawerSettings="Settings", drawerLanguage="Language", drawerLogout="Logout",
        dashboardAspirant="Aspirant 👋", dashboardNoTargets2="No targets set",
        dashboardViewAll="View All", dashboardCreateTarget="Create Custom Target",
        dashboardWeeklyConsistency="Weekly Consistency",
        dashboardWeeklySubtitle="Your study activity this week",
        dashboardNoActivity="No activity data yet",
        dashboardStartStudying="Start studying to see your progress",
        dashboardQuickAccess="Quick Access", dashboardRecommended="Recommended for You",
        dashboardMySchedule="My Schedule", dashboardUpcomingEvents="Upcomings",
        dashboardNoClasses="No upcoming classes scheduled",
        dashboardNoMeetingLink="No meeting link available. Please check back shortly.",
        dashboardClassEnded="This class has already ended.",
        dashboardAlreadyRegistered="You are already registered for this class.",
        dashboardCreateCustomTarget="Create Your Daily Target",
        dashboardBuildPlan="Build your plan, one task at a time.",
        dashboardWhatNext="What's your next task?",
    ),
    co = CourseStrings(
        coursesTitle="Courses", coursesEnrollNow="Enroll Now", coursesEnrolled="Enrolled",
        coursesContinue="Continue", coursesCompleted="Completed", coursesLessons="Lessons",
        coursesProgress="Progress", coursesFree="Free", coursesPaid="Paid",
        coursesContent="📋 Course Content", coursesNoCurriculum="No curriculum available yet.",
        coursesRate="Rate this Course", coursesRateSubtitle="Your feedback helps thousands of students",
        coursesSubmitReview="Submit Review", coursesThankYou="Thank you! Your review has been submitted.",
        coursesReviews="⭐ Student Reviews", coursesNoReviews="No reviews yet",
        coursesBeFirst="Be the first to share your experience!", coursesVerified="Verified",
        lessonCompleted="Lesson Completed", lessonMarkComplete="Mark as Complete", lessonSaving="Saving…",
        lessonNoPdf="No PDF attached to this lesson", lessonNoVideo="No video attached to this lesson",
        lessonLoading="Loading lesson…", lessonLiveClass="Live Class", lessonJoinLive="Join Live Class",
        lessonLiveNotReady="Live class link will be available when the session starts.",
        lessonChapterQuiz="Chapter Quiz", lessonCantLoadPdf="Couldn't load PDF",
        lessonOpenBrowser="Open in Browser", lessonLoadingPdf="Loading PDF…",
        courseByAuthor="By", courseYourProgress="Your Progress",
        courseCertEarned="Certificate Earned! 🎉", courseCertTitle="Certificate of Completion",
        courseCertTap="Tap to view & download your certificate",
        courseCertComplete="Complete all lessons to earn your certificate",
        courseShareCert="Share Certificate", courseShareCertBtn="Share 🎓",
        courseCourseCompleted="Course Completed", courseContinueLearning="Continue Learning",
        courseStartLearning="Start Learning", courseEnrollFree="Enroll Free",
        courseAbout="About this Course", courseFreeTrial="Free Trial Lesson",
        courseWatch="Watch →", courseSyllabus="Course Syllabus",
        courseShowLess="Show less ↑", courseShowAll="Show all ↓",
        courseStudentReviews="Student Reviews", coursePriceSummary="Price Summary",
        courseCoinsDiscount="Coins Discount", courseCouponApplied="✅ Coupon applied!",
        courseTotalPayable="Total Payable", courseUseCoins="Use Coins for Discount",
        courseYouHaveCoins="You have", courseCouponCode="Coupon Code",
        courseCouponSaved="Saved ₹", courseSecurePayment="🔒 Secure payment · Instant access after payment",
        courseGrabNow="Grab now →", courseTapStar="Tap a star to rate",
        courseHindiEnglish="Hindi + English",
        courseInProgress="In Progress", courseNoCoursesYet="No courses here yet",
        courseExploreStore="Explore Store tab to enroll",
        courseMyCertificates="🏆 My Certificates",
        courseOverallProgress="Overall Progress", courseTapRead="Tap Read to open full document",
        courseEnrollTitle="Enroll in",
    ),
    qz = QuizStrings(
        quizTitle="Quizzes", quizDaily="Daily Quiz", quizTopic="Topic Quiz", quizMock="Mock Test",
        quizStart="Start Quiz", quizQuestions="Questions", quizTimeLimit="Time Limit",
        quizSubmit="Submit Quiz", quizResult="Quiz Result", quizScore="Score",
        quizCorrect="Correct", quizWrong="Wrong",
        quizCoinsEarned="Coins Earned",
        quizReview="Review Answers", quizNext="Next →", quizFinish="Finish",
        quizSkip="Skip →", quizPrevious="← Previous", quizHint="Explanation",
        quizAttempted="✓ Attempted", quizDuration="Duration", quizRules="Quiz Rules",
        quizNoQuestions="This quiz has no questions yet. Please contact admin.",
        quizRetake="Retake Quiz 🔄", quizStartNow="Start Quiz 🚀",
        quizSubmitting="Submitting quiz…", quizQuitTitle="Quit Quiz?",
        quizQuitBody="Your progress will be lost if you leave now.",
        quizQuit="Quit", quizKeepGoing="Keep Going",
        quizExcellent="Excellent!", quizGoodJob="Good Job!", quizKeepPracticing="Keep Practicing!",
        quizSelectCorrect="Select the correct option:",
        quizSkipped="⏭ Skipped", quizAnswered="📝 Answered",
        quizNoAvailable="No quizzes available", quizCheckLater="Check back later!",
        quizReviewAll="View Analysis", quizRetakeQuiz="Retake Quiz",
        quizBackToQuizzes="Back to Quizzes", quizNavTitle="Question Navigator",
        quizCorrectAns="✅ Correct", quizWrongAns="❌ Wrong",
        quizSaveNext="Save & Next →", quizSubmitTest="Submit Test",
        quizStartTest="Start Test", quizTestOverview="Test Overview",
        quizCanResume="You can resume if you exit accidentally",
        quizAvgScore="Avg Score", quizFullMock="Full Mock Tests",
        quizMiniTest="Subject-wise Mini Tests", quizPrevYear="Previous Year Papers",
        quizAllTests="All Tests", quizFeatured="⭐ Featured",
        quizPracticeReal="Practice like the real exam",
        quizNoTestsCategory="No tests in this category",
        quizNoTestsYet="No mock tests available yet",
        quizTestsComingSoon="New tests are added regularly. Check back soon!",
        quizCoinsEarned2="Coins Earned!", quizNoCoins="No Coins This Time",
        quizAddedWallet="Added to your wallet",
        quizAlreadyEarned="Already earned coins for this quiz",
        quizSubjectAnalysis="Subject-wise Analysis",
        quizViewLeaderboard="View Leaderboard", quizRetryTest="Retry Test",
        quizBackToTests="Back to Tests", quizCreateCustom="Create Custom Test",
        quizSelectSubjects="Select Subjects", quizNegativeMarking="Negative Marking",
        quizCustomTest="Custom Test", quizStartCustom="Start Custom Test 🚀",
        quizSubmitTestTitle="Submit Test?",
        quizLoadingQ="Loading questions…", quizSettingUp="Setting up your test",
        quizPreparingQ="Preparing questions…", quizYourRank="Your Rank",
    ),
    ct = ContentStrings(
        caTitle="Current Affairs", caBookmark="Bookmark", caBookmarked="Bookmarked",
        caShare="Share Article", caMcqPractice="Practice MCQs", caImportant="Important",
        caSubtitle="Stay updated, score higher", caSearchHint="Search topics, keywords...",
        caNoSaved="No saved articles yet", caNoArticles="No articles found",
        caBookmarkHint="Bookmark articles to see them here",
        caTryFilter="Try a different search or filter", caReadMore="Read more ↓",
        caLoadingQ="Loading questions…", caNoMcqs="No MCQs available",
        caGoBack="Go Back", caBackToArticle="Back to Article",
        caNoMcqsBody="Admin hasn't added questions for this article yet.",
        caNxtQuestion="Next Question →", caSeeResults="See Results",
        caExcellent="Excellent!", caWellDone="Well Done!", caGoodEffort="Good Effort!", caKeepPracticing="Keep Practicing!",
        materialsTitle="Student Hub", materialsSubtitle="Notes, PDFs, PYQs & Books",
        materialsDownload="Download", materialsDownloaded="Downloaded",
        materialsUpload="Upload", materialsView="View", materialsSearchHint="Search notes, papers, books...",
        materialsPopular="🔥 Popular", materialsNewest="🆕 Newest", materialsTopRated="⭐ Top Rated",
        materialsPinned="📌 Pinned by Admin", materialsTrending="🔥 Trending This Week",
        materialsRecent="🆕 Recently Added", materialsAll="📂 All Resources",
        materialsFilterSubject="Filter by Subject", materialsFilterLanguage="Language",
        materialsFilters="Filters", materialsExplore="🔍 Explore",
        materialsMyUploads="📤 My Uploads",
    ),
    ct2 = ContentStrings2(
        materialsSave="Save material", materialsRemoveSaved="Remove from saved",
        materialsTapOpen="Tap to open", materialsNoPreview="No preview yet",
        materialsUploadTitle="Upload Your Notes",
        materialsUploading="Uploading…", materialsSubmitReview="Submit for Review",
        materialsReviewNote="📋 All uploads are reviewed before publishing",
        materialsNoSaved="No saved materials", materialsNoResources="No resources found",
        materialsBookmarkHint="Bookmark materials to see them here",
        materialsNoUploads="No uploads yet",
        materialsUploadHint="Tap Upload to share study materials with others",
        materialsPublished="✅ Published", materialsRejected="❌ Rejected",
        materialsNoDownloads="No downloads yet",
        materialsDownloadHint="Download study materials to access them here",
        materialsPremiumContent="Premium Content", materialsChargeCoins="Charge coins for full access",
        materialsDownloadedDone="Downloaded ✓", materialsUnlockPro="Unlock with Pro",
        materialsDownloadFree="Download Free",
        caLoadingQuestions="Loading questions…", caSavedDone="Saved ✓",
        caGateWaiting="Unlocking your article…", caGateReady="Your article is ready!",
        caGateSubtitle="Just a few seconds while we load this for you",
        caGateContinue="Continue to Article", caGateWaitButton="Wait {s}s",
        caGateReadyMcq="Your quiz is ready!", caGateContinueMcq="Start MCQ",
    ),
    jr = JobRoomStrings(
        jobsTitle="Job Vacancies", jobsApplyNow="Apply Now", jobsLastDate="Last Date",
        jobsPosts="Posts", jobsSave="Save", jobsSaved="Saved",
        jobsOpeningsCountLabel="Job Posts",
        jobsSearchHint="Search jobs, departments, location…",
        jobsNoJobs="No jobs found", jobsTryFilter="Try a different search or category",
        jobsFeatured="⭐ Featured", jobsAllJobs="All Jobs",
        roomsTitle="Study Rooms", roomsChoose="Choose Your Room",
        roomsChooseHint="Tap your room to start studying. Locked rooms unlock as you progress.",
        roomsJoin="Join Room", roomsLeave="Leave Room",
        roomsStartSession="Start Session", roomsEndSession="End Session",
        roomsStudying="Studying Now", roomsOnline="Online",
        roomsTierBronze="Bronze", roomsTierSilver="Starter", roomsTierGold="Serious", roomsTierDiamond="Achiever",
        roomsEarnCoins="Earn coins by studying!", roomsSessionActive="Session Active",
        roomsLive="Live", roomsLocked="Locked", roomsStudied="Studied",
        roomsYourRoom="Your Room", roomsClaimPromotion="Claim Promotion!",
        roomsMetRequirements="You've met all requirements!",
        roomsClaimNow="Claim Now 🚀", roomsLater="Later", roomsRequirements="Requirements",
        roomsKeepStudying="Got it, I'll keep studying!",
        roomsReadyForNext="Ready for promotion!", roomsPromotedMidnight="All requirements met! You'll be promoted at midnight.",
        roomsGroupStudy="Group Study", roomsTapToStart="Tap your room to start",
        jobsNew="🆕 New", jobsApplicationClosed="Application Closed",
        jobsNoLink="No Link Available", jobsApplyOfficialSite="Apply / Official Site",
        jobsAboutJob="About this Job", jobsEligibility="Eligibility & Details",
        jobsImportantDates="Important Dates", jobsApplyStart="Apply Start",
        jobsAlerts="Job Alerts", jobsAlertsSubtitle="Get notified when new vacancies are posted",
        roomsSearchHint="Search rooms, topics...",
        roomsFeatured="⭐ Featured Rooms", roomsAllRooms="All Rooms",
        roomsLeaderboard="Today's Leaderboard", roomsLeaderboardResets="Resets midnight",
        roomsFocusSession="🎯 Focus Session",
        roomsFocusTip1="Focus for 25 min, then take a 5 min break",
        roomsFocusTip2="After 4 sessions, take a longer 15 min break",
        roomsFocusTip3="Keep phone away during focus session",
        roomsFocusTip4="Note down distracting thoughts, revisit in break",
        roomsCreate="Create Study Room", roomsTodayTopic="Today's Focus Topic",
        roomsMaxMembers="Max Members", roomsRequiresCode="Requires join code to enter",
        roomsCreateBtn="Create Room 🚀", roomsEnterCode="Enter the room code shared by the admin",
        roomsJoinBtn="Join Room", roomsTodayStudyTogether="Study together, rank higher",
    ),
    fp = FocusProfileStrings(
        focusEndSessionTitle="End Session?", focusKeepStudying="Keep Studying",
        focusJoining="Joining room…", focusSettingUp="Setting up your session",
        focusAfk="AFK Detected", focusAfkIdle="Idle time not counted toward coins",
        focusImBack="I'm Back", focusSessionComplete="Session Complete!",
        focusGreatWork="Great work!", focusYou="You", focusThisSession="this session",
        focusTotal="Total", focusBackToRooms="Back to Rooms",
        focusTapMessage="Tap to message", focusFirstHere="You're the first one here!",
        focusOthersJoin="Others will appear when they join",
        focusSaving="Saving session…", focusActive="Active",
        profileTitle="Profile", profileEdit="Edit Profile", profileStreak="Day Streak",
        profileCoins="Total Coins", profileAchievements="Achievements", profileBadges="Badges",
        profileStudyTime="Study Time", profileShare="Share Profile", profileRank="Rank & Progress",
        profileSubjectProgress="Subject Progress", profileWeeklyStreak="Weekly Streak",
        profileHeatmap="Study Heatmap", profileLast28="Last 28 days",
        profileLess="Less", profileMore="More",
        profileDayStreak="day streak — keep it up!", profileShowLess="Show Less", profileStudy="Study",
        profileCoinWallet="Coin Wallet", profileViewAll="View all",
        profileCurrentBalance="Current Balance", profileHowToEarn="How to earn:",
        profileEarnQuiz="Daily quiz +5", profileEarnStreak="Streak bonus +15",
        profileEarnReferral="Referral +50",
        profileNoTransactions="No transactions yet — complete a quiz to earn coins!",
        profileMyCourses="My Courses",
        recallKeepGoing="Keep going", recallSwipeRate="Swipe card to rate",
        recallGotIt="Got it! →", recallMastered="✅ MASTERED",
        recallTapReveal="Tap card to reveal answer", recallRevealAnswer="Reveal Answer",
        recallGotItBtn="✅ Got it!", recallSessionComplete="Session Complete",
        recallBackToSubjects="Back to Subjects",
    ),
    st = SettingsStrings(
        achievementsSubtitle="Track your milestones", achievementsInProgress="🔒 In Progress",
        challengesTitle="Weekly Challenges", challengesThisWeek="This week",
        challengesNone="No challenges this week", challengesCheckBack="Check back soon!",
        challengesGoal="Goal", challengesClaimed="Reward Claimed ✅",
        challengesClaim="🎁 Claim Reward", challengesKeepStudying="Keep studying to complete this challenge!",
        settingsTitle="Settings", settingsAccount="Account", settingsAppearance="Appearance",
        settingsDarkMode="Dark Mode", settingsDarkModeSubtitle="Switch to dark theme",
        settingsLanguage="Language", settingsLanguageSubtitle="English / हिन्दी",
        settingsStudyPrefs="Study Preferences", settingsReminder="Daily Study Reminder",
        settingsReminderSubtitle="Remind me to study every day",
        settingsAutoPlay="Auto-play Videos", settingsAutoPlaySubtitle="Play next video automatically",
        settingsSound="Sound Effects", settingsSoundSubtitle="Play sounds for actions & alerts",
        settingsHaptics="Haptic Feedback", settingsHapticsSubtitle="Vibrate on taps & interactions",
        settingsStorage="Storage & Data", settingsClearCache="Clear Cache",
        settingsClearCacheSubtitle="Free up storage space",
    ),
    st2 = SettingsStrings2(
        settingsOffline="Offline Mode", settingsOfflineSubtitle="Access saved content offline",
        settingsAbout="About", settingsVersion="App Version", settingsVersionSubtitle="BPSCNotes v1.0.0",
        settingsRate="Rate the App", settingsRateSubtitle="Love the app? Leave a review!",
        settingsShare="Share with Friends", settingsShareSubtitle="Invite friends & earn {coins} coins",
        settingsPrivacy="Privacy Policy", settingsPrivacySubtitle="How we handle your data",
        settingsTerms="Terms of Service", settingsTermsSubtitle="Our terms & conditions",
        settingsSupport="Contact Support", settingsSupportSubtitle="Get help from our team",
        settingsLogout="Log Out", settingsLogoutSubtitle="Sign out of your account",
        settingsDeleteAccount="Delete Account", settingsDeleteSubtitle="Permanently delete all data",
        settingsDeleteConfirmTitle="Delete Account?",
        settingsDeleteConfirmBody="This will permanently delete your account, all progress, coins, and study data. This action cannot be undone.",
        settingsDeleteForever="Delete Forever",
        settingsDownloadedContent="Downloaded Content", settingsManageOffline="Manage offline files",
        settingsClearing="Clearing…", settingsCacheCleared="✅ Cache cleared successfully",
    ),
    pay = PaymentStrings(
        paymentTitle="Go Pro", paymentActivated="Subscription Activated! 🎉",
        paymentWelcome="Welcome to BPSCNotes Pro. All premium content is now unlocked.",
        paymentGetPro="Get BPSCNotes Pro", paymentUnlockAll="Unlock all premium content",
        paymentWhatsIncluded="What's included", paymentChoosePlan="Choose your plan",
        paymentSecure="Secure payment via Cashfree · UPI, Cards, Net Banking accepted",
        paymentMonthly="Monthly", paymentQuarterly="Quarterly", paymentAnnual="Annual",
        paymentSubscribe="Subscribe Now", paymentMostPopular="Most Popular", paymentBestValue="Best Value",
        paymentActivate="Activate Pro", paymentSuccess="Subscription Activated! 🎉",
        paymentFailed="Payment Failed. Please try again.",
        paymentCoupon="Coupon Code", paymentEnterCoupon="Enter coupon code",
        paymentApply="Apply", paymentApplied="✓ Applied",
        paymentBreakdown="Price Breakdown", paymentBasePrice="Base price",
        paymentCouponDiscount="Coupon discount", paymentTotal="Total payable",
        paymentCreating="Creating order…", paymentConfirming="Confirming payment…",
        paymentBenefit1="📄 All premium PDFs & study notes",
        paymentBenefit2="🎬 Recorded lectures & video lessons",
        paymentBenefit3="📥 Offline downloads",
        paymentBenefit4="❓ Unlimited mock tests",
        paymentBenefit5="🏆 Leaderboard & rank tracking",
        paymentBenefit6="🤖 AI-powered study assistant",
        paymentBenefit7="🎯 Daily targets & study rooms",
        coursePaymentTitle="Complete Purchase",
        courseUnlocked="Course Unlocked! 🎉", courseUnlockedBody="is now available in My Learning.",
        coursePurchaseBtn="Pay & Enroll →", coursePurchaseComplete="Complete Purchase",
        coursePrice="Course Price", courseLifetime="✅ Lifetime access",
        courseOffline="✅ Offline downloads", courseCertificate="✅ Certificate on completion",
        courseSecure="Secure payment via Cashfree · UPI, Cards, Net Banking",
        courseProcessing="Processing…",
    ),
    m = MiscStrings(
        targetTitle="Daily Targets", targetAdd="Add Target", targetComplete="Mark Complete",
        targetNoTargets="No targets for today. Tap + Add to create your daily plan.",
        targetCompleted="Completed", targetCoinsEarned="Coins earned",
        targetCreate="Create Target", targetCreateSheet="Create Today's Targets",
        targetPlaceholder="e.g. Polity - Fundamental Rights",
        targetStreak="Streak at risk!", targetStreakProtect="Complete at least 1 topic to protect your streak",
        targetAllFilter="All", targetMax="Max 10 targets allowed",
        targetCarried="Carried", targetLoading="Loading targets…",
        targetFailed="Failed to load targets", targetMorning="Morning",
        targetAfternoon="Afternoon", targetNight="Night",
        targetEasy="Easy", targetMedium="Medium", targetHard="Hard",
        walletTitle="Coin Wallet", walletBalance="Balance", walletEarned="Earned",
        walletSpent="Spent", walletHistory="History", walletEarnCoins="Earn Coins",
        walletDailyStreak="Daily Streak", walletCheckIn="Check In Now — Earn Coins",
        walletCheckedIn="Checked in today ✓", walletCheckingIn="Checking in…",
        walletNoTasks="No tasks available", walletNoTransactions="No transactions yet",
        walletInviteFriend="Invite a Friend", walletWatchAd="Scroll up to watch an ad",
        pdfReadFreePages="You've read free pages out of",
        pdfPurchaseUnlock="Purchase to unlock all pages with lifetime access.",
        pdfPageNum="Page", pdfPageLocked="Page is locked",
        pdfPurchaseAccess="Purchase to access all pages",
        loginIAgree="I agree to the ",
        editBio="Bio / About me",
        registerCreateProfile="Create Your Profile",
        registerPersonalize="Just a few details to personalise your experience",
        registerContinue="Continue",
        registerDataSecure="Your data is secure and never shared.",
        registerPersonalDetails="PERSONAL DETAILS",
        examStartPreparing="Start Preparing 🚀",
        examPrimaryTip="First exam you tap becomes your PRIMARY.",
        examPrimaryTapChange="PRIMARY · tap to change",
        examBiharState="Bihar State",
        marketGetFree="Get Free",
        paymentStartLearning="Start Learning →",
        paymentOpenFailed="Failed to open payment screen",
        studyFocusJustNow="Just now",
        notifAllRead="All notifications marked as read ✓",
    ),
    m3 = MiscStrings3(
        notifTitle="Notifications", notifMarkRead="Mark all read", notifNone="No notifications yet",
        notifToday="Today", notifYesterday="Yesterday", notifJustNow="Just now",
        chatRoomChat="Room Chat", chatLive="Live", chatConnecting="Connecting…",
        chatReconnecting="Reconnecting…", chatLoading="Loading messages…",
        chatNoMessages="No messages yet", chatStartConversation="Start the conversation!",
        chatToday="Today", chatMessageHint="Message everyone in this room…",
        chatFailedSend="Failed to send", chatYou="You",
    ),
    m2 = MiscStrings2(
        pomodoroFocus="Focus Time", pomodoroBreak="Short Break", pomodoroLongBreak="Long Break 🎉",
        pomodoroPause="Pause", pomodoroStart="Start",
        pomodoroFocusDone="🎉 Focus session done!", pomodoroFocusDoneBody="Great work! Time for a break.",
        pomodoroBreakOver="⏰ Break over!", pomodoroBreakOverBody="Back to studying. You've got this!",
        pomodoroLongDone="🚀 Long break done!", pomodoroLongDoneBody="Ready for another focus sprint?",
        recallTitle="Active Recall", recallSubtitle="Flashcard study sessions",
        recallOverallMastery="Overall Mastery", recallRetryWeak="Retry Weak Cards",
        recallNoCards="No flashcards available", recallAskAdmin="Ask admin to add flashcards.",
        recallChooseSubject="Choose Subject", recallLoading="Loading flashcards…",
        recallFailed="Failed to load flashcards",
        recallEasy="Easy", recallMedium="Medium", recallHard="Hard",
        pdfUnlock="🔓 Unlock Full PDF", pdfBuyAccess="Buy Full Access",
        pdfMaybeLater="Maybe later", pdfFullAccess="Full access",
        pdfLoadingPdf="Loading PDF…", pdfCantLoad="Couldn't load PDF",
        pdfGoBack="Go Back", pdfNoPages="No pages found",
        marketTitle="Marketplace", marketSubtitle="Buy & sell study notes",
        marketSell="Sell Notes", marketSearchHint="Search courses…",
        marketNoNotes="No notes yet",
        placeholdersBilledMonthly="Billed monthly",
        placeholdersBilledQuarterly="Billed every 3 months",
        placeholdersBilledAnnually="Billed annually",
        placeholdersExclusiveNotes="Exclusive notes, papers & courses",
        placeholdersUnlockAll="Unlock all premium study materials, notes, and videos.",
        placeholdersPrioritySupport="✅ Priority support",
        placeholdersAllPremium="All premium content · No ads · Priority support",
        placeholdersNoPremiumYet="No premium content yet",
        placeholdersCheckBack="Check back soon for exclusive content",
        placeholdersNotesReader="Notes Reader", placeholdersOpenWith="Open with",
    ),
    m4 = MiscStrings4(
        marketBeFirst="Be the first to upload your study notes and earn!",
        marketUpload="Upload Notes", marketFeatured="⭐ Featured",
        topicQuizTitle="Topic Quiz", topicQuizDetails="Quiz Details",
        topicQuizStart="Start Topic Quiz 🚀", topicQuizPerQuestion="Per Question",
        topicQuizCoin="Each correct answer earns 1 coin",
        topicQuizSkip="You can skip questions", topicQuizHint="Use hints for tricky questions",
        topicQuizTimer="30 seconds per question", topicQuizReview="Full review available at the end",
        demotionStudyNow="Study Now 🔥", demotionDismiss="Dismiss",
        promotionCongrats="🎉 Congratulations! 🎉", promotionPromotedTo="You've been promoted to",
        promotionPerks="Your new perks:",
        pipSessionActive="Session Active", pipReturn="Return",
        downloadsTitle="My Downloads", downloadsSubtitle="Offline study files",
        downloadsNone="No downloads yet",
        downloadsNoneHint="Download study materials to access them offline",
        downloadsBrowse="Browse Student Hub",
        filterAll="All", filterPrelims="Prelims", filterMains="Mains", filterSaved="Saved 🔖",
        quizReviewExplanation="Correct answer & explanation revealed after submission.",
        dashboardActiveRecall="Active Recall",
        mockSampleQ1="Which Article abolishes untouchability?",
        mockSampleA="Article 17", mockSampleQ2="The Dandi March was led in which year?",
        materialsOpenPdf="Open full document in PDF viewer",
        materialsUnlockPro2="Unlock with BPSCNotes Pro",
        materialsShareHint="Share notes with 10,000+ BPSC aspirants",
        materialsNotesTitle="Notes Title *",
        materialsAuthorName="Author (your name)",
        materialsContentType="Content Type",
        materialsTags="Tags (comma separated)",
        materialsTagHint="e.g. Constitution, Parliament",
        materialsDownloadDevice="Download to device",
        materialsLifetimeAccess="Lifetime access, no expiry",
        targetSwipeNavigate="Swipe to navigate",
        targetAllDone="You've completed today's targets.\nYour streak is safe!",
        courseCertOfCompletion="Certificate of Completion",
        courseCertDownload="Download",
        courseCertShare="Share",
        courseCertNotAvailable="Certificate not available yet",
        permOpenSettings="Open App Settings",
        permNotNow="Not now",
        recallRelatedMcq="Related MCQ",
        examLoadFailed="Failed to load exams",
        registerNameHint="e.g. Rahul Kumar",
        courseBpscExpert="BPSC Subject Expert",
        lessonViewerTimeout="The viewer timed out or the link is unavailable.",
        notifSettingsHint="We'll notify you about quizzes,\ndaily targets and important updates",
        placeholdersSeeAll2="See all",
        placeholdersPremiumNotes="📄 Premium Notes & Papers",
        placeholdersPremiumCourses="🎓 Premium Courses",
        roomCodeHint="Room Code (e.g. BPSC2026)",
        roomCreate="Create",
        roomTabChat="Chat", roomTabMembers="Members",
        roomTabLeaderboard="Leaderboard", roomTabPomodoro="Pomodoro",
        mockAttempts="Attempts", mockUnlock="Unlock",
        mockCustom="Custom", mockSubjectWise="Subject",
        mockLeaderboard="Leaderboard",
        mockQuitTitle="Quit Mock Test?",
        mockQuitBody="Your answers will be lost. Are you sure you want to quit?",
        mcqQuitTitle="Quit MCQ Quiz?",
    ),
    m5 = MiscStrings5(
        recallReviseAgain="← Revise Again",
        recallAnswer="Answer",
        recallExample="Example",
        recallMastered="Mastered",
        placeholderFileMissing="⚠️ File missing",
        placeholderOnDevice="✓ On device",
        placeholderGetPro="🚀 Get BPSCNotes Pro",
        placeholderPremiumPdfs="✅ All premium PDFs & notes",
        placeholderOfflineDownloads="✅ Offline downloads",
        dashRegistered="Registered",
        dashRegister="Register",
        dashProgress="Progress",
        mockCreateCustom="Create Custom",
        mockInstructions="Instructions",
        mockPercentile="Percentile",
        mockNegativeMarking="-0.33 per wrong answer",
        profileMaxTier="Max tier! 🎉",
        profileLast28="Last 28 days",
        profileBadgeEarned="Badge Earned!",
        profileNotEarned="Not yet earned",
        materialAbout="About",
        materialPriceCoins="🪙 Price (coins)",
        materialUnlockAccess="🔓 Unlock Full Access",
        materialPrice="Price",
        quizImageQ="🖼️ Image Quiz",
        quizAccuracy="Accuracy",
        quizCorrectAnswer="✓ Correct",
        quizScore="Score",
        myLearningNotesTitle="Notes Title",
        myLearningUpiHint="yourname@upi",
        adLoading="Loading ad…",
        adSponsored="Sponsored",
        adGreatSession="🎯 Great session!",
        focusActive="active",
        targetCoins="coins",
        targetCarriedForward="📅 Carried Forward",
        courseWhatYouLearn="📚 What You'll Learn",
        courseAboutInstructor="👨‍🏫 About the Instructor",
        permOpenSettings="Open App Settings",
        registerEmailHint="e.g. rahul@gmail.com",
        examBack="← Back",
        marketOwned="Owned",
        roomsReset="Reset",
        walletCoins="coins",
        lessonInAppBrowser="Opens in secure in-app browser",
        uiTryAgain="Try Again",
        quizDailyScore="Score",
        quizDailyAccuracy="Accuracy",
        closeLabel="Close",
        marketRulesTitle="Sell Your Notes — Marketplace Rules",
        marketRulesSubtitle="Earn coins by sharing your study materials with other students",
        marketRule1="📤 Upload your notes, PDFs, or guides and set your own price (e.g. ₹500 in coins)",
        marketRule2="✅ Our team reviews every submission before it goes live for other students",
        marketRule3="💬 If the price seems too high, we may suggest a fairer price — you can negotiate up to 3 rounds",
        marketRule4="🚀 Once approved, your material goes live and other students can buy it",
        marketRule5="💰 You earn 60% of every sale — we keep 40% to run and maintain the platform",
        marketRulesGotIt="Got it, let's go!",
        marketRulesInfoTooltip="Marketplace rules",
    ),

    m6 = MiscStrings6(
        permStayUpdated="Stay Updated with Notifications",
        permStreakWarn="🔥 Streak protection warnings",
        permStudentsNote="Students who enable notifications are 3× more consistent.",
        permEnableBtn="Enable Notifications",
        adNoLimit="No limit — watch as many as you want!",
        adAdvertisement="Advertisement",
        adSkip="Skip",
        materialOpen="Open",
        materialPreview="Preview",
        materialUnlockPro="🚀 Unlock with BPSCNotes Pro",
        roomCreate="Create",
        roomSubject="Subject",
        uiSomethingWrong="Something went wrong",
        permMaybeLater="Maybe later",
        permBlocked="Notifications Blocked",
        mlShareNotes="Share notes with 10,000+ BPSC aspirants",
        mlContentType="Content Type",
        mlAttachFile="Tap to attach file (PDF / DOC)",
        roomPrivate="Private Room",
        roomJoinPrivate="Join Private Room",
    ),
)

// ── HINDI ──────────────────────────────────────────────────────────────────────
val HindiStrings: AppStrings = mkAppStrings(
    c = CommonStrings(
        ok="ठीक है", yes="हाँ", no="नहीं", cancel="रद्द करें", save="सहेजें",
        close="बंद करें", back="वापस", retry="पुनः प्रयास", loading="लोड हो रहा है…",
        error="त्रुटि", success="सफलता", submit="जमा करें", next="अगला",
        done="हो गया", skip="छोड़ें", search="खोजें", noData="कोई डेटा उपलब्ध नहीं",
        seeAll="सभी देखें", viewAll="सभी देखें", free="मुफ़्त", premium="प्रीमियम",
        coins="सिक्के", minutes="मिनट", hours="घंटा", days="दिन", all="सभी",
        start="शुरू करें", goBack="वापस जाएं", tryAgain="पुनः प्रयास करें", version="v1.0.0",
    ),
    n = NavAuthStrings(
        navDashboard="होम", navMyLearning="मेरी पढ़ाई", navRooms="स्टडी रूम", navProfile="प्रोफ़ाइल",
        splashTagline="स्मार्ट पढ़ें। बेहतर याद करें। ऊंचा रैंक पाएं।",
        langSelectTitle="अपनी भाषा चुनें",
        langSelectSubtitle="आप इसे कभी भी सेटिंग से बदल सकते हैं",
        langSelectContinue="आगे बढ़ें",
        onboardingGetStarted="शुरू करें", onboardingSkip="छोड़ें",
        onboarding1Title="दैनिक लक्ष्य", onboarding1Subtitle="ट्रैक पर रहें",
        onboarding1Body="अपने दैनिक अध्ययन लक्ष्य बनाएं। हम हर कदम पर आपकी प्रगति ट्रैक करते हैं।",
        onboarding2Title="सक्रिय स्मरण", onboarding2Subtitle="ज्यादा याद रखें",
        onboarding2Body="हर विषय में फ्लैशकार्ड और स्पेस्ड रिपिटिशन। कम पढ़ें, ज्यादा याद रखें।",
        onboarding3Title="ग्रुप स्टडी रूम", onboarding3Subtitle="पढ़ते हुए कमाएं",
        onboarding3Body="BPSC अभ्यर्थियों के साथ वर्चुअल स्टडी रूम में जुड़ें और सिक्के कमाएं।",
        loginSubtitle="जारी रखने के लिए साइन इन करें",
        loginEnterMobile="मोबाइल नंबर दर्ज करें", loginSendOtp="OTP भेजें",
        loginMobileHint="मोबाइल नंबर", loginTermsPrivacy="नियम और गोपनीयता नीति",
        otpTitle="नंबर सत्यापित करें", otpSentTo="WhatsApp OTP भेजा गया +91",
        otpResend="WhatsApp OTP फिर भेजें", otpVerify="सत्यापित करें और जारी रखें",
        otpChangeNumber="मोबाइल नंबर बदलें", otpDidntReceive="WhatsApp OTP नहीं मिला?",
    ),
    pe = ProfileEditStrings(
        editPersonalInfo="व्यक्तिगत जानकारी", editFullName="पूरा नाम *",
        editEmail="ईमेल पता", editDistrict="जिला", editExamSettings="परीक्षा सेटिंग",
        editPrepLevel="तैयारी स्तर", editTargetYear="लक्षित वर्ष",
        editSaveChanges="बदलाव सहेजें", editSaving="सहेजा जा रहा है…",
        editMobile="मोबाइल नंबर", editVerified="सत्यापित ✓", editNotVerified="असत्यापित",
        prepBeginner="शुरुआती", prepIntermediate="मध्यम", prepAdvanced="उन्नत",
        examSetupChoose="अपनी परीक्षाएं चुनें",
        examSetupTapHint="एक बार टैप = प्राथमिक · दोबारा टैप = द्वितीयक",
        examSetupSearch="परीक्षा खोजें...", examSetupPrimary="प्राथमिक", examSetupSecondary="द्वितीयक",
        examSetupTargetYear="अपनी योजना देखें", examSetupPlan="आपकी परीक्षा योजना",
        examSetupPersonalize="हम क्या व्यक्तिगत बनाएंगे", examSetupNext="अगला →",
        thisYear="इस साल 🔥", nextYear="अगले साल", longTerm="दीर्घकालिक",
    ),
    db = DashboardStrings(
        dashboardHello="नमस्ते", dashboardGoodMorning="सुप्रभात",
        dashboardGoodAfternoon="नमस्कार", dashboardGoodEvening="शुभ संध्या",
        dashboardDailyTargets="दैनिक लक्ष्य", dashboardAddTarget="+ लक्ष्य जोड़ें",
        dashboardNoTargets="अभी कोई लक्ष्य नहीं। एक जोड़ें!",
        dashboardTodayProgress="आज की प्रगति", dashboardContinueLearning="पढ़ाई जारी रखें",
        dashboardDailyQuiz="दैनिक क्विज़", dashboardCurrentAffairs="समसामयिकी",
        dashboardLatestJobs="नई नौकरियां", dashboardStudyRoom="स्टडी रूम",
        dashboardCoins="सिक्के", dashboardStreak="स्ट्रीक", dashboardAccuracy="सटीकता",
        dashboardStudyTime="पढ़ाई", dashboardTodayQuizzes="आज के क्विज़",
        dashboardTodayFocus="आज का फ़ोकस", dashboardNoQuizzes="आज कोई क्विज़ नहीं। बाद में देखें!",
        dashboardQuizNoQuestions="इस क्विज़ में अभी कोई प्रश्न नहीं है।", dashboardDone="✓ हो गया",
        drawerCourses="कोर्स", drawerStudyMaterials="स्टूडेंट हब",
        drawerCurrentAffairs="समसामयिकी", drawerMockTests="मॉक टेस्ट",
        drawerJobAlerts="नौकरी अलर्ट", drawerStudyRooms="स्टडी रूम",
        drawerCoinWallet="सिक्का वॉलेट", drawerAchievements="उपलब्धियां",
        drawerSettings="सेटिंग", drawerLanguage="भाषा", drawerLogout="लॉग आउट",
        dashboardAspirant="अभ्यर्थी 👋", dashboardNoTargets2="कोई लक्ष्य निर्धारित नहीं",
        dashboardViewAll="सभी देखें", dashboardCreateTarget="कस्टम लक्ष्य बनाएं",
        dashboardWeeklyConsistency="साप्ताहिक निरंतरता",
        dashboardWeeklySubtitle="इस सप्ताह आपकी अध्ययन गतिविधि",
        dashboardNoActivity="अभी कोई गतिविधि डेटा नहीं",
        dashboardStartStudying="प्रगति देखने के लिए पढ़ाई शुरू करें",
        dashboardQuickAccess="त्वरित पहुँच", dashboardRecommended="आपके लिए अनुशंसित",
        dashboardMySchedule="मेरा शेड्यूल", dashboardUpcomingEvents="आगामी कार्यक्रम",
        dashboardNoClasses="कोई आगामी क्लास निर्धारित नहीं",
        dashboardNoMeetingLink="मीटिंग लिंक उपलब्ध नहीं। कृपया बाद में देखें।",
        dashboardClassEnded="यह क्लास पहले ही समाप्त हो चुकी है।",
        dashboardAlreadyRegistered="आप पहले से इस क्लास के लिए पंजीकृत हैं।",
        dashboardCreateCustomTarget="अपना दैनिक लक्ष्य बनाएं",
        dashboardBuildPlan="एक-एक कार्य से अपनी योजना बनाएं।",
        dashboardWhatNext="आपका अगला कार्य क्या है?",
    ),
    co = CourseStrings(
        coursesTitle="कोर्स", coursesEnrollNow="अभी नामांकन करें", coursesEnrolled="नामांकित",
        coursesContinue="जारी रखें", coursesCompleted="पूर्ण", coursesLessons="पाठ",
        coursesProgress="प्रगति", coursesFree="मुफ़्त", coursesPaid="सशुल्क",
        coursesContent="📋 कोर्स सामग्री", coursesNoCurriculum="अभी कोई पाठ्यक्रम उपलब्ध नहीं।",
        coursesRate="इस कोर्स को रेट करें", coursesRateSubtitle="आपकी राय हजारों छात्रों की मदद करती है",
        coursesSubmitReview="समीक्षा जमा करें", coursesThankYou="धन्यवाद! आपकी समीक्षा जमा हो गई।",
        coursesReviews="⭐ छात्र समीक्षाएं", coursesNoReviews="अभी कोई समीक्षा नहीं",
        coursesBeFirst="अपना अनुभव साझा करने वाले पहले बनें!", coursesVerified="सत्यापित",
        lessonCompleted="पाठ पूर्ण", lessonMarkComplete="पूर्ण के रूप में चिह्नित करें",
        lessonSaving="सहेजा जा रहा है…", lessonNoPdf="इस पाठ में PDF नहीं है",
        lessonNoVideo="इस पाठ में वीडियो नहीं है", lessonLoading="पाठ लोड हो रहा है…",
        lessonLiveClass="लाइव क्लास", lessonJoinLive="लाइव क्लास जॉइन करें",
        lessonLiveNotReady="सत्र शुरू होने पर लाइव लिंक उपलब्ध होगा।",
        lessonChapterQuiz="अध्याय क्विज़", lessonCantLoadPdf="PDF लोड नहीं हो सका",
        lessonOpenBrowser="ब्राउज़र में खोलें", lessonLoadingPdf="PDF लोड हो रहा है…",
        courseByAuthor="द्वारा", courseYourProgress="आपकी प्रगति",
        courseCertEarned="प्रमाणपत्र अर्जित! 🎉", courseCertTitle="पूर्णता प्रमाणपत्र",
        courseCertTap="अपना प्रमाणपत्र देखने और डाउनलोड करने के लिए टैप करें",
        courseCertComplete="प्रमाणपत्र अर्जित करने के लिए सभी पाठ पूरे करें",
        courseShareCert="प्रमाणपत्र साझा करें", courseShareCertBtn="साझा करें 🎓",
        courseCourseCompleted="कोर्स पूर्ण", courseContinueLearning="सीखना जारी रखें",
        courseStartLearning="सीखना शुरू करें", courseEnrollFree="मुफ़्त नामांकन",
        courseAbout="इस कोर्स के बारे में", courseFreeTrial="मुफ़्त ट्रायल पाठ",
        courseWatch="देखें →", courseSyllabus="कोर्स पाठ्यक्रम",
        courseShowLess="कम दिखाएं ↑", courseShowAll="सब दिखाएं ↓",
        courseStudentReviews="छात्र समीक्षाएं", coursePriceSummary="मूल्य सारांश",
        courseCoinsDiscount="सिक्का छूट", courseCouponApplied="✅ कूपन लागू!",
        courseTotalPayable="कुल देय", courseUseCoins="छूट के लिए सिक्के उपयोग करें",
        courseYouHaveCoins="आपके पास हैं", courseCouponCode="कूपन कोड",
        courseCouponSaved="बचाया ₹", courseSecurePayment="🔒 सुरक्षित भुगतान · भुगतान के बाद तुरंत पहुँच",
        courseGrabNow="अभी लें →", courseTapStar="रेट करने के लिए स्टार टैप करें",
        courseHindiEnglish="हिंदी + अंग्रेज़ी",
        courseInProgress="प्रगति में", courseNoCoursesYet="अभी यहाँ कोई कोर्स नहीं",
        courseExploreStore="नामांकन के लिए Store टैब देखें",
        courseMyCertificates="🏆 मेरे प्रमाणपत्र",
        courseOverallProgress="समग्र प्रगति", courseTapRead="पूरा दस्तावेज़ खोलने के लिए टैप करें",
        courseEnrollTitle="नामांकन करें",
    ),
    qz = QuizStrings(
        quizTitle="क्विज़", quizDaily="दैनिक क्विज़", quizTopic="विषय क्विज़", quizMock="मॉक टेस्ट",
        quizStart="क्विज़ शुरू करें", quizQuestions="प्रश्न", quizTimeLimit="समय सीमा",
        quizSubmit="क्विज़ जमा करें", quizResult="परिणाम", quizScore="अंक",
        quizCorrect="सही", quizWrong="गलत",
        quizCoinsEarned="अर्जित सिक्के",
        quizReview="उत्तर देखें", quizNext="अगला →", quizFinish="समाप्त",
        quizSkip="छोड़ें →", quizPrevious="← पिछला", quizHint="संकेत",
        quizAttempted="✓ प्रयास किया", quizDuration="समय अवधि", quizRules="क्विज़ नियम",
        quizNoQuestions="इस क्विज़ में अभी कोई प्रश्न नहीं है।",
        quizRetake="क्विज़ फिर दें 🔄", quizStartNow="क्विज़ शुरू करें 🚀",
        quizSubmitting="क्विज़ जमा हो रहा है…", quizQuitTitle="क्विज़ छोड़ें?",
        quizQuitBody="अगर आप अभी चले गए तो प्रगति खो जाएगी।",
        quizQuit="छोड़ें", quizKeepGoing="जारी रखें",
        quizExcellent="शानदार!", quizGoodJob="अच्छा काम!", quizKeepPracticing="अभ्यास जारी रखें!",
        quizSelectCorrect="सही विकल्प चुनें:",
        quizSkipped="⏭ छोड़ा", quizAnswered="📝 उत्तर दिया",
        quizNoAvailable="कोई क्विज़ उपलब्ध नहीं", quizCheckLater="बाद में देखें!",
        quizReviewAll="विश्लेषण देखें", quizRetakeQuiz="क्विज़ दोबारा दें",
        quizBackToQuizzes="क्विज़ पर वापस जाएं", quizNavTitle="प्रश्न नेविगेटर",
        quizCorrectAns="✅ सही", quizWrongAns="❌ गलत",
        quizSaveNext="सहेजें और अगला →", quizSubmitTest="टेस्ट जमा करें",
        quizStartTest="टेस्ट शुरू करें", quizTestOverview="टेस्ट अवलोकन",
        quizCanResume="यदि गलती से बाहर हो जाएं तो फिर से शुरू कर सकते हैं",
        quizAvgScore="औसत अंक", quizFullMock="पूर्ण मॉक टेस्ट",
        quizMiniTest="विषयवार मिनी टेस्ट", quizPrevYear="पिछले वर्ष के प्रश्न",
        quizAllTests="सभी टेस्ट", quizFeatured="⭐ विशेष",
        quizPracticeReal="असली परीक्षा की तरह अभ्यास करें",
        quizNoTestsCategory="इस श्रेणी में कोई टेस्ट नहीं",
        quizNoTestsYet="अभी कोई मॉक टेस्ट उपलब्ध नहीं",
        quizTestsComingSoon="नए टेस्ट नियमित रूप से जोड़े जाते हैं। बाद में देखें!",
        quizCoinsEarned2="सिक्के अर्जित!", quizNoCoins="इस बार कोई सिक्का नहीं",
        quizAddedWallet="आपके वॉलेट में जोड़ा गया",
        quizAlreadyEarned="इस क्विज़ के लिए सिक्के पहले ही अर्जित हो चुके हैं",
        quizSubjectAnalysis="विषयवार विश्लेषण",
        quizViewLeaderboard="लीडरबोर्ड देखें", quizRetryTest="टेस्ट फिर दें",
        quizBackToTests="टेस्ट पर वापस जाएं", quizCreateCustom="कस्टम टेस्ट बनाएं",
        quizSelectSubjects="विषय चुनें", quizNegativeMarking="नेगेटिव मार्किंग",
        quizCustomTest="कस्टम टेस्ट", quizStartCustom="कस्टम टेस्ट शुरू करें 🚀",
        quizSubmitTestTitle="टेस्ट जमा करें?",
        quizLoadingQ="प्रश्न लोड हो रहे हैं…", quizSettingUp="आपका टेस्ट सेट हो रहा है",
        quizPreparingQ="प्रश्न तैयार हो रहे हैं…", quizYourRank="आपकी रैंक",
        awTitle="उत्तर लेखन",
        awSubtitle="दैनिक मुख्य परीक्षा उत्तर अभ्यास",
        awTodayBadge="आज का प्रश्न",
        awStartWriting="लिखना शुरू करें ✍️",
        awQuestionsTab="प्रश्न",
        awMyAnswersTab="मेरे उत्तर",
        awStatusNew="✍️ अभी लिखें",
        awStatusPending="⏳ समीक्षा में",
        awStatusReviewed="✅ जांचा गया",
        awMarks="अंक",
        awWordLimit="शब्द सीमा",
        awWords="शब्द",
        awTips="लेखन सुझाव",
        awYourAnswer="आपका उत्तर",
        awModelAnswer="आदर्श उत्तर",
        awFeedback="परीक्षक की प्रतिक्रिया",
        awScore="अंक",
        awWriteHint="यहाँ अपना उत्तर लिखना शुरू करें…",
        awSubmit="उत्तर जमा करें",
        awSubmitting="जमा हो रहा है…",
        awConfirmTitle="अपना उत्तर जमा करें?",
        awConfirmBody="हर प्रश्न के लिए एक ही प्रयास मिलता है। जमा करने के बाद आदर्श उत्तर खुल जाएगा और मेंटर आपके उत्तर की समीक्षा करेंगे।",
        awOverLimit="सीमा से अधिक",
        awEmpty="अभी कोई प्रश्न नहीं",
        awEmptyBody="नए उत्तर-लेखन प्रश्न नियमित रूप से आते हैं — जल्द वापस देखें!",
        awNoSubmissions="आपने अभी तक कोई उत्तर नहीं लिखा",
        awNoSubmissionsBody="कोई प्रश्न चुनें और अपना पहला मुख्य परीक्षा उत्तर लिखें!",
        awPendingNote="आपका उत्तर परीक्षक के पास है — समीक्षा होते ही आपको सूचना मिलेगी।",
        awDashSubtitle="मुख्य परीक्षा अभ्यास · आदर्श उत्तर · विशेषज्ञ समीक्षा",
        awTypeMode="⌨️ टाइप करें",
        awPhotoMode="📷 फोटो",
        awPhotoHint="अपनी कॉपी में उत्तर लिखें, फिर हर पेज की फोटो क्रम से लें (अधिकतम 5 फोटो)।",
        awTakePhoto="कैमरा",
        awFromGallery="गैलरी",
        awPhotos="फोटो",
        awPeerReview="सहपाठी समीक्षा",
        awPeerReviewSub="अन्य अभ्यर्थियों के उत्तरों की समीक्षा करें और रिव्यू क्रेडिट कमाएं।",
        awReviewNow="अभी समीक्षा करें",
        awReviewsGiven="समीक्षाएं दीं",
        awPendingReviews="समीक्षा हेतु",
        awReviewCredits="रिव्यू क्रेडिट",
        awReviewLockedNoSub="सहपाठी समीक्षा शुरू करने के लिए पहले अपना उत्तर जमा करें।",
        awReviewLockedNotReviewed="जब आपके उत्तर की पहली समीक्षा हो जाएगी, तब आप दूसरों की समीक्षा कर सकेंगे।",
        awHelpFellow="एक साथी अभ्यर्थी की मदद करें। रिव्यू क्रेडिट कमाएं।",
        awReviewBannerTitle="ईमानदार और रचनात्मक समीक्षा दें।",
        awReviewBannerBody="आपकी समीक्षा दूसरे अभ्यर्थी को बेहतर बनने में मदद करेगी।",
        awAnonymous="गुमनाम",
        awStudentAnswer="छात्र का उत्तर",
        awYourReview="आपकी समीक्षा",
        awReviewQ1="1. क्या उत्तर ने प्रश्न की मांग को संबोधित किया?",
        awReviewQ2="2. उत्तर की समग्र रेटिंग",
        awReviewQ3="3. सबसे अधिक सुधार किसमें चाहिए?",
        awReviewQ4="4. उत्तर सुधारने के लिए एक सुझाव",
        awPartly="आंशिक रूप से",
        awSuggestionHint="जैसे: हाल के उदाहरण जोड़ें और निष्कर्ष को मजबूत बनाएं।",
        awSubmitReview="समीक्षा जमा करें और 1 क्रेडिट कमाएं",
        awNoMoreReviews="सब हो गया!",
        awNoMoreReviewsBody="अभी समीक्षा के लिए कोई उत्तर नहीं है — बाद में देखें।",
        awPeerReviewsReceived="आपके उत्तर पर सहपाठी समीक्षाएं",
        awUnderPeerReview="आपका उत्तर जमा हो गया है और सहपाठियों द्वारा समीक्षा हो रही है।",
        awAreaContent="विषय-वस्तु",
        awAreaStructure="संरचना",
        awAreaAnalysis="विश्लेषण",
        awAreaBihar="बिहार दृष्टिकोण",
        awAreaPresentation="प्रस्तुति",
        awAreaConclusion="निष्कर्ष",
        awInsightsTab="इनसाइट्स",
        awInsightsTitle="आपकी गतिविधि",
        awInsightsSub="अपनी प्रगति और योगदान देखें।",
        awAnswersWritten="लिखे गए उत्तर",
        awReviewsReceived="मिली समीक्षाएं",
        awAvgRating="औसत रेटिंग",
        awWritingStreak="लेखन स्ट्रीक",
        awDays="दिन",
        awThisMonth="इस महीने",
        awTotalWords="कुल शब्द",
        awMentorScore="औसत मेंटर स्कोर",
        awGoalTitle="आपका लक्ष्य",
        awGoalBody="इस महीने 10 अच्छे उत्तर लिखें",
        awGoalDone="पूरा हुआ! 🎉",
        awReviewerLevel="समीक्षक स्तर",
        awLevelBeginner="शुरुआती",
        awLevelActive="सक्रिय समीक्षक",
        awLevelAdvanced="उन्नत समीक्षक",
        awLevelExpert="विशेषज्ञ समीक्षक",
        awKeepItUp="जारी रखें!",
        awNoInsights="अपने आंकड़े देखने के लिए पहला उत्तर लिखें!",
        awChars="अक्षर",
        awPyq="PYQ",
        awModelAnswerTomorrow="आदर्श उत्तर कल खुलेगा — तुलना के लिए वापस आएं!",
        awAreaIntro="भूमिका",
        awAreaValueAdd="मूल्य संवर्धन",
        awUpTo3="(अधिकतम 3)",
        awTopWeaknesses="शीर्ष 3 कमजोरियां",
        awTopReviewers="शीर्ष समीक्षक",
        awTopWriters="शीर्ष लेखक",
        awAnswersLower="उत्तर",
        awGiveToGet="सुझाव: समीक्षा दें ताकि आपके उत्तर की समीक्षा जल्दी हो 🤝",
    ),
    ct = ContentStrings(
        caTitle="समसामयिकी", caBookmark="बुकमार्क", caBookmarked="बुकमार्क किया",
        caShare="लेख साझा करें", caMcqPractice="MCQ अभ्यास करें", caImportant="महत्वपूर्ण",
        caSubtitle="अपडेट रहें, ऊंचा स्कोर करें", caSearchHint="विषय, कीवर्ड खोजें...",
        caNoSaved="अभी कोई सहेजा हुआ लेख नहीं", caNoArticles="कोई लेख नहीं मिला",
        caBookmarkHint="बुकमार्क किए लेख यहां दिखेंगे",
        caTryFilter="अलग खोज या फ़िल्टर आज़माएं", caReadMore="और पढ़ें ↓",
        caLoadingQ="प्रश्न लोड हो रहे हैं…", caNoMcqs="कोई MCQ उपलब्ध नहीं",
        caGoBack="वापस जाएं", caBackToArticle="लेख पर वापस जाएं",
        caNoMcqsBody="एडमिन ने अभी इस लेख के लिए प्रश्न नहीं जोड़े हैं।",
        caNxtQuestion="अगला प्रश्न →", caSeeResults="परिणाम देखें",
        caExcellent="शानदार!", caWellDone="बहुत अच्छा!", caGoodEffort="अच्छा प्रयास!", caKeepPracticing="अभ्यास जारी रखें!",
        materialsTitle="स्टूडेंट हब", materialsSubtitle="नोट्स, PDFs, PYQs और पुस्तकें",
        materialsDownload="डाउनलोड", materialsDownloaded="डाउनलोड हो गया",
        materialsUpload="अपलोड करें", materialsView="देखें",
        materialsSearchHint="नोट्स, पेपर, पुस्तकें खोजें...",
        materialsPopular="🔥 लोकप्रिय", materialsNewest="🆕 नया", materialsTopRated="⭐ शीर्ष रेटेड",
        materialsPinned="📌 एडमिन द्वारा पिन", materialsTrending="🔥 इस सप्ताह ट्रेंडिंग",
        materialsRecent="🆕 हाल ही में जोड़ा", materialsAll="📂 सभी संसाधन",
        materialsFilterSubject="विषय से फ़िल्टर", materialsFilterLanguage="भाषा",
        materialsFilters="फ़िल्टर", materialsExplore="🔍 खोजें",
        materialsMyUploads="📤 मेरे अपलोड",
    ),
    ct2 = ContentStrings2(
        materialsSave="सामग्री सहेजें", materialsRemoveSaved="सहेजे से हटाएं",
        materialsTapOpen="खोलने के लिए टैप करें", materialsNoPreview="अभी कोई प्रीव्यू नहीं",
        materialsUploadTitle="अपने नोट्स अपलोड करें",
        materialsUploading="अपलोड हो रहा है…", materialsSubmitReview="समीक्षा के लिए जमा करें",
        materialsReviewNote="📋 सभी अपलोड प्रकाशन से पहले समीक्षा किए जाते हैं",
        materialsNoSaved="कोई सहेजी सामग्री नहीं", materialsNoResources="कोई संसाधन नहीं मिला",
        materialsBookmarkHint="सामग्री बुकमार्क करें और यहाँ देखें",
        materialsNoUploads="अभी कोई अपलोड नहीं",
        materialsUploadHint="अध्ययन सामग्री साझा करने के लिए अपलोड टैप करें",
        materialsPublished="✅ प्रकाशित", materialsRejected="❌ अस्वीकृत",
        materialsNoDownloads="अभी कोई डाउनलोड नहीं",
        materialsDownloadHint="यहाँ देखने के लिए अध्ययन सामग्री डाउनलोड करें",
        materialsPremiumContent="प्रीमियम सामग्री", materialsChargeCoins="पूर्ण पहुँच के लिए सिक्के लें",
        materialsDownloadedDone="डाउनलोड ✓", materialsUnlockPro="Pro से अनलॉक करें",
        materialsDownloadFree="मुफ़्त डाउनलोड",
        caLoadingQuestions="प्रश्न लोड हो रहे हैं…", caSavedDone="सहेजा ✓",
        caGateWaiting="आपका लेख अनलॉक हो रहा है…", caGateReady="आपका लेख तैयार है!",
        caGateSubtitle="इसे लोड करने में बस कुछ सेकंड लगेंगे",
        caGateContinue="लेख पर जाएँ", caGateWaitButton="{s} सेकंड प्रतीक्षा करें",
        caGateReadyMcq="आपका क्विज़ तैयार है!", caGateContinueMcq="MCQ शुरू करें",
    ),
    jr = JobRoomStrings(
        jobsTitle="नौकरी रिक्तियां", jobsApplyNow="अभी आवेदन करें", jobsLastDate="अंतिम तारीख",
        jobsPosts="पद", jobsSave="सहेजें", jobsSaved="सहेजा गया",
        jobsOpeningsCountLabel="जॉब पोस्ट",
        jobsSearchHint="नौकरी, विभाग, स्थान खोजें…",
        jobsNoJobs="कोई नौकरी नहीं मिली", jobsTryFilter="अलग खोज या श्रेणी आज़माएं",
        jobsFeatured="⭐ विशेष", jobsAllJobs="सभी नौकरियां",
        roomsTitle="स्टडी रूम", roomsChoose="अपना रूम चुनें",
        roomsChooseHint="पढ़ाई शुरू करने के लिए अपने रूम पर टैप करें।",
        roomsJoin="रूम जॉइन करें", roomsLeave="रूम छोड़ें",
        roomsStartSession="सत्र शुरू करें", roomsEndSession="सत्र समाप्त करें",
        roomsStudying="पढ़ाई हो रही है", roomsOnline="ऑनलाइन",
        roomsTierBronze="कांस्य", roomsTierSilver="स्टार्टर", roomsTierGold="सीरियस", roomsTierDiamond="अचीवर",
        roomsEarnCoins="पढ़ाई करके सिक्के कमाएं!", roomsSessionActive="सत्र सक्रिय",
        roomsLive="लाइव", roomsLocked="लॉक्ड", roomsStudied="पढ़ाई की",
        roomsYourRoom="आपका रूम", roomsClaimPromotion="पदोन्नति प्राप्त करें!",
        roomsMetRequirements="आपने सभी आवश्यकताएं पूरी कर ली हैं!",
        roomsClaimNow="अभी प्राप्त करें 🚀", roomsLater="बाद में", roomsRequirements="आवश्यकताएं",
        roomsKeepStudying="ठीक है, मैं पढ़ाई जारी रखूंगा!",
        roomsReadyForNext="पदोन्नति के लिए तैयार!", roomsPromotedMidnight="सभी आवश्यकताएं पूरी! आधी रात को पदोन्नति होगी।",
        roomsGroupStudy="ग्रुप स्टडी", roomsTapToStart="शुरू करने के लिए रूम टैप करें",
        jobsNew="🆕 नया", jobsApplicationClosed="आवेदन बंद",
        jobsNoLink="लिंक उपलब्ध नहीं", jobsApplyOfficialSite="आवेदन / आधिकारिक साइट",
        jobsAboutJob="इस नौकरी के बारे में", jobsEligibility="पात्रता और विवरण",
        jobsImportantDates="महत्वपूर्ण तिथियाँ", jobsApplyStart="आवेदन शुरू",
        jobsAlerts="नौकरी अलर्ट", jobsAlertsSubtitle="नई रिक्तियाँ पोस्ट होने पर सूचना पाएं",
        roomsSearchHint="रूम, विषय खोजें...",
        roomsFeatured="⭐ विशेष रूम", roomsAllRooms="सभी रूम",
        roomsLeaderboard="आज का लीडरबोर्ड", roomsLeaderboardResets="आधी रात रीसेट",
        roomsFocusSession="🎯 फ़ोकस सत्र",
        roomsFocusTip1="25 मिनट फ़ोकस करें, फिर 5 मिनट ब्रेक लें",
        roomsFocusTip2="4 सत्रों के बाद 15 मिनट का लंबा ब्रेक लें",
        roomsFocusTip3="फ़ोकस सत्र के दौरान फ़ोन दूर रखें",
        roomsFocusTip4="विचलित करने वाले विचार लिखें, ब्रेक में देखें",
        roomsCreate="स्टडी रूम बनाएं", roomsTodayTopic="आज का फ़ोकस विषय",
        roomsMaxMembers="अधिकतम सदस्य", roomsRequiresCode="प्रवेश के लिए कोड चाहिए",
        roomsCreateBtn="रूम बनाएं 🚀", roomsEnterCode="एडमिन द्वारा साझा रूम कोड दर्ज करें",
        roomsJoinBtn="रूम जॉइन करें", roomsTodayStudyTogether="साथ पढ़ें, ऊंचा रैंक पाएं",
    ),
    fp = FocusProfileStrings(
        focusEndSessionTitle="सत्र समाप्त करें?", focusKeepStudying="पढ़ाई जारी रखें",
        focusJoining="रूम जॉइन हो रहा है…", focusSettingUp="सत्र सेट हो रहा है",
        focusAfk="AFK पता चला", focusAfkIdle="निष्क्रिय समय सिक्कों में नहीं गिना जाएगा",
        focusImBack="मैं वापस हूं", focusSessionComplete="सत्र पूर्ण!",
        focusGreatWork="शानदार काम!", focusYou="आप", focusThisSession="इस सत्र में",
        focusTotal="कुल", focusBackToRooms="रूम पर वापस जाएं",
        focusTapMessage="संदेश के लिए टैप करें",
        focusFirstHere="आप यहाँ पहले हैं!", focusOthersJoin="जब अन्य जुड़ेंगे तो यहाँ दिखेंगे",
        focusSaving="सत्र सहेजा जा रहा है…", focusActive="सक्रिय",
        profileTitle="प्रोफ़ाइल", profileEdit="प्रोफ़ाइल संपादित करें",
        profileStreak="दिन की स्ट्रीक", profileCoins="कुल सिक्के",
        profileAchievements="उपलब्धियां", profileBadges="बैज",
        profileStudyTime="पढ़ाई का समय", profileShare="प्रोफ़ाइल साझा करें",
        profileRank="रैंक और प्रगति", profileSubjectProgress="विषय प्रगति",
        profileWeeklyStreak="साप्ताहिक स्ट्रीक", profileHeatmap="स्टडी हीटमैप",
        profileLast28="पिछले 28 दिन", profileLess="कम", profileMore="ज्यादा",
        profileDayStreak="दिन की स्ट्रीक — जारी रखें!", profileShowLess="कम दिखाएं", profileStudy="पढ़ाई",
        profileCoinWallet="सिक्का वॉलेट", profileViewAll="सभी देखें",
        profileCurrentBalance="वर्तमान शेष", profileHowToEarn="कैसे कमाएं:",
        profileEarnQuiz="दैनिक क्विज़ +5", profileEarnStreak="स्ट्रीक बोनस +15",
        profileEarnReferral="रेफरल +50",
        profileNoTransactions="अभी कोई लेनदेन नहीं — सिक्के कमाने के लिए क्विज़ दें!",
        profileMyCourses="मेरे कोर्स",
        recallKeepGoing="जारी रखें", recallSwipeRate="रेट करने के लिए कार्ड स्वाइप करें",
        recallGotIt="समझ गया! →", recallMastered="✅ महारत हासिल",
        recallTapReveal="उत्तर देखने के लिए कार्ड टैप करें", recallRevealAnswer="उत्तर दिखाएं",
        recallGotItBtn="✅ समझ गया!", recallSessionComplete="सत्र पूर्ण",
        recallBackToSubjects="विषयों पर वापस जाएं",
    ),
    st = SettingsStrings(
        achievementsSubtitle="अपने मील के पत्थर ट्रैक करें", achievementsInProgress="🔒 प्रगति में",
        challengesTitle="साप्ताहिक चुनौतियां", challengesThisWeek="इस सप्ताह",
        challengesNone="इस सप्ताह कोई चुनौती नहीं", challengesCheckBack="जल्द वापस देखें!",
        challengesGoal="लक्ष्य", challengesClaimed="पुरस्कार प्राप्त ✅",
        challengesClaim="🎁 पुरस्कार प्राप्त करें",
        challengesKeepStudying="इस चुनौती को पूरा करने के लिए पढ़ाई जारी रखें!",
        settingsTitle="सेटिंग", settingsAccount="खाता", settingsAppearance="दिखावट",
        settingsDarkMode="डार्क मोड", settingsDarkModeSubtitle="डार्क थीम पर स्विच करें",
        settingsLanguage="भाषा", settingsLanguageSubtitle="English / हिन्दी",
        settingsStudyPrefs="पढ़ाई प्राथमिकताएं",
        settingsReminder="दैनिक पढ़ाई रिमाइंडर", settingsReminderSubtitle="हर दिन पढ़ाई की याद दिलाएं",
        settingsAutoPlay="वीडियो ऑटो-प्ले", settingsAutoPlaySubtitle="अगला वीडियो अपने आप चलाएं",
        settingsSound="ध्वनि प्रभाव", settingsSoundSubtitle="क्रियाओं के लिए ध्वनि",
        settingsHaptics="हैप्टिक फ़ीडबैक", settingsHapticsSubtitle="टैप पर वाइब्रेट",
        settingsStorage="स्टोरेज और डेटा", settingsClearCache="कैश साफ करें",
        settingsClearCacheSubtitle="स्टोरेज खाली करें",
    ),
    st2 = SettingsStrings2(
        settingsOffline="ऑफ़लाइन मोड", settingsOfflineSubtitle="सहेजी सामग्री ऑफ़लाइन देखें",
        settingsAbout="बारे में", settingsVersion="ऐप वर्शन", settingsVersionSubtitle="BPSCNotes v1.0.0",
        settingsRate="ऐप को रेट करें", settingsRateSubtitle="ऐप पसंद है? समीक्षा दें!",
        settingsShare="दोस्तों को शेयर करें", settingsShareSubtitle="दोस्तों को आमंत्रित करें और {coins} सिक्के कमाएं",
        settingsPrivacy="गोपनीयता नीति", settingsPrivacySubtitle="हम आपका डेटा कैसे संभालते हैं",
        settingsTerms="सेवा की शर्तें", settingsTermsSubtitle="हमारी शर्तें",
        settingsSupport="सहायता केंद्र", settingsSupportSubtitle="हमारी टीम से सहायता पाएं",
        settingsLogout="लॉग आउट", settingsLogoutSubtitle="अपने खाते से साइन आउट करें",
        settingsDeleteAccount="खाता हटाएं", settingsDeleteSubtitle="सभी डेटा स्थायी रूप से हटाएं",
        settingsDeleteConfirmTitle="खाता हटाएं?",
        settingsDeleteConfirmBody="यह आपका खाता, सभी प्रगति, सिक्के और डेटा स्थायी रूप से हटा देगा।",
        settingsDeleteForever="हमेशा के लिए हटाएं",
        settingsDownloadedContent="डाउनलोड की गई सामग्री", settingsManageOffline="ऑफ़लाइन फ़ाइलें प्रबंधित करें",
        settingsClearing="साफ हो रहा है…", settingsCacheCleared="✅ कैश सफलतापूर्वक साफ हुआ",
    ),
    pay = PaymentStrings(
        paymentTitle="प्रो बनें", paymentActivated="सब्सक्रिप्शन सक्रिय! 🎉",
        paymentWelcome="BPSCNotes Pro में आपका स्वागत है। सभी प्रीमियम सामग्री अनलॉक है।",
        paymentGetPro="BPSCNotes Pro लें", paymentUnlockAll="सभी प्रीमियम सामग्री अनलॉक करें",
        paymentWhatsIncluded="क्या शामिल है", paymentChoosePlan="अपनी योजना चुनें",
        paymentSecure="Cashfree के माध्यम से सुरक्षित भुगतान · UPI, कार्ड, नेट बैंकिंग",
        paymentMonthly="मासिक", paymentQuarterly="त्रैमासिक", paymentAnnual="वार्षिक",
        paymentSubscribe="अभी सब्सक्राइब करें", paymentMostPopular="सबसे लोकप्रिय",
        paymentBestValue="सर्वश्रेष्ठ मूल्य", paymentActivate="प्रो सक्रिय करें",
        paymentSuccess="सब्सक्रिप्शन सक्रिय! 🎉", paymentFailed="भुगतान विफल। कृपया पुनः प्रयास करें।",
        paymentCoupon="कूपन कोड", paymentEnterCoupon="कूपन कोड दर्ज करें",
        paymentApply="लागू करें", paymentApplied="✓ लागू हुआ",
        paymentBreakdown="मूल्य विवरण", paymentBasePrice="आधार मूल्य",
        paymentCouponDiscount="कूपन छूट", paymentTotal="कुल देय",
        paymentCreating="ऑर्डर बन रहा है…", paymentConfirming="भुगतान की पुष्टि हो रही है…",
        paymentBenefit1="📄 सभी प्रीमियम PDFs और स्टडी नोट्स",
        paymentBenefit2="🎬 रिकॉर्डेड लेक्चर और वीडियो",
        paymentBenefit3="📥 ऑफ़लाइन डाउनलोड",
        paymentBenefit4="❓ असीमित मॉक टेस्ट",
        paymentBenefit5="🏆 लीडरबोर्ड और रैंक ट्रैकिंग",
        paymentBenefit6="🤖 AI-पावर्ड स्टडी असिस्टेंट",
        paymentBenefit7="🎯 दैनिक लक्ष्य और स्टडी रूम",
        coursePaymentTitle="खरीदारी पूरी करें",
        courseUnlocked="कोर्स अनलॉक! 🎉", courseUnlockedBody="अब मेरी पढ़ाई में उपलब्ध है।",
        coursePurchaseBtn="भुगतान करें और नामांकन करें →",
        coursePurchaseComplete="खरीदारी पूरी करें", coursePrice="कोर्स की कीमत",
        courseLifetime="✅ आजीवन पहुँच", courseOffline="✅ ऑफ़लाइन डाउनलोड",
        courseCertificate="✅ पूरा होने पर प्रमाणपत्र",
        courseSecure="Cashfree के माध्यम से सुरक्षित भुगतान", courseProcessing="प्रक्रिया हो रही है…",
    ),
    m = MiscStrings(
        targetTitle="दैनिक लक्ष्य", targetAdd="लक्ष्य जोड़ें", targetComplete="पूर्ण चिह्नित करें",
        targetNoTargets="आज के लिए कोई लक्ष्य नहीं। + जोड़ें पर टैप करें।",
        targetCompleted="पूर्ण", targetCoinsEarned="सिक्के अर्जित",
        targetCreate="लक्ष्य बनाएं", targetCreateSheet="आज के लक्ष्य बनाएं",
        targetPlaceholder="जैसे: राज्यव्यवस्था - मूल अधिकार",
        targetStreak="स्ट्रीक खतरे में!", targetStreakProtect="स्ट्रीक बचाने के लिए कम से कम 1 विषय पूरा करें",
        targetAllFilter="सभी", targetMax="अधिकतम 10 लक्ष्य",
        targetCarried="आगे बढ़ाया", targetLoading="लक्ष्य लोड हो रहे हैं…",
        targetFailed="लक्ष्य लोड करने में विफल",
        targetMorning="सुबह", targetAfternoon="दोपहर", targetNight="रात",
        targetEasy="आसान", targetMedium="मध्यम", targetHard="कठिन",
        walletTitle="सिक्का वॉलेट", walletBalance="शेष", walletEarned="अर्जित",
        walletSpent="खर्च", walletHistory="इतिहास", walletEarnCoins="सिक्के कमाएं",
        walletDailyStreak="दैनिक स्ट्रीक", walletCheckIn="चेक इन करें — सिक्के कमाएं",
        walletCheckedIn="आज चेक इन हो गया ✓", walletCheckingIn="चेक इन हो रहा है…",
        walletNoTasks="कोई कार्य उपलब्ध नहीं", walletNoTransactions="अभी कोई लेनदेन नहीं",
        walletInviteFriend="दोस्त को आमंत्रित करें", walletWatchAd="सिक्के कमाने के लिए ऊपर स्क्रॉल करें",
        pdfReadFreePages="आपने पढ़े पृष्ठ कुल में से",
        pdfPurchaseUnlock="आजीवन पहुँच के साथ सभी पृष्ठ अनलॉक करने के लिए खरीदें।",
        pdfPageNum="पृष्ठ", pdfPageLocked="पृष्ठ लॉक है",
        pdfPurchaseAccess="सभी पृष्ठ देखने के लिए खरीदें",
        loginIAgree="मैं सहमत हूं ",
        editBio="बायो / मेरे बारे में",
        registerCreateProfile="अपनी प्रोफ़ाइल बनाएं",
        registerPersonalize="आपका अनुभव व्यक्तिगत बनाने के लिए कुछ विवरण",
        registerContinue="जारी रखें",
        registerDataSecure="आपका डेटा सुरक्षित है और कभी साझा नहीं किया जाता।",
        registerPersonalDetails="व्यक्तिगत विवरण",
        examStartPreparing="तैयारी शुरू करें 🚀",
        examPrimaryTip="पहले टैप की गई परीक्षा प्राथमिक बनती है।",
        examPrimaryTapChange="प्राथमिक · बदलने के लिए टैप करें",
        examBiharState="बिहार राज्य",
        marketGetFree="मुफ़्त में लें",
        paymentStartLearning="पढ़ाई शुरू करें →",
        paymentOpenFailed="भुगतान स्क्रीन खोलने में विफल",
        studyFocusJustNow="अभी",
        notifAllRead="सभी सूचनाएं पढ़ी चिह्नित ✓",
    ),
    m3 = MiscStrings3(
        notifTitle="सूचनाएं", notifMarkRead="सभी पढ़ा चिह्नित करें",
        notifNone="अभी कोई सूचना नहीं",
        notifToday="आज", notifYesterday="कल", notifJustNow="अभी",
        chatRoomChat="रूम चैट", chatLive="लाइव", chatConnecting="कनेक्ट हो रहा है…",
        chatReconnecting="फिर से कनेक्ट हो रहा है…", chatLoading="संदेश लोड हो रहे हैं…",
        chatNoMessages="अभी कोई संदेश नहीं", chatStartConversation="बातचीत शुरू करें!",
        chatToday="आज", chatMessageHint="सभी को संदेश भेजें…",
        chatFailedSend="भेजने में विफल", chatYou="आप",
    ),
    m2 = MiscStrings2(
        pomodoroFocus="फ़ोकस समय", pomodoroBreak="छोटा ब्रेक", pomodoroLongBreak="लंबा ब्रेक 🎉",
        pomodoroPause="रोकें", pomodoroStart="शुरू करें",
        pomodoroFocusDone="🎉 फ़ोकस सत्र पूर्ण!", pomodoroFocusDoneBody="शानदार काम! ब्रेक का समय।",
        pomodoroBreakOver="⏰ ब्रेक खत्म!", pomodoroBreakOverBody="वापस पढ़ाई पर। आप कर सकते हैं!",
        pomodoroLongDone="🚀 लंबा ब्रेक खत्म!", pomodoroLongDoneBody="एक और फ़ोकस स्प्रिंट के लिए तैयार?",
        recallTitle="सक्रिय स्मरण", recallSubtitle="फ्लैशकार्ड अध्ययन सत्र",
        recallOverallMastery="समग्र महारत", recallRetryWeak="कमज़ोर कार्ड दोबारा करें",
        recallNoCards="कोई फ्लैशकार्ड उपलब्ध नहीं", recallAskAdmin="एडमिन से फ्लैशकार्ड जोड़ने को कहें।",
        recallChooseSubject="विषय चुनें", recallLoading="फ्लैशकार्ड लोड हो रहे हैं…",
        recallFailed="फ्लैशकार्ड लोड करने में विफल",
        recallEasy="आसान", recallMedium="मध्यम", recallHard="कठिन",
        pdfUnlock="🔓 पूरा PDF अनलॉक करें", pdfBuyAccess="पूर्ण पहुँच खरीदें",
        pdfMaybeLater="बाद में", pdfFullAccess="पूर्ण पहुँच",
        pdfLoadingPdf="PDF लोड हो रहा है…", pdfCantLoad="PDF लोड नहीं हो सका",
        pdfGoBack="वापस जाएं", pdfNoPages="कोई पृष्ठ नहीं मिला",
        marketTitle="मार्केटप्लेस", marketSubtitle="स्टडी नोट्स खरीदें और बेचें",
        marketSell="नोट्स बेचें", marketSearchHint="नोट्स, विषय खोजें…",
        marketNoNotes="अभी कोई नोट्स नहीं",
        placeholdersBilledMonthly="मासिक बिल",
        placeholdersBilledQuarterly="हर 3 महीने बिल",
        placeholdersBilledAnnually="वार्षिक बिल",
        placeholdersExclusiveNotes="एक्सक्लूसिव नोट्स, पेपर और कोर्स",
        placeholdersUnlockAll="सभी प्रीमियम अध्ययन सामग्री, नोट्स और वीडियो अनलॉक करें।",
        placeholdersPrioritySupport="✅ प्राथमिकता सहायता",
        placeholdersAllPremium="सभी प्रीमियम · कोई विज्ञापन नहीं · प्राथमिकता सहायता",
        placeholdersNoPremiumYet="अभी कोई प्रीमियम सामग्री नहीं",
        placeholdersCheckBack="एक्सक्लूसिव सामग्री के लिए जल्द वापस देखें",
        placeholdersNotesReader="नोट्स रीडर", placeholdersOpenWith="इससे खोलें",
    ),
    m4 = MiscStrings4(
        marketBeFirst="अपने स्टडी नोट्स अपलोड करें और कमाएं!",
        marketUpload="नोट्स अपलोड करें", marketFeatured="⭐ विशेष",
        topicQuizTitle="विषय क्विज़", topicQuizDetails="क्विज़ विवरण",
        topicQuizStart="विषय क्विज़ शुरू करें 🚀", topicQuizPerQuestion="प्रति प्रश्न",
        topicQuizCoin="हर सही उत्तर पर 1 सिक्का मिलेगा",
        topicQuizSkip="प्रश्न छोड़ सकते हैं", topicQuizHint="कठिन प्रश्नों पर संकेत उपयोग करें",
        topicQuizTimer="प्रति प्रश्न 30 सेकंड", topicQuizReview="अंत में पूरी समीक्षा उपलब्ध",
        demotionStudyNow="अभी पढ़ें 🔥", demotionDismiss="खारिज करें",
        promotionCongrats="🎉 बधाई! 🎉", promotionPromotedTo="आपको पदोन्नत किया गया",
        promotionPerks="आपके नए लाभ:",
        pipSessionActive="सत्र सक्रिय", pipReturn="वापस जाएं",
        downloadsTitle="मेरे डाउनलोड", downloadsSubtitle="ऑफ़लाइन अध्ययन फ़ाइलें",
        downloadsNone="अभी कोई डाउनलोड नहीं",
        downloadsNoneHint="ऑफ़लाइन पहुँच के लिए अध्ययन सामग्री डाउनलोड करें",
        downloadsBrowse="अध्ययन सामग्री देखें",
        filterAll="सभी", filterPrelims="प्रारंभिक", filterMains="मुख्य", filterSaved="सहेजे 🔖",
        quizReviewExplanation="जमा करने के बाद सही उत्तर और व्याख्या दिखाई जाएगी।",
        dashboardActiveRecall="सक्रिय स्मरण",
        mockSampleQ1="कौन सा अनुच्छेद अस्पृश्यता समाप्त करता है?",
        mockSampleA="अनुच्छेद 17", mockSampleQ2="दांडी मार्च किस वर्ष नेतृत्व किया गया था?",
        materialsOpenPdf="PDF व्यूअर में पूरा दस्तावेज़ खोलें",
        materialsUnlockPro2="BPSCNotes Pro से अनलॉक करें",
        materialsShareHint="10,000+ BPSC अभ्यर्थियों के साथ नोट्स शेयर करें",
        materialsNotesTitle="नोट्स का शीर्षक *",
        materialsAuthorName="लेखक (आपका नाम)",
        materialsContentType="सामग्री का प्रकार",
        materialsTags="टैग (कॉमा से अलग करें)",
        materialsTagHint="जैसे संविधान, संसद",
        materialsDownloadDevice="डिवाइस पर डाउनलोड करें",
        materialsLifetimeAccess="आजीवन पहुँच, कोई समाप्ति नहीं",
        targetSwipeNavigate="नेविगेट करने के लिए स्वाइप करें",
        targetAllDone="आपने आज के सभी लक्ष्य पूरे कर लिए।\nआपका स्ट्रीक सुरक्षित है!",
        courseCertOfCompletion="पूर्णता का प्रमाणपत्र",
        courseCertDownload="डाउनलोड",
        courseCertShare="शेयर",
        courseCertNotAvailable="प्रमाणपत्र अभी उपलब्ध नहीं है",
        permOpenSettings="ऐप सेटिंग खोलें",
        permNotNow="अभी नहीं",
        recallRelatedMcq="संबंधित MCQ",
        examLoadFailed="परीक्षाएँ लोड करने में विफल",
        registerNameHint="जैसे राहुल कुमार",
        courseBpscExpert="BPSC विषय विशेषज्ञ",
        lessonViewerTimeout="व्यूअर टाइमआउट हो गया या लिंक उपलब्ध नहीं है।",
        notifSettingsHint="हम आपको क्विज़, दैनिक लक्ष्यों और महत्वपूर्ण अपडेट के बारे में सूचित करेंगे",
        placeholdersSeeAll2="सभी देखें",
        placeholdersPremiumNotes="📄 प्रीमियम नोट्स और पेपर",
        placeholdersPremiumCourses="🎓 प्रीमियम कोर्स",
        roomCodeHint="रूम कोड (जैसे BPSC2026)",
        roomCreate="बनाएं",
        roomTabChat="चैट", roomTabMembers="सदस्य",
        roomTabLeaderboard="लीडरबोर्ड", roomTabPomodoro="पोमोडोरो",
        mockAttempts="प्रयास", mockUnlock="अनलॉक",
        mockCustom="कस्टम", mockSubjectWise="विषय",
        mockLeaderboard="लीडरबोर्ड",
        mockQuitTitle="मॉक टेस्ट छोड़ें?",
        mockQuitBody="आपके उत्तर खो जाएंगे। क्या आप वाकई छोड़ना चाहते हैं?",
        mcqQuitTitle="MCQ क्विज़ छोड़ें?",
    ),
    m5 = MiscStrings5(
        recallReviseAgain="← फिर से संशोधन करें",
        recallAnswer="उत्तर",
        recallExample="उदाहरण",
        recallMastered="माहिर हो गए",
        placeholderFileMissing="⚠️ फ़ाइल गायब है",
        placeholderOnDevice="✓ डिवाइस पर है",
        placeholderGetPro="🚀 BPSCNotes Pro लें",
        placeholderPremiumPdfs="✅ सभी प्रीमियम PDF और नोट्स",
        placeholderOfflineDownloads="✅ ऑफलाइन डाउनलोड",
        dashRegistered="पंजीकृत",
        dashRegister="पंजीकरण करें",
        dashProgress="प्रगति",
        mockCreateCustom="कस्टम बनाएं",
        mockInstructions="निर्देश",
        mockPercentile="पर्सेंटाइल",
        mockNegativeMarking="प्रत्येक गलत उत्तर पर -0.33",
        profileMaxTier="अधिकतम टियर! 🎉",
        profileLast28="पिछले 28 दिन",
        profileBadgeEarned="बैज अर्जित!",
        profileNotEarned="अभी तक अर्जित नहीं",
        materialAbout="परिचय",
        materialPriceCoins="🪙 मूल्य (सिक्के)",
        materialUnlockAccess="🔓 पूर्ण पहुँच अनलॉक करें",
        materialPrice="मूल्य",
        quizImageQ="🖼️ चित्र प्रश्न",
        quizAccuracy="सटीकता",
        quizCorrectAnswer="✓ सही उत्तर",
        quizScore="स्कोर",
        myLearningNotesTitle="नोट्स शीर्षक",
        myLearningUpiHint="yourname@upi",
        adLoading="विज्ञापन लोड हो रहा है…",
        adSponsored="प्रायोजित",
        adGreatSession="🎯 बेहतरीन सत्र!",
        focusActive="सक्रिय",
        targetCoins="सिक्के",
        targetCarriedForward="📅 आगे बढ़ाया गया",
        courseWhatYouLearn="📚 आप क्या सीखेंगे",
        courseAboutInstructor="👨‍🏫 प्रशिक्षक के बारे में",
        permOpenSettings="ऐप सेटिंग खोलें",
        registerEmailHint="जैसे rahul@gmail.com",
        examBack="← वापस",
        marketOwned="खरीदा हुआ",
        roomsReset="रीसेट",
        walletCoins="सिक्के",
        lessonInAppBrowser="सुरक्षित इन-ऐप ब्राउज़र में खुलता है",
        uiTryAgain="पुनः प्रयास करें",
        quizDailyScore="स्कोर",
        quizDailyAccuracy="सटीकता",
        closeLabel="बंद करें",
        marketRulesTitle="अपने नोट्स बेचें — मार्केटप्लेस नियम",
        marketRulesSubtitle="अपनी स्टडी मैटेरियल अन्य छात्रों के साथ शेयर करके कॉइन कमाएं",
        marketRule1="📤 अपने नोट्स, PDF या गाइड अपलोड करें और अपनी कीमत खुद तय करें (जैसे ₹500 कॉइन में)",
        marketRule2="✅ लाइव होने से पहले हमारी टीम हर सबमिशन की जांच करती है",
        marketRule3="💬 अगर कीमत ज्यादा लगे, तो हम उचित कीमत सुझा सकते हैं — आप अधिकतम 3 राउंड तक बातचीत कर सकते हैं",
        marketRule4="🚀 अप्रूव होने के बाद आपकी मैटेरियल लाइव हो जाती है और अन्य छात्र उसे खरीद सकते हैं",
        marketRule5="💰 हर बिक्री पर आपको 60% मिलता है — बाकी 40% प्लेटफ़ॉर्म चलाने के लिए रखा जाता है",
        marketRulesGotIt="समझ गया, चलिए शुरू करें!",
        marketRulesInfoTooltip="मार्केटप्लेस नियम",
    ),
    m6 = MiscStrings6(
        permStayUpdated="सूचनाओं के साथ अपडेट रहें",
        permStreakWarn="🔥 स्ट्रीक सुरक्षा चेतावनियाँ",
        permStudentsNote="सूचनाएं चालू करने वाले छात्र 3× अधिक नियमित होते हैं।",
        permEnableBtn="सूचनाएं सक्षम करें",
        adNoLimit="कोई सीमा नहीं — जितना चाहें देखें!",
        adAdvertisement="विज्ञापन",
        adSkip="छोड़ें",
        materialOpen="खोलें",
        materialPreview="पूर्वावलोकन",
        materialUnlockPro="🚀 BPSCNotes Pro से अनलॉक करें",
        roomCreate="बनाएं",
        roomSubject="विषय",
        uiSomethingWrong="कुछ गलत हो गया",
        permMaybeLater="अभी नहीं",
        permBlocked="सूचनाएं अवरुद्ध हैं",
        mlShareNotes="10,000+ BPSC छात्रों के साथ नोट्स साझा करें",
        mlContentType="सामग्री प्रकार",
        mlAttachFile="फ़ाइल संलग्न करें (PDF / DOC)",
        roomPrivate="प्राइवेट रूम",
        roomJoinPrivate="प्राइवेट रूम जॉइन करें",
    ),
)