package com.bludosmodding.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class FabricGameVersion(
    val version: String,
    val stable: Boolean
)

data class FabricLoaderVersion(
    val version: String,
    val stable: Boolean
)

interface FabricApi {
    @GET("v2/versions/game")
    suspend fun getGameVersions(): List<FabricGameVersion>

    @GET("v2/versions/loader")
    suspend fun getLoaderVersions(): List<FabricLoaderVersion>

    companion object {
        private const val BASE_URL = "https://meta.fabricmc.net/"

        fun create(): FabricApi {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(FabricApi::class.java)
        }
    }
}
