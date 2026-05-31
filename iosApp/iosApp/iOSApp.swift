import SwiftUI
import Shared
import FirebaseCore

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(OmniAppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // This will intercept all omnihub:// requests
                    print("📲 iOS DeepLink received: \(url.absoluteString)")

                    // Call Kotlin Shared Code
                    // Kotlin Object becomes a shared instance in Swift (usually .shared or called directly)
                    DeepLinkBuffer.shared.setDeepLink(url: url.absoluteString)
                }
        }
    }
}
