package com.omar.musica.ui.anim

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically

private val expressiveSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessLow
)

private val expressiveSlideSpring = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

val OPEN_SCREEN_ENTER_ANIMATION: EnterTransition =
    slideInHorizontally(initialOffsetX = { it }, animationSpec = expressiveSlideSpring) +
            fadeIn(animationSpec = expressiveSpring)

val OPEN_SCREEN_EXIT_ANIMATION: ExitTransition =
    scaleOut(targetScale = 0.9f, animationSpec = expressiveSpring) +
            fadeOut(animationSpec = expressiveSpring)

val POP_SCREEN_ENTER_ANIMATION: EnterTransition =
    scaleIn(initialScale = 0.9f, animationSpec = expressiveSpring) +
            fadeIn(animationSpec = expressiveSpring)

val POP_SCREEN_EXIT_ANIMATION: ExitTransition =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = expressiveSlideSpring) +
            fadeOut(animationSpec = expressiveSpring)

val SLIDE_UP_ENTER_ANIMATION: EnterTransition =
    slideInVertically(initialOffsetY = { it / 2 }, animationSpec = expressiveSlideSpring) +
            fadeIn(animationSpec = expressiveSpring)

val SLIDE_DOWN_EXIT_ANIMATION: ExitTransition =
    slideOutVertically(targetOffsetY = { it / 2 }, animationSpec = expressiveSlideSpring) +
            fadeOut(animationSpec = expressiveSpring)