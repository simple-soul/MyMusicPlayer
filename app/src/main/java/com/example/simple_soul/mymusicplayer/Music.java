package com.example.simple_soul.mymusicplayer;

/**
 * Created by simple_soul on 2017/4/5.
 */

public class Music
{
    private int id;
    private String title;
    private String act;
    private String album;
    private String album_url;
    private int duration;
    private String url;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getAct()
    {
        return act;
    }

    public void setAct(String act)
    {
        this.act = act;
    }

    public String getAlbum()
    {
        return album;
    }

    public void setAlbum(String album)
    {
        this.album = album;
    }

    public String getAlbum_url()
    {
        return album_url;
    }

    public void setAlbum_url(String album_url)
    {
        this.album_url = album_url;
    }

    public int getDuration()
    {
        return duration;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }
}
