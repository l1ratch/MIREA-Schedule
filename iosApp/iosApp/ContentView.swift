import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    @Environment(\.scenePhase) private var scenePhase

    func makeUIViewController(context: Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        context.coordinator.setup(with: controller)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        context.coordinator.updateScenePhase(scenePhase)
    }

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    class Coordinator: NSObject {
        private weak var controller: UIViewController?
        private var isAppeared = true
        private var observers: [NSObjectProtocol] = []

        func setup(with controller: UIViewController) {
            self.controller = controller

            let center = NotificationCenter.default
            let resignObs = center.addObserver(
                forName: UIApplication.willResignActiveNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.transitionToInactive()
            }
            let bgObs = center.addObserver(
                forName: UIApplication.didEnterBackgroundNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.transitionToInactive()
            }
            let activeObs = center.addObserver(
                forName: UIApplication.didBecomeActiveNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.transitionToActive()
            }
            let fgObs = center.addObserver(
                forName: UIApplication.willEnterForegroundNotification,
                object: nil,
                queue: .main
            ) { [weak self] _ in
                self?.transitionToActive()
            }

            observers = [resignObs, bgObs, activeObs, fgObs]
        }

        func updateScenePhase(_ phase: ScenePhase) {
            switch phase {
            case .background, .inactive:
                transitionToInactive()
            case .active:
                transitionToActive()
            @unknown default:
                break
            }
        }

        private func transitionToInactive() {
            guard isAppeared, let controller = controller else { return }
            isAppeared = false
            // Signals viewDidDisappear to ComposeUIViewController.
            // This immediately pauses CADisplayLink and Skiko Metal rendering loop,
            // allowing iOS to suspend the process cleanly and consume 0% battery when locked.
            controller.beginAppearanceTransition(false, animated: false)
            controller.endAppearanceTransition()
        }

        private func transitionToActive() {
            guard !isAppeared, let controller = controller else { return }
            isAppeared = true
            // Resumes Compose rendering and CADisplayLink seamlessly without reloading
            controller.beginAppearanceTransition(true, animated: false)
            controller.endAppearanceTransition()
        }

        deinit {
            let center = NotificationCenter.default
            for obs in observers {
                center.removeObserver(obs)
            }
        }
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
