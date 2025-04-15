package com.agrojurado.sfmappv2.utils

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

@SuppressLint("ClickableViewAccessibility")
fun Fragment.setupKeyboardHidingOnTap(rootView: View) {
    val gestureDetector = GestureDetector(requireContext(), object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            val focusedView = activity?.currentFocus
            if (focusedView != null) { // Si hay un elemento con foco (como un EditText)
                hideKeyboard(focusedView)
                focusedView.clearFocus()
            }
            return true // Indicamos que el evento fue manejado
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // Ignorar deslizamientos
            return false
        }
    })

    // Asegurarse de que la vista raíz pueda recibir eventos táctiles
    rootView.isClickable = true
    rootView.isFocusableInTouchMode = true

    // Aplicar el listener de toque
    rootView.setOnTouchListener { _, event ->
        gestureDetector.onTouchEvent(event)
        false // No consumir el evento para permitir que otros listeners funcionen
    }
}

fun Fragment.hideKeyboard(view: View) {
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}