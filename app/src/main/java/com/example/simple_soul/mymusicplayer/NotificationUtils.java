package com.example.simple_soul.mymusicplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.widget.RemoteViews;

import java.util.Map;

/**
 * Created by simple_soul on 2017/4/5.
 */
public class NotificationUtils
{
    private Context context;
    private NotificationManager manager;
    private Map<Integer, Notification> map;
    private RemoteViews remoteViews;
    private Notification notification;

    public NotificationUtils(Context context)
    {
        this.context = context;
        manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public void showNotification(Music music)
    {
        if (notification == null)
        {
            notification = new Notification();

            notification.icon = R.drawable.music;
            notification.flags = Notification.FLAG_INSISTENT;

            //notification整体的点击事件
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(new ComponentName(context,
                    MusicActivity.class));//用ComponentName得到class对象
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);// 关键的一步，设置启动模式，两种情况
            PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notification.contentIntent = pIntent;

            remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.item_notification_music);
            //下一首
            Intent intent2 = new Intent(MusicService.ACTION_NEXT);
            PendingIntent pIntent2 = PendingIntent.getBroadcast(context, 1, intent2,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.item_not_image_next, pIntent2);
            //上一首
            Intent intent3 = new Intent(MusicService.ACTION_PREVIOUS);
            PendingIntent pIntent3 = PendingIntent.getBroadcast(context, 2, intent3,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.item_not_image_previous, pIntent3);
            //切换状态
            Intent intent4 = new Intent(MusicService.ACTION_STATUS);
            PendingIntent pIntent4 = PendingIntent.getBroadcast(context, 3, intent4,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.item_not_image_status, pIntent4);
            //关闭
            Intent intent5 = new Intent(MusicService.ACTION_EXIT);
            PendingIntent pIntent5 = PendingIntent.getBroadcast(context, 4, intent5,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.item_not_tv_exit, pIntent5);
        }
        remoteViews.setTextViewText(R.id.item_not_tv_title, music.getTitle());
        remoteViews.setTextViewText(R.id.item_not_tv_act,
                music.getAct() + " - " + music.getAlbum());
        remoteViews.setImageViewBitmap(R.id.item_not_image,
                BitmapFactory.decodeFile(music.getAlbum_url()));

        notification.contentView = remoteViews;

        manager.notify(1, notification);

    }

    public void cancel()
    {
        manager.cancelAll();
    }

    public void playing()
    {
        remoteViews.setImageViewResource(R.id.item_not_image_status, R.drawable.pause);
        manager.notify(1, notification);
    }

    public void pause()
    {
        remoteViews.setImageViewResource(R.id.item_not_image_status, R.drawable.play);
        manager.notify(1, notification);
    }


}
