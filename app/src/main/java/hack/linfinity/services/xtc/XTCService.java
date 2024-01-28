package hack.linfinity.services.xtc;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;

import hack.linfinity.services.xtc.restarter.XTCJobService;
import hack.linfinity.services.xtc.restarter.XTCRestartServiceBroadcastReceiver;

public class XTCService extends NotificationListenerService {
    String dataDir;
    String homeDir;
    String xtcBinary;
    Thread xtcThrd;
    Process ps;
    public static XTCService thisService;
    boolean running = false;
    public static final String TAG = "LinfinityXTC";

    public XTCService() {
        super();
    }


    @Override
    public void onCreate() {
        super.onCreate();
        thisService = this;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            restartForeground();
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "xtc.permanence";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }

    private void runXTCServer() throws IOException, InterruptedException {
        //Process ps = Runtime.getRuntime().exec("env");
        Runtime.getRuntime().exec("mkdir " + homeDir + "/XTCAirShare");
        ps = Runtime.getRuntime().exec(xtcBinary + " server", new String[]{"HOME=" + homeDir}, new File(dataDir));
        Log.d("LinfinityXTC", "Launched XTC");
        BufferedReader reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
        do {
            int read;
            char[] buffer = new char[4096];
            while ((read = reader.read(buffer)) > 0) {
                String line = new String(buffer, 0, read);
                Log.d("LinfinityXTC", line);
            }
        } while(ps.isAlive());
        int exit = ps.waitFor();
        Log.d("LinfinityXTC", "Shutdown " + Integer.toString(exit));
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences speed = getSharedPreferences("xtc", 0);
        if(!speed.getBoolean("enabled", false))
        {
            stopSelf();
            return START_NOT_STICKY;
        }
        super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "restarting Service !!");
        if (intent == null) {
            XTCProcessMainClass bck = new XTCProcessMainClass();
            bck.launchService(this);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            restartForeground();
        }

        start();
        return START_STICKY;
    }

    public static void startXTCService(Context c) {
        SharedPreferences speed = c.getSharedPreferences("xtc", 0);
        if(!speed.getBoolean("enabled", false)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            XTCRestartServiceBroadcastReceiver.scheduleJob(c);
        } else {
            XTCProcessMainClass bck = new XTCProcessMainClass();
            bck.launchService(c);
        }
    }

    private void start() {
        dataDir = getApplicationContext().getApplicationInfo().dataDir;
        homeDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        xtcBinary = dataDir + "/xtc";
        try {
            PrintWriter dnf = new PrintWriter(dataDir + "/xtc_devname");
            dnf.write(Settings.Secure.getString(getContentResolver(), "bluetooth_name"));
            dnf.flush();
            dnf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        XTCFrontendServer.run();
        xtcThrd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runXTCServer();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        xtcThrd.start();
    }

    public void stopServer() {
        try {
            Log.d(TAG, "Stopping frontend server");
            XTCFrontendServer.stop();
            Log.d(TAG, "Destroying");
            ps.destroy();
            Log.d(TAG, "Destroying forcibly");
            ps.destroyForcibly();
            Log.d(TAG, "Thanos-snapping process");
            Field f = ps.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            long pid = f.getLong(ps);
            f.setAccessible(false);
            Runtime.getRuntime().exec("kill -9 " + Long.toString(pid));
            Runtime.getRuntime().exec("killall -9 " + xtcBinary);
            Runtime.getRuntime().exec("killall -9 xtc");
            Log.d(TAG, "Stopping");
            xtcThrd.join();
            Log.d(TAG, "Stopped");
        } catch (InterruptedException | NoSuchFieldException | IllegalAccessException | IOException e) {
            e.printStackTrace();
        }
    }

    public void restartForeground() {
        SharedPreferences speed = getSharedPreferences("xtc", 0);
        if(!speed.getBoolean("enabled", false)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.i(TAG, "restarting foreground");
            startMyOwnForeground();
            Log.i(TAG, "restarting foreground successful");
            start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy called");
        SharedPreferences speed = getSharedPreferences("xtc", 0);
        if(!speed.getBoolean("enabled", false)) return;
        Intent broadcastIntent = new Intent(XTCJobService.RESTART_INTENT);
        sendBroadcast(broadcastIntent);
        stopServer();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved called");
        SharedPreferences speed = getSharedPreferences("xtc", 0);
        if(!speed.getBoolean("enabled", false)) return;
        Intent broadcastIntent = new Intent(XTCJobService.RESTART_INTENT);
        sendBroadcast(broadcastIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
