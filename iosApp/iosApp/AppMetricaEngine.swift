import AppMetricaCore
import Shared

/// iOS-движок анонимной аналитики. Класс AppMetrica активируется в iOSApp.init,
/// сюда приходят только события и тумблер из общих настроек.
final class AppMetricaEngine: AnalyticsEngine {
    func logEvent(name: String, params: [String : String]) {
        AppMetrica.reportEvent(name, attributes: params)
    }

    func setEnabled(enabled: Bool) {
        AppMetrica.setDataSendingEnabled(enabled)
    }
}
