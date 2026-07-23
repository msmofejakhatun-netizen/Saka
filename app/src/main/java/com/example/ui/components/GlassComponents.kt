package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldGreen

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x1F2B3564), // Very transparent Indigo-blue
                        Color(0x0F111827)  // Very transparent Dark gray
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x26FFFFFF), // semi-transparent white border top
                        Color(0x08FFFFFF)  // faint transparent white border bottom
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        content()
    }
}

@Composable
fun PremiumGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Elegant pulsing glow animation in background corners
    val infiniteTransition = rememberInfiniteTransition(label = "BackgroundGlow")
    val scaleFactor by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulsing"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D22)) // Deep space background
    ) {
        // Glowing violet spot in top-left
        Box(
            modifier = Modifier
                .size((300 * scaleFactor).dp)
                .offset(x = (-50).dp, y = (-50).dp)
                .blur(80.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x408B5CF6), Color.Transparent)
                    )
                )
        )

        // Glowing emerald spot in bottom-right
        Box(
            modifier = Modifier
                .size((350 * scaleFactor).dp)
                .align(Alignment.BottomEnd)
                .offset(x = 50.dp, y = 50.dp)
                .blur(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x3510B981), Color.Transparent)
                    )
                )
        )

        content()
    }
}

@Composable
fun PremiumLoadingState(
    modifier: Modifier = Modifier,
    text: String = "Processing..."
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = EmeraldGreen,
            strokeWidth = 3.dp,
            modifier = Modifier.size(24.dp)
        )
    }
}
