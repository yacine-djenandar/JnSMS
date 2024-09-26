package com.jnano.jngcsmsapp

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase


@Database(entities = [Appointment::class], version = 2, autoMigrations = [AutoMigration(from = 1, to = 2)])
abstract class Database: RoomDatabase() {

    abstract fun appointmentDAO(): AppointmentEntryDAO


}