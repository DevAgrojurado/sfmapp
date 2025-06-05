package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.setFragmentResult
import com.agrojurado.sfmappv2.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraDialogFragment : BottomSheetDialogFragment() {

    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: MaterialButton
    private lateinit var btnCloseCamera: View
    private lateinit var btnFlipCamera: ImageButton
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private val mainHandler = Handler(Looper.getMainLooper())
    private var onPhotoSavedListener: ((String?) -> Unit)? = null

    // Variable para alternar entre cámara frontal y trasera
    private var isBackCamera = true

    companion object {
        const val TAG = "CameraDialogFragment"
        fun newInstance(onPhotoSaved: (String?) -> Unit): CameraDialogFragment {
            return CameraDialogFragment().apply {
                this.onPhotoSavedListener = onPhotoSaved
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_camera, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        btnCapture = view.findViewById(R.id.btnCapture)
        btnCloseCamera = view.findViewById(R.id.btnCloseCamera)
        btnFlipCamera = view.findViewById(R.id.btnFlipCamera)
        cameraExecutor = Executors.newSingleThreadExecutor()

        startCamera()
        setupListeners()

        // Configurar el BottomSheet como pantalla completa
        dialog?.let { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.isDraggable = false
                it.layoutParams = it.layoutParams.apply {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(requireActivity().windowManager.defaultDisplay.rotation)
                    .build()

                val cameraSelector = if (isBackCamera) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraDialogFragment", "Error starting camera: ${e.message}", e)
                mainHandler.post {
                    dismiss()
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun setupListeners() {
        btnCapture.setOnClickListener {
            // Animación de escala
            it.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    takePhoto()
                }
                .start()
        }

        btnFlipCamera.setOnClickListener {
            // Animación de rotación al voltear cámara
            it.animate()
                .rotationY(180f)
                .setDuration(300)
                .withEndAction {
                    it.rotationY = 0f
                    isBackCamera = !isBackCamera
                    startCamera()
                }
                .start()
        }

        btnCloseCamera.setOnClickListener {
            dismiss()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(requireContext().filesDir, "photo_temp_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null && photoFile.exists()) {
                        // Comprimir la imagen
                        val compressedPath = compressImage(photoFile)
                        Log.d("CameraDialogFragment", "Photo saved and compressed at: $compressedPath")
                        mainHandler.post {
                            onPhotoSavedListener?.invoke(compressedPath)
                            setFragmentResult(
                                EvaluacionGeneralFragment.REQUEST_KEY_PHOTO,
                                Bundle().apply { putString(EvaluacionGeneralFragment.BUNDLE_KEY_PHOTO, compressedPath) }
                            )
                            dismiss()
                        }
                    } else {
                        Log.e("CameraDialogFragment", "Failed to save photo")
                        mainHandler.post {
                            onPhotoSavedListener?.invoke(null)
                            dismiss()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraDialogFragment", "Photo capture failed: ${exception.message}", exception)
                    mainHandler.post {
                        onPhotoSavedListener?.invoke(null)
                        dismiss()
                    }
                }
            }
        )
    }

    private fun compressImage(file: File): String {
        // Leer la metadata EXIF para obtener la orientación
        val exif = ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        // Rotar el Bitmap según la orientación EXIF
        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap // ORIENTATION_NORMAL u otros casos
        }

        // Redimensionar el Bitmap rotado
        val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 1024, 1024, true)
        val outputFile = File(requireContext().filesDir, "photo_compressed_${System.currentTimeMillis()}.jpg")
        outputFile.outputStream().use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) // Comprimir al 70% de calidad
        }

        // Liberar memoria
        if (rotatedBitmap != bitmap) rotatedBitmap.recycle()
        bitmap.recycle()

        file.delete() // Eliminar el archivo original sin comprimir
        return outputFile.absolutePath
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}