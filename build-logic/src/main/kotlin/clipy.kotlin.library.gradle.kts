import convention.configureCoroutineKotlin
import convention.configureKotlin
import convention.configureTest

plugins {
    id("org.jetbrains.kotlin.jvm")
}

configureKotlin()
configureCoroutineKotlin()
configureTest()
