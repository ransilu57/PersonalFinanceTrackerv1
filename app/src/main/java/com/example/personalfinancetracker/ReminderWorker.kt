package com.example.personalfinancetracker

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ReminderWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notification = NotificationCompat.Builder(applicationContext, "budget_notifications")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Daily Reminder")
            .setContentText("Don't forget to log your expenses today!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}