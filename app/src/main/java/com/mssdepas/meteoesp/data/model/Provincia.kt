package com.mssdepas.meteoesp.data.model

import com.google.gson.annotations.SerializedName

data class Provincia(
    @SerializedName("CODPROV") val id: String,
    @SerializedName("NOMBRE_PROVINCIA") val nombre: String,
    @SerializedName("CODAUTON") val codAuton: String,
    @SerializedName("COMUNIDAD_CIUDAD_AUTONOMA") val comunidad: String,
    @SerializedName("CAPITAL_PROVINCIA") val capital: String
)
