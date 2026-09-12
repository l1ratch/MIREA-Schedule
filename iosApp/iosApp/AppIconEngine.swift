import UIKit
import Shared

final class AppIconEngine: IconEngine {
    func applyIcon(name: String) {
        // "default" -> nil = вернуть первичную иконку из Assets.xcassets
        let iconName: String? = name == AppIconManager.iconDefault ? nil : name
        // setAlternateIconName с тем же значением не вызывает системный диалог —
        // iOS сам хранит выбор между запусками, применяем только при смене.
        UIApplication.shared.setAlternateIconName(iconName, completionHandler: nil)
    }
}
