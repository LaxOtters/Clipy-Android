import convention.configureKotlinAndroid
import convention.configureTest

plugins {
    id("com.android.application")
    id("clipy.lint")
}

configureKotlinAndroid()
configureTest()
