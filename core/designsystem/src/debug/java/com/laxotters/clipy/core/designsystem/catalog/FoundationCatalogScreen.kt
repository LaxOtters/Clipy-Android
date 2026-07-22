package com.laxotters.clipy.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laxotters.clipy.core.designsystem.theme.ClipyColors
import com.laxotters.clipy.core.designsystem.theme.ClipyTheme
import com.laxotters.clipy.core.designsystem.theme.ClipyTypography

@Composable
fun FoundationCatalogScreen(modifier: Modifier = Modifier) {
    val colors = ClipyTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.neutral.gray50)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text(
            text = "Clipy Foundation Catalog",
            color = colors.neutral.gray950,
            style = ClipyTheme.typography.heading1,
        )
        ColorFoundationSection(colors)
        AlphaFoundationSection(colors)
        OverlayExample(colors)
        GradientSection()
        TypographySection(colors, ClipyTheme.typography)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ColorFoundationSection(colors: ClipyColors) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Colors")
        PaletteSection("Primary / Indigo", primaryEntries(colors))
        PaletteSection("Accent / Mint", accentEntries(colors))
        PaletteSection("Neutral / Gray", neutralEntries(colors))
        PaletteSection(
            title = "Error",
            entries = listOf(
                CatalogColor("error/100", "colors.error.error100", "#FFDAD6", colors.error.error100),
                CatalogColor("error/700", "colors.error.error700", "#BA1A1A", colors.error.error700),
            ),
        )
    }
}

@Composable
private fun PaletteSection(
    title: String,
    entries: List<CatalogColor>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = ClipyTheme.colors.neutral.gray950,
            style = ClipyTheme.typography.heading4,
        )
        entries.forEach { ColorSwatch(it) }
    }
}

@Composable
private fun ColorSwatch(entry: CatalogColor) {
    val displayedColor = entry.color.compositeOver(entry.backdrop)
    val labelColor = if (displayedColor.luminance() > 0.5f) Color.Black else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(entry.backdrop)
            .background(entry.color)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(entry.name, color = labelColor, style = ClipyTheme.typography.body2SemiBold)
        Text(entry.path, color = labelColor, style = ClipyTheme.typography.body3Regular)
        Text(entry.value, color = labelColor, style = ClipyTheme.typography.body3Regular)
    }
}

@Composable
private fun AlphaFoundationSection(colors: ClipyColors) {
    val entries = listOf(
        CatalogColor("Alpha/White/10", "colors.alpha.white10", "10%", colors.alpha.white10, colors.neutral.gray700),
        CatalogColor("Alpha/White/20", "colors.alpha.white20", "20%", colors.alpha.white20, colors.neutral.gray700),
        CatalogColor("Alpha/White/70", "colors.alpha.white70", "70%", colors.alpha.white70, colors.neutral.gray700),
        CatalogColor(
            "Alpha/White/0.2",
            "colors.alpha.whitePoint2",
            "0.2%",
            colors.alpha.whitePoint2,
            colors.neutral.gray700,
        ),
        CatalogColor("Alpha/Black/15", "colors.alpha.black15", "15%", colors.alpha.black15),
        CatalogColor("Alpha/Black/20", "colors.alpha.black20", "20%", colors.alpha.black20),
        CatalogColor("Alpha/Black/60", "colors.alpha.black60", "60%", colors.alpha.black60),
    )

    PaletteSection("Alpha", entries)
}

@Composable
private fun OverlayExample(colors: ClipyColors) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Overlay Background")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.primary.indigo100),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Content below overlay",
                color = colors.primary.indigo900,
                style = ClipyTheme.typography.body1SemiBold,
            )
            Box(Modifier.fillMaxSize().background(colors.overlayBackground))
        }
        Text(
            text = "colors.overlayBackground · Black 20%",
            color = colors.neutral.gray700,
            style = ClipyTheme.typography.body3Regular,
        )
    }
}

@Composable
private fun GradientSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Gradient")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ClipyTheme.gradients.linear)
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = "linear\ngradients.linear",
                color = Color.White,
                style = ClipyTheme.typography.body2SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ClipyTheme.gradients.linearMint)
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Text(
                text = "linear_mint\ngradients.linearMint",
                color = Color.White,
                style = ClipyTheme.typography.body2SemiBold,
            )
        }
    }
}

@Composable
private fun TypographySection(
    colors: ClipyColors,
    typography: ClipyTypography,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle("Typography")
        typographyEntries(typography).forEach { entry ->
            TypographySpecimen(entry, colors)
        }
    }
}

@Composable
private fun TypographySpecimen(
    entry: CatalogTypography,
    colors: ClipyColors,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(entry.name, color = colors.neutral.gray950, style = entry.style)
        Text(entry.path, color = colors.neutral.gray700, style = ClipyTheme.typography.body3Medium)
        Text(entry.specification, color = colors.neutral.gray600, style = ClipyTheme.typography.body3Regular)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = ClipyTheme.colors.neutral.gray950,
        style = ClipyTheme.typography.heading3,
    )
}

@Preview(showBackground = true, widthDp = 390)
@Composable
private fun FoundationCatalogPreview() {
    ClipyTheme {
        FoundationCatalogScreen()
    }
}

private fun primaryEntries(colors: ClipyColors) = listOf(
    CatalogColor("Primary/50", "colors.primary.indigo50", "#FFFFFF", colors.primary.indigo50),
    CatalogColor("Primary/100", "colors.primary.indigo100", "#F9F9FE", colors.primary.indigo100),
    CatalogColor("Primary/150", "colors.primary.indigo150", "#F4F2FD", colors.primary.indigo150),
    CatalogColor("Primary/200", "colors.primary.indigo200", "#CFCCF8", colors.primary.indigo200),
    CatalogColor("Primary/300", "colors.primary.indigo300", "#A49FF2", colors.primary.indigo300),
    CatalogColor("Primary/400", "colors.primary.indigo400", "#7A73EB", colors.primary.indigo400),
    CatalogColor("Primary/500", "colors.primary.indigo500", "#4F46E5", colors.primary.indigo500),
    CatalogColor("Primary/600", "colors.primary.indigo600", "#291FD9", colors.primary.indigo600),
    CatalogColor("Primary/700", "colors.primary.indigo700", "#2118AD", colors.primary.indigo700),
    CatalogColor("Primary/800", "colors.primary.indigo800", "#181280", colors.primary.indigo800),
    CatalogColor("Primary/900", "colors.primary.indigo900", "#100C53", colors.primary.indigo900),
)

private fun accentEntries(colors: ClipyColors) = listOf(
    CatalogColor("Accent/50", "colors.accent.mint50", "#FFFFFF", colors.accent.mint50),
    CatalogColor("Accent/100", "colors.accent.mint100", "#D6F7F3", colors.accent.mint100),
    CatalogColor("Accent/200", "colors.accent.mint200", "#ACEEE6", colors.accent.mint200),
    CatalogColor("Accent/300", "colors.accent.mint300", "#82E5D9", colors.accent.mint300),
    CatalogColor("Accent/400", "colors.accent.mint400", "#57DDCC", colors.accent.mint400),
    CatalogColor("Accent/500", "colors.accent.mint500", "#2DD4BF", colors.accent.mint500),
    CatalogColor("Accent/600", "colors.accent.mint600", "#23AB9A", colors.accent.mint600),
    CatalogColor("Accent/700", "colors.accent.mint700", "#1A8174", colors.accent.mint700),
    CatalogColor("Accent/800", "colors.accent.mint800", "#12564E", colors.accent.mint800),
    CatalogColor("Accent/900", "colors.accent.mint900", "#092C28", colors.accent.mint900),
)

private fun neutralEntries(colors: ClipyColors) = listOf(
    CatalogColor("Neutral/50", "colors.neutral.gray50", "#FAFAFA", colors.neutral.gray50),
    CatalogColor("Neutral/100", "colors.neutral.gray100", "#F4F4F5", colors.neutral.gray100),
    CatalogColor("Neutral/200", "colors.neutral.gray200", "#E4E4E7", colors.neutral.gray200),
    CatalogColor("Neutral/300", "colors.neutral.gray300", "#D4D4D8", colors.neutral.gray300),
    CatalogColor("Neutral/400", "colors.neutral.gray400", "#A1A1AA", colors.neutral.gray400),
    CatalogColor("Neutral/500", "colors.neutral.gray500", "#71717A", colors.neutral.gray500),
    CatalogColor("Neutral/600", "colors.neutral.gray600", "#52525B", colors.neutral.gray600),
    CatalogColor("Neutral/700", "colors.neutral.gray700", "#3F3F46", colors.neutral.gray700),
    CatalogColor("Neutral/800", "colors.neutral.gray800", "#27272A", colors.neutral.gray800),
    CatalogColor("Neutral/900", "colors.neutral.gray900", "#18181B", colors.neutral.gray900),
    CatalogColor("Neutral/950", "colors.neutral.gray950", "#09090B", colors.neutral.gray950),
)

private fun typographyEntries(typography: ClipyTypography) = listOf(
    CatalogTypography("Heading 1", "typography.heading1", "Bold · 28/38sp · -0.6sp", typography.heading1),
    CatalogTypography("Heading 2", "typography.heading2", "Bold · 24/34sp · -0.4sp", typography.heading2),
    CatalogTypography("Heading 3", "typography.heading3", "Bold · 20/30sp · -0.2sp", typography.heading3),
    CatalogTypography("Heading 4", "typography.heading4", "SemiBold · 18/28sp", typography.heading4),
    CatalogTypography("Body 1/Semibold", "typography.body1SemiBold", "SemiBold · 16/24sp", typography.body1SemiBold),
    CatalogTypography("Body 1/Medium", "typography.body1Medium", "Medium · 16/24sp", typography.body1Medium),
    CatalogTypography("Body 1/Regular", "typography.body1Regular", "Regular · 16/24sp", typography.body1Regular),
    CatalogTypography("Body 2/Semibold", "typography.body2SemiBold", "SemiBold · 14/20sp", typography.body2SemiBold),
    CatalogTypography("Body 2/Medium", "typography.body2Medium", "Medium · 14/20sp", typography.body2Medium),
    CatalogTypography("Body 2/Regular", "typography.body2Regular", "Regular · 14/20sp", typography.body2Regular),
    CatalogTypography("Body 3/Bold", "typography.body3Bold", "Bold · 12/16sp", typography.body3Bold),
    CatalogTypography("Body 3/Semibold", "typography.body3SemiBold", "SemiBold · 12/16sp", typography.body3SemiBold),
    CatalogTypography("Body 3/Medium", "typography.body3Medium", "Medium · 12/16sp", typography.body3Medium),
    CatalogTypography("Body 3/Regular", "typography.body3Regular", "Regular · 12/16sp", typography.body3Regular),
    CatalogTypography("Tag 1/Bold", "typography.tag1Bold", "Bold · 10/15sp · 0.8sp", typography.tag1Bold),
    CatalogTypography("Tag 2/Bold", "typography.tag2Bold", "Bold · 8/10sp · 0.6sp", typography.tag2Bold),
)

private data class CatalogColor(
    val name: String,
    val path: String,
    val value: String,
    val color: Color,
    val backdrop: Color = Color.White,
)

private data class CatalogTypography(
    val name: String,
    val path: String,
    val specification: String,
    val style: TextStyle,
)
