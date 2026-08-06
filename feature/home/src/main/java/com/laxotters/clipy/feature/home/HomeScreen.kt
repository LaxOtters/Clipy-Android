package com.laxotters.clipy.feature.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.laxotters.clipy.core.designsystem.component.ClipyTopbar
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.core.ui.R.string.core_clipy
import com.laxotters.clipy.core.ui.extension.rememberThrottledClick

@Composable
fun HomeRoute(
    onSessionClick: (sessionId: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HomeScreen(
        state = state,
        onStartNewSessionClick = {
            onSessionClick(state.sessionId)
        },
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onStartNewSessionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClipyTheme.colors.neutral.gray50,
        topBar = {
            ClipyTopbar(
                title = stringResource(core_clipy),
                modifier = Modifier.statusBarsPadding(),
                titleColor = ClipyTheme.colors.primary.indigo500,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            NewSessionHero(
                enabled = true,
                onClick = onStartNewSessionClick,
            )
            Spacer(modifier = Modifier.height(30.dp))
            GuideSection()
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun NewSessionHero(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .dropShadow(
                shape = HeroShape,
                shadow = HeroShadow,
            )
            .clip(HeroShape)
            .background(ClipyTheme.gradients.linearMint)
            .padding(32.dp),
    ) {
        Column {
            Text(
                text = stringResource(R.string.home_hero_title),
                style = ClipyTheme.typography.heading1,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.home_hero_description),
                style = ClipyTheme.typography.body2Medium,
                color = ClipyTheme.colors.alpha.white70,
            )
            Spacer(modifier = Modifier.height(26.dp))
            StartSessionButton(
                enabled = enabled,
                onClick = rememberThrottledClick {
                    onClick()
                },
            )
        }
    }
}

@Composable
private fun StartSessionButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val disabledSemantics = if (enabled) {
        Modifier
    } else {
        Modifier.semantics { disabled() }
    }

    Row(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.6f)
            .clip(ActionButtonShape)
            .background(ClipyTheme.colors.alpha.white20)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = ActionButtonShape,
            )
            .then(disabledSemantics)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_begin_comparison),
            style = ClipyTheme.typography.body1SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Image(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun GuideSection() {
    Column {
        Text(
            text = stringResource(R.string.home_how_it_works),
            style = ClipyTheme.typography.heading3,
            color = ClipyTheme.colors.neutral.gray900,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            GuideCard(
                title = R.string.home_browse_title,
                description = R.string.home_browse_description,
                illustration = R.drawable.home_browse,
            )
            GuideCard(
                title = R.string.home_collect_title,
                description = R.string.home_collect_description,
                illustration = R.drawable.home_collect,
            )
            GuideCard(
                title = R.string.home_decide_title,
                description = R.string.home_decide_description,
                illustration = R.drawable.home_decide,
            )
        }
    }
}

@Composable
private fun GuideCard(
    @StringRes title: Int,
    @StringRes description: Int,
    @DrawableRes illustration: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .dropShadow(
                shape = GuideCardShape,
                shadow = GuideCardShadow,
            )
            .clip(GuideCardShape)
            .background(ClipyTheme.colors.primary.indigo50)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(title),
                style = ClipyTheme.typography.heading4,
                color = ClipyTheme.colors.neutral.gray900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(description),
                style = ClipyTheme.typography.body1Regular,
                color = ClipyTheme.colors.neutral.gray600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier.size(100.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(illustration),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val HeroShape = RoundedCornerShape(16.dp)
private val ActionButtonShape = RoundedCornerShape(percent = 50)
private val GuideCardShape = RoundedCornerShape(16.dp)

private val HeroShadow = Shadow(
    radius = 40.dp,
    spread = (-12).dp,
    offset = DpOffset(x = 0.dp, y = 20.dp),
    color = Color(0x1A1A1B23),
)

private val GuideCardShadow = Shadow(
    radius = 7.5.dp,
    spread = 0.dp,
    offset = DpOffset(x = 0.dp, y = 4.dp),
    color = Color.Black.copy(alpha = 0.10f),
)

@Preview(
    name = "Home",
    showBackground = true,
)
@Composable
private fun HomeScreenPreview() {
    ClipyTheme {
        HomeScreen(
            state = HomeUiState(),
            onStartNewSessionClick = { },
        )
    }
}
