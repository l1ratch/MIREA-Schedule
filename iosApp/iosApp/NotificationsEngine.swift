import UserNotifications
import Shared

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
