import convention.configureCoroutineAndroid
import convention.configureKotlinAndroid

plugins {
    id("com.android.library")
    id("clipy.lint")
}

configureKotlinAndroid()
configureCoroutineAndroid()
