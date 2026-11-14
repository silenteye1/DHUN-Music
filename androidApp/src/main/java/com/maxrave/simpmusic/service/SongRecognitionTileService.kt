package com.maxrave.simpmusic.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.maxrave.simpmusic.MainActivity
import com.maxrave.simpmusic.utils.SongRecognitionTrigger

class SongRecognitionTileService : TileService() {

    companion object {
        const val ACTION_RECOGNIZE_SONG = "com.maxrave.simpmusic.ACTION_RECOGNIZE_SONG"
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Identify Song"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                subtitle = "Dhun Music"
            }
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        SongRecognitionTrigger.requestListen()

        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_RECOGNIZE_SONG
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}