package com.app.shadeselection
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.app.lsk21androidshadeselection.ShadeSelectionActivity
import com.app.shadeselection.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_test)
        binding.startTest.setOnClickListener {
            startActivity(Intent(this@TestActivity, ShadeSelectionActivity:: class.java))
        }

    }
    //https://developers.google.com/sceneform/reference/com/google/ar/sceneform/rendering/Light
}//
