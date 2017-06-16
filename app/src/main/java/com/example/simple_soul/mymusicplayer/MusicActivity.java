package com.example.simple_soul.mymusicplayer;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/**
 * Created by simple-soul on 17-4-2.
 */

public class MusicActivity extends BaseActivity implements ServiceConnection, View.OnClickListener, AdapterView.OnItemClickListener, SeekBar.OnSeekBarChangeListener
{
    private Button previous, status, next;
    private ListView listView;
    private TextView title, now, duration;
    private SeekBar seekBar;
    private LinearLayout layout;

    private List<Music> musicList;
    private MyMusicAdapter adapter;
    private Music currentMusic;
    private int currentId;
    private Messenger messengerFormService;
    private boolean isPlaying = false;

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 200;
    private Intent intent;

    @Override
    public View initView()
    {
        View view = View.inflate(this, R.layout.activity_music, null);

        previous = (Button) view.findViewById(R.id.music_btn_previous);
        status = (Button) view.findViewById(R.id.music_btn_status);
        next = (Button) view.findViewById(R.id.music_btn_next);
        listView = (ListView) view.findViewById(R.id.music_list);
        title = (TextView) view.findViewById(R.id.music_tv_title);
        now = (TextView) view.findViewById(R.id.music_tv_now);
        duration = (TextView) view.findViewById(R.id.music_tv_duration);
        seekBar = (SeekBar) view.findViewById(R.id.music_seek);
        layout = (LinearLayout) view.findViewById(R.id.music_lLayout);

        previous.setOnClickListener(this);
        status.setOnClickListener(this);
        next.setOnClickListener(this);
        listView.setOnItemClickListener(this);
        seekBar.setOnSeekBarChangeListener(this);

        return view;
    }

    @Override
    public void initData()
    {
        // 版本判断。当手机系统大于 23 时，才有必要去判断权限是否获取.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (checkSelfPermission(
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("main", "requestPermissions");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            }
            else
            {
                Log.i("main", "已有权限");
                startService();
            }
        }
        else
        {
            Log.i("main", "6.0以下");
            startService();
        }
    }

    private void startService()
    {
        //开启服务
        intent = new Intent(this, MusicService.class);
        if (!isServiceWork(this, "com.example.simple_soul.mymusicplayer.MusicService"))
        {
            startService(intent);
        }
        bindService(intent, this, BIND_AUTO_CREATE);
    }

    private void setCurrentMusic(int currentId)
    {
        currentMusic = musicList.get(currentId);
        this.currentId = currentId;
        title.setText(currentMusic.getTitle());
        String time = String.format("%02d:%02d", currentMusic.getDuration() / 1000 / 60,
                currentMusic.getDuration() / 1000 % 60);
        duration.setText(time);
        seekBar.setMax(currentMusic.getDuration());
        if (currentMusic.getAlbum_url() != null)
        {
            Drawable drawable = Drawable.createFromPath(currentMusic.getAlbum_url());
            layout.setBackground(drawable);
            layout.getBackground().setAlpha(125);
        }
    }

    @Override //绑定service成功
    public void onServiceConnected(ComponentName name, IBinder binder)
    {
        messengerFormService = new Messenger(binder);
        Messenger messenger = new Messenger(mHandler);
        Message message = Message.obtain();
        message.what = MusicService.BIND;
        message.replyTo = messenger;
        try
        {
            messengerFormService.send(message);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
    }

    @Override //绑定service失败
    public void onServiceDisconnected(ComponentName name) {}

    //接收service传输的数据
    private Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                //从service传递来的音乐列表
                case MusicService.LIST:
                    musicList = (List<Music>) msg.obj;
                    currentId = msg.arg1;
                    adapter = new MyMusicAdapter(MusicActivity.this, musicList);
                    listView.setAdapter(adapter);

                    setCurrentMusic(currentId);
                    //当前后台正在播放音乐
                    if(msg.arg2 == 1)
                    {
                        isPlaying = true;
                        status.setText("停止");
                    }
                    break;
                //自动切换下一首或notification点击
                case MusicService.PREVIOUS:
                case MusicService.NEXT:
                    setCurrentMusic(msg.arg1);
                    isPlaying = true;
                    status.setText("停止");
                    break;
                //更新进度条
                case MusicService.CHANGE:
                    seekBar.setProgress(msg.arg1);
                    String time = String.format("%02d:%02d", msg.arg1 / 1000 / 60,
                            msg.arg1 / 1000 % 60);
                    now.setText(time);
                    break;
                case MusicService.START:
                    isPlaying = true;
                    status.setText("停止");
                    break;
                case MusicService.STOP:
                    isPlaying = false;
                    status.setText("开始");
                    break;
                case MusicService.EXIT:
                    Log.i("main", "MusicService.EXIT");
                    isPlaying = false;
                    seekBar.setProgress(0);
                    status.setText("开始");
                    now.setText("00:00");
                    break;
            }
        }
    };

    @Override
    public void onClick(View v)
    {
        Message message = Message.obtain();
        switch (v.getId())
        {
            case R.id.music_btn_previous:
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
                isPlaying = true;
                status.setText("停止");
                break;

            case R.id.music_btn_status:

                if (isPlaying)
                {
                    isPlaying = false;
                    message.what = MusicService.STOP;
                    status.setText("开始");

                }
                else
                {
                    isPlaying = true;
                    if(!isServiceWork(this, "com.example.simple_soul.mymusicplayer.MusicService"))
                    {
                        startService(intent);
                        bindService(intent, this, BIND_AUTO_CREATE);
                    }
                    message.what = MusicService.START;
                    status.setText("停止");
                }

                break;

            case R.id.music_btn_next:
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
                isPlaying = true;
                status.setText("停止");
                break;
        }
        try
        {
            messengerFormService.send(message);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
    }

    @Override //listView点击事件
    public void onItemClick(AdapterView<?> parent, View view, int position, long id)
    {
        //告诉service点击了哪一首歌
        Message message = Message.obtain();
        message.what = MusicService.CLICK;
        message.arg1 = position;
        try
        {
            messengerFormService.send(message);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
        //更改当前页面的显示信息
        isPlaying = true;
        status.setText("停止");
        setCurrentMusic(position);
    }

    @Override //当seekBar改变时
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    @Override //当开始拖动进度条时
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override //当松手时
    public void onStopTrackingTouch(SeekBar seekBar)
    {
        //告诉service改变歌曲进度
        Message message = Message.obtain();
        message.what = MusicService.DRAG;
        message.arg1 = seekBar.getProgress();
        try
        {
            messengerFormService.send(message);
        }
        catch (RemoteException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case REQUEST_CODE_ASK_PERMISSIONS:
            {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Toast.makeText(this, "已获取权限", Toast.LENGTH_SHORT).show();
                    Log.i("main", "已获取权限");
                    startService();
                }
                else
                {
                    Toast.makeText(this, "未获取权限", Toast.LENGTH_SHORT).show();
                    Log.i("main", "未获取权限");
                }
                return;
            }

        }
    }

    public boolean isServiceWork(Context mContext, String serviceName)
    {
        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = myAM.getRunningServices(40);
        if (myList.size() <= 0)
        {
            return false;
        }
        for (int i = 0; i < myList.size(); i++)
        {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName))
            {
                isWork = true;
                break;
            }
        }
        return isWork;
    }
}
