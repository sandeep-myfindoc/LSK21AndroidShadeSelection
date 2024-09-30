package com.app.teethdetectioncameralibrary.viewModel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.app.lsk21androidshadeselection.viewModel.BaseAndroidViewModel
import com.app.lsk21androidshadeselection.modal.airesponse_modal.AIResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ShadeSelectionViewModel(application: Application) : BaseAndroidViewModel(application){
    var errMessage = MutableLiveData<String>("")
    private val _teetShadeResponseLiveData = MutableLiveData<AIResponse>()
    val teetShadeResponseLiveData: LiveData<AIResponse>
        get() = _teetShadeResponseLiveData
    public suspend fun fetchShades(file: File){
        try{
            var response = apiService.uploadFile(
                MultipartBody.Part.createFormData(
                    "shade_image",
                    file.name,
                    file.asRequestBody())).body()

            _teetShadeResponseLiveData.postValue(response)
        }catch(ex: Exception){
            errMessage.value = ex.message.toString()
        }
    }


}

