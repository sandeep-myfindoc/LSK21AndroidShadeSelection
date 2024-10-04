package com.app.lsk21androidshadeselection.util

interface ResultReceiver {
    fun onSucess(response: String)
    fun onFailure (response: String)
}