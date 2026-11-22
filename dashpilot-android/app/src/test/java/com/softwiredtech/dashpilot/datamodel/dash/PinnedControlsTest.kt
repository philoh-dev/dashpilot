package com.softwiredtech.dashpilot.datamodel.dash

import org.junit.Assert.assertEquals
import org.junit.Test

class PinnedControlsTest {
    @Test fun inheritsExistingSinglePinWhenListHasNotBeenSaved() {
        assertEquals(listOf("frunk"), PinnedControls.decode(null, "frunk"))
        assertEquals(emptyList<String>(), PinnedControls.decode(null, null))
    }

    @Test fun clearingTheListDoesNotResurrectLegacyPin() {
        assertEquals(emptyList<String>(), PinnedControls.decode("", "frunk"))
    }

    @Test fun newListTakesPrecedenceAndKeepsPinOrder() {
        assertEquals(listOf("trunk", "frunk", "glovebox"),
            PinnedControls.decode("trunk,frunk,glovebox", "frunk"))
    }

    @Test fun duplicateAndEmptyEntriesAreRemoved() {
        assertEquals(listOf("trunk", "frunk"), PinnedControls.decode("trunk,,frunk,trunk,", null))
    }

    @Test fun togglingOnePinPreservesTheOtherPinsAndAppendsNewPins() {
        val pins = listOf("trunk", "frunk", "glovebox")
        assertEquals(listOf("trunk", "glovebox"), PinnedControls.toggle(pins, "frunk"))
        assertEquals(pins + "mirrors", PinnedControls.toggle(pins, "mirrors"))
    }

    @Test fun savingMoreThanSixDoesNotDiscardOffscreenCommands() {
        val ids = (1..8).map { "command_$it" }
        assertEquals(ids, PinnedControls.decode(ids.joinToString(","), null))
        assertEquals(ids - "command_7", PinnedControls.toggle(ids, "command_7"))
    }
}
