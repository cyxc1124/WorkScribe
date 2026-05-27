package club.cyxc.workscribe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

private val WheelItemHeight = 44.dp
private const val WheelVisibleRows = 5

@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours = remember { (0..23).map { "%02d".format(it) } }
    val minutes = remember { (0..59).map { "%02d".format(it) } }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelPickerColumn(
            items = hours,
            selectedIndex = hour,
            onSelectedIndexChange = onHourChange,
            suffix = "时",
            modifier = Modifier.weight(1f),
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        WheelPickerColumn(
            items = minutes,
            selectedIndex = minute,
            onSelectedIndexChange = onMinuteChange,
            suffix = "分",
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WheelTimePickerBottomSheet(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String = "滑动选择小时与分钟",
) {
    var pickerHour by remember { mutableIntStateOf(initialHour) }
    var pickerMinute by remember { mutableIntStateOf(initialMinute) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            WheelTimePicker(
                hour = pickerHour,
                minute = pickerMinute,
                onHourChange = { pickerHour = it },
                onMinuteChange = { pickerMinute = it },
            )
            HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = { onConfirm(pickerHour, pickerMinute) },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text("确定")
                }
            }
        }
    }
}

@Composable
private fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    suffix: String,
    modifier: Modifier = Modifier,
) {
    val wheelHeight = WheelItemHeight * WheelVisibleRows
    val verticalPadding = WheelItemHeight * (WheelVisibleRows / 2)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex.coerceIn(0, items.lastIndex),
    )

    val snapLayoutInfoProvider = remember(listState) {
        SnapLayoutInfoProvider(
            lazyListState = listState,
            snapPosition = SnapPosition.Center,
        )
    }
    val flingBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider)

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) {
                selectedIndex
            } else {
                val viewportCenter =
                    layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
                layoutInfo.visibleItemsInfo
                    .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                    ?.index
                    ?.coerceIn(0, items.lastIndex)
                    ?: selectedIndex
            }
        }
    }

    LaunchedEffect(selectedIndex) {
        if (!listState.isScrollInProgress && centeredIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex.coerceIn(0, items.lastIndex))
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .collect { index ->
                if (index != selectedIndex) {
                    onSelectedIndexChange(index)
                }
            }
    }

    Box(
        modifier = modifier.height(wheelHeight),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = verticalPadding),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(items.size) { index ->
                val distance = abs(index - centeredIndex)
                val alpha = when (distance) {
                    0 -> 1f
                    1 -> 0.55f
                    else -> 0.3f
                }
                val weight = if (distance == 0) FontWeight.Bold else FontWeight.Normal
                Box(
                    modifier = Modifier
                        .height(WheelItemHeight)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = items[index],
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = weight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.alpha(alpha),
                        )
                        Text(
                            text = suffix,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .alpha(alpha * 0.9f),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    RoundedCornerShape(10.dp),
                ),
        )

        val fadeColor = MaterialTheme.colorScheme.surface
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to fadeColor,
                        0.22f to Color.Transparent,
                        0.78f to Color.Transparent,
                        1f to fadeColor,
                    ),
                ),
        )
    }
}
