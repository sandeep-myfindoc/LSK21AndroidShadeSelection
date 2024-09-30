package com.app.geniecalc.network

import android.content.Context
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

// Single ton Class
object RetrofitHelper {
    private const val baseUrl = "http://3.141.188.111:5000/" +
            "" +
            "/"
    private const val cacheSize:Long = 2 * 1024 * 1024 // 2MB
    private const val hasNetwork = false

    fun getClient(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    fun getClientForS3(): Retrofit? {
        val httpClient = OkHttpClient.Builder()
        httpClient.callTimeout(20, TimeUnit.MINUTES)
        httpClient.connectTimeout(20,TimeUnit.MINUTES)
        httpClient.readTimeout(20,TimeUnit.MINUTES)
        httpClient.writeTimeout(20,TimeUnit.MINUTES)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    fun getClient(token: String,context: Context): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(provideOkHtpClient(token,context))
            .build()
    }

    fun provideOkHtpClient(token: String,context: Context):OkHttpClient{
        return OkHttpClient.Builder()
            .cache(cache(context))
            //.addNetworkInterceptor(NetworkInterceptor())// Used when n/w is on
            .addInterceptor(AuthInterceptor(token,context))// Used when N/W is Off or On
            .build()
    }
    fun cache(context: Context):Cache{
        return Cache(File(context.cacheDir,""),cacheSize)
    }
    private class AuthInterceptor(val token:String,val context: Context): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request().newBuilder()
            request.addHeader("authorization","Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MywiaWF0IjoxNzE1MzIzMzcyLCJleHAiOjE3NDY4NTkzNzJ9.i0-Vdz3qjjMHcWhMVJS6nkLie8ov5FkckwsxOMl1xxw")
            return chain.proceed(request.build())
        }

    }
    /*
    * Used when N/w is available
    */
    private class NetworkInterceptor: Interceptor{
        override fun intercept(chain: Interceptor.Chain): Response {
            var cacheControl = CacheControl.Builder()
                                .maxAge(5,TimeUnit.DAYS)// request comes from N/W after expire date else it comes from cache
                                .build()
            var response = chain.proceed(chain.request())
            var responseBuilder = response.newBuilder()
            responseBuilder.removeHeader("Pragma")// this header tells not use cache so we remove it
            responseBuilder.removeHeader("Cache-Control")
            responseBuilder.addHeader("Cache-Control",cacheControl.toString())
            return responseBuilder.build()
        }

    }
    /*
    * Used in both case  when N/w is available or Not available
    */
    private class OfflineInterceptor: Interceptor{
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            // prevent caching when network is on. For that we use the "networkInterceptor"
            if (!true) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(1, TimeUnit.DAYS)
                    .build()
                request = request.newBuilder()
                    .removeHeader("Pragma")
                    .removeHeader("Cache-Control")
                    .cacheControl(cacheControl)
                    .build()
            }
            return chain.proceed(request)
        }

    }

}