package baishuai.github.io.smsforward.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Telephony
import android.telephony.TelephonyManager
import baishuai.github.io.smsforward.IApp
import baishuai.github.io.smsforward.R
import baishuai.github.io.smsforward.forward.ForwardRepo
import baishuai.github.io.smsforward.ui.MainActivity
import baishuai.github.io.smsforward.ui.MainFragment
import baishuai.github.io.smsforward.ui.PrefsFragment
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by bai on 17-4-29.
 */


class ForwardService : Service() {

    @Inject
    lateinit var forward: ForwardRepo

    @Inject
    lateinit var receiver: InSmsListener

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate")
        IApp[this].applicationComponent.inject(this)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Timber.d("onStartCommand " + intent.action)
        when (intent.action) {
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                forward.forwardSms(Telephony.Sms.Intents.getMessagesFromIntent(intent))
            }
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> {
                forward.forwardMissCall(intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER))
            }
            MainFragment.REGISTER_RECEIVER -> {
                val notificationIntent = Intent(this, MainActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
                val notification = Notification.Builder(this)
                        .setContentTitle(getString(R.string.forward_service))
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setTicker(getString(R.string.forward_service))
                        .setContentIntent(pendingIntent)
                        .build()
                startForeground(1, notification)

                val filter = IntentFilter()
                filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
                filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
                registerReceiver(receiver, filter)
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(MainFragment.REGISTER_RECEIVER, true)
                        .apply()
                Timber.d("registerReceiver")
            }
            MainFragment.UNREGISTER_RECEIVER -> {
                stopForeground(true)
                unregisterReceiver(receiver)
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putBoolean(MainFragment.REGISTER_RECEIVER, false)
                        .apply()
                Timber.d("unregisterReceiver")
                stopSelf()
            }
            PrefsFragment.UPDATE_REPOS -> {
                forward.updateRepos()
            }
        }
        return START_REDELIVER_INTENT
    }

}