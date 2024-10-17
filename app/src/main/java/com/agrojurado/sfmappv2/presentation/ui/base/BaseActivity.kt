package com.agrojurado.sfmappv2.presentation.ui.base

import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.agrojurado.sfmappv2.R

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_SfmAppV2) // Set the base theme
        setContentView(getLayoutResourceId())

        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getActivityTitle()

        // Apply custom toolbar color
        val customColor = ContextCompat.getColor(this, getToolbarColor())
        toolbar.setBackgroundColor(customColor)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    abstract fun getLayoutResourceId(): Int
    abstract fun getActivityTitle(): String

    @ColorRes
    open fun getToolbarColor(): Int = R.color.green // Default color
}