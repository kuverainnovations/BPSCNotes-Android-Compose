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
    val editMobile: String, val editVerified: String,
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
)

data class QuizStrings(
    val quizTitle: String, val quizDaily: String, val quizTopic: String, val quizMock: String,
    val quizStart: String, val quizQuestions: String, val quizTimeLimit: String,
    val quizSubmit: String, val quizResult: String, val quizScore: String,
    val quizCorrect: String, val quizWrong: String, val quizPassed: String, val quizFailed: String,
    val quizCoinsEarned: String, val quizReview: String, val quizNext: String, val quizFinish: String,
    val quizSkip: String, val quizPrevious: String, val quizHint: String,
    val quizAttempted: String, val quizDuration: String, val quizRules: String,
    val quizNoQuestions: String, val quizRetake: String, val quizStartNow: String,
    val quizSubmitting: String, val quizQuitTitle: String, val quizQuitBody: String,
    val quizQuit: String, val quizKeepGoing: String,
    val quizExcellent: String, val quizGoodJob: String, val quizKeepPracticing: String,
    val quizSelectCorrect: String, val quizSkipped: String, val quizAnswered: String,
    val quizNoAvailable: String, val quizCheckLater: String,
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
    val materialsExplore: String, val materialsMyUploads: String,
)

data class JobRoomStrings(
    val jobsTitle: String, val jobsApplyNow: String, val jobsLastDate: String,
    val jobsPosts: String, val jobsSave: String, val jobsSaved: String,
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
)

// ── Facade — 12 fields, all call sites keep using str.xxx unchanged ───────────
data class AppStrings(
    val _c: CommonStrings, val _n: NavAuthStrings, val _pe: ProfileEditStrings,
    val _db: DashboardStrings, val _co: CourseStrings, val _qz: QuizStrings,
    val _ct: ContentStrings, val _jr: JobRoomStrings, val _fp: FocusProfileStrings,
    val _st: SettingsStrings, val _st2: SettingsStrings2, val _pay: PaymentStrings,
    val _m: MiscStrings, val _m3: MiscStrings3, val _m2: MiscStrings2, val _m4: MiscStrings4,
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
    val quizSubmit get() = _qz.quizSubmit; val quizResult get() = _qz.quizResult; val quizScore get() = _qz.quizScore
    val quizCorrect get() = _qz.quizCorrect; val quizWrong get() = _qz.quizWrong
    val quizPassed get() = _qz.quizPassed; val quizFailed get() = _qz.quizFailed
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
    val materialsExplore get() = _ct.materialsExplore; val materialsMyUploads get() = _ct.materialsMyUploads

    val jobsTitle get() = _jr.jobsTitle; val jobsApplyNow get() = _jr.jobsApplyNow; val jobsLastDate get() = _jr.jobsLastDate
    val jobsPosts get() = _jr.jobsPosts; val jobsSave get() = _jr.jobsSave; val jobsSaved get() = _jr.jobsSaved
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
    val focusSaving get() = _fp.focusSaving; val focusActive get() = _fp.focusActive
    val profileTitle get() = _fp.profileTitle; val profileEdit get() = _fp.profileEdit; val profileStreak get() = _fp.profileStreak
    val profileCoins get() = _fp.profileCoins; val profileAchievements get() = _fp.profileAchievements; val profileBadges get() = _fp.profileBadges
    val profileStudyTime get() = _fp.profileStudyTime; val profileShare get() = _fp.profileShare; val profileRank get() = _fp.profileRank
    val profileSubjectProgress get() = _fp.profileSubjectProgress; val profileWeeklyStreak get() = _fp.profileWeeklyStreak
    val profileHeatmap get() = _fp.profileHeatmap; val profileLast28 get() = _fp.profileLast28
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
}

// ── Helper to build AppStrings from grouped sub-objects ───────────────────────
private fun mkAppStrings(
    c: CommonStrings, n: NavAuthStrings, pe: ProfileEditStrings,
    db: DashboardStrings, co: CourseStrings, qz: QuizStrings,
    ct: ContentStrings, jr: JobRoomStrings, fp: FocusProfileStrings,
    st: SettingsStrings, st2: SettingsStrings2, pay: PaymentStrings,
    m: MiscStrings, m3: MiscStrings3, m2: MiscStrings2, m4: MiscStrings4,
) = AppStrings(_c=c,_n=n,_pe=pe,_db=db,_co=co,_qz=qz,_ct=ct,_jr=jr,_fp=fp,
    _st=st,_st2=st2,_pay=pay,_m=m,_m3=m3,_m2=m2,_m4=m4)

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
        navDashboard="Dashboard", navMyLearning="My Learning", navRooms="Study Rooms", navProfile="Profile",
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
        otpTitle="Verify Your Number", otpSentTo="OTP sent to +91",
        otpResend="Resend OTP", otpVerify="Verify & Continue",
        otpChangeNumber="Change Mobile Number", otpDidntReceive="Didn't receive OTP?",
    ),
    pe = ProfileEditStrings(
        editPersonalInfo="Personal Info", editFullName="Full Name *", editEmail="Email Address",
        editDistrict="District", editExamSettings="Exam Settings", editPrepLevel="Preparation Level",
        editTargetYear="Target Year", editSaveChanges="Save Changes", editSaving="Saving…",
        editMobile="Mobile Number", editVerified="Verified ✓",
        prepBeginner="Beginner", prepIntermediate="Intermediate", prepAdvanced="Advanced",
        examSetupChoose="Choose Your Exams",
        examSetupTapHint="Tap once to set primary · Tap again to add secondary",
        examSetupSearch="Search exam...", examSetupPrimary="PRIMARY", examSetupSecondary="SECONDARY",
        examSetupTargetYear="Target Exam Year", examSetupPlan="Your Exam Plan",
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
        drawerCourses="Courses", drawerStudyMaterials="Study Materials",
        drawerCurrentAffairs="Current Affairs", drawerMockTests="Mock Tests",
        drawerJobAlerts="Job Alerts", drawerStudyRooms="Study Rooms",
        drawerCoinWallet="Coin Wallet", drawerAchievements="Achievements",
        drawerSettings="Settings", drawerLanguage="Language", drawerLogout="Logout",
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
    ),
    qz = QuizStrings(
        quizTitle="Quizzes", quizDaily="Daily Quiz", quizTopic="Topic Quiz", quizMock="Mock Test",
        quizStart="Start Quiz", quizQuestions="Questions", quizTimeLimit="Time Limit",
        quizSubmit="Submit Quiz", quizResult="Quiz Result", quizScore="Score",
        quizCorrect="Correct", quizWrong="Wrong", quizPassed="Passed! 🎉",
        quizFailed="Better luck next time!", quizCoinsEarned="Coins Earned",
        quizReview="Review Answers", quizNext="Next →", quizFinish="Finish",
        quizSkip="Skip →", quizPrevious="← Previous", quizHint="Hint",
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
        materialsTitle="Study Materials", materialsSubtitle="Notes, PDFs, PYQs & Books",
        materialsDownload="Download", materialsDownloaded="Downloaded",
        materialsUpload="Upload", materialsView="View", materialsSearchHint="Search notes, papers, books...",
        materialsPopular="🔥 Popular", materialsNewest="🆕 Newest", materialsTopRated="⭐ Top Rated",
        materialsPinned="📌 Pinned by Admin", materialsTrending="🔥 Trending This Week",
        materialsRecent="🆕 Recently Added", materialsAll="📂 All Resources",
        materialsFilterSubject="Filter by Subject", materialsExplore="🔍 Explore",
        materialsMyUploads="📤 My Uploads",
    ),
    jr = JobRoomStrings(
        jobsTitle="Job Vacancies", jobsApplyNow="Apply Now", jobsLastDate="Last Date",
        jobsPosts="Posts", jobsSave="Save", jobsSaved="Saved",
        jobsSearchHint="Search jobs, departments, location…",
        jobsNoJobs="No jobs found", jobsTryFilter="Try a different search or category",
        jobsFeatured="⭐ Featured", jobsAllJobs="All Jobs",
        roomsTitle="Study Rooms", roomsChoose="Choose Your Room",
        roomsChooseHint="Tap your room to start studying. Locked rooms unlock as you progress.",
        roomsJoin="Join Room", roomsLeave="Leave Room",
        roomsStartSession="Start Session", roomsEndSession="End Session",
        roomsStudying="Studying Now", roomsOnline="Online",
        roomsTierBronze="Bronze", roomsTierSilver="Silver", roomsTierGold="Gold", roomsTierDiamond="Diamond",
        roomsEarnCoins="Earn coins by studying!", roomsSessionActive="Session Active",
        roomsLive="Live", roomsLocked="Locked", roomsStudied="Studied",
        roomsYourRoom="Your Room", roomsClaimPromotion="Claim Promotion!",
        roomsMetRequirements="You've met all requirements!",
        roomsClaimNow="Claim Now 🚀", roomsLater="Later", roomsRequirements="Requirements",
        roomsKeepStudying="Got it, I'll keep studying!",
        roomsReadyForNext="Ready for promotion!", roomsPromotedMidnight="All requirements met! You'll be promoted at midnight.",
        roomsGroupStudy="Group Study", roomsTapToStart="Tap your room to start",
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
        settingsShare="Share with Friends", settingsShareSubtitle="Invite friends & earn 75 coins",
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
        paymentSecure="Secure payment via Razorpay · UPI, Cards, Net Banking accepted",
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
        courseSecure="Secure payment via Razorpay · UPI, Cards, Net Banking",
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
        marketSell="Sell Notes", marketSearchHint="Search notes, subjects…",
        marketNoNotes="No notes yet",
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
        downloadsBrowse="Browse Study Materials",
        filterAll="All", filterPrelims="Prelims", filterMains="Mains", filterSaved="Saved 🔖",
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
        otpTitle="नंबर सत्यापित करें", otpSentTo="OTP भेजा गया +91",
        otpResend="OTP फिर भेजें", otpVerify="सत्यापित करें और जारी रखें",
        otpChangeNumber="मोबाइल नंबर बदलें", otpDidntReceive="OTP नहीं मिला?",
    ),
    pe = ProfileEditStrings(
        editPersonalInfo="व्यक्तिगत जानकारी", editFullName="पूरा नाम *",
        editEmail="ईमेल पता", editDistrict="जिला", editExamSettings="परीक्षा सेटिंग",
        editPrepLevel="तैयारी स्तर", editTargetYear="लक्षित वर्ष",
        editSaveChanges="बदलाव सहेजें", editSaving="सहेजा जा रहा है…",
        editMobile="मोबाइल नंबर", editVerified="सत्यापित ✓",
        prepBeginner="शुरुआती", prepIntermediate="मध्यम", prepAdvanced="उन्नत",
        examSetupChoose="अपनी परीक्षाएं चुनें",
        examSetupTapHint="एक बार टैप = प्राथमिक · दोबारा टैप = द्वितीयक",
        examSetupSearch="परीक्षा खोजें...", examSetupPrimary="प्राथमिक", examSetupSecondary="द्वितीयक",
        examSetupTargetYear="लक्ष्य परीक्षा वर्ष", examSetupPlan="आपकी परीक्षा योजना",
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
        drawerCourses="कोर्स", drawerStudyMaterials="अध्ययन सामग्री",
        drawerCurrentAffairs="समसामयिकी", drawerMockTests="मॉक टेस्ट",
        drawerJobAlerts="नौकरी अलर्ट", drawerStudyRooms="स्टडी रूम",
        drawerCoinWallet="सिक्का वॉलेट", drawerAchievements="उपलब्धियां",
        drawerSettings="सेटिंग", drawerLanguage="भाषा", drawerLogout="लॉग आउट",
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
    ),
    qz = QuizStrings(
        quizTitle="क्विज़", quizDaily="दैनिक क्विज़", quizTopic="विषय क्विज़", quizMock="मॉक टेस्ट",
        quizStart="क्विज़ शुरू करें", quizQuestions="प्रश्न", quizTimeLimit="समय सीमा",
        quizSubmit="क्विज़ जमा करें", quizResult="परिणाम", quizScore="अंक",
        quizCorrect="सही", quizWrong="गलत", quizPassed="उत्तीर्ण! 🎉",
        quizFailed="अगली बार बेहतर करें!", quizCoinsEarned="अर्जित सिक्के",
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
        materialsTitle="अध्ययन सामग्री", materialsSubtitle="नोट्स, PDFs, PYQs और पुस्तकें",
        materialsDownload="डाउनलोड", materialsDownloaded="डाउनलोड हो गया",
        materialsUpload="अपलोड करें", materialsView="देखें",
        materialsSearchHint="नोट्स, पेपर, पुस्तकें खोजें...",
        materialsPopular="🔥 लोकप्रिय", materialsNewest="🆕 नया", materialsTopRated="⭐ शीर्ष रेटेड",
        materialsPinned="📌 एडमिन द्वारा पिन", materialsTrending="🔥 इस सप्ताह ट्रेंडिंग",
        materialsRecent="🆕 हाल ही में जोड़ा", materialsAll="📂 सभी संसाधन",
        materialsFilterSubject="विषय से फ़िल्टर", materialsExplore="🔍 खोजें",
        materialsMyUploads="📤 मेरे अपलोड",
    ),
    jr = JobRoomStrings(
        jobsTitle="नौकरी रिक्तियां", jobsApplyNow="अभी आवेदन करें", jobsLastDate="अंतिम तारीख",
        jobsPosts="पद", jobsSave="सहेजें", jobsSaved="सहेजा गया",
        jobsSearchHint="नौकरी, विभाग, स्थान खोजें…",
        jobsNoJobs="कोई नौकरी नहीं मिली", jobsTryFilter="अलग खोज या श्रेणी आज़माएं",
        jobsFeatured="⭐ विशेष", jobsAllJobs="सभी नौकरियां",
        roomsTitle="स्टडी रूम", roomsChoose="अपना रूम चुनें",
        roomsChooseHint="पढ़ाई शुरू करने के लिए अपने रूम पर टैप करें।",
        roomsJoin="रूम जॉइन करें", roomsLeave="रूम छोड़ें",
        roomsStartSession="सत्र शुरू करें", roomsEndSession="सत्र समाप्त करें",
        roomsStudying="पढ़ाई हो रही है", roomsOnline="ऑनलाइन",
        roomsTierBronze="कांस्य", roomsTierSilver="रजत", roomsTierGold="स्वर्ण", roomsTierDiamond="हीरा",
        roomsEarnCoins="पढ़ाई करके सिक्के कमाएं!", roomsSessionActive="सत्र सक्रिय",
        roomsLive="लाइव", roomsLocked="लॉक्ड", roomsStudied="पढ़ाई की",
        roomsYourRoom="आपका रूम", roomsClaimPromotion="पदोन्नति प्राप्त करें!",
        roomsMetRequirements="आपने सभी आवश्यकताएं पूरी कर ली हैं!",
        roomsClaimNow="अभी प्राप्त करें 🚀", roomsLater="बाद में", roomsRequirements="आवश्यकताएं",
        roomsKeepStudying="ठीक है, मैं पढ़ाई जारी रखूंगा!",
        roomsReadyForNext="पदोन्नति के लिए तैयार!", roomsPromotedMidnight="सभी आवश्यकताएं पूरी! आधी रात को पदोन्नति होगी।",
        roomsGroupStudy="ग्रुप स्टडी", roomsTapToStart="शुरू करने के लिए रूम टैप करें",
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
        settingsShare="दोस्तों को शेयर करें", settingsShareSubtitle="दोस्तों को आमंत्रित करें और 75 सिक्के कमाएं",
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
        paymentSecure="Razorpay के माध्यम से सुरक्षित भुगतान · UPI, कार्ड, नेट बैंकिंग",
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
        courseSecure="Razorpay के माध्यम से सुरक्षित भुगतान", courseProcessing="प्रक्रिया हो रही है…",
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
    ),
)