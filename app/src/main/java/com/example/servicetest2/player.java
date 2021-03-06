package com.example.servicetest2;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

public class player extends AppCompatActivity implements View.OnClickListener{
    private TextView musicLength,musicCur;
    private SeekBar seekBar;
    private Timer timer;
    private boolean isSeekBarChanging;//互斥变量，防止进度条与定时器冲突。
    private int currentPosition;//当前音乐播放的进度
    SimpleDateFormat format;

    private MusicService.MusicBinder musicBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicBinder = (MusicService.MusicBinder) service;
            musicBinder.initmediaplayer(MusicQueue.i);
            seekBar.setMax(musicBinder.mediaPlayer.getDuration());
            musicLength.setText(format.format(musicBinder.mediaPlayer.getDuration())+"");
            musicCur.setText("00:00");
            timer = new Timer();
            timer.schedule(new TimerTask() {

                Runnable updateUI = new Runnable() {
                    @Override
                    public void run() {
                        musicCur.setText(format.format(musicBinder.mediaPlayer.getCurrentPosition())+"");
                    }
                };
                @Override
                public void run() {
                    if(!isSeekBarChanging){
                        seekBar.setProgress(musicBinder.mediaPlayer.getCurrentPosition());
                        runOnUiThread(updateUI);
                    }
                }
            },0,50);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        format = new SimpleDateFormat("mm:ss");

        Button play = (Button) findViewById(R.id.play);
        Button pause = (Button) findViewById(R.id.pause);
        Button stop = (Button) findViewById(R.id.stop);
        Button next = (Button) findViewById(R.id.next);

        musicLength = (TextView) findViewById(R.id.music_length);
        musicCur = (TextView) findViewById(R.id.music_cur);

        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new MySeekBar());

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);
        next.setOnClickListener(this);

        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);


    }


    @Override
    public void onClick(View v) {
        if (musicBinder.musicurl == ""){ //如果当前播放器为空或者与要播放的音乐不一直，初始化播放器
            musicBinder.initmediaplayer(MusicQueue.i);
            seekBar.setMax(musicBinder.mediaPlayer.getDuration());
            musicLength.setText(format.format(musicBinder.mediaPlayer.getDuration())+"");
            musicCur.setText("00:00");
        }
        switch (v.getId()){
            case R.id.play:
                musicBinder.play();
                //监听播放时回调函数

                break;
            case R.id.pause:
                musicBinder.pause();
                break;
            case R.id.stop:
                musicBinder.stop();
                break;
            case R.id.next:   //上一首同理即可，后续要增加一下判断是否为最后一个或第一个
                musicBinder.stop();
                musicBinder.initmediaplayer(MusicQueue.i+1);
                musicBinder.play();
                break;

            default:
                break;
        }
    }
    //进度条事件处理
    public class MySeekBar implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        /*滚动时,应当暂停后台定时器*/
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = true;
        }
        /*滑动结束后，重新设置值*/
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeekBarChanging = false;
            musicBinder.mediaPlayer.seekTo(seekBar.getProgress());
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}
