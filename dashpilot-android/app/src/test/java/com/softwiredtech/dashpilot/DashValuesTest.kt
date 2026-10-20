package com.softwiredtech.dashpilot

import com.softwiredtech.dashpilot.datamodel.dash.DashState
import com.softwiredtech.dashpilot.datamodel.dash.DisplaySettings
import com.softwiredtech.dashpilot.datamodel.dash.dashValues
import org.junit.Assert.assertEquals
import org.junit.Test

class DashValuesTest {
    @Test
    fun systemThemeOverridesStoredPreference() {
        for (stored in listOf(false, true)) {
            val state = DashState(displaySettings = DisplaySettings(darkMode = stored))
            for (system in listOf(false, true)) {
                assertEquals(system, state.dashValues(systemDarkMode = system)["darkMode"])
            }
        }
    }

    @Test
    fun systemThemeCanChangeWithoutChangingTelemetry() {
        val state = DashState()
        for (system in listOf(false, true, false)) {
            val values = state.dashValues(systemDarkMode = system)
            assertEquals(system, values["darkMode"])
            assertEquals(state.dashValues().filterKeys { it != "darkMode" },
                values.filterKeys { it != "darkMode" })
        }
    }

    @Test
    fun omittedSystemThemePreservesStoredPreference() {
        for (stored in listOf(false, true)) {
            val state = DashState(displaySettings = DisplaySettings(darkMode = stored))
            assertEquals(stored, state.dashValues()["darkMode"])
        }
    }
}
