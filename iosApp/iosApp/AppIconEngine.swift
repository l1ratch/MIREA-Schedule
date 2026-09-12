import UIKit
import Shared

final class AppIconEngine: AppIconManagerIconEngine {
    func applyIcon(name: String) {
        // "default" -> nil = вернуть первичную иконку из Assets.xcassets.
        // Литерал, а не AppIconManager.ICON_DEFAULT: const val не экспортируется
        // в Swift (inline на стороне Kotlin), значение зафиксировано в common-коде.
        let iconName: String? = name == "default" ? nil : name
        // setAlternateIconName с тем же значением не вызывает системный диалог —
        // iOS сам хранит выбор между запусками, применяем только при смене.
        UIApplication.shared.setAlternateIconName(iconName, completionHandler: nil)
    }
}
