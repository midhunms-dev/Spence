package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.ui.Formatters

class ExpenseWidgetProvider : AppWidgetProvider() {

    companion object {
        const val EXTRA_OPEN_ADD_TRANSACTION = "extra_open_add_transaction"
        private const val PREFS_NAME = "expense_widget_prefs"
        private const val KEY_LIQUID_BALANCE = "liquid_balance"
        private const val KEY_CREDIT_AVAILABLE = "credit_available"

        fun updateWidgetBalances(
            context: Context,
            liquidBalance: Double,
            creditAvailable: Double
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putFloat(KEY_LIQUID_BALANCE, liquidBalance.toFloat())
                .putFloat(KEY_CREDIT_AVAILABLE, creditAvailable.toFloat())
                .apply()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ExpenseWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, liquidBalance, creditAvailable)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            liquidBalance: Double? = null,
            creditAvailable: Double? = null
        ) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val liquid = liquidBalance ?: prefs.getFloat(KEY_LIQUID_BALANCE, 0f).toDouble()
            val credit = creditAvailable ?: prefs.getFloat(KEY_CREDIT_AVAILABLE, 0f).toDouble()

            val views = RemoteViews(context.packageName, R.layout.widget_expense)

            views.setTextViewText(
                R.id.widget_text_liquid_balance,
                Formatters.formatCurrency(liquid)
            )

            views.setTextViewText(
                R.id.widget_text_credit_available,
                Formatters.formatCurrency(credit)
            )

            // Intent to open Main screen when clicking root
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Intent to open Add Transaction dialog directly when clicking "+ Add Expense"
            val addTxIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_OPEN_ADD_TRANSACTION, true)
            }
            val addTxPendingIntent = PendingIntent.getActivity(
                context,
                1,
                addTxIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addTxPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
}
