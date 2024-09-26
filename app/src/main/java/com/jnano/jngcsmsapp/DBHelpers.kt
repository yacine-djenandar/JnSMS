package com.jnano.jngcsmsapp

import android.content.Context
import androidx.room.Room

object DBHelpers {

    private var instance: Database? = null

    fun getInstance(applicationContext: Context): Database {

        if (instance == null) {
            instance = Room.databaseBuilder(
                applicationContext,
                Database::class.java,
                "appointments_db"
            )
                .build()
        }

        return instance!!
    }


}