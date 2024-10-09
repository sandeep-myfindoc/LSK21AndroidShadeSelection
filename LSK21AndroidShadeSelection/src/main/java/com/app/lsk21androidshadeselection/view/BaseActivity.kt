package com.app.lsk21androidshadeselection.view

import android.app.ProgressDialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


open class BaseActivity : AppCompatActivity() {
    private var progressDialog: ProgressDialog? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    fun showProgressBar(){
        if(progressDialog==null)
            progressDialog = ProgressDialog(this@BaseActivity)
        progressDialog!!.setMessage("Please wait....")
        progressDialog!!.setCancelable(false)
        progressDialog!!.show()

    }
    fun hideProgressBar(){
        if(progressDialog!=null && progressDialog!!.isShowing){
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }
    fun showToast(msg:String){
        if(msg==null)
            return
        Toast.makeText(this@BaseActivity,msg,Toast.LENGTH_LONG).show()
    }
}