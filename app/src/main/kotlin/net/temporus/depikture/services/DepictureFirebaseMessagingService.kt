package net.temporus.depikture.services

import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import net.temporus.depikture.objects.Lobby


class DepictureFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Log.d(TAG, "From: " + remoteMessage!!.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            val remoteData: Map<String, String> = remoteMessage.data
            val lobby: Lobby = Gson().fromJson(remoteData["lobby"], Lobby::class.java)
            val extra: String = remoteData["extra"].toString()

            val intent = Intent("DFMS")
            intent.putExtra("header", "Lobby")
            intent.putExtra("lobby", lobby)
            intent.putExtra("extra", extra)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.notification!!.body!!)
        }
    }

    companion object {
        private val TAG = "Main"
    }


}
