package com.app.lsk21androidshadeselection.modal.airesponse_modal

import com.google.gson.annotations.SerializedName

data class RecommendedColor(
    @SerializedName("hex_value")
    val hexValue: String,
    @SerializedName("shade_code")
    val shadeCode: String,
    @SerializedName("similarity_score")
    val similarityScore: Double
)
