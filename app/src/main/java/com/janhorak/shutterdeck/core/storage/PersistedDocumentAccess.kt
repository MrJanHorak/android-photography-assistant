package com.janhorak.shutterdeck.core.storage

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

fun documentAttachmentLabel(uriString: String): String {
    val trimmed = uriString.trim()
    if (trimmed.isBlank()) return ""

    val encodedLabel = trimmed
        .substringBefore('?')
        .substringAfterLast('/')
        .ifBlank { trimmed }

    return URLDecoder.decode(encodedLabel, StandardCharsets.UTF_8)
}

fun ContentResolver.persistReadPermissionIfNeeded(uriString: String): Boolean {
    val trimmed = uriString.trim()
    if (trimmed.isBlank()) return true

    val uri = Uri.parse(trimmed)
    if (hasPersistedReadPermission(uri)) return true

    return try {
        takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        true
    } catch (_: SecurityException) {
        false
    }
}

fun ContentResolver.releasePersistedReadPermissionIfHeld(uriString: String) {
    val trimmed = uriString.trim()
    if (trimmed.isBlank()) return

    val uri = Uri.parse(trimmed)
    if (!hasPersistedReadPermission(uri)) return

    releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
}

private fun ContentResolver.hasPersistedReadPermission(uri: Uri): Boolean =
    persistedUriPermissions.any { permission ->
        permission.uri == uri && permission.isReadPermission
    }
