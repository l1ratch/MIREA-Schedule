import UIKit
import UserNotifications
import Shared

// Оба платформенных движка живут в одном файле: один Swift-файл в фазе
// Sources (NotificationsEngine.swift) детерминированно выпадал из плана
// сборки Xcode при корректном pbxproj — здесь компиляция гарантирована.
final class AppIconEngine: AppIconManagerIconEngine {
    func applyIcon(name: String) {
        // "default" -> nil = вернуть первичную иконку из asset catalog.
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

/// iOS-движок локальных напоминаний о занятиях (UNUserNotificationCenter).
/// Разрешение запрашивается только в момент включения тумблера в настройках.
final class NotificationsEngine: NotificationsManagerNotificationEngine {
    func requestAuthorization() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    func schedule(id: String, title: String, body: String, dateEpochMillis: Int64) {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        let date = Date(timeIntervalSince1970: TimeInterval(dateEpochMillis) / 1000)
        let components = Calendar.current.dateComponents(
            [.year, .month, .day, .hour, .minute], from: date)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        UNUserNotificationCenter.current().add(
            UNNotificationRequest(identifier: id, content: content, trigger: trigger))
    }

    func cancelAll() {
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
    }
}
