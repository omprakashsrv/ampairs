//
// CrashTestView.swift
// Firebase Crashlytics Test Crash Button
//
// This view provides a test crash button to verify Firebase Crashlytics integration.
// Only visible in DEBUG builds.
//

import SwiftUI
import FirebaseCrashlytics

struct CrashTestView: View {
    var body: some View {
        #if DEBUG
        VStack {
            Spacer()
            HStack {
                Spacer()
                Button(action: {
                    triggerTestCrash()
                }) {
                    HStack {
                        Image(systemName: "exclamationmark.triangle.fill")
                        Text("Test Crash")
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 12)
                    .background(Color.red)
                    .foregroundColor(.white)
                    .cornerRadius(8)
                    .shadow(radius: 4)
                }
                .padding(.trailing, 20)
                .padding(.bottom, 20)
            }
        }
        #endif
    }

    /// Triggers a test crash for Firebase Crashlytics verification
    private func triggerTestCrash() {
        // Log a message before crash to help identify test crashes
        Crashlytics.crashlytics().log("Test crash button pressed - verifying Crashlytics integration")

        // Set a custom key to identify test crashes
        Crashlytics.crashlytics().setCustomValue("test", forKey: "crash_type")
        Crashlytics.crashlytics().setCustomValue(Date().timeIntervalSince1970, forKey: "crash_timestamp")

        // Force a crash using array out of bounds
        let numbers = [0]
        let _ = numbers[1]  // This will crash with "Index out of range"
    }
}

// Preview for SwiftUI canvas
struct CrashTestView_Previews: PreviewProvider {
    static var previews: some View {
        CrashTestView()
    }
}
