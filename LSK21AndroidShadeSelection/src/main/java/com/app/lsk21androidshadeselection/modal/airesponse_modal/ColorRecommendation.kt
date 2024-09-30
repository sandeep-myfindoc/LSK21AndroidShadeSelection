package com.app.lsk21androidshadeselection.modal.airesponse_modal
import com.google.gson.annotations.SerializedName

data class ColorRecommendation(
    @SerializedName("color_1")
    val color1: RecommendedColor,
    @SerializedName("color_2")
    val color2: RecommendedColor,
    @SerializedName("color_3")
    val color3: RecommendedColor
)
