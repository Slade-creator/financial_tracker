package com.studentassoc.financialtracker.Repository;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


import com.studentassoc.financialtracker.DTO.TransactionDao;
import com.studentassoc.financialtracker.Model.Transaction;

@Database(entities = {Transaction.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static AppDatabase instance;

    public abstract TransactionDao transactionDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "financial_tracker_database"
            )
                    .fallbackToDestructiveMigration() // For dev remove when in prod
                    .build();
        }
        return instance;
    }
}
