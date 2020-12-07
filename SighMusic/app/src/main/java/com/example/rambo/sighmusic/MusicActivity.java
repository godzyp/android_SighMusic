package com.example.rambo.sighmusic;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.design.widget.NavigationView;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;


import com.example.rambo.sighmusic.bean.Mp3Info;
import com.example.rambo.sighmusic.functions.Subscriber;
import com.example.rambo.sighmusic.service.DownloadService;
import com.example.rambo.sighmusic.service.MusicService;
import com.example.rambo.sighmusic.utility.Constants;
import com.example.rambo.sighmusic.utility.LrcUtil;
import com.example.rambo.sighmusic.utility.MediaUtil;
import com.example.rambo.sighmusic.utility.SpTools;
import com.example.rambo.sighmusic.utility.StatusBarUtil;
import com.example.rambo.sighmusic.view.LrcView;
import com.example.rambo.sighmusic.view.MusicPlayerView;
import com.example.rambo.sighmusic.view.SlidingMenu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MusicActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MusicActivity";
    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1;
    @BindView(R.id.mpv)
    MusicPlayerView mpv;
    @BindView(R.id.main_bg)
    RelativeLayout mainView;
    @BindView(R.id.listview)
    ListView mLeftView;

    //Some thing special!

    @BindView(R.id.streaming)
    EditText mStream;
    @BindView(R.id.done)
    ImageView mUrlDone;
    //END SPECIAL

    @BindView(R.id.next)
    ImageView mNext;
    @BindView(R.id.previous)
    ImageView mPrevious;
    @BindView(R.id.left)
    ImageView mIv_left;
    @BindView(R.id.sm)
    SlidingMenu mSlidingMenu;
    @BindView(R.id.lrc)
    LrcView mCurrentLrc;
    @BindView(R.id.textViewSong)
    TextView mSong;
    @BindView(R.id.textViewSinger)
    TextView mSinger;
    @BindView(R.id.play_mode)
    ImageView mPlayMode;

    //aog
    @BindView(R.id.drawer_layout)
    DrawerLayout drawer;

    private int mPosition;
    private boolean mIsPlaying = false;
    private List<Mp3Info> mMusicList = new ArrayList<>();

    //通知栏
    private RemoteViews remoteViews;

    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private Mp3Info mMp3Info;

    //接受子线程消息
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == Constants.MSG_PROGRESS) {
                int currentPosition = msg.arg1;
                int totalDuration = msg.arg2;
                mpv.setProgress(currentPosition);
                mpv.setMax(totalDuration);
                mCurrentLrc.updateTime(currentPosition);
            }
            if (msg.what == Constants.MSG_PREPARED) {
                mPosition = msg.arg1;
                mIsPlaying = (boolean) msg.obj;
                switchSongUI(mPosition, mIsPlaying);
            }
            if (msg.what == Constants.MSG_PLAY_STATE) {
                mIsPlaying = (boolean) msg.obj;
                refreshPlayStateUI(mIsPlaying);
            }
            if (msg.what == Constants.MSG_CANCEL) {
                mIsPlaying = false;
                finish();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //初始化视图
        initView();

        //黄油刀绑定
        ButterKnife.bind(this);

        initPermission();
        bindService();
        bindService();
        //TODO
    }


    //重写返回键，可以让侧边栏返回
    @Override
    public void onBackPressed() {
            drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            }
        }



    //绑定服务
    private void bindService(){
        Intent intent = new Intent(MusicActivity.this, DownloadService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
    }

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceDisconnected(ComponentName name) {
            System.out.println("--Service Disconnected--");
        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }
    };

    //初始化
    private void init(){
        initData();
        initEvent();
    }

    //一开始的权限管理
    private void initPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 没有权限。
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // 用户拒绝过这个权限了，应该提示用户，为什么需要这个权限。
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
            } else {
                // 申请授权。
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
            }
        } else {
            init();
        }
    }

    //请求权限的结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == MY_PERMISSIONS_READ_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            } else {
                // Permission Denied
                Toast.makeText(MusicActivity.this, "Permission Denied, Music can't run.", Toast.LENGTH_SHORT).show();
                finish();
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //初始化视图
    private void initView() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //状态栏相关
        StatusBarUtil.enableTranslucentStatusbar(this);
        setContentView(R.layout.activity_main);

        //aog
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        remoteViews = new RemoteViews(getPackageName(), R.layout.customnotice);//通知栏布局
        //调用下面“创建通知栏”的方法
        createNotification();
    }

    //初始化音乐数据
    private void initData() {
        //音乐列表
        mMusicList = MediaUtil.getMp3Infos(this);
        //启动音乐服务
        startMusicService();
        //消息管理
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //lsitview创建适配
        mLeftView.setAdapter(new MediaListAdapter());
        //初始化控件UI，默认显示历史播放歌曲
        mPosition = SpTools.getInt(getApplicationContext(), "music_current_position", 0);
        mIsPlaying = MusicService.isPlaying();
        switchSongUI(mPosition, mIsPlaying);
    }

    /**
     * 开始音乐服务并传输数据
     */
    private void startMusicService() {
        Intent musicService = new Intent();
        musicService.setClass(getApplicationContext(), MusicService.class);
        musicService.putParcelableArrayListExtra("music_list", (ArrayList<? extends Parcelable>) mMusicList);
        musicService.putExtra("messenger", new Messenger(handler));
        startService(musicService);
    }

    /**
     * 刷新播放控件的歌名，歌手，图片，按钮的形状
     */
    private void switchSongUI(int position, boolean isPlaying) {
        if (mMusicList.size() > 0 && position < mMusicList.size()) {
            // 1.获取播放数据
            mMp3Info = mMusicList.get(position);
            // 2.设置歌曲名，歌手
            String mSongTitle = mMp3Info.getTitle();
            String mSingerArtist = mMp3Info.getArtist();
            mSong.setText(mSongTitle);
            mSinger.setText(mSingerArtist);
            // 3.更新notification通知栏和播放控件UI
            Bitmap mBitmap = MediaUtil.getArtwork(MusicActivity.this, mMp3Info.getId(), mMp3Info.getAlbumId(), true, false);
            remoteViews.setImageViewBitmap(R.id.widget_album, mBitmap);
            remoteViews.setTextViewText(R.id.widget_title, mMp3Info.getTitle());
            remoteViews.setTextViewText(R.id.widget_artist, mMp3Info.getArtist());
            refreshPlayStateUI(isPlaying);

            // 4.更换音乐背景

            mpv.setCoverBitmap(mBitmap);
            assert mBitmap!=null;
                Palette.from(mBitmap).generate(new Palette.PaletteAsyncListener() {
                    @Override
                    public void onGenerated(Palette p) {
                        int mutedColor = p.getMutedColor(Color.BLACK);
                        Palette.Swatch darkMutedSwatch = p.getDarkMutedSwatch();
                        mainView.setBackgroundColor(darkMutedSwatch != null ? darkMutedSwatch.getRgb() : mutedColor);
                        mLeftView.setBackgroundColor(darkMutedSwatch != null ? darkMutedSwatch.getRgb() : mutedColor);

                    }
                });

            // 5.设置歌词
            File mFile = MediaUtil.getLrcFile(mMp3Info.getUrl());
            if (mFile != null) {
                Log.i(TAG, "switchSongUI: mFile != null");
                try {
                    BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(new FileInputStream(mFile)));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = inputStreamReader.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    mCurrentLrc.loadLrc(sb.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LrcUtil.getMusicLrc(mMp3Info.getTitle(), mMp3Info.getArtist(), new Subscriber<String>() {
                    @Override
                    public void onComplete(String s) {
                        mCurrentLrc.loadLrc(s);
                        //保存歌词到本地
                        File file = new File(mMp3Info.getUrl().replace(".mp3", ".lrc"));
                        FileOutputStream fileOutputStream;
                        try {
                            fileOutputStream = new FileOutputStream(file);
                            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
                            outputStreamWriter.write(s);
                            outputStreamWriter.close();
                            fileOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mCurrentLrc.reset();
                            }
                        });
                    }
                });
            }
            // 6.选中左侧播放中的歌曲颜色
            changeColorNormalPrv();
            changeColorSelected();
        } else {
            Toast.makeText(this, "当前没有音乐，记得去下载再来。", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 刷新播放控件及通知
     */
    private void refreshPlayStateUI(boolean isPlaying) {
        updateMpv(isPlaying);
        updateNotification();
    }

    /**
     * 更新播放控件
     */
    private void updateMpv(boolean isPlaying) {
        // content播放控件
        if (isPlaying) {
            mpv.start();
        } else {
            mpv.stop();
        }

    }

    /**
     * 更新通知栏UI
     */
    private void updateNotification() {
        Intent intent_play_pause;
        // 创建并设置通知栏
        if (mIsPlaying) {
            remoteViews.setImageViewResource(R.id.widget_play, R.drawable.widget_play);
        } else {
            remoteViews.setImageViewResource(R.id.widget_play, R.drawable.widget_pause);
        }
        // 设置播放
        if (mIsPlaying) {//如果正在播放——》暂停
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PAUSE);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 4, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        if (!mIsPlaying) {//如果暂停——》播放
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PLAY);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 5, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        mNotificationManager.notify(Constants.NOTIFICATION_CEDE, mBuilder.build());
    }

    /**
     * 创建通知栏
     */
    @SuppressLint("NewApi")
    private void createNotification() {
        mBuilder = new NotificationCompat.Builder(this);
        // 点击跳转到主界面
        Intent intent_main = new Intent(this, MusicActivity.class);
        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);  //得到返回栈
        //stackBuilder.addParentStack(MusicActivity.class);  //向返回栈中压入activity，这里注意不是压入的父activity，而是点击通知启动的activity
        //stackBuilder.addNextIntent(intent_main);
        PendingIntent pending_intent_go = PendingIntent.getActivity(this, 1, intent_main, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notice, pending_intent_go);

        // 4个参数context, requestCode, intent, flags
        Intent intent_cancel = new Intent();
        intent_cancel.setAction(Constants.ACTION_CLOSE);
        PendingIntent pending_intent_close = PendingIntent.getBroadcast(this, 2, intent_cancel, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_close, pending_intent_close);

        // 上一曲
        Intent intent_prv = new Intent();
        intent_prv.setAction(Constants.ACTION_PRV);
        PendingIntent pending_intent_prev = PendingIntent.getBroadcast(this, 3, intent_prv, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_prev, pending_intent_prev);

        // 设置播放暂停
        Intent intent_play_pause;
        if (mIsPlaying) {//如果正在播放——》暂停
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PAUSE);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 4, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        if (!mIsPlaying) {//如果暂停——》播放
            intent_play_pause = new Intent();
            intent_play_pause.setAction(Constants.ACTION_PLAY);
            PendingIntent pending_intent_play = PendingIntent.getBroadcast(this, 5, intent_play_pause, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(R.id.widget_play, pending_intent_play);
        }
        // 下一曲
        Intent intent_next = new Intent();
        intent_next.setAction(Constants.ACTION_NEXT);
        PendingIntent pending_intent_next = PendingIntent.getBroadcast(this, 6, intent_next, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.widget_next, pending_intent_next);

        mBuilder.setSmallIcon(R.mipmap.ic_launcher); // 设置顶部图标（状态栏）
        mBuilder.setContent(remoteViews);
        mBuilder.setOngoing(true);
    }

    //一开始时注册点击事件
    private void initEvent() {
        mIv_left.setOnClickListener(this);
        mpv.setOnClickListener(this);
        mPrevious.setOnClickListener(this);
        mPlayMode.setOnClickListener(this);
        mNext.setOnClickListener(this);
        mLeftView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //点击左侧菜单
                changeColorNormal();
                sendBroadcast(Constants.ACTION_LIST_ITEM, i);
                //TODO YB的滑动，貌似没用，考虑干掉
                //mSlidingMenu.switchMenu(false);
            }
        });
    }


    //点击事件效果
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.left://切换左右布局

                drawer.openDrawer(GravityCompat.START);;
                break;
            case R.id.mpv://自定义播放控件，点击播放或暂停
                if (mIsPlaying) {
                    sendBroadcast(Constants.ACTION_PAUSE);
                } else {
                    sendBroadcast(Constants.ACTION_PLAY);
                }
                break;
            case R.id.previous://上一首
                sendBroadcast(Constants.ACTION_PRV);
                break;
            case R.id.play_mode://切换播放模式
                MusicService.playMode++;
                switch (MusicService.playMode % 3) {
                    case 0://随机播放
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_shuffle_normal);
                        break;
                    case 1://单曲循环
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_loopsingle_normal);
                        break;
                    case 2://列表播放
                        mPlayMode.setImageResource(R.drawable.player_btn_mode_playall_normal);
                        break;
                }
                break;
            case R.id.next://下一首
                sendBroadcast(Constants.ACTION_NEXT);
                break;
        }
    }

    //发送广播
    private void sendBroadcast(String action) {
        Intent intent = new Intent();
        intent.setAction(action);
        sendBroadcast(intent);
    }
    //发送广播2
    private void sendBroadcast(String action, int position) {
        Intent intent = new Intent();
        intent.putExtra("position", position);
        intent.setAction(action);
        sendBroadcast(intent);
    }

    /**
     * 左侧音乐列表适配器
     */
    private class MediaListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMusicList.size();
        }

        @Override
        public Object getItem(int position) {
            return mMusicList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(MusicActivity.this, R.layout.music_listitem, null);
                holder.mImgAlbum = (ImageView) convertView.findViewById(R.id.img_album);
                holder.mTvTitle = (TextView) convertView.findViewById(R.id.tv_title);
                holder.mTvArtist = (TextView) convertView.findViewById(R.id.tv_artist);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.mImgAlbum.setImageBitmap(MediaUtil.getArtwork(MusicActivity.this, mMusicList.get(position).getId(), mMusicList.get(position).getAlbumId(), true, true));
            holder.mTvTitle.setText(mMusicList.get(position).getTitle());
            holder.mTvArtist.setText(mMusicList.get(position).getArtist());

            if (mPosition == position) {
                holder.mTvTitle.setTextColor(getResources().getColor(R.color.colorAccent));
            } else {
                holder.mTvTitle.setTextColor(getResources().getColor(R.color.colorNormal));
            }
            holder.mTvTitle.setTag(position);

            return convertView;
        }
    }
    //内部类
    private static class ViewHolder {
        ImageView mImgAlbum;
        TextView mTvTitle;
        TextView mTvArtist;
    }
    //改变颜色
    public void changeColorNormal() {
        TextView tv = (TextView) mLeftView.findViewWithTag(mPosition);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorNormal));
        }
    }
    //改变颜色
    public void changeColorNormalPrv() {
        TextView tv = (TextView) mLeftView.findViewWithTag(MusicService.prv_position);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorNormal));
        }
    }
    //改变颜色
    public void changeColorSelected() {
        TextView tv = (TextView) mLeftView.findViewWithTag(mPosition);
        if (tv != null) {
            tv.setTextColor(getResources().getColor(R.color.colorAccent));
        }
    }

    //退出的时候把歌曲位置存储，但只有在关闭通知栏的时候才有用
    @Override
    protected void onDestroy() {
        super.onDestroy();
        SpTools.setInt(getApplicationContext(), "music_current_position", mPosition);
        unbindService(conn);
    }

    //TODO finish download func with intent service
    public void streamToggled(View view){
        startDownloadService();
    }

    //开始下载服务
    private void startDownloadService(){
        String targetUrl = mStream.getText().toString();
        Intent intent = new Intent(this, DownloadService.class);
        intent.putExtra("target",targetUrl);
        startService(intent);

    }

}
