package com.example.spent.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.spent.R

private data class TutorialStep(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val focusLabel: String,
    val positionRatioY: Float,
    val spotlightSize: Size
)

@Composable
fun InteractiveTutorialOverlay(
    onDismiss: () -> Unit
) {
    val steps = remember {
        listOf(
            TutorialStep(
                titleRes = R.string.tutorial_step_header_title,
                descRes = R.string.tutorial_step_header_desc,
                icon = Icons.Default.AccountBalanceWallet,
                focusLabel = "Safe-to-Spend & Balance",
                positionRatioY = 0.22f,
                spotlightSize = Size(900f, 480f)
            ),
            TutorialStep(
                titleRes = R.string.tutorial_step_actions_title,
                descRes = R.string.tutorial_step_actions_desc,
                icon = Icons.Default.AddCircle,
                focusLabel = "Quick Action Buttons",
                positionRatioY = 0.44f,
                spotlightSize = Size(900f, 220f)
            ),
            TutorialStep(
                titleRes = R.string.tutorial_step_envelopes_title,
                descRes = R.string.tutorial_step_envelopes_desc,
                icon = Icons.Default.Category,
                focusLabel = "Category Envelopes",
                positionRatioY = 0.58f,
                spotlightSize = Size(900f, 280f)
            ),
            TutorialStep(
                titleRes = R.string.tutorial_step_tools_title,
                descRes = R.string.tutorial_step_tools_desc,
                icon = Icons.Default.Build,
                focusLabel = "Financial Toolbox",
                positionRatioY = 0.78f,
                spotlightSize = Size(900f, 420f)
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "arrow_bounce")
    val arrowOffsetAnim by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arrow_offset"
    )

    fun handleNextOrDismiss() {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { handleNextOrDismiss() }
    ) {
        // Spotlight Effect using BlendMode.Clear
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.8f))
            
            val spotlightCenter = Offset(size.width / 2, size.height * step.positionRatioY)
            
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(
                    spotlightCenter.x - step.spotlightSize.width / 2,
                    spotlightCenter.y - step.spotlightSize.height / 2
                ),
                size = step.spotlightSize,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress indicators
            Row(
                modifier = Modifier.padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                steps.forEachIndexed { idx, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(if (idx == currentStepIndex) 32.dp else 12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (idx <= currentStepIndex) MaterialTheme.colorScheme.primary 
                                else Color.White.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Info Card
            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "step_content",
                modifier = Modifier.weight(1f)
            ) { targetStep ->
                Box(modifier = Modifier.fillMaxSize()) {
                    val isTop = targetStep.positionRatioY < 0.5f
                    val alignment = if (isTop) Alignment.BottomCenter else Alignment.TopCenter
                    val verticalOffset = if (isTop) (-60).dp else 60.dp

                    Column(
                        modifier = Modifier
                            .align(alignment)
                            .offset(y = verticalOffset)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isTop) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(44.dp)
                                    .graphicsLayer(rotationZ = 180f)
                                    .offset(y = arrowOffsetAnim.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(16.dp, RoundedCornerShape(24.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = targetStep.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = targetStep.focusLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(targetStep.titleRes),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(targetStep.descRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = { handleNextOrDismiss() },
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = if (currentStepIndex == steps.size - 1) stringResource(R.string.tutorial_btn_got_it) 
                                               else stringResource(R.string.btn_next),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (isTop) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(44.dp)
                                    .offset(y = arrowOffsetAnim.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.tutorial_tap_to_skip),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
