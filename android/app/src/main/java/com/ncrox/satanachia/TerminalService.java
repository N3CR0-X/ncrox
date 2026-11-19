package com.ncrox.satanachia;

import android.app.*;
import android.content.*;
import android.os.*;

public class TerminalService extends Service {
  public static final String ACTION_START="com.ncrox.satanachia.START";
  public static final String ACTION_LOCK="com.ncrox.satanachia.LOCK";
  public static final String ACTION_UNLOCK="com.ncrox.satanachia.UNLOCK";
  private static final String CHANNEL="ncrox_terminal";
  private PowerManager.WakeLock wakeLock;
  @Override public void onCreate(){super.onCreate();createChannel(); startForeground(712,notification(false));}
  @Override public int onStartCommand(Intent intent,int flags,int startId){String action=intent==null?ACTION_START:intent.getAction();if(ACTION_LOCK.equals(action))acquire();else if(ACTION_UNLOCK.equals(action))release();startForeground(712,notification(wakeLock!=null&&wakeLock.isHeld()));return START_STICKY;}
  private void acquire(){if(wakeLock!=null&&wakeLock.isHeld())return;PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"ncrox:terminal");wakeLock.setReferenceCounted(false);wakeLock.acquire();getSharedPreferences("ncrox",MODE_PRIVATE).edit().putBoolean("wake_lock",true).apply();}
  private void release(){if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();getSharedPreferences("ncrox",MODE_PRIVATE).edit().putBoolean("wake_lock",false).apply();}
  private void createChannel(){if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(CHANNEL,"ncrox terminal",NotificationManager.IMPORTANCE_LOW);c.setDescription("Keeps the terminal service active");((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}}
  private Notification notification(boolean locked){Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);String text=locked?"Terminal ativo · wake lock ligado":"Terminal ativo · wake lock desligado";return new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("Satanachia ncrox").setContentText(text).setOngoing(true).setContentIntent(pi).build();}
  @Override public void onDestroy(){release();super.onDestroy();}
  @Override public IBinder onBind(Intent intent){return null;}
}
