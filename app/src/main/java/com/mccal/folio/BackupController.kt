package com.mccal.folio

import android.appwidget.AppWidgetManager
import android.content.Context
import android.net.Uri
import android.os.UserManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupController(
    private val activity: ComponentActivity,
    private val model: LauncherModel,
    private val widgets: WidgetController,
) {
    var preview by mutableStateOf<LayoutImportPreview?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set
    var pickerPending by mutableStateOf(false)
        private set

    private val store = activity.getSharedPreferences("layout_backup_pending", Context.MODE_PRIVATE)
    private val userManager = activity.getSystemService(UserManager::class.java)
    private val scope = layoutBackupScope(activity)
    private var operation: String? = null
    private var generation = 0
    private var importRaw: String? = null
    private val createDocument = activity.activityResultRegistry.register(
        "duo.backup.create", activity, ActivityResultContracts.CreateDocument("application/json")
    ) createCallback@{ uri ->
        if (operation != OP_EXPORT) return@createCallback
        if (uri == null) clearTransaction() else {
            store.edit().putString(KEY_URI, uri.toString()).apply()
            writeExport(uri, generation)
        }
    }
    private val openDocument = activity.activityResultRegistry.register(
        "duo.backup.open", activity, ActivityResultContracts.OpenDocument()
    ) openCallback@{ uri ->
        if (operation != OP_IMPORT) return@openCallback
        if (uri == null) clearTransaction() else {
            store.edit().putString(KEY_URI, uri.toString()).apply()
            readImport(uri, generation)
        }
    }

    fun restore() {
        val saved = runCatching { Triple(store.getString(KEY_OPERATION, null), store.getString(KEY_PREVIEW, null), store.getString(KEY_URI, null)) }.getOrNull()
        operation = saved?.first
        val raw = saved?.second
        importRaw = raw.takeIf { operation == OP_PREVIEW }
        if (operation == OP_PREVIEW && raw == null) clearTransaction()
        else if (raw != null) parsePreview(raw, persist = false)
        else if (saved?.third != null && operation == OP_IMPORT) readImport(Uri.parse(saved.third), generation)
        else if (saved?.third != null && operation == OP_EXPORT) writeExport(Uri.parse(saved.third), generation)
        else if (operation != null) pickerPending = true
    }

    fun startExport(fileName: String = "twofold-layout.json") {
        val state = model.stateForBackup() ?: run { errorMessage = damagedMessage(); return }
        val raw = runCatching { encodeBackup(state) }.getOrElse {
            errorMessage = failureMessage(it, R.string.layout_backup_could_not_be_prepared); return
        }
        begin(OP_EXPORT, raw)
        try { createDocument.launch(fileName) }
        catch (error: Exception) { errorMessage = failureMessage(error, R.string.document_picker_unavailable); clearTransaction(false) }
    }

    /** Saves a backup straight to Download/Folio, no picker. */
    fun saveToFolioFolder(name: String? = null) {
        val state = model.stateForBackup() ?: run { errorMessage = damagedMessage(); return }
        val raw = runCatching { encodeBackup(state) }.getOrElse {
            errorMessage = failureMessage(it, R.string.layout_backup_could_not_be_prepared); return
        }
        val name = FolioFiles.fileName(name, "twofold-layout")
        activity.lifecycleScope.launch {
            val saved = withContext(Dispatchers.IO) { FolioFiles.save(activity, name, "application/json", raw.toByteArray())?.let { FolioFiles.displayName(activity, it) ?: name } }
            if (saved != null) successMessage = savedMessage(activity.getString(R.string.saved_to_as, FolioFiles.displayPath, saved))
            else errorMessage = activity.getString(R.string.layout_backup_could_not_be_saved)
        }
    }

    fun startImport() {
        begin(OP_IMPORT)
        try { openDocument.launch(arrayOf("application/json", "text/json", "text/plain")) }
        catch (error: Exception) { errorMessage = failureMessage(error, R.string.document_picker_unavailable); clearTransaction(false) }
    }

    fun applyImport(): Boolean {
        val raw = importRaw ?: return false
        val token = generation
        activity.lifecycleScope.launch {
            val state = model.state.first { !it.loading }
            appsNotListed()?.let { if (token == generation) { errorMessage = it; clearTransaction(clearMessages = false) }; return@launch }
            val result = runCatching { withContext(Dispatchers.Default) {
                decodeLayoutBackup(raw, state.apps, state.profiles, scope)
            } }
            result.rethrowCancellation()
            if (token != generation || operation != OP_PREVIEW) return@launch
            result.onSuccess { imported ->
                val changed = model.applyImportedLayout(imported)
                successMessage = activity.getString(if (changed) R.string.layout_restored_widgets_are_ready_to_rec else R.string.this_layout_is_already_active)
                clearTransaction(clearMessages = false)
            }.onFailure { errorMessage = failureMessage(it, R.string.this_layout_backup_is_no_longer_valid) }
        }
        return true
    }

    fun cancelImport() = clearTransaction()
    fun clearMessage() { errorMessage = null; successMessage = null }

    fun resumePendingPicker(): Boolean = when (operation) {
        OP_EXPORT -> runCatching { createDocument.launch("twofold-layout.json") }.isSuccess
        OP_IMPORT -> runCatching { openDocument.launch(arrayOf("application/json", "text/json", "text/plain")) }.isSuccess
        else -> false
    }

    private fun encodeBackup(state: LauncherState): String {
        val all = widgetDescriptors(state)
        val (portable, skipped) = withoutUnportableWidgets(state, all)
        skippedWidgets = skipped
        val descriptors = all.filter { d -> portable.widgetPlacements.any { it.slot == d.slot } }
        return encodeLayoutBackup(portable, descriptors, scope)
    }

    /**
     * What the person is told when a backup step fails: a short translated sentence, never the exception's own text ("Failed
     * requirement", "Widget 3 is unavailable"). The raw text goes to Diagnostics, where it can be read and shared on purpose.
     */
    private fun failureMessage(error: Throwable, fallback: Int): String {
        caught("Backup: ${error::class.java.simpleName}: ${error.message}") { throw error }
        return activity.getString(when (backupFailureOf(error)) {
            BackupFailure.TOO_LARGE -> R.string.layout_backup_is_too_large
            BackupFailure.NEWER_VERSION -> R.string.layout_backup_is_from_a_newer_folio
            BackupFailure.OTHER -> fallback
        })
    }

    /** Widgets the last export left out because their app is missing; the success message says how many. */
    private var skippedWidgets = 0
    private fun savedMessage(base: String) = if (skippedWidgets > 0)
        base + " " + activity.resources.getQuantityString(R.plurals.backup_widgets_left_out, skippedWidgets, skippedWidgets) else base

    /**
     * Why a backup can't be read over a damaged layout yet, or null when it can. The stand-in Home has no apps until a
     * refresh lists them, and a backup read before that would leave every app out.
     */
    private fun appsNotListed(): String? = if (!model.layoutDamaged || model.appsListed) null
        else activity.getString(if (model.listingApps) R.string.loading_apps else R.string.apps_could_not_be_loaded)

    /** Why no backup is made while the saved layout can't be read: the Home on screen is only a stand-in. */
    private fun damagedMessage() = listOf(R.string.layout_backup_could_not_be_prepared, R.string.saved_home_layout_could_not_be_read)
        .joinToString(" ") { activity.getString(it) }

    private fun begin(value: String, payload: String? = null) {
        generation++
        preview = null; errorMessage = null; successMessage = null
        importRaw = null
        operation = value; pickerPending = true
        val editor = store.edit().clear().putString(KEY_OPERATION, value)
        payload?.let { editor.putString(KEY_EXPORT, it) }
        editor.apply()
    }

    private fun writeExport(uri: Uri, token: Int) {
        activity.lifecycleScope.launch {
            val result = runCatching {
                val raw = store.getString(KEY_EXPORT, null) ?: error("The export snapshot is unavailable")
                withContext(Dispatchers.IO) {
                    activity.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(raw) }
                        ?: error("The selected document could not be opened")
                }
            }
            result.rethrowCancellation()
            if (token != generation || operation != OP_EXPORT) return@launch
            result.onSuccess { successMessage = savedMessage(activity.getString(R.string.layout_backup_saved)) }
                .onFailure { errorMessage = failureMessage(it, R.string.layout_backup_could_not_be_saved) }
            clearTransaction(clearMessages = false)
        }
    }

    private fun readImport(uri: Uri, token: Int) {
        activity.lifecycleScope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { readBounded(uri) } }
            result.rethrowCancellation()
            if (token != generation || operation != OP_IMPORT) return@launch
            result.onSuccess {
                operation = OP_PREVIEW; importRaw = it
                store.edit().putString(KEY_OPERATION, OP_PREVIEW).putString(KEY_PREVIEW, it).remove(KEY_URI).apply()
                parsePreview(it, persist = false)
            }
                .onFailure {
                    errorMessage = failureMessage(it, R.string.layout_backup_could_not_be_read)
                    clearTransaction(clearMessages = false)
                }
        }
    }

    private fun parsePreview(raw: String, persist: Boolean) {
        val token = generation
        activity.lifecycleScope.launch {
            val state = model.state.first { !it.loading }
            appsNotListed()?.let { if (token == generation) { errorMessage = it; clearTransaction(clearMessages = false) }; return@launch }
            val result = runCatching { withContext(Dispatchers.Default) {
                decodeLayoutBackup(raw, state.apps, state.profiles, scope)
            } }
            result.rethrowCancellation()
            result.onSuccess {
                if (token != generation || operation != OP_PREVIEW) return@onSuccess
                importRaw = raw
                preview = it; pickerPending = false; operation = OP_PREVIEW
                if (persist) store.edit().putString(KEY_OPERATION, OP_PREVIEW).putString(KEY_PREVIEW, raw).apply()
            }.onFailure {
                if (token != generation) return@onFailure
                errorMessage = failureMessage(it, R.string.this_layout_backup_is_invalid)
                clearTransaction(clearMessages = false)
            }
        }
    }

    private fun readBounded(uri: Uri): String {
        val input = activity.contentResolver.openInputStream(uri) ?: error("The selected document could not be opened")
        return input.use {
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                require(output.size() + count <= MAX_LAYOUT_BACKUP_BYTES) { BACKUP_TOO_LARGE }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    private fun widgetDescriptors(state: LauncherState): List<BackupWidgetDescriptor> = state.widgetPlacements.mapNotNull { placement ->
        if (placement.id < 0) return@mapNotNull null
        // Its app is gone or Android unbound it: nothing to describe. The backup leaves it out and says so (withoutUnportableWidgets).
        val info = widgets.manager.getAppWidgetInfo(placement.id) ?: return@mapNotNull null
        BackupWidgetDescriptor(placement.slot, info.provider.flattenToString(), userManager.getSerialNumberForUser(info.profile),
            info.loadLabel(activity.packageManager).toString(), if (info.profile == android.os.Process.myUserHandle()) activity.getString(R.string.personal) else activity.getString(R.string.work),
            isWork = info.profile != android.os.Process.myUserHandle())
    }

    private fun clearTransaction(clearMessages: Boolean = true) {
        generation++
        operation = null; pickerPending = false; preview = null
        importRaw = null
        store.edit().clear().apply()
        if (clearMessages) clearMessage()
    }

    private fun Result<*>.rethrowCancellation() {
        exceptionOrNull()?.let { if (it is CancellationException) throw it }
    }

    companion object {
        private const val KEY_OPERATION = "operation"
        private const val KEY_PREVIEW = "preview"
        private const val KEY_EXPORT = "export"
        private const val KEY_URI = "uri"
        private const val OP_EXPORT = "export"
        private const val OP_IMPORT = "import"
        private const val OP_PREVIEW = "preview"

    }
}
