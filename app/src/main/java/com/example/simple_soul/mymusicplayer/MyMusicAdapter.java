package com.example.simple_soul.mymusicplayer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by simple_soul on 2017/4/5.
 */

public class MyMusicAdapter extends BaseAdapter
{
    private List<Music> musicList;
    private Context context;

    public MyMusicAdapter(Context context, List<Music> musicList)
    {
        this.context = context;
        this.musicList = musicList;
    }

    @Override
    public int getCount()
    {
        return musicList.size();
    }

    @Override
    public Object getItem(int position)
    {
        return musicList.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        convertView = View.inflate(context, R.layout.item_list_music, null);
        TextView title = (TextView) convertView.findViewById(R.id.item_music_tv_title);
        TextView actor = (TextView) convertView.findViewById(R.id.item_music_tv_act);
        TextView duration = (TextView) convertView.findViewById(R.id.item_music_tv_duration);

        Music music = musicList.get(position);
        title.setText(music.getTitle());
        actor.setText(music.getAct()+" - "+music.getAlbum());
        String time = String.format("%02d:%02d", music.getDuration()/1000/60, music.getDuration()/1000%60);
        duration.setText(time);

        return convertView;
    }
}
