package com.app.lsk21androidshadeselection.network;

import com.app.lsk21androidshadeselection.modal.airesponse_modal.AIResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part


// define the service here that you want to call
//VerifyOtpData used for empty response
interface ApiService {//@Body RequestBody params
    @Multipart
    @POST("shade_selection")
    suspend fun uploadFile(
        @Part file : MultipartBody.Part
    ) : Response<AIResponse>
}
/*
* There are three way to pass Header
* 1) Using Header annotaion along @GET or @POSt Annotation. we use it to pass static values in header
* 2) Pass Header inside the function call
* 3) to pass dynamic header we use intercept
* */