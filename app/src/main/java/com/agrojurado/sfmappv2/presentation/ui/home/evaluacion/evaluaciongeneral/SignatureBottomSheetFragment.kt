package com.agrojurado.sfmappv2.presentation.ui.home.evaluacion.evaluaciongeneral

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import com.github.gcacace.signaturepad.views.SignaturePad
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.agrojurado.sfmappv2.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SignatureBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var signaturePad: SignaturePad
    private lateinit var btnClearFirma: Button
    private lateinit var btnSaveFirma: Button
    private lateinit var btnCloseSignature: View
    private var onSignatureSavedListener: ((String?) -> Unit)? = null
    private var originalOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    companion object {
        const val TAG = "SignatureBottomSheetFragment"
        fun newInstance(onSignatureSaved: (String?) -> Unit): SignatureBottomSheetFragment {
            return SignatureBottomSheetFragment().apply {
                this.onSignatureSavedListener = onSignatureSaved
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        originalOrientation = requireActivity().requestedOrientation
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signature_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signaturePad = view.findViewById(R.id.signaturePad)
        btnClearFirma = view.findViewById(R.id.btnClearFirma)
        btnSaveFirma = view.findViewById(R.id.btnSaveFirma)
        btnCloseSignature = view.findViewById(R.id.btnCloseSignature)

        // Deshabilitar el guardado del estado del SignaturePad
        signaturePad.isSaveEnabled = false

        setupSignaturePad()
        setupListeners()

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

    private fun setupSignaturePad() {
        signaturePad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {}
            override fun onSigned() {
                btnSaveFirma.isEnabled = true
                btnClearFirma.isEnabled = true
            }
            override fun onClear() {
                btnSaveFirma.isEnabled = false
                btnClearFirma.isEnabled = false
            }
        })
    }

    private fun setupListeners() {
        btnClearFirma.setOnClickListener {
            signaturePad.clear()
        }
        btnSaveFirma.setOnClickListener {
            if (signaturePad.isEmpty) {
                Toast.makeText(requireContext(), "Por favor, dibuje una firma", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val signaturePath = saveSignatureToFile()
            onSignatureSavedListener?.invoke(signaturePath)
            dismiss()
        }
        btnCloseSignature.setOnClickListener {
            dismiss()
        }
    }

    private fun saveSignatureToFile(): String? {
        val originalBitmap = signaturePad.signatureBitmap ?: return null
        // Usar el Bitmap original sin escalar para máxima calidad
        // Alternativamente, puedes escalar a 1024x1024 si el original es demasiado grande
        val file = File(requireContext().cacheDir, "signature_temp_${System.currentTimeMillis()}.png")

        return try {
            // Sobrescribir el archivo con calidad máxima
            FileOutputStream(file).use { out ->
                originalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // Calidad restaurada a 100
                out.flush()
            }
            file.absolutePath
        } catch (e: IOException) {
            Log.e("SignatureBottomSheet", "Error al guardar la firma: ${e.message}", e)
            null
        } finally {
            // Reciclar el Bitmap para liberar memoria
            if (!originalBitmap.isRecycled) originalBitmap.recycle()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Limpiar el SignaturePad como respaldo para evitar guardar el Bitmap
        signaturePad.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requireActivity().requestedOrientation = originalOrientation
    }
}