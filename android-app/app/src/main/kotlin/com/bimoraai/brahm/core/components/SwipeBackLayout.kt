package com.bimoraai.brahm.core.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

/**
 * Wraps content with a horizontal swipe-to-go-back gesture.
 * Swipe right (threshold 80dp) calls navController.popBackStack().
 */
@Composable
fun SwipeBackLayout(
    navController: NavController,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offsetX = 0f },
                    onDragEnd = {
                        if (offsetX > 80f) navController.popBackStack()
                        offsetX = 0f
                    },
                    onDragCancel = { offsetX = 0f },
                    onHorizontalDrag = { _, delta ->
                        if (delta > 0) offsetX += delta
                    },
                )
            },
    ) {
        content()
    }
}
