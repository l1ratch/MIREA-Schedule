package com.jetbrains.kmpapp.screens.other

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import com.jetbrains.kmpapp.data.api.VkAvatarResolver
import com.jetbrains.kmpapp.screens.components.PlatformBackHandler
import org.koin.compose.koinInject

data class StudentResource(
    val title: String,
    val description: String,
    val url: String,
    val icon: ImageVector,
    val accentColor: Color
)

val STUDENT_RESOURCES = listOf(
    StudentResource(
        title = "Личный кабинет студента",
        description = "Доступ к оценкам, приказам, договорам и электронным сервисам университета.",
        url = "https://lk.mirea.ru/",
        icon = Icons.Default.AccountCircle,
        accentColor = Color(0xFF007AFF)
    ),
    StudentResource(
        title = "СДО МИРЭА",
        description = "Система дистанционного обучения: курсы, тесты, лекционные материалы и задания.",
        url = "https://online-edu.mirea.ru/",
        icon = Icons.Default.School,
        accentColor = Color(0xFF5856D6)
    ),
    StudentResource(
        title = "Пульс МИРЭА",
        description = "Сервис для отметок посещаемости на парах, контроля успеваемости и баллов БРС.",
        url = "https://pulse.mirea.ru/",
        icon = Icons.Default.MonitorHeart,
        accentColor = Color(0xFFFF2D55)
    ),
    StudentResource(
        title = "Облако студента",
        description = "Корпоративное облачное хранилище Nextcloud для учебных файлов и совместной работы.",
        url = "https://cloud.mirea.ru/",
        icon = Icons.Default.Cloud,
        accentColor = Color(0xFF00C7BE)
    ),
    StudentResource(
        title = "Справочник студента",
        description = "База знаний и инструкций: контакты отделений, регламенты и ответы на вопросы.",
        url = "https://student.mirea.ru/help/ ",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        accentColor = Color(0xFFFF9500)
    ),
    StudentResource(
        title = "Предложение идей университету",
        description = "Платформа студенческих инициатив для предложений по улучшению вуза.",
        url = "https://vote.mirea.ru/",
        icon = Icons.Default.Lightbulb,
        accentColor = Color(0xFFFFCC00)
    )
)

@Composable
fun ResourcesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val vkAvatarResolver: VkAvatarResolver = koinInject()
    val folderStack = remember { mutableStateListOf<ResourceFolder>() }

    val handleBack: () -> Unit = {
        if (folderStack.isNotEmpty()) folderStack.removeAt(folderStack.lastIndex) else onBack()
    }
    PlatformBackHandler(onBack = handleBack)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = handleBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад"
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = folderStack.lastOrNull()?.title ?: "Ресурсы университета",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        modifier = modifier
            .fillMaxSize()
    ) { innerPadding ->
        if (folderStack.isEmpty()) {
            RootResourcesContent(
                innerPadding = innerPadding,
                uriHandler = uriHandler,
                openFolder = { folderStack.add(it) }
            )
        } else {
            FolderResourcesContent(
                folder = folderStack.last(),
                innerPadding = innerPadding,
                uriHandler = uriHandler,
                vkAvatarResolver = vkAvatarResolver,
                openFolder = { folderStack.add(it) }
            )
        }
    }
}

@Composable
private fun RootResourcesContent(
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    uriHandler: androidx.compose.ui.platform.LocalUriHandler,
    openFolder: (ResourceFolder) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Официальные цифровые сервисы РТУ МИРЭА, необходимые для учебы и взаимодействия с университетом.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        STUDENT_RESOURCES.forEach { res ->
            StudentResourceCard(resource = res, uriHandler = uriHandler)
        }

        Spacer(modifier = Modifier.height(4.dp))

        FolderCard(folder = OTHER_RESOURCES_FOLDER, onClick = openFolder)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun FolderResourcesContent(
    folder: ResourceFolder,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    uriHandler: androidx.compose.ui.platform.LocalUriHandler,
    vkAvatarResolver: VkAvatarResolver,
    openFolder: (ResourceFolder) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (folder.children.isEmpty()) {
            Text(
                text = "Раздел ещё наполняется",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        } else {
            folder.children.forEach { entry ->
                when (entry) {
                    is ResourceFolder -> FolderCard(folder = entry, onClick = openFolder)
                    is ResourceLink -> ResourceLinkCard(
                        link = entry,
                        uriHandler = uriHandler,
                        vkAvatarResolver = vkAvatarResolver
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun StudentResourceCard(
    resource: StudentResource,
    uriHandler: androidx.compose.ui.platform.LocalUriHandler
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(resource.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resource.icon,
                        contentDescription = null,
                        tint = resource.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resource.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = resource.url.removePrefix("https://").removeSuffix("/"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = resource.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { uriHandler.openUri(resource.url) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Перейти к сервису",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Папка: только название и иконка, без ссылки и описания. */
@Composable
private fun FolderCard(
    folder: ResourceFolder,
    onClick: (ResourceFolder) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(folder) }
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(folder.accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = folder.icon,
                    contentDescription = null,
                    tint = folder.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = folder.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Ссылка внутри папки: оформлена как карточки раздела «Ресурсы университета». */
@Composable
private fun ResourceLinkCard(
    link: ResourceLink,
    uriHandler: androidx.compose.ui.platform.LocalUriHandler,
    vkAvatarResolver: VkAvatarResolver
) {
    val hasLink = link.url != null
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinkAvatar(link = link, resolver = vkAvatarResolver)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (hasLink) {
                        Text(
                            text = link.url.orEmpty().removePrefix("https://").removeSuffix("/"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = link.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { link.url?.let { uriHandler.openUri(it) } },
                enabled = hasLink,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = if (hasLink) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (hasLink) Icons.AutoMirrored.Filled.OpenInNew else Icons.Filled.HourglassEmpty,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasLink) "Перейти к сервису" else "Скоро",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Аватарка ссылки: если есть сеть и известен screen_name ВК — тянем реальное фото, иначе символ-эмблема. */
@Composable
private fun LinkAvatar(
    link: ResourceLink,
    resolver: VkAvatarResolver
) {
    var avatarUrl by remember(link.vkScreen, link.url) { mutableStateOf<String?>(null) }
    LaunchedEffect(link.vkScreen, link.url) {
        val screen = link.vkScreen
        avatarUrl = if (screen != null && link.url != null) resolver.resolveAvatarUrl(screen) else null
    }

    if (avatarUrl != null) {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp)),
            loading = { EmblemPlaceholder(emblem = link.emblem, accentColor = link.accentColor) },
            error = { EmblemPlaceholder(emblem = link.emblem, accentColor = link.accentColor) }
        )
    } else {
        EmblemPlaceholder(emblem = link.emblem, accentColor = link.accentColor)
    }
}

@Composable
private fun EmblemPlaceholder(
    emblem: String?,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(accentColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        val symbol = resourceEmblemEmoji(emblem)
        if (symbol != null) {
            Text(
                text = symbol,
                fontSize = 22.sp
            )
        } else {
            Icon(
                imageVector = Icons.Filled.HourglassEmpty,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}