package com.mccal.folio

import android.content.ComponentName
import android.content.Intent
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecondInstanceWidgetIntegrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun shell(command: String) = android.os.ParcelFileDescriptor.AutoCloseInputStream(
        instrumentation.uiAutomation.executeShellCommand(command)).bufferedReader().use { it.readText().trim() }

    private fun await(timeout: Long = 15_000, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeout
        while (!condition()) {
            check(SystemClock.uptimeMillis() < deadline) { "Timed out waiting for Folio" }
            SystemClock.sleep(40)
        }
    }

    private fun launcherModel(activity: MainActivity): LauncherModel {
        lateinit var model: LauncherModel
        instrumentation.runOnMainSync { model = ViewModelProvider(activity)[LauncherModel::class.java] }
        await { !model.state.value.loading }
        return model
    }

    private fun widgets(activity: MainActivity) = MainActivity::class.java.getDeclaredField("widgets")
        .apply { isAccessible = true }.get(activity) as WidgetController

    @Test fun settingsInstanceKeepsWidgetsHomeAddedAfterItOpened() {
        check(android.os.Build.HARDWARE in listOf("ranchu", "goldfish"))
        val app = FolioTestPackages.app
        val previousHome = shell("cmd role get-role-holders android.app.role.HOME").lineSequence().firstOrNull().orEmpty()
        val hadBindGrant = shell("dumpsys appwidget").lineSequence().any { it.contains("user=0 package=$app") }
        val otherHome = previousHome.takeIf { it.isNotEmpty() && it != app } ?: "com.google.android.apps.nexuslauncher"
        shell("appwidget grantbind --package $app --user 0")
        shell("cmd role add-role-holder android.app.role.HOME $otherHome 0")
        instrumentation.runOnMainSync { FolioActions.home?.get()?.finish() }
        shell("input keyevent KEYCODE_HOME")
        var home: MainActivity? = null
        var settings: MainActivity? = null
        var before: HomeLayout? = null
        var idsBefore = emptySet<Int>()
        var labels = true
        try {
            await { FolioActions.home?.get()?.isDestroyed != false }
            instrumentation.targetContext.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(app, "com.mccal.folio.FolioSettingsApp")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            await { FolioActions.home?.get()?.isDestroyed == false }
            val second = requireNotNull(FolioActions.home?.get())
            settings = second
            val settingsModel = launcherModel(second)

            shell("cmd role add-role-holder android.app.role.HOME $app 0")
            shell("input keyevent KEYCODE_HOME")
            await { FolioActions.home?.get().let { it != null && it !== second && !it.isDestroyed } }
            val main = requireNotNull(FolioActions.home?.get())
            home = main
            val homeModel = launcherModel(main)
            val homeWidgets = widgets(main)
            labels = homeModel.state.value.labels

            var id = -1
            var provider: ComponentName? = null
            instrumentation.runOnMainSync {
                val layout = homeModel.state.value.layout
                before = layout
                idsBefore = homeWidgets.host.appWidgetIds.toSet()
                val clock = homeWidgets.manager.installedProviders.first { it.provider.className.endsWith("AnalogAppWidgetProvider") }
                provider = clock.provider
                id = homeWidgets.host.allocateAppWidgetId()
                assertTrue(homeWidgets.manager.bindAppWidgetIdIfAllowed(id, clock.provider))
                homeModel.restoreLayout(layout.copy(widgetPlacements = layout.widgetPlacements +
                    WidgetPlacement(homeModel.nextWidgetSlot(), id, 1, 0, 0, 2, 2)))
            }
            instrumentation.waitForIdleSync()
            assertTrue(id in homeWidgets.host.appWidgetIds)

            instrumentation.runOnMainSync { settingsModel.setLabels(!labels) }
            instrumentation.waitForIdleSync()
            SystemClock.sleep(500)
            instrumentation.waitForIdleSync()

            assertTrue("The Settings instance pruned a widget Home placed after it opened", id in homeWidgets.host.appWidgetIds)
            assertEquals(provider, homeWidgets.manager.getAppWidgetInfo(id)?.provider)
        } finally {
            val cleanupHome = otherHome
            try {
                instrumentation.runOnMainSync { settings?.finish() }
                await { settings?.isDestroyed != false }
                val main = home
                if (main != null && before != null) instrumentation.runOnMainSync {
                    val model = ViewModelProvider(main)[LauncherModel::class.java]
                    val hostWidgets = widgets(main)
                    model.setLabels(labels)
                    model.restoreLayout(requireNotNull(before))
                    hostWidgets.host.appWidgetIds.filter { it !in idsBefore }.forEach(hostWidgets.host::deleteAppWidgetId)
                }
            } finally {
                shell("cmd role add-role-holder android.app.role.HOME $cleanupHome 0")
                try {
                    instrumentation.runOnMainSync { FolioActions.home?.get()?.finish() }
                } finally {
                    shell("appwidget ${if (hadBindGrant) "grantbind" else "revokebind"} --package $app --user 0")
                    if (previousHome.isNotEmpty()) shell("cmd role add-role-holder android.app.role.HOME $previousHome 0")
                    else shell("cmd role remove-role-holder android.app.role.HOME $cleanupHome 0")
                }
            }
        }
    }
}
