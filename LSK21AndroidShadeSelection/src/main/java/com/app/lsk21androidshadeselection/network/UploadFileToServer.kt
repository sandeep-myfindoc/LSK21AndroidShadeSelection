package com.app.lsk21androidshadeselection.network

import android.os.AsyncTask
import com.app.lsk21androidshadeselection.util.ResultReceiver
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UploadFileToServer(private val filePath: String, private val callBack: ResultReceiver) :
    AsyncTask<Unit, Unit, String>() {

    override fun doInBackground(vararg params: Unit?): String? {
        val file = File(filePath)
        if (!file.exists()) return null // Check if file exists
        var dataOutputStream: DataOutputStream? = null
        try{
            val boundary = "*****"
            val lineEnd = "\r\n"
            val twoHyphens = "--"
            val url = URL("http://3.141.188.111:5000/shade_selection")
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.doOutput = true
            connection.requestMethod = "POST"
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
            connection.setRequestProperty(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6NCwiaWF0IjoxNzI4NTYyNTI1LCJleHAiOjE3NjAwOTg1MjV9.epfKv20ORvfCDThnriUEWCHpnNJt9wo9z5rGQPHcCRA"
            )
            /*connection.setRequestProperty(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6MywiaWF0IjoxNzE1MzIzMzcyLCJleHAiOjE3NDY4NTkzNzJ9.i0-Vdz3qjjMHcWhMVJS6nkLie8ov5FkckwsxOMl1xxw"
            )*/
            // Create a DataOutputStream to write the data to the server
            dataOutputStream = DataOutputStream(connection.outputStream)
            // Add file part
            dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd)
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"shade_image\";filename=\"${file.name}\"$lineEnd")
            dataOutputStream.writeBytes(lineEnd)

            // Read file and write to output stream
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                dataOutputStream.write(buffer, 0, bytesRead)
            }
            dataOutputStream.writeBytes(lineEnd)
            dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
            // Close streams
            inputStream.close()
            dataOutputStream.flush()
            dataOutputStream.close()
            // Get response
            val responseCode = connection.responseCode
            val responseMessage = connection.responseMessage
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Handle successful response
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                println("Response: $response")
                return response
            } else {
                println("Upload failed: $responseCode $responseMessage")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            dataOutputStream?.flush()
            dataOutputStream?.close()
        }
        /*val requestFile = RequestBody.create("multipart/form-data".toMediaTypeOrNull(), file)
        val body = MultipartBody.Part.createFormData("shade_image", file.name, requestFile)*/
        return null
    }

    fun readStream(inputStream: InputStream): String {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()
        bufferedReader.forEachLine { stringBuilder.append(it) }
        return stringBuilder.toString()
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        if(result!=null)
            callBack.onSucess(result.toString())
        else
            callBack.onFailure(result.toString())
    }

}