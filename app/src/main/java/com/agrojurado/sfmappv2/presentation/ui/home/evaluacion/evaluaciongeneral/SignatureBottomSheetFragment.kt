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
        // Guardar la orientación actual antes de cambiarla
        originalOrientation = requireActivity().requestedOrientation
        // Forzar la orientación a horizontal (landscape)
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

        setupSignaturePad()
        setupListeners()

        // Configurar el BottomSheet para que sea de pantalla completa
        dialog?.let { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.isDraggable = false
                // Establecer altura y ancho al máximo
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
        btnClearFirma.setOnClickListener { signaturePad.clear() }
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
        val bitmap = signaturePad.signatureBitmap ?: return null
        val file = File(requireContext().filesDir, "signature_temp_${System.currentTimeMillis()}.png")
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("SignatureBottomSheet", "Error saving signature: ${e.message}", e)
            null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restaurar la orientación original al cerrar el BottomSheet
        requireActivity().requestedOrientation = originalOrientation
    }
}