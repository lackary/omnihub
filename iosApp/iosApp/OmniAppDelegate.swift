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

class OmniAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        print("🚀 AppDelegate: Firebase configured.")
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
