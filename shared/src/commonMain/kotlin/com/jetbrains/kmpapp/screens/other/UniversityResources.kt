package com.jetbrains.kmpapp.screens.other

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Древо «Ресурсов университета»: папки (только название + иконка) и ссылки
 * (оформляются как карточки официальных сервисов). Новая папка/ссылка =
 * строки ниже; экран навигации строит UI по этому дереву.
 */
sealed interface ResourceEntry {
    val title: String
}

/** Папка: не имеет ни ссылки, ни описания — только название и иконка. */
data class ResourceFolder(
    override val title: String,
    val icon: ImageVector,
    val accentColor: Color,
    val children: List<ResourceEntry>
) : ResourceEntry

/** Ссылка внутри папки: url == null означает «в разработке» (нажатие без действия). */
data class ResourceLink(
    override val title: String,
    val description: String,
    val url: String?,
    val emblem: String?,
    val vkScreen: String?,
    val accentColor: Color
) : ResourceEntry

/** Эмблема-символ для аватарки ссылки; при отсутствии отображения — заглушка. */
val RESOURCE_EMBLEM_EMOJI = mapOf(
    "Слон" to "🐘",
    "Робот" to "🤖",
    "Панда" to "🐼",
    "Динозавр" to "🦖",
    "Лев" to "🦁",
    "Феникс" to "🐦🔥",
    "летучая мышь" to "🦇",
    "Ворон" to "🐦⬛"
)

fun resourceEmblemEmoji(emblem: String?): String? {
    if (emblem == null) return null
    return RESOURCE_EMBLEM_EMOJI[emblem] ?: emblem.take(1).uppercase()
}

private val VK_BLUE = Color(0xFF0077FF)
private val TG_BLUE = Color(0xFF29A9EB)
private val IN_DEV_GRAY = Color(0xFF8E8E93)

private val VK_INSTITUTES = listOf(
    ResourceLink(
        title = "ИКБ",
        description = "Институт кибербезопасности и цифровых технологий",
        url = "https://vk.ru/ikb_sumirea",
        emblem = "Слон",
        vkScreen = "ikb_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ИИИ",
        description = "Институт искусственного интеллекта",
        url = "https://vk.ru/iii_sumirea",
        emblem = "Робот",
        vkScreen = "iii_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ИИТ",
        description = "Институт информационных технологий",
        url = "https://vk.ru/it_sumirea",
        emblem = "Панда",
        vkScreen = "it_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ИТУ",
        description = "Институт технологий управления",
        url = "https://vk.ru/itu_sumirea",
        emblem = "Динозавр",
        vkScreen = "itu_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ИПТИП",
        description = "Институт перспективных технологий и индустриального программирования",
        url = "https://vk.ru/iptip_sumirea",
        emblem = "Лев",
        vkScreen = "iptip_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ИТХТ имени М.В. Ломоносова",
        description = "Институт тонких химических технологий имени М.В. Ломоносова",
        url = "https://vk.ru/itht_sumirea",
        emblem = "Феникс",
        vkScreen = "itht_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ИРИ",
        description = "Институт радиоэлектроники и информатики",
        url = "https://vk.ru/iri_sumirea",
        emblem = "летучая мышь",
        vkScreen = "iri_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "КПК",
        description = "Колледж программирования и кибербезопасности",
        url = "https://vk.ru/college_sumirea",
        emblem = "Ворон",
        vkScreen = "college_sumirea",
        accentColor = VK_BLUE
    ),
    ResourceLink(
        title = "ПИШ",
        description = "Передовые инженерные школы",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "Фрязино",
        description = "Филиал РТУ МИРЭА в г. Фрязино",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "Ставрополь",
        description = "Филиал РТУ МИРЭА в г. Ставрополе",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    )
)

private val TG_INSTITUTES = listOf(
    ResourceLink(
        title = "ИИИ",
        description = "Институт искусственного интеллекта",
        url = "https://t.me/iii_sumirea",
        emblem = "Робот",
        vkScreen = null,
        accentColor = TG_BLUE
    ),
    ResourceLink(
        title = "ИКБ",
        description = "Институт кибербезопасности и цифровых технологий",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "ИИТ",
        description = "Институт информационных технологий",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "ИТУ",
        description = "Институт технологий управления",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "ИПТИП",
        description = "Институт перспективных технологий и индустриального программирования",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "ИТХТ имени М.В. Ломоносова",
        description = "Институт тонких химических технологий имени М.В. Ломоносова",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "ИРИ",
        description = "Институт радиоэлектроники и информатики",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "КПК",
        description = "Колледж программирования и кибербезопасности",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "ПИШ",
        description = "Передовые инженерные школы",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "Фрязино",
        description = "Филиал РТУ МИРЭА в г. Фрязино",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    ),
    ResourceLink(
        title = "Ставрополь",
        description = "Филиал РТУ МИРЭА в г. Ставрополе",
        url = null,
        emblem = null,
        vkScreen = null,
        accentColor = IN_DEV_GRAY
    )
)

private val INSTITUTES_FOLDER = ResourceFolder(
    title = "Институты",
    icon = Icons.Filled.Groups,
    accentColor = Color(0xFF34C759),
    children = listOf(
        ResourceFolder(
            title = "ВК",
            icon = Icons.Filled.Chat,
            accentColor = VK_BLUE,
            children = VK_INSTITUTES
        ),
        ResourceFolder(
            title = "ТГ",
            icon = Icons.Filled.Send,
            accentColor = TG_BLUE,
            children = TG_INSTITUTES
        )
    )
)

private val STUDENT_ORGANIZATIONS_FOLDER = ResourceFolder(
    title = "Студенческие организации",
    icon = Icons.Filled.VolunteerActivism,
    accentColor = Color(0xFFFF9500),
    children = listOf(
        ResourceFolder(
            title = "Студенческий союз РТУ МИРЭА",
            icon = Icons.Filled.VolunteerActivism,
            accentColor = Color(0xFFFF2D55),
            children = emptyList()
        )
    )
)

/** Папка «Другие ресурсы» — добавляется в самый низ раздела «Ресурсы университета». */
val OTHER_RESOURCES_FOLDER = ResourceFolder(
    title = "Другие ресурсы",
    icon = Icons.Filled.Folder,
    accentColor = Color(0xFF007AFF),
    children = listOf(
        ResourceFolder(
            title = "Университет",
            icon = Icons.Filled.AccountBalance,
            accentColor = Color(0xFF5856D6),
            children = emptyList()
        ),
        INSTITUTES_FOLDER,
        STUDENT_ORGANIZATIONS_FOLDER
    )
)