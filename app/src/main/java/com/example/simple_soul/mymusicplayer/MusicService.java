package com.example.simple_soul.mymusicplayer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener
{
    public static final int BIND = 0x01;
    public static final int START = 0x02;
    public static final int STOP = 0x03;
    public static final int PREVIOUS = 0x04;
    public static final int NEXT = 0x05;
    public static final int LIST = 0x06;
    public static final int CLICK = 0x07;
    public static final int CHANGE = 0x08;
    public static final int DRAG = 0x09;
    public static final int EXIT = 0x0a;
    public static final String ACTION_STATUS = "status";
    public static final String ACTION_PREVIOUS = "previous";
    public static final String ACTION_NEXT = "next";
    public static final String ACTION_EXIT = "exit";

    private MediaPlayer player;
    private Messenger messengerFormActivity;
    private List<Music> musicList;
    private Music currentMusic;
    private int currentId;
    private NotificationUtils utils;
    private MyBroadcastReceiver receiver;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        getMusicList();
        Log.i("main", "onStartCommand");

        registerReceiver();
        //一旦被杀死就不会再重启
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        Log.i("main", "onBind");
        Messenger messenger = new Messenger(mHandler);
        return messenger.getBinder();
    }

    private void getMusicList()
    {
        //获取内存卡音乐列表
        musicList = new ArrayList<>();
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        while (cursor.moveToNext())
        {
            //过滤时长少于一分钟的
            if (cursor.getInt(
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) >= 60000)
            {
                Music music = new Music();
                music.setTitle(cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
                music.setAct(cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                music.setAlbum(cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
                music.setUrl(cursor.getString(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                music.setAlbum_url(getAlbumArt(cursor.getInt(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));
                music.setDuration(cursor.getInt(
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                musicList.add(music);
            }
        }
        //获取本地音乐列表
        Cursor cursor2 = getContentResolver().query(MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        while (cursor2.moveToNext())
        {
            if (cursor2.getInt(
                    cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)) >= 60000)
            {
                Music music = new Music();
                music.setId(
                        cursor2.getInt(cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
                music.setTitle(cursor2.getString(
                        cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)));
                music.setAct(cursor2.getString(
                        cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)));
                music.setAlbum(cursor2.getString(
                        cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)));
                music.setUrl(cursor2.getString(
                        cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)));
                music.setAlbum_url(getAlbumArt(cursor2.getInt(
                        cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))));
                music.setDuration(cursor2.getInt(
                        cursor2.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)));
                musicList.add(music);
            }
        }

        if (musicList != null)
        {
            setCurrentMusic(0);
        }
        else
        {
            Toast.makeText(this, "你咋一首歌都没有呢", Toast.LENGTH_SHORT).show();
        }
    }

    private void setCurrentMusic(int currentId)
    {
        currentMusic = musicList.get(currentId);
        this.currentId = currentId;
    }

    private void initMusic()
    {
        try
        {
            if(player == null)
            {
                player = new MediaPlayer();
                player.setOnCompletionListener(this);

            }
            player.setDataSource(currentMusic.getUrl());
            player.prepare();
            if(utils == null)
            {
                utils = new NotificationUtils(getApplicationContext());
            }
            utils.showNotification(currentMusic);
            utils.pause();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    //进度条控制
    private Runnable runnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (player.isPlaying())
            {
                int currentPosition = player.getCurrentPosition();
                Message message = Message.obtain();
                message.arg1 = currentPosition;
                message.what = CHANGE;
                try
                {
                    messengerFormActivity.send(message);
                }
                catch (RemoteException e)
                {
                    e.printStackTrace();
                }
            }
            if(player != null)
            {
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case BIND:
                    messengerFormActivity = msg.replyTo;
                    //将音乐列表传回去
                    Message message = Message.obtain();
                    message.what = LIST;
                    //将当前音乐和播放状态传回去(考虑activity退出后再进入的情况)
                    if(player != null && player.isPlaying())
                    {
                        message.arg2 = 1;
                    }
                    else
                    {
                        message.arg2 = 0;
                    }
                    message.arg1 = currentId;
                    message.obj = musicList;
                    try
                    {
                        messengerFormActivity.send(message);
                    }
                    catch (RemoteException e)
                    {
                        e.printStackTrace();
                    }
                    //开启进度
                    if (player == null)
                    {
                        initMusic();
                        mHandler.postDelayed(runnable, 0);
                    }
                    break;

                case START:
                    player.start();
                    utils.playing();
                    break;

                case STOP:
                    player.pause();
                    utils.pause();
                    break;

                case PREVIOUS:
                case NEXT:
                case CLICK:
                    player.reset();
                    setCurrentMusic(msg.arg1);
                    initMusic();
                    player.start();
                    utils.showNotification(currentMusic);
                    utils.playing();
                    break;
                //拖动进度条
                case DRAG:
                    player.seekTo(msg.arg1);
                    break;
            }
        }
    };

    private void registerReceiver()
    {
        receiver = new MyBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("status");
        filter.addAction("previous");
        filter.addAction("next");
        filter.addAction("exit");
        registerReceiver(receiver, filter);
    }

    //根据专辑id拿到专辑图片
    private String getAlbumArt(int album_id)
    {
        String mUriAlbums = "content://media/external/audio/albums";
        String[] projection = new String[]{"album_art"};
        Cursor cur = getContentResolver().query(
                Uri.parse(mUriAlbums + "/" + Integer.toString(album_id)), projection, null, null,
                null);
        String album_art = null;
        if (cur.getCount() > 0 && cur.getColumnCount() > 0)
        {
            cur.moveToNext();
            album_art = cur.getString(0);
        }
        cur.close();
        return album_art;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.i("main", "onUnBind");
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy()
    {
        Log.i("main", "onDestroy");
        unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onCompletion(MediaPlayer mp)
    {
        //自动下一首
        Message message = Message.obtain();
        message.what = NEXT;
        if (currentId + 1 < musicList.size())
        {
            currentId++;
        }
        else
        {
            currentId = 0;
        }
        message.arg1 = currentId;
        setCurrentMusic(currentId);
        player.reset();
        initMusic();
        player.start();
        utils.showNotification(currentMusic);
        utils.playing();
        try
        {
            messengerFormActivity.send(message);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Message message = Message.obtain();
            switch (intent.getAction())
            {
                case ACTION_STATUS:
                    Log.i("main", "ACTION_STATUS");
                    if (player.isPlaying())
                    {
                        player.pause();
                        utils.pause();
                        message.what = STOP;
                    }
                    else
                    {
                        player.start();
                        utils.playing();
                        message.what = START;
                    }
                    break;
                case ACTION_NEXT:
                    if (currentId + 1 < musicList.size())
                    {
                        currentId++;
                    }
                    else
                    {
                        currentId = 0;
                    }
                    setCurrentMusic(currentId);
                    message.arg1 = currentId;
                    message.what = MusicService.NEXT;
                    player.reset();
                    initMusic();
                    player.start();
                    utils.showNotification(currentMusic);
                    utils.playing();
                    break;
                case ACTION_PREVIOUS:
                    if (currentId - 1 >= 0)
                    {
                        currentId--;
                    }
                    else
                    {
                        currentId = musicList.size() - 1;
                    }
                    setCurrentMusic(currentId);
                    message.arg1 = currentId;
                    message.what = MusicService.PREVIOUS;
                    player.reset();
                    initMusic();
                    player.start();
                    utils.showNotification(currentMusic);
                    utils.playing();
                    break;
                case ACTION_EXIT:
                    Log.i("main", "ACTION_EXIT");
                    utils.cancel();
                    player.stop();
                    try
                    {
                        player.prepare();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    message.what = EXIT;
                    break;
            }
            try
            {
                messengerFormActivity.send(message);
            }
            catch (RemoteException e)
            {
                e.printStackTrace();
            }
        }
    }
}
