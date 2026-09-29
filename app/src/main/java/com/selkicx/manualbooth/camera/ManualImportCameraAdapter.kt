package com.selkicx.manualbooth.camera

import com.selkicx.manualbooth.domain.adapters.CameraAdapter
import com.selkicx.manualbooth.domain.adapters.CameraConnectionState
import com.selkicx.manualbooth.domain.adapters.CapturedPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Fallback path (spec section 15) for events where no direct camera
 * connection is available: the operator imports JPEGs via the Android
 * Storage Access Framework (SD card reader, USB drive, etc.) instead of
 * a live tethered feed.
 */
class ManualImportCameraAdapter : CameraAdapter {

    private val _connectionState = MutableStateFlow(CameraConnectionState.CONNECTED)
    override val connectionState: Flow<CameraConnectionState> = _connectionState.asStateFlow()

    private val incoming = MutableSharedFlow<CapturedPhoto>(extraBufferCapacity = 16)
    override fun observeIncomingPhotos(): Flow<CapturedPhoto> = incoming.asSharedFlow()

    override suspend fun connect() {
        _connectionState.value = CameraConnectionState.CONNECTED
    }

    override suspend fun disconnect() {
        _connectionState.value = CameraConnectionState.DISCONNECTED
    }

    override suspend fun fetchFullResolution(cameraFilename: String): String = cameraFilename

    /** Called by the SAF file-picker UI once the operator selects photos to import. */
    suspend fun importPhotos(filePaths: List<String>) {
        val now = System.currentTimeMillis()
        filePaths.forEach { path ->
            incoming.emit(
                CapturedPhoto(
                    cameraFilename = path.substringAfterLast('/'),
                    thumbnailPath = path,
                    fullResPath = path,
                    captureTimestamp = now,
                    receiveTimestamp = now
                )
            )
        }
    }
}
