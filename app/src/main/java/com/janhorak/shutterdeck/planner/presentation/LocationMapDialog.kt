package com.janhorak.shutterdeck.planner.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.janhorak.shutterdeck.core.data.db.LocationEntity
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale

@Composable
fun LocationMapDialog(
    location: LocationEntity,
    onDismiss: () -> Unit,
) {
    val latitude = location.latitude ?: return
    val longitude = location.longitude ?: return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val externalMapIntent = remember(location.id, location.name, latitude, longitude) {
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "geo:0,0?q=${formatCoordinate(latitude)},${formatCoordinate(longitude)}(${Uri.encode(location.name)})",
            ),
        )
    }
    val canOpenExternalMap = remember(context, externalMapIntent) {
        externalMapIntent.resolveActivity(context.packageManager) != null
    }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        val currentMapView = mapView
        if (currentMapView == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> currentMapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> currentMapView.onPause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            currentMapView.onResume()
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                currentMapView.onPause()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = location.name,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "${formatCoordinate(latitude)}, ${formatCoordinate(longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    factory = { viewContext ->
                        MapView(viewContext).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(15.0)
                            mapView = this
                            updateLocationPreview(location)
                        }
                    },
                    update = { currentMapView ->
                        mapView = currentMapView
                        currentMapView.updateLocationPreview(location)
                    },
                    onRelease = { releasedMapView ->
                        if (mapView === releasedMapView) {
                            mapView = null
                        }
                        releasedMapView.onDetach()
                    },
                )
                Text(
                    text = "Map tiles © OpenStreetMap contributors. A network connection is required to load this preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canOpenExternalMap) {
                    OutlinedButton(
                        onClick = { context.openExternalMap(externalMapIntent) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open in map app")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Close")
                }
            }
        }
    }
}

private fun MapView.updateLocationPreview(location: LocationEntity) {
    val latitude = location.latitude ?: return
    val longitude = location.longitude ?: return
    val point = GeoPoint(latitude, longitude)
    controller.setCenter(point)
    overlays.clear()
    Marker(this).apply {
        position = point
        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        title = location.name
    }.also(overlays::add)
    invalidate()
}

private fun Context.openExternalMap(intent: Intent) {
    if (this !is Activity) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startActivity(intent)
}

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)
