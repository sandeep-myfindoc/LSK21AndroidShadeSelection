package com.app.shadeselection
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.app.lsk21androidshadeselection.view.ShadeSelectionActivity
import com.app.shadeselection.databinding.ActivityTestBinding

class TestActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTestBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_test)
        binding.startTest.setOnClickListener {
            val intent = Intent(this@TestActivity, ShadeSelectionActivity::class.java)
            intent.putExtra("selectedEventType", "shade")
            startActivityForResult(intent, 1)
        }

        binding.startTest2.setOnClickListener{
            val intent = Intent(this@TestActivity, ShadeSelectionActivity::class.java)
            intent.putExtra("selectedEventType", "shadeNew")
            startActivityForResult(intent, 1)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            //showToast(data?.getStringExtra("data")!!)
        }
    }
    private fun showToast(msg:String){
        Toast.makeText(this@TestActivity,msg,Toast.LENGTH_LONG).show()
    }
    //https://developers.google.com/sceneform/reference/com/google/ar/sceneform/rendering/Light
}//
