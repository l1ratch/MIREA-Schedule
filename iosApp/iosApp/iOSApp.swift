import SwiftUI
import AppMetricaCore
import Shared

@main
struct iOSApp: App {
    init() {
        KoinKt.doInitKoin()
        AppMetrica.activate(with: AppMetricaConfiguration(apiKey: "fc0cde08-05c5-4718-96ee-e9674b8c33e7"))
        AppAnalytics.shared.setEngine(engine: AppMetricaEngine())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
