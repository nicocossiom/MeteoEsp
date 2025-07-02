// data/model/Municipio.kt
package com.mssdepas.meteoesp.data.model

import com.google.gson.annotations.SerializedName

data class Municipio(
    @SerializedName("CODIGOINE") val codigoINE: String,   // e.g. 28079xxx…
    @SerializedName("NOMBRE")    val nombre: String,
    @SerializedName("CODPROV")   val codProv: String      // 28 for Madrid
)