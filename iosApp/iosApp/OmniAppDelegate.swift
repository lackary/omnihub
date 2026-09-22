//
//  AppDelegate.swift
//  iosApp
//
//  Created by YuChan Huang on 2026/5/30.
//

import Foundation
import UIKit
import FirebaseCore
import GoogleSignIn
import Shared

class SwiftGoogleSignInProvider: NSObject, GoogleSignInProvider {
    @MainActor
    func signIn(completion: @escaping (GoogleAuthTokens?) -> Void) {
        guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
              let rootVC = windowScene.windows.first(where: { $0.isKeyWindow })?.rootViewController else {
            print("❌ [GoogleSignIn] Error: Unable to find key window rootViewController")
            completion(nil)
            return
        }
        var topVC = rootVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: topVC) { result, error in
            if let error = error {
                print("❌ [GoogleSignIn] Error: \(error.localizedDescription)")
                completion(nil)
                return
            }
            guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                print("❌ [GoogleSignIn] Error: Missing idToken")
                completion(nil)
                return
            }
            let accessToken = user.accessToken.tokenString
            let tokens = GoogleAuthTokens(idToken: idToken, accessToken: accessToken)
            completion(tokens)
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }
}

class OmniAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        print("🚀 AppDelegate: Firebase configured.")

        IosAuthManager.companion.setGoogleSignInProvider(provider: SwiftGoogleSignInProvider())
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey : Any] = [:]
    ) -> Bool {
        return GIDSignIn.sharedInstance.handle(url)
    }
}
