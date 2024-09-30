package com.app.lsk21androidshadeselection.modal.airesponse_modal

import com.google.gson.annotations.SerializedName

data class AIResponse(
    @SerializedName("color_recommendation")
    val colorRecommendation: ColorRecommendation,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: String
)
