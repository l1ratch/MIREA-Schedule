import UIKit
import Shared

final class AppIconEngine: AppIconManagerIconEngine {
    func applyIcon(name: String) {
        // "default" -> nil = вернуть первичную иконку из Assets.xcassets.
        // Литерал, а не AppIconManager.ICON_DEFAULT: const val не экспортируется
        // в Swift (inline на стороне Kotlin), значение зафиксировано в common-коде.
        let iconName: String? = name == "default" ? nil : name
        // Ошибки (например, PNG с альфа-каналом iOS отвергает) логируем,
        // чтобы сбой смены иконки не был немым.
        UIApplication.shared.setAlternateIconName(iconName) { error in
            if let error = error {
                print("AppIconEngine: setAlternateIconName failed: \(error.localizedDescription)")
            }
        }
    }
}
