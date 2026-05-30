package com.janhorak.shutterdeck.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class PersistedDocumentAccessTest {

    @Test
    fun documentAttachmentLabel_decodesFinalPathSegment() {
        val uri = "content://media/external/images/media/Scouting%20Spot%201.jpg?token=abc"

        assertEquals("Scouting Spot 1.jpg", documentAttachmentLabel(uri))
    }

    @Test
    fun documentAttachmentLabel_blankUri_returnsBlank() {
        assertEquals("", documentAttachmentLabel("   "))
    }
}
