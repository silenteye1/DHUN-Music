package com.maxrave.simpmusic

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun UpdateDialog(
    state: UpdateState,
    onDismiss: () -> Unit,
    onUpdateClick: (String) -> Unit
) {
    if (state is UpdateState.Idle || state is UpdateState.AlreadyLatest) return

    Dialog(
        onDismissRequest = {
            if (state !is UpdateState.Downloading) onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = state !is UpdateState.Downloading,
            dismissOnClickOutside = state !is UpdateState.Downloading
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is UpdateState.Checking -> {
                        BasicText(
                            text = "Checking for updates...",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }

                    is UpdateState.UpdateAvailable -> {
                        BasicText(
                            text = "New Update Available!",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BasicText(
                            text = "Version ${state.versionName}",
                            style = TextStyle(
                                color = Color(0xFF60A5FA),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF121216))
                                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            BasicText(
                                text = state.changelog,
                                style = TextStyle(
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .clickable { onDismiss() }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                BasicText(
                                    text = "Later",
                                    style = TextStyle(color = Color.LightGray, fontSize = 14.sp)
                                )
                            }

                            Spacer(modifier = Modifier.size(10.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2563EB))
                                    .clickable { onUpdateClick(state.downloadUrl) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                BasicText(
                                    text = "Update Now",
                                    style = TextStyle(
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    is UpdateState.Downloading -> {
                        val animatedProgress by animateFloatAsState(
                            targetValue = state.progress,
                            label = "download_progress"
                        )
                        val percent = (animatedProgress * 100).toInt()

                        BasicText(
                            text = "Downloading Update...",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(18.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF334155))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.coerceIn(0.02f, 1f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Color(0xFF38BDF8))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        BasicText(
                            text = "$percent%",
                            style = TextStyle(
                                color = Color(0xFF38BDF8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    is UpdateState.ReadyToInstall -> {
                        BasicText(
                            text = "Download Complete!",
                            style = TextStyle(
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicText(
                            text = "Opening installer...",
                            style = TextStyle(color = Color(0xFF94A3B8), fontSize = 14.sp)
                        )
                    }

                    is UpdateState.Error -> {
                        BasicText(
                            text = "Update Error",
                            style = TextStyle(
                                color = Color(0xFFEF4444),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicText(
                            text = state.message,
                            style = TextStyle(color = Color(0xFFE2E8F0), fontSize = 13.sp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444))
                                .clickable { onDismiss() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            BasicText(
                                text = "Close",
                                style = TextStyle(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}