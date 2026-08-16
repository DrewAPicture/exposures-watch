package com.exposures.watch.sync

import com.exposures.datalayer.DataLayerPaths
import com.exposures.watch.ExposuresApplication
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Manifest-registered (see AndroidManifest.xml) so the system can start this even when the app
 * isn't running. Delegates immediately to [EquipmentSyncReceiver]/[PhotoStatusReceiver] — those
 * hold the actual logic and are unit tested; this class is just the GMS entry point wiring, which
 * can't be meaningfully tested outside a real device/emulator pair.
 */
class WearMessageListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val container get() = (application as ExposuresApplication).container

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != DataLayerPaths.CAPTURE_RESULT_COMMAND) return
        val payload = String(messageEvent.data)
        serviceScope.launch {
            PhotoStatusReceiver(container.repository).handleCaptureResultMessage(payload)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        try {
            for (event in dataEvents) {
                if (event.type != DataEvent.TYPE_CHANGED) continue
                val json = DataMapItem.fromDataItem(event.dataItem).dataMap.getString(DataLayerPaths.KEY_PAYLOAD)
                    ?: continue
                val path = event.dataItem.uri.path
                serviceScope.launch {
                    when (path) {
                        DataLayerPaths.CAMERA_BODIES -> EquipmentSyncReceiver(container.repository).handleCameraBodiesPayload(json)
                        DataLayerPaths.LENSES -> EquipmentSyncReceiver(container.repository).handleLensesPayload(json)
                        DataLayerPaths.LIGHT_METERS -> EquipmentSyncReceiver(container.repository).handleLightMetersPayload(json)
                        DataLayerPaths.ROLLS -> EquipmentSyncReceiver(container.repository).handleFilmRollsPayload(json)
                        DataLayerPaths.PHOTO_STATUSES -> PhotoStatusReceiver(container.repository).handlePhotoStatusPayload(json)
                    }
                }
            }
        } finally {
            dataEvents.release()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
