package com.app.lsk21androidshadeselection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.app.lsk21androidshadeselection.databinding.ActivityShadeSelectionBinding

class ShadeSelectionActivity : AppCompatActivity() {
    private lateinit var binding:ActivityShadeSelectionBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_shade_selection)
        binding.startTest.setOnClickListener {
            val resultIntent = Intent()
            resultIntent.putExtra("data","Data From App")
            setResult(Activity.RESULT_OK,resultIntent)
            finish()
        }
    }
}