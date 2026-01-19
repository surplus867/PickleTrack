package com.example.shared

import platform.Foundation.NSProcessInfo

actual class Platform {
    actual fun name(): String = NSProcessInfo.processInfo.operatingSystemVersionString
}
