package com.example.data.device.android

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import com.example.domain.device.DeviceActionResult
import com.example.domain.device.DeviceErrorCode
import com.example.domain.device.DeviceScreenshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ScreenCaptureManager {

    companion object {
        val instance by lazy { ScreenCaptureManager() }
    }

    private var projectionResultCode: Int = Activity.RESULT_CANCELED
    private var projectionData: Intent? = null

    val isConsentGranted: Boolean
        get() = projectionResultCode == Activity.RESULT_OK && projectionData != null

    fun setConsentResult(resultCode: Int, data: Intent?) {
        this.projectionResultCode = resultCode
        this.projectionData = data
    }

    fun clearConsent() {
        this.projectionResultCode = Activity.RESULT_CANCELED
        this.projectionData = null
    }

    fun createScreenCaptureIntent(context: Context): Intent? {
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        return projectionManager?.createScreenCaptureIntent()
    }

    suspend fun captureScreenshot(context: Context): DeviceActionResult = withContext(Dispatchers.Default) {
        if (!isConsentGranted || projectionData == null) {
            return@withContext DeviceActionResult.failure(
                actionName = "takeScreenshot",
                errorCode = DeviceErrorCode.SCREEN_CAPTURE_NOT_AVAILABLE,
                message = "MediaProjection permission not granted. User must explicitly grant screen capture consent."
            )
        }

        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            ?: return@withContext DeviceActionResult.failure(
                actionName = "takeScreenshot",
                errorCode = DeviceErrorCode.CAPABILITY_NOT_AVAILABLE,
                message = "MediaProjectionManager service is not available on this device"
            )

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)

        val width = if (metrics.widthPixels > 0) metrics.widthPixels else 1080
        val height = if (metrics.heightPixels > 0) metrics.heightPixels else 1920
        val density = if (metrics.densityDpi > 0) metrics.densityDpi else DisplayMetrics.DENSITY_DEFAULT

        var mediaProjection: MediaProjection? = null
        var virtualDisplay: VirtualDisplay? = null
        var imageReader: ImageReader? = null
        var image: Image? = null
        var bitmap: Bitmap? = null

        try {
            mediaProjection = projectionManager.getMediaProjection(projectionResultCode, projectionData!!.clone() as Intent)
            if (mediaProjection == null) {
                return@withContext DeviceActionResult.failure(
                    actionName = "takeScreenshot",
                    errorCode = DeviceErrorCode.SCREEN_CAPTURE_NOT_AVAILABLE,
                    message = "Failed to obtain MediaProjection instance"
                )
            }

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "NemrawyScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.surface,
                null,
                null
            )

            // Allow display pipeline to render initial frame
            delay(150)

            image = imageReader.acquireLatestImage()
            if (image == null) {
                // Retry once
                delay(150)
                image = imageReader.acquireLatestImage()
            }

            if (image == null) {
                return@withContext DeviceActionResult.failure(
                    actionName = "takeScreenshot",
                    errorCode = DeviceErrorCode.ACTION_FAILED,
                    message = "No frame buffer rendered in virtual display"
                )
            }

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width

            bitmap = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val croppedBitmap = if (rowPadding > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, width, height)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val base64String = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

            val screenshot = DeviceScreenshot(
                width = width,
                height = height,
                format = "JPEG",
                base64Data = base64String
            )

            DeviceActionResult.success(
                actionName = "takeScreenshot",
                message = "Screen captured successfully ($width x $height)",
                screenshot = screenshot
            )
        } catch (e: SecurityException) {
            DeviceActionResult.failure(
                actionName = "takeScreenshot",
                errorCode = DeviceErrorCode.PERMISSION_REQUIRED,
                message = "Security exception during screen capture: ${e.message}"
            )
        } catch (e: Exception) {
            DeviceActionResult.failure(
                actionName = "takeScreenshot",
                errorCode = DeviceErrorCode.ACTION_FAILED,
                message = "Screen capture failed: ${e.message ?: "Unknown error"}"
            )
        } finally {
            image?.close()
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        }
    }
}
