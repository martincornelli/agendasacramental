package com.example.agendasacramental

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class AgendaNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "AgendaWorker"
        const val CHANNEL_ID = "agenda_sacramental_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "agenda_notification_worker"

        fun programar(context: Context) {
            val prefs = context.getSharedPreferences(NotifPrefs.PREFS_NAME, Context.MODE_PRIVATE)
            val habilitada = prefs.getBoolean(NotifPrefs.KEY_HABILITADA, false)
            Log.d(TAG, "programar() habilitada=$habilitada")

            if (!habilitada) {
                WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
                Log.d(TAG, "programar() Worker cancelado")
                return
            }

            val hora = prefs.getInt(NotifPrefs.KEY_HORA, 9)
            val minuto = prefs.getInt(NotifPrefs.KEY_MINUTO, 0)
            val unidad = prefs.getString(NotifPrefs.KEY_UNIDAD, "") ?: ""
            Log.d(TAG, "programar() hora=$hora:$minuto unidad=$unidad")

            val ahora = Calendar.getInstance()
            val proxima = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hora)
                set(Calendar.MINUTE, minuto)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(ahora)) add(Calendar.DAY_OF_MONTH, 1)
            }
            val delayMs = proxima.timeInMillis - ahora.timeInMillis
            Log.d(TAG, "programar() delay=${delayMs / 1000 / 60} minutos, próxima=${proxima.time}")

            val request = PeriodicWorkRequestBuilder<AgendaNotificationWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "programar() Worker encolado correctamente")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "doWork() iniciado")

        val prefs = applicationContext.getSharedPreferences(NotifPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val habilitada = prefs.getBoolean(NotifPrefs.KEY_HABILITADA, false)
        Log.d(TAG, "doWork() habilitada=$habilitada")
        if (!habilitada) return Result.success()

        val diasAntes = prefs.getInt(NotifPrefs.KEY_DIAS_ANTES, 2)
        val numeroUnidad = prefs.getString(NotifPrefs.KEY_UNIDAD, "") ?: ""
        Log.d(TAG, "doWork() diasAntes=$diasAntes unidad=$numeroUnidad")

        if (numeroUnidad.isBlank()) {
            Log.d(TAG, "doWork() unidad vacía — saliendo")
            return Result.success()
        }

        // Verificar día de la semana
        val hoy = Calendar.getInstance()
        val diaSemana = hoy.get(Calendar.DAY_OF_WEEK)
        val diaObjetivo = diasAntesADiaCalendar(diasAntes)
        Log.d(TAG, "doWork() diaSemana=$diaSemana diaObjetivo=$diaObjetivo")

        if (diaSemana != diaObjetivo) {
            Log.d(TAG, "doWork() no es el día correcto — saliendo")
            return Result.success()
        }

        // Leer datos del próximo domingo desde SharedPreferences
        val estado = prefs.getString("notif_agenda_estado", null)
        val preside = prefs.getString("notif_agenda_preside", "") ?: ""
        val fechaMs = prefs.getLong("notif_agenda_fecha", -1L)
        val agendaId = prefs.getString("notif_agenda_id", "") ?: ""
        Log.d(TAG, "doWork() agenda en prefs: estado=$estado fechaMs=$fechaMs agendaId=$agendaId")

        if (estado == null || fechaMs == -1L) {
            Log.d(TAG, "doWork() no hay agenda guardada en prefs — saliendo")
            return Result.success()
        }

        // Verificar que la fecha es del próximo domingo
        val proximoDomingo = Calendar.getInstance().apply {
            time = hoy.time
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val domingoFin = Calendar.getInstance().apply {
            time = proximoDomingo.time
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        }

        val fechaAgenda = Date(fechaMs)
        Log.d(TAG, "doWork() fecha agenda: $fechaAgenda rango: ${proximoDomingo.time} - ${domingoFin.time}")

        if (fechaAgenda.before(proximoDomingo.time) || fechaAgenda.after(domingoFin.time)) {
            Log.d(TAG, "doWork() la agenda guardada no es del próximo domingo — saliendo")
            return Result.success()
        }

        Log.d(TAG, "doWork() mostrando notificación estado=$estado preside=$preside")
        mostrarNotificacion(estado, fechaMs, agendaId)
        return Result.success()
    }

    private fun mostrarNotificacion(estado: String, fechaMs: Long, agendaId: String) {
        Log.d(TAG, "mostrarNotificacion() iniciado")
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Agenda Sacramental",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Recordatorio de reunión sacramental" }
            manager.createNotificationChannel(channel)
        }

        if (!manager.areNotificationsEnabled()) {
            Log.w(TAG, "mostrarNotificacion() notificaciones deshabilitadas en el sistema")
            return
        }

        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("open_agenda_id", agendaId)
            }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fecha = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date(fechaMs))
        val confirmada = estado == "CONFIRMADA"
        val localizedContext = LocaleHelper.aplicarIdiomaAContexto(applicationContext, LocaleHelper.getIdiomaGuardado(applicationContext))
        val body = localizedContext.getString(if (confirmada) R.string.notif_push_confirmada else R.string.notif_push_no_confirmada, fecha)

        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(localizedContext.getString(R.string.app_name))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notif)
        Log.d(TAG, "mostrarNotificacion() notificación enviada exitosamente")
    }
}
