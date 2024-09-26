package com.jnano.jngcsmsapp.utils.retrofit

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelpers {

    private var instance: ServerApi? = null

    private var currentUrl: String? = null

    fun getIntance(url: String): ServerApi {
        if (instance == null || currentUrl == null || currentUrl != url) {
            val retrofit = Retrofit
                .Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            instance = retrofit.create(ServerApi::class.java)
            currentUrl = url
        }
        return instance!!
    }


}