import SwiftUI
import AppMetricaCore
import Shared

@main
struct iOSApp: App {
    init() {
        KoinKt.doInitKoin()
        if let configuration = AppMetricaConfiguration(apiKey: "fc0cde08-05c5-4718-96ee-e9674b8c33e7") {
            AppMetrica.activate(with: configuration)
        }
        AppAnalytics.shared.setEngine(engine: AppMetricaEngine())
        AppIconManager.shared.setEngine(newEngine: AppIconEngine())
        NotificationsManager.shared.setEngine(newEngine: NotificationsEngine())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
