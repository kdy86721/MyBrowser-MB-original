package com.mybrowser.appui.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

/**
 * 순정 AOSP QR 스캐너 (GMS 미사용):
 * - CameraX 실시간 뷰파인더 + 순수 Java ZXing MultiFormatReader
 * - Zero-permission Photo Picker 이미지 QR 판독 지원
 * - 카메라 미지원 기기/거부 시 URL/QR 문자열 직접 열기 지원
 */
@Composable
fun QrScannerOverlay(
  onQrDecoded: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    )
  }
  var manualInput by remember { mutableStateOf("") }
  var scanFeedback by remember { mutableStateOf<String?>(null) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      hasCameraPermission = granted
      if (!granted) {
        scanFeedback = "카메라 권한이 거부되어 이미지 또는 직접 입력을 사용합니다"
      }
    }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
      if (uri != null) {
        val decoded = decodeQrFromUri(context, uri)
        if (!decoded.isNullOrBlank()) {
          onQrDecoded(decoded)
        } else {
          scanFeedback = "선택한 이미지에서 QR 코드를 찾지 못했습니다"
        }
      }
    }

  val design = LocalDesignConfig.current

  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize(),
  ) {
    Column(
      modifier =
        Modifier.fillMaxSize()
          .statusBarsPadding()
          .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
          .padding(BrowserSpacing.Grid2),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 640.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1_5),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          ) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("QR 스캔", style = MaterialTheme.typography.titleLarge)
          }
          SpringIconButton(onClick = onDismiss, modifier = Modifier.testTag("qr_close_button")) {
            Icon(Icons.Outlined.Close, contentDescription = "닫기")
          }
        }

        // CameraX Preview Box
        Surface(
          shape = design.cardOuterShape(),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
          if (hasCameraPermission) {
            CameraXQrPreview(
              lifecycleOwner = lifecycleOwner,
              onQrDecoded = onQrDecoded,
              onCameraError = { scanFeedback = "카메라를 열 수 없어 이미지 스캔을 사용하세요" },
              modifier = Modifier.fillMaxSize().clip(design.cardOuterShape()),
            )
          } else {
            Column(
              modifier = Modifier.fillMaxSize().padding(BrowserSpacing.Grid3),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              Icon(
                Icons.Outlined.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
              )
              Spacer(Modifier.height(12.dp))
              Text("카메라로 QR 코드 비추기", style = MaterialTheme.typography.titleMedium)
              Spacer(Modifier.height(12.dp))
              Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                shape = design.cardInnerShape(),
                modifier = Modifier.testTag("qr_request_camera_button"),
              ) {
                Text("카메라 켜기")
              }
            }
          }
        }

        scanFeedback?.let { msg ->
          Text(
            text = msg,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
        ) {
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            shape = design.cardInnerShape(),
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("이미지에서 QR 읽기")
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(BrowserSpacing.Grid1),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = manualInput,
            onValueChange = { manualInput = it },
            singleLine = true,
            shape = design.cardInnerShape(),
            label = { Text("QR URL 직접 열기") },
            modifier = Modifier.weight(1f),
          )
          Button(
            onClick = {
              if (manualInput.isNotBlank()) onQrDecoded(manualInput.trim())
            },
            enabled = manualInput.isNotBlank(),
            shape = design.cardInnerShape(),
            modifier = Modifier.height(52.dp),
          ) {
            Text("열기")
          }
        }
      }
    }
  }
}

@Composable
private fun CameraXQrPreview(
  lifecycleOwner: androidx.lifecycle.LifecycleOwner,
  onQrDecoded: (String) -> Unit,
  onCameraError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val previewView = remember { PreviewView(context) }
  val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
  val reader = remember {
    MultiFormatReader().apply {
      setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
    }
  }
  var delivered by remember { mutableStateOf(false) }

  DisposableEffect(lifecycleOwner) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    val mainExecutor = ContextCompat.getMainExecutor(context)
    providerFuture.addListener(
      {
        try {
          val provider = providerFuture.get()
          val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
          }
          val analysis =
            ImageAnalysis.Builder()
              .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
              .build()
          analysis.setAnalyzer(cameraExecutor) { imageProxy ->
            if (!delivered) {
              val decoded = decodeImageProxy(imageProxy, reader)
              if (!decoded.isNullOrBlank()) {
                delivered = true
                mainExecutor.execute { onQrDecoded(decoded) }
              }
            }
            imageProxy.close()
          }
          provider.unbindAll()
          provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis,
          )
        } catch (_: Throwable) {
          mainExecutor.execute { onCameraError() }
        }
      },
      mainExecutor,
    )

    onDispose {
      try {
        providerFuture.get().unbindAll()
      } catch (_: Throwable) {
      }
      cameraExecutor.shutdown()
    }
  }

  AndroidView(factory = { previewView }, modifier = modifier)
}

private fun decodeImageProxy(image: ImageProxy, reader: MultiFormatReader): String? {
  val plane = image.planes.firstOrNull() ?: return null
  val width = image.width
  val height = image.height
  if (width <= 0 || height <= 0) return null

  val buffer = plane.buffer
  buffer.rewind()
  val rowStride = plane.rowStride
  val pixelStride = plane.pixelStride
  val bytes =
    if (pixelStride == 1 && rowStride == width && buffer.remaining() >= width * height) {
      ByteArray(width * height).also { buffer.get(it) }
    } else {
      val rowBuffer = ByteArray(rowStride)
      ByteArray(width * height).also { out ->
        for (row in 0 until height) {
          val remaining = buffer.remaining()
          if (remaining <= 0) break
          val toRead = minOf(rowStride, remaining)
          buffer.get(rowBuffer, 0, toRead)
          val outOffset = row * width
          if (pixelStride == 1) {
            val copyLen = minOf(width, toRead)
            System.arraycopy(rowBuffer, 0, out, outOffset, copyLen)
          } else {
            for (col in 0 until width) {
              val srcIdx = col * pixelStride
              if (srcIdx < toRead) {
                out[outOffset + col] = rowBuffer[srcIdx]
              }
            }
          }
        }
      }
    }

  return try {
    val source =
      PlanarYUVLuminanceSource(
        bytes,
        width,
        height,
        0,
        0,
        width,
        height,
        false,
      )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    reader.decodeWithState(bitmap).text
  } catch (_: Throwable) {
    null
  } finally {
    reader.reset()
  }
}

private fun decodeQrFromUri(context: android.content.Context, uri: Uri): String? {
  return try {
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
      BitmapFactory.decodeStream(input, null, boundsOpts)
    }
    var sampleSize = 1
    val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
    while (maxDim / sampleSize > 1280) {
      sampleSize *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val bmp =
      context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOpts)
      } ?: return null

    try {
      val pixels = IntArray(bmp.width * bmp.height)
      bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
      val source = RGBLuminanceSource(bmp.width, bmp.height, pixels)
      val bitmap = BinaryBitmap(HybridBinarizer(source))
      MultiFormatReader().decode(bitmap).text
    } finally {
      bmp.recycle()
    }
  } catch (_: Throwable) {
    null
  }
}
