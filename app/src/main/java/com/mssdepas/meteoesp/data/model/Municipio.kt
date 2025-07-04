// data/model/Municipio.kt
package com.mssdepas.meteoesp.data.model

import com.google.gson.annotations.SerializedName

data class Municipio(
    @SerializedName("CODIGOINE") val codigoINE: String,   // e.g. 28079xxx…
    @SerializedName("NOMBRE")    val nombre: String,
    @SerializedName("CODPROV")   val codProv: String,     // 28 for Madrid
    @SerializedName("LONGITUD_ETRS89_REGCAN95") val longitud: String, // e.g. -3.7037902
    @SerializedName("LATITUD_ETRS89_REGCAN95")  val latitud: String,  // e.g. 40.4167754
)