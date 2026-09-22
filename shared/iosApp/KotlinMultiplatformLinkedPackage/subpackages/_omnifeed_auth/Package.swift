// swift-tools-version: 5.9
import PackageDescription
let package = Package(
  name: "_omnifeed_auth",
  platforms: [
    .iOS("15.0")
  ],
  products: [
    .library(
      name: "_omnifeed_auth",
      type: .none,
      targets: ["_omnifeed_auth"]
    )
  ],
  dependencies: [
    .package(
      url: "https://github.com/google/GoogleSignIn-iOS.git",
      from: "9.0.0"
    ),
    .package(
      url: "https://github.com/firebase/firebase-ios-sdk.git",
      from: "12.14.0"
    )
  ],
  targets: [
    .target(
      name: "_omnifeed_auth",
      dependencies: [
        .product(
          name: "GoogleSignIn",
          package: "GoogleSignIn-iOS"
        ),
        .product(
          name: "FirebaseCore",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseAuth",
          package: "firebase-ios-sdk"
        ),
        .product(
          name: "FirebaseFirestore",
          package: "firebase-ios-sdk"
        )
      ]
    )
  ]
)
