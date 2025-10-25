import SwiftUI

@main
struct iOSApp: App {
    init() {
        // Firebase initialization is handled in Kotlin/Native FirebaseAuthProvider
        // No Swift-side initialization needed for KMP Firebase implementation
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}