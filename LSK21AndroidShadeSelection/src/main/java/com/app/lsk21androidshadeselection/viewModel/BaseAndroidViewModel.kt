package com.app.lsk21androidshadeselection.viewModel

import android.app.Application
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import com.app.geniecalc.network.RetrofitHelper
import com.app.lsk21androidshadeselection.network.ApiService

open class BaseAndroidViewModel(application: Application) :AndroidViewModel(application) {
    protected lateinit var context: Context
    protected lateinit var apiService: ApiService
    init {
        context = application.applicationContext
        apiService = RetrofitHelper.getClient("",context).create(ApiService :: class.java)

    }
    fun getAndroidDeviceId(): String{
        var android_id = Settings.Secure.getString(context.contentResolver,
            Settings.Secure.ANDROID_ID);
        return android_id
    }
}