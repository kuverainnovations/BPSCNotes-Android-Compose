package com.example.bpscnotes.presentation.auth.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.bpscnotes.core.ui.t.BpscColors
import com.example.bpscnotes.presentation.navigation.Routes.Screen

@Composable
fun OnboardingWizardScreen(
    navController: NavHostController,
    viewModel: OnboardingWizardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isDone) {
        if (state.isDone) {
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Step indicator
        WizardHeader(step = state.step, onBack = { viewModel.back() })

        when (state.step) {
            WizardStep.EXAM        -> ExamSelectionStep(state, viewModel)
            WizardStep.TARGET_YEAR -> TargetYearStep(state, viewModel)
            WizardStep.DAILY_GOAL  -> DailyGoalStep(state, viewModel)
        }

        state.error?.let { err ->
            Spacer(Modifier.height(8.dp))
            Text(
                text = err,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun WizardHeader(step: WizardStep, onBack: () -> Unit) {
    val steps = WizardStep.entries
    val current = steps.indexOf(step)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step != WizardStep.EXAM) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = BpscColors.TextPrimary)
            }
        } else {
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.weight(1f))

        // Progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            steps.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(if (index == current) 24.dp else 8.dp, 8.dp)
                        .background(
                            if (index <= current) BpscColors.Primary else BpscColors.TextHint,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun ExamSelectionStep(state: OnboardingWizardUiState, vm: OnboardingWizardViewModel) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = str.obWhichExam,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = BpscColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = str.obSelectPrimary,
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(Modifier.height(16.dp))

        if (state.isLoadingExams) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BpscColors.Primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.exams) { exam ->
                    val selected = state.selectedExam?.name == exam.name
                    ExamCard(
                        name     = exam.fullName.ifBlank { exam.name },
                        emoji    = exam.emoji,
                        selected = selected,
                        onClick  = { vm.selectExam(exam) }
                    )
                }
            }
        }

        Button(
            onClick  = { vm.nextFromExam() },
            enabled  = state.selectedExam != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(54.dp),
            shape    = RoundedCornerShape(16.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
        ) {
            Text(str.langSelectContinue, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ExamCard(name: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) BpscColors.Primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) BpscColors.Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text  = name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = if (selected) BpscColors.Primary else BpscColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun TargetYearStep(state: OnboardingWizardUiState, vm: OnboardingWizardViewModel) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (currentYear..(currentYear + 4)).toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text  = str.obWhenClear,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = BpscColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = str.obChooseYear,
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary
        )
        Spacer(Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            years.forEach { year ->
                val selected = state.targetYear == year
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) BpscColors.Primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) BpscColors.Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { vm.setTargetYear(year) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = year.toString(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (selected) BpscColors.Primary else BpscColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick   = { vm.setTargetYear(null); vm.nextFromTargetYear() },
                modifier  = Modifier.weight(1f).height(54.dp),
                shape     = RoundedCornerShape(16.dp)
            ) {
                Text(str.skip, color = BpscColors.TextSecondary)
            }
            Button(
                onClick   = { vm.nextFromTargetYear() },
                modifier  = Modifier.weight(1f).height(54.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
            ) {
                Text(str.langSelectContinue, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DailyGoalStep(state: OnboardingWizardUiState, vm: OnboardingWizardViewModel) {
    val str = com.example.bpscnotes.core.language.LocalStrings.current
    val options = listOf(
        30  to "30 min/day\nLight prep",
        60  to "1 hr/day\nModerate prep",
        90  to "1.5 hrs/day\nSerious prep",
        120 to "2+ hrs/day\nFull focus"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text  = str.obHowMuchTime,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = BpscColors.TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text  = str.obRemind,
            style = MaterialTheme.typography.bodyMedium,
            color = BpscColors.TextSecondary
        )
        Spacer(Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { (mins, label) ->
                val selected = state.dailyGoalMins == mins
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) BpscColors.Primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) BpscColors.Primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { vm.setDailyGoal(mins) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text  = label,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (selected) BpscColors.Primary else BpscColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = BpscColors.Primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick   = { vm.finish() },
            enabled   = !state.isSaving,
            modifier  = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(54.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = BpscColors.Primary)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(str.obStartPreparing, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
