package com.mssdepas.meteoesp.data.remote

import com.mssdepas.meteoesp.data.model.Municipio
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TiempoNetApiService {

    @GET("json/v2/provincias")
    suspend fun getProvincias(): ProvinciasResponse

    @GET("json/v2/provincias/{codProv}/municipios")
    suspend fun getMunicipios(
        @Path("codProv") codProv: String
    ): MunicipiosResponse

    @GET("json/v2/municipios")
    suspend fun getMunicipio(@Query("nombre") nombre: String): MunicipiosResponse

    @GET("json/v2/provincias/{codProv}/municipios/{id}")
    suspend fun getWeather(
        @Path("codProv") codProv: String,
        @Path("id") id: String
    ): WeatherResponse
}