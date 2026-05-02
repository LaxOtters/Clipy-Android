import convention.configureCoroutineKotlin
import convention.configureKotlin
import convention.configureTest

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("clipy.lint")
}

configureKotlin()
configureCoroutineKotlin()
configureTest()
