package com.mimo.draw.ui

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object SpringConfig {
    const val STIFFNESS_VERY_LOW = 50f
    const val STIFFNESS_LOW = 200f
    const val STIFFNESS_MEDIUM = 1500f
    const val STIFFNESS_HIGH = 10000f

    const val DAMPING_RATIO_NO_BOUNCY = 1.0f
    const val DAMPING_RATIO_LOW_BOUNCY = 0.75f
    const val DAMPING_RATIO_MEDIUM_BOUNCY = 0.5f
    const val DAMPING_RATIO_HIGH_BOUNCY = 0.2f
}

@Composable
fun springFloat(
    targetValue: Float,
    stiffness: Float = SpringConfig.STIFFNESS_MEDIUM,
    dampingRatio: Float = SpringConfig.DAMPING_RATIO_MEDIUM_BOUNCY
): Float {
    val spring = spring<Float>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
    val animatedValue by animateFloatAsState(
        targetValue = targetValue,
        animationSpec = spring
    )
    return animatedValue
}

@Composable
fun springDp(
    targetValue: Dp,
    stiffness: Float = SpringConfig.STIFFNESS_MEDIUM,
    dampingRatio: Float = SpringConfig.DAMPING_RATIO_MEDIUM_BOUNCY
): Dp {
    val spring = spring<Dp>(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )
    val animatedValue by animateDpAsState(
        targetValue = targetValue,
        animationSpec = spring
    )
    return animatedValue
}

@Composable
fun <T> springAnimation(
    targetValue: T,
    typeConverter: TwoWayConverter<T, AnimationVector>,
    stiffness: Float = SpringConfig.STIFFNESS_MEDIUM,
    dampingRatio: Float = SpringConfig.DAMPING_RATIO_MEDIUM_BOUNCY,
    label: String = "spring"
): State<T> {
    val spring = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = typeConverter.convertToVector(targetValue).value * 0.01f
    )
    return animateValueAsState(
        targetValue = targetValue,
        typeConverter = typeConverter,
        animationSpec = spring,
        label = label
    )
}

@Composable
fun AnimatedSpringVisibility(
    visible: Boolean,
    stiffness: Float = SpringConfig.STIFFNESS_MEDIUM,
    dampingRatio: Float = SpringConfig.DAMPING_RATIO_MEDIUM_BOUNCY,
    content: @Composable () -> Unit
) {
    val targetAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    )
    val targetScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness
        )
    )

    if (targetAlpha > 0.01f) {
        content()
    }
}
