package com.jnano.jngcsmsapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Appointment(
    @PrimaryKey val id: Int,
    val date: Long,
    val patientId: Int,
    val patientName: String,
    val patientPhone: String,
    val note: String,
    var sent: Boolean = false,
)
