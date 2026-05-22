package io.lackstudio.omnihub

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [35],
    packageName = "io.lackstudio.omnihub.shared"
)
actual abstract class BaseComposeTest actual constructor()
