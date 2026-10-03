package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AccountEntity
import com.example.data.EmiEntity
import com.example.ui.Formatters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "emi_reminders_channel"
    const val EXTRA_OPEN_TAB = "extra_open_tab"
    const val TAB_EMIS = "emis"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.emi_notification_channel_name)
            val descriptionText = context.getString(R.string.emi_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks active EMIs and fires notification if any are due today or within next 3 days.
     */
    fun checkAndNotifyDueEmis(
        context: Context,
        emis: List<EmiEntity>,
        accountsMap: Map<Long, AccountEntity>
    ) {
        createNotificationChannel(context)

        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val curKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)

        for (emi in emis) {
            if (!emi.isActive || emi.isCompleted) continue
            if (emi.lastDeductedMonthYear == curKey) continue // Already paid this month

            val dayDiff = emi.dueDayOfMonth - currentDay

            // Alert if due today or within next 3 days
            if (dayDiff in 0..3) {
                val account = accountsMap[emi.linkedAccountId]
                val dueText = when (dayDiff) {
                    0 -> "Due TODAY (${emi.dueDayOfMonth}th)"
                    1 -> "Due TOMORROW"
                    else -> "Due in $dayDiff days (${emi.dueDayOfMonth}th)"
                }

                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_OPEN_TAB, TAB_EMIS)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    emi.id.toInt(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("EMI Reminder: ${emi.title}")
                    .setContentText("${Formatters.formatCurrency(emi.monthlyAmount)} $dueText • Linked to ${account?.name ?: "Account"}")
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(
                                "Monthly payment of ${Formatters.formatCurrency(emi.monthlyAmount)} is $dueText.\n" +
                                        "Linked to: ${account?.name ?: "Account"}\n" +
                                        "Installment ${emi.paidInstallments + 1} of ${emi.totalInstallments}."
                            )
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                try {
                    NotificationManagerCompat.from(context).notify(emi.id.toInt() + 1000, notification)
                } catch (e: SecurityException) {
                    // POST_NOTIFICATIONS permission not granted yet by user
                }
            }
        }
    }
}
