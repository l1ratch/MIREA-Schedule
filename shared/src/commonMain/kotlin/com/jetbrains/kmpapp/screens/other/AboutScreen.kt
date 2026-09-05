package com.jetbrains.kmpapp.screens.other

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.jetbrains.kmpapp.data.model.AppVersion
import com.jetbrains.kmpapp.data.model.GitHubContributor
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import com.jetbrains.kmpapp.screens.components.swipeToDismissBack
import kmp_app_template.shared.generated.resources.Res
import kmp_app_template.shared.generated.resources.app_icon
import org.jetbrains.compose.resources.painterResource

@Composable
fun AboutScreen(
    viewModel: OtherViewModel,
    onBack: () -> Unit,
    onOpenDebugMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformBackHandler(onBack = onBack)
    val uriHandler = LocalUriHandler.current

    val contributors by viewModel.contributors.collectAsState()
    val isLoadingContributors by viewModel.isLoadingContributors.collectAsState()

    // Easter Egg: 8 taps on version text to unlock debug menu
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapMark by remember { mutableStateOf<kotlin.time.TimeMark?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadContributors()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "О программе",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
            .swipeToDismissBack(requireEdge = true, onBack = onBack)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Real App Icon from Resources
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = "Иконка приложения",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {
                    val mark = lastTapMark
                    if (mark != null && mark.elapsedNow().inWholeMilliseconds < 1500L) {
                        tapCount++
                    } else {
                        tapCount = 1
                    }
                    lastTapMark = kotlin.time.TimeSource.Monotonic.markNow()

                    if (tapCount >= 8) {
                        tapCount = 0
                        onOpenDebugMenu()
                    }
                }
            ) {
                Text(
                    text = "Расписание MIREA",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = AppVersion.DISPLAY_VERSION,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Project Repository Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Проект на GitHub",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Открытый исходный код, документация и релизы приложения.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { uriHandler.openUri(AppVersion.GITHUB_REPO_URL) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppVersion.GITHUB_REPO,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Contributors Card (Replacing tech block)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Участники проекта",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isLoadingContributors) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    contributors.forEachIndexed { index, contributor ->
                        if (index > 0) {
                            androidx.compose.material3.HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }

                        ContributorRow(
                            contributor = contributor,
                            onClick = { uriHandler.openUri(contributor.htmlUrl) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ContributorRow(
    contributor: GitHubContributor,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (contributor.avatarUrl != null) {
                AsyncImage(
                    model = contributor.avatarUrl,
                    contentDescription = contributor.login,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contributor.login.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = contributor.login,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = contributor.role ?: "Контрибьютор (${contributor.contributions} коммитов)",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = "Открыть профиль",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
