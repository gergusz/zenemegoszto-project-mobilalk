package hu.szte.gergusz.musicshare.services;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.model.Music;

public class MusicNotificationManager {
    private static final String CHANNEL_ID = "media_playback_channel";
    private Context context;

    public MusicNotificationManager(Context context) {
        this.context = context;
        createNotificationChannel();
    }

    public NotificationManagerCompat getNotificationManager() {
        return NotificationManagerCompat.from(context);
    }

    private void createNotificationChannel() {
        CharSequence name = "Media Playback";
        String description = "Notifications for media playback";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
    public void showNotification(Music music) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permission required to show notifications", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        Intent playIntent = new Intent(context, MusicService.class);
        playIntent.setAction(MusicService.ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(context, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(context, MusicService.class);
        pauseIntent.setAction(MusicService.ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(context, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(context, MusicService.class);
        stopIntent.setAction(MusicService.ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(music.getTitle())
                .setContentText(music.getArtist())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .addAction(R.drawable.baseline_play_arrow_64, "Play", playPendingIntent)
                .addAction(R.drawable.baseline_pause_64, "Pause", pausePendingIntent)
                .addAction(R.drawable.baseline_stop_64, "Stop", stopPendingIntent)
                .setSmallIcon(R.drawable.baseline_library_music_64);

        if (music.getAlbumArtBytes() != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(music.getAlbumArtBytes(), 0, music.getAlbumArtBytes().length);
            builder.setLargeIcon(bitmap);
        } else if (music.getAlbumArtUri() != null) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(context, music.getAlbumArtUri());
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    builder.setLargeIcon(bitmap);
                }
                retriever.release();
            } catch (Exception e) {
                Log.e("MediaNotification", "Error retrieving album art: ", e);
            }
        }

        Notification notification = builder.build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, notification);
    }
}