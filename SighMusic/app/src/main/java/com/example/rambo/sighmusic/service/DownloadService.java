package com.example.rambo.sighmusic.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.rambo.sighmusic.MusicActivity;
import com.example.rambo.sighmusic.R;
import com.example.rambo.sighmusic.view.MusicPlayerView;
import com.example.rambo.sighmusic.view.SlidingMenu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static android.content.ContentValues.TAG;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class DownloadService extends IntentService {
    private NotificationCompat.Builder builder;
    private NotificationManager manager;
    private Handler handler = new Handler();
    private int downloadLength = 0;
    private int fileLength = 0;
    private static final int NOTIFICATION_ID = 1288;



    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_FOO = "com.example.rambo.sighmusic.service.action.FOO";
    private static final String ACTION_BAZ = "com.example.rambo.sighmusic.service.action.BAZ";

    // TODO: Rename parameters
    private static final String EXTRA_PARAM1 = "com.example.rambo.sighmusic.service.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.example.rambo.sighmusic.service.extra.PARAM2";

    public DownloadService() {
        super("DownloadService");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionFoo(Context context, String param1, String param2) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_FOO);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionBaz(Context context, String param1, String param2) {
        Intent intent = new Intent(context, DownloadService.class);
        intent.setAction(ACTION_BAZ);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    //调用worker线程来处理工作，每次只处理一个intent
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null){
            initNotification();
            Bundle bundle = intent.getExtras();
            String url = bundle.getString("target");
            manager.notify(NOTIFICATION_ID,builder.build());
            downloadFile(url);
            //已经执行完下载
            builder.setProgress(0,0,false);
            builder.setContentText("Successfully Downloaded");
            manager.notify(NOTIFICATION_ID, builder.build());
            //刷新媒体库
            updateMusic(Environment.getExternalStorageDirectory()
                            .getAbsolutePath() + "/SighMusic/MusicDownload"+fileNameGene(url));

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //TODO refreshing audio base
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionFoo(String param1, String param2) {
        // TODO: Handle action Foo
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }



    private Runnable run = new Runnable() {
        public void run() {
            int pec=(int) (downloadLength*100 / fileLength);
            builder.setContentText("正在下载: "+pec+" %");
            builder.setProgress(100, pec, false);//显示进度条，参数分别是最大值、当前值、是否显示具体进度（false显示具体进度，true就只显示一个滚动色带）
            manager.notify(NOTIFICATION_ID,builder.build());
            handler.postDelayed(run, 1000);
        }
    };
    @Override
    public void onDestroy(){
        handler.removeCallbacks(run);
        super.onDestroy();
    }

    //下载文件
    private void downloadFile(String downloadUrl) {
        File dirs = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/SighMusic/MusicDownload");
        if (!dirs.exists()){
            dirs.mkdirs();
        }
        File file = new File(dirs,fileNameGene(downloadUrl));
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "No Such Folder！");
            e.printStackTrace();
            return;
        }
        InputStream inputStream = null;
        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection downLoadCon = (HttpURLConnection) url.openConnection();
            downLoadCon.setRequestMethod("GET");
            fileLength = Integer.valueOf(downLoadCon.getHeaderField("Content-Length"));//文件大小
            inputStream = downLoadCon.getInputStream();
            int respondCode = downLoadCon.getResponseCode();//服务器返回的响应码
            if (respondCode == 200) {
                byte[] buffer = new byte[1024 * 8];// 数据块，等下把读取到的数据储存在这个数组，这个东西的大小看需要定，不要太小。
                handler.post(run);
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                     downloadLength = downloadLength + len;
                    Log.d(TAG, downloadLength + "/" + fileLength);
                }
            } else {
                Log.d(TAG, "respondCode:" + respondCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                    //解决YB的bug
                    downloadLength=0;
                    handler.removeCallbacks(run);


                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    //文件名生成
    private String fileNameGene(String url){
        if (url!=null && !url.equals("")){
            for(int i = url.length()-1; i>=0;i--){
                char temp = url.charAt(i);
                if (temp == '/' || temp == '\\'){
                    return url.substring(i);
                }
            }
        }
        return "unknown";
    }

    //初始化通知栏
    public void initNotification(){
        builder = new NotificationCompat.Builder(this,String.valueOf(NOTIFICATION_ID));
        builder.setSmallIcon(R.mipmap.ic_launcher).setContentTitle("正在下载音乐文件...").setContentText("downloading");//图标、标题、内容这三个设置是必须要有的。
        manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    //刷新系统媒体库
    private void updateMusic(String filename){//filename是我们的文件全名，包括后缀哦
        MediaScannerConnection.scanFile(this,
                new String[] { filename }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                });
    }


}
