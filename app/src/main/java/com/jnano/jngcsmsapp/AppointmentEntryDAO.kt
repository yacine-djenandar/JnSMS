package com.jnano.jngcsmsapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update


@Dao
interface AppointmentEntryDAO {

    @Query("SELECT * FROM Appointment WHERE Date >= :fromDate AND Date < :toDate")
    fun getAllEntriesBetweenDates(fromDate: Long, toDate: Long): List<Appointment>


    @Query("SELECT * FROM Appointment WHERE Date >= :fromDate AND Date < :toDate AND sent = 0")
    fun getUnsentEntriesBetweenDates(fromDate: Long, toDate: Long): List<Appointment>

    @Query("SELECT * FROM APPOINTMENT WHERE id = :id")
    fun getEntryByID(id: Int): Appointment?

    @Query("SELECT * FROM Appointment WHERE id in (:ids)")
    fun getAppointmentsByIds(ids: String): List<Appointment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertEntries(vararg entries: Appointment)

    @Update
    fun updateEntries(vararg entries: Appointment)

    @Delete
    fun deleteEntries(vararg entries: Appointment)


}