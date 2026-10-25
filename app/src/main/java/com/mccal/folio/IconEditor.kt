package com.mccal.folio

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One app's own icon: a picture of your own, with the icon shown as it will be. The choice is applied when the icon is
 * drawn.
 */
@Composable
internal fun AppIconEditor(app: AppEntry, current: AppIconOverride, onChange: (AppIconOverride) -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = FolioSpace.LARGE.dp).padding(bottom = FolioSpace.XL.dp).verticalScroll(rememberScrollState()).testTag("icon-editor")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The icon as it will look, whatever the launcher's look is.
            CompositionLocalProvider(LocalAppIconStyles provides mapOf(app.id to current)) {
                AppIcon(app, null, Modifier.size(56.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.edit_icon_for_1, app.label), color = Color.White, fontSize = FolioType.TITLE.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.done), color = LocalAccent.current.ink, fontSize = FolioType.BODY.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onDone).padding(FolioSpace.SMALL.dp).testTag("icon-editor-done"))
        }
        // Your own picture: Android's photo picker (it hands over only the one picture), cropped to a square from its middle.
        SheetGroupLabel(stringResource(R.string.your_picture))
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) scope.launch { AppIconPictures.save(context, app.id, uri)?.let { stamp -> onChange(AppIconOverride(picture = stamp)) } }
        }
        ChipRow {
            IosChip(false, { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                { Text(stringResource(if (current.hasPicture) R.string.change_picture else R.string.choose_picture)) }, Modifier.testTag("icon-picture-choose"))
            if (current.hasPicture) IosChip(false, { onChange(AppIconOverride()) }, { Text(stringResource(R.string.remove_picture)) }, Modifier.testTag("icon-picture-remove"))
        }
        CardNote(stringResource(R.string.edit_icon_picture_note))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    FlowRow(Modifier.fillMaxWidth().padding(top = FolioSpace.SNUG.dp, bottom = FolioSpace.MEDIUM.dp), horizontalArrangement = Arrangement.spacedBy(FolioSpace.SMALL.dp),
        verticalArrangement = Arrangement.spacedBy(FolioSpace.SMALL.dp)) { content() }
}
