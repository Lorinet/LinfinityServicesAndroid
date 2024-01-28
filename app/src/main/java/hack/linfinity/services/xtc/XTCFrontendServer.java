package hack.linfinity.services.xtc;

import android.app.Notification;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class XTCFrontendServer {
    private static ServerSocket serverSocket;
    private static Socket clientSocket;
    private static OutputStream out;
    private static InputStream in;
    private static final int port = 1339;
    private static Thread serverThrd;

    static class XTCNotification {
        public String app;
        public String title;
        public String body;
        public String[] actions;

        public XTCNotification(String ap, String tits, String bod, String[] act) {
            app = ap;
            title = tits;
            body = bod;
            actions = act;
        }
    }

    public static void run() {
        serverThrd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (serverSocket != null) return;
                    Log.d("XTCFrontendServer", "Starting server");
                    serverSocket = new ServerSocket();
                    serverSocket.setReuseAddress(true);
                    serverSocket.bind(new InetSocketAddress(port));
                    Log.d("XTCFrontendServer", "Started server");
                    while (true) {
                        try {
                            Log.d("XTCFrontend", "Listening");
                            clientSocket = serverSocket.accept();
                            Log.d("XTCFrontend", "New client");
                            out = clientSocket.getOutputStream();
                            in = clientSocket.getInputStream();
                            int toby = 0;
                            int retc = 0;
                            byte[] finbuf = new byte[0];
                            byte[] tembuf;
                            while (in.available() == 0) ;
                            while (in.available() > 0) {
                                tembuf = new byte[2048];
                                retc = in.read(tembuf);
                                int old = toby;
                                toby += retc;
                                byte[] aux = new byte[toby];
                                System.arraycopy(finbuf, 0, aux, 0, finbuf.length);
                                System.arraycopy(tembuf, 0, aux, old, retc);
                                finbuf = aux;
                            }
                            String req = new String(finbuf, "ASCII");
                            Log.d("XTCFrontend", req);
                            JSONObject inv = new JSONObject(req);
                            Log.d("XTCFrontend", inv.toString());
                            JSONArray ouv = new JSONArray();
                            if (inv.getString("service").equals("xtc_notifications")) {
                                if (inv.getString("action").equals("get_notifications")) {
                                    StatusBarNotification[] nots = XTCService.thisService.getActiveNotifications();
                                    for (StatusBarNotification not : nots) {
                                        String pack = not.getPackageName();
                                        Bundle extras = not.getNotification().extras;
                                        String title = extras.getString("android.title");
                                        String text = extras.getCharSequence("android.text").toString();
                                        PackageManager pm = XTCService.thisService.getApplicationContext().getPackageManager();
                                        ApplicationInfo ai;
                                        try {
                                            ai = pm.getApplicationInfo(pack, 0);
                                        } catch (final PackageManager.NameNotFoundException e) {
                                            ai = null;
                                        }
                                        String app = (String) (ai != null ? pm.getApplicationLabel(ai) : pack);
                                        ArrayList<String> actlist = new ArrayList<>();
                                        for(Notification.Action a : not.getNotification().actions) {
                                            actlist.add(a.title.toString());
                                        }
                                        ouv.put(new XTCNotification(app, title, text, actlist.toArray(new String[0])));
                                    }
                                }
                            }

                            out.write((" " + ouv.toString()).getBytes(Charset.forName("ASCII")));
                            out.flush();
                            in.read();
                            in.close();
                            out.close();
                            clientSocket.close();
                        } catch (Exception e) {
                            SharedPreferences sp = XTCService.thisService.getSharedPreferences("xtc", 0);
                            boolean en = sp.getBoolean("enabled", false);
                            if (!en) return;
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        serverThrd.start();
    }


    public static void stop() {
        try {
            serverSocket.close();
            serverThrd.join();
            serverSocket = null;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
