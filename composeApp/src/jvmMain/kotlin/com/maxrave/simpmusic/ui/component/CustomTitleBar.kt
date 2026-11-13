package com.maxrave.simpmusic.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.maxrave.simpmusic.extension.DesktopWindowChrome
import com.maxrave.simpmusic.ui.icon.Close
import com.maxrave.simpmusic.ui.icon.Remove
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.UnfoldLess
import com.maxrave.simpmusic.ui.icon.UnfoldMore
import com.maxrave.simpmusic.ui.theme.typo
import java.awt.MouseInfo
import java.awt.Window

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomTitleBar(
    title: String,
    windowState: WindowState,
    window: Window,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Black,
    titleColor: Color = Color.White,
) {
    var isMaximized by remember { mutableStateOf(windowState.placement == WindowPlacement.Maximized) }

    var dragStartX by remember { mutableStateOf(0) }
    var dragStartY by remember { mutableStateOf(0) }

    LaunchedEffect(windowState.placement) {
        isMaximized = windowState.placement == WindowPlacement.Maximized
    }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopWindowChrome.TITLE_BAR_HEIGHT_DP.dp)
                .background(containerColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                            } else {
                                windowState.placement = WindowPlacement.Maximized
                            }
                        },
                    )
                }.pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            dragStartX = mouseLocation.x - window.x
                            dragStartY = mouseLocation.y - window.y
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val mouseLocation = MouseInfo.getPointerInfo().location
                            if (windowState.placement == WindowPlacement.Maximized) {
                                windowState.placement = WindowPlacement.Floating
                                dragStartX = (windowState.size.width.value / 2).toInt()
                                dragStartY = 20
                            }
                            window.setLocation(
                                mouseLocation.x - dragStartX,
                                mouseLocation.y - dragStartY,
                            )
                        },
                    )
                },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App Title on Left side
            Text(
                text = title,
                style = typo().labelSmall,
                color = titleColor,
                modifier = Modifier.padding(start = 16.dp),
            )

            Spacer(modifier = Modifier.weight(1f))

            // Standard Windows Right-corner Control Buttons
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Minimize Button
                WindowsActionButton(
                    onClick = { windowState.isMinimized = true },
                    hoverColor = titleColor.copy(alpha = 0.12f),
                    contentColor = titleColor,
                ) {
                    Icon(
                        imageVector = SimpIcons.Remove,
                        tint = titleColor,
                        contentDescription = "Minimize",
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Maximize / Restore Button
                WindowsActionButton(
                    onClick = {
                        if (windowState.placement == WindowPlacement.Maximized) {
                            windowState.placement = WindowPlacement.Floating
                        } else {
                            windowState.placement = WindowPlacement.Maximized
                        }
                    },
                    hoverColor = titleColor.copy(alpha = 0.12f),
                    contentColor = titleColor,
                ) {
                    Icon(
                        imageVector = if (isMaximized) SimpIcons.UnfoldLess else SimpIcons.UnfoldMore,
                        tint = titleColor,
                        contentDescription = if (isMaximized) "Restore" else "Maximize",
                        modifier = Modifier.size(16.dp).rotate(45f),
                    )
                }

                // Close Button (Red background on hover)
                WindowsActionButton(
                    onClick = onCloseRequest,
                    hoverColor = Color(0xFFE81123),
                    contentColor = titleColor,
                    hoverContentColor = Color.White,
                ) { isHovered ->
                    Icon(
                        imageVector = SimpIcons.Close,
                        tint = if (isHovered) Color.White else titleColor,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WindowsActionButton(
    onClick: () -> Unit,
    hoverColor: Color,
    contentColor: Color,
    hoverContentColor: Color = contentColor,
    content: @Composable (isHovered: Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier =
            Modifier
                .width(46.dp)
                .fillMaxHeight()
                .background(if (isHovered) hoverColor else Color.Transparent)
                .hoverable(interactionSource)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
        contentAlignment = Alignment.Center,
    ) {
        content(isHovered)
    }
}