package com.jnano.jngcsmsapp.utils.retrofit

import com.jnano.jngcsmsapp.Appointment
import com.jnano.jngcsmsapp.utils.ResponseObject
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Date


interface ServerApi {


    @GET("/appointments")
    suspend fun getAppointments(@Query("date") date: Date): ResponseObject<MutableList<Appointment>>

}