package com.mssdepas.meteoesp.data.remote

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.mssdepas.meteoesp.data.model.Municipio
import com.mssdepas.meteoesp.data.model.Provincia

public data class ProvinciasResponse(
    val provincias: List<Provincia>
)

public data class MunicipiosResponse(
    val municipios: List<Municipio>
)

data class WeatherResponse(
    @SerializedName("elaborado") val elaborado: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("fecha") val fecha: String?,
    @SerializedName("temperatura_actual") val temperaturaActual: String?,
    @SerializedName("stateSky") val estadoCielo: EstadoCielo?,
    @SerializedName("temperaturas") val temperaturas: Temperaturas?,
    @SerializedName("humedad") val humedad: String?,
    @SerializedName("viento") val viento: String?,
    @SerializedName("precipitacion") val precipitacion: String?,
    @SerializedName("lluvia") val lluvia: String?,
    @SerializedName("municipio") val municipio: MunicipioInfo?,
    @SerializedName("pronostico") val pronostico: Pronostico?,
    @SerializedName("proximos_dias") val proximosDias: List<ProximoDia>?
)

data class EstadoCielo(
    @SerializedName("description") val descripcion: String?,
    @SerializedName("id") val id: String?
)

data class Temperaturas(
    @SerializedName("max") val max: String?,
    @SerializedName("min") val min: String?
)

data class MunicipioInfo(
    @SerializedName("CODIGOINE") val codigoINE: String?,
    @SerializedName("NOMBRE") val nombre: String?,
    @SerializedName("NOMBRE_PROVINCIA") val nombreProvincia: String?
)

data class Pronostico(
    @SerializedName("hoy") val hoy: DiaPronostico?,
    @SerializedName("manana") val manana: DiaPronostico?
)

data class DiaPronostico(
    @SerializedName("estado_cielo") val estadoCielo: List<String>?,
    @SerializedName("precipitacion") val precipitacion: List<String>?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("prob_precipitacion") val probPrecipitacion: List<String>?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("prob_tormenta") val probTormenta: List<String>?,
    @SerializedName("nieve") val nieve: List<String>?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("prob_nieve") val probNieve: List<String>?,
    @SerializedName("temperatura") val temperatura: List<String>?,
    @SerializedName("sens_termica") val sensTermica: List<String>?,
    @SerializedName("humedad_relativa") val humedadRelativa: List<String>?,
    @SerializedName("viento") val viento: List<VientoHora>?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("racha_max") val rachaMax: List<String>?,
    @JsonAdapter(ListOrStringDeserializer::class) // <--- ENSURE THIS IS PRESENT (it was in your original file)
    @SerializedName("estado_cielo_descripcion") val estadoCieloDescripcion: List<String>?
)

data class VientoHora(
    @SerializedName("@attributes") val atributos: Periodo?,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("velocidad") val velocidad: String?
)

data class Periodo(
    @SerializedName("periodo") val periodo: String?
)

data class ProximoDia(
    @SerializedName("@attributes") val atributos: FechaDia?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("prob_precipitacion") val probPrecipitacion: List<String>?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("estado_cielo") val estadoCielo: List<String>?,
    @JsonAdapter(VientoListDeserializer::class)
    @SerializedName("viento") val viento: List<VientoDia>?,
    @JsonAdapter(RachaMaxDeserializer::class)
    @SerializedName("racha_max") val rachaMax: List<Periodo>?,
    @SerializedName("temperatura") val temperatura: TemperaturaDia?,
    @SerializedName("sens_termica") val sensTermica: TemperaturaDia?,
    @SerializedName("humedad_relativa") val humedadRelativa: HumedadDia?,
    @SerializedName("uv_max") val uvMax: String?,
    @JsonAdapter(ListOrStringDeserializer::class)
    @SerializedName("estado_cielo_descripcion") val estadoCieloDescripcion: List<String>?
)

data class FechaDia(
    @SerializedName("fecha") val fecha: String?
)

data class VientoDia(
    @SerializedName("@attributes") val atributos: Periodo?,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("velocidad") val velocidad: String?
)

data class TemperaturaDia(
    @SerializedName("maxima") val maxima: String?,
    @SerializedName("minima") val minima: String?,
    @SerializedName("dato") val dato: List<String>? = null
)

data class HumedadDia(
    @SerializedName("maxima") val maxima: String?,
    @SerializedName("minima") val minima: String?,
    @SerializedName("dato") val dato: List<String>? = null
)