package com.laxotters.clipy.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme

@Composable
internal fun ClipySplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset("clipy_logo_animation.json"),
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
    )

    LaunchedEffect(composition, progress) {
        if (composition != null && progress == 1f) {
            onAnimationFinished()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ClipyTheme.gradients.linearMint),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(176.dp),
        )
    }
}
