package club.cyxc.workscribe.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import club.cyxc.workscribe.data.DayNoteLimits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayNoteEditBottomSheet(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(initialContent) { mutableStateOf(initialContent) }

    ModalBottomSheet(
        onDismissRequest = {
            onSave(draft)
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = "编辑备注",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = draft,
                onValueChange = { value ->
                    draft = if (value.length <= DayNoteLimits.MAX_LENGTH) {
                        value
                    } else {
                        value.take(DayNoteLimits.MAX_LENGTH)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("记录当天的工作或生活备忘…") },
                minLines = 4,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
                supportingText = {
                    Text("${draft.length}/${DayNoteLimits.MAX_LENGTH}")
                },
            )
            TextButton(
                onClick = {
                    onSave(draft)
                    onDismiss()
                },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text("完成")
            }
        }
    }
}
