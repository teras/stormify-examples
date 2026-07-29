import SwiftUI
import Shared

@main
struct StormifyDemoApp: App {
    init() {
        // One explicit open before any screen exists, so no background task can race
        // another into building a second instance.
        Database.shared.prepare()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
