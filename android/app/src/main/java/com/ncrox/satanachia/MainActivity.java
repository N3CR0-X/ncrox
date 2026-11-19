package com.ncrox.satanachia;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
  private TextView output;
  private EditText input;
  private Button accessButton, lockButton;
  private ScrollView scroll;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private File cwd;
  private final int green = Color.rgb(125,255,99);
  private final ArrayList<String> history = new ArrayList<>();
  private int historyPos = 0;
  private boolean controlNext = false;

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(Color.BLACK);
    getWindow().setNavigationBarColor(Color.BLACK);
    cwd = chooseStartDir();
    startKeepAliveService();

    LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL); page.setBackgroundColor(Color.BLACK); page.setPadding(8,4,8,4);
    scroll = new ScrollView(this); output = new TextView(this); output.setTextColor(green); output.setTextSize(14); output.setTypeface(Typeface.MONOSPACE); output.setTextIsSelectable(true); output.setPadding(2,2,2,4); scroll.addView(output); page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

    accessButton = new Button(this); accessButton.setText("Conceder acesso aos arquivos"); accessButton.setAllCaps(false); accessButton.setOnClickListener(v -> requestFileAccess()); page.addView(accessButton);
    LinearLayout lockRow = new LinearLayout(this); lockRow.setOrientation(LinearLayout.HORIZONTAL);
    lockButton = new Button(this); lockButton.setAllCaps(false); lockButton.setOnClickListener(v -> toggleWakeLock()); lockRow.addView(lockButton,new LinearLayout.LayoutParams(-1,-2)); page.addView(lockRow);

    LinearLayout keys1 = new LinearLayout(this); keys1.setOrientation(LinearLayout.HORIZONTAL);
    addKey(keys1,"ESC",()->insert("\u001b")); addKey(keys1,"TAB",this::submit); addKey(keys1,"CTRL",()->controlNext=!controlNext); addKey(keys1,"ALT",()->insert("\u001b")); addKey(keys1,"↑",()->historyMove(-1)); addKey(keys1,"HOME",()->moveCursor(false)); addKey(keys1,"END",()->moveCursor(true)); page.addView(keys1);
    LinearLayout keys2 = new LinearLayout(this); keys2.setOrientation(LinearLayout.HORIZONTAL);
    addKey(keys2,"←",()->moveBy(-1)); addKey(keys2,"↓",()->historyMove(1)); addKey(keys2,"→",()->moveBy(1)); addKey(keys2,"PGUP",()->scrollByPage(-1)); addKey(keys2,"PGDN",()->scrollByPage(1)); addKey(keys2,"LOCK",this::toggleWakeLock); page.addView(keys2);

    input = new EditText(this); input.setSingleLine(true); input.setTextColor(green); input.setHintTextColor(Color.GRAY); input.setHint(""); input.setTypeface(Typeface.MONOSPACE); input.setTextSize(14); input.setImeOptions(EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI); input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green)); page.addView(input,new LinearLayout.LayoutParams(-1,-2));
    input.setOnKeyListener((v,keyCode,event)->{
      if(keyCode==KeyEvent.KEYCODE_TAB && event.getAction()==KeyEvent.ACTION_DOWN){submit();return true;}
      if(keyCode==KeyEvent.KEYCODE_ENTER && event.getAction()==KeyEvent.ACTION_DOWN){return true;}
      if(keyCode==KeyEvent.KEYCODE_DPAD_UP && event.getAction()==KeyEvent.ACTION_DOWN){historyMove(-1);return true;}
      if(keyCode==KeyEvent.KEYCODE_DPAD_DOWN && event.getAction()==KeyEvent.ACTION_DOWN){historyMove(1);return true;}
      if(controlNext && event.getAction()==KeyEvent.ACTION_DOWN && event.getUnicodeChar()!=0){char ch=(char)event.getUnicodeChar(); if(ch>='@'&&ch<='_'){insert(String.valueOf((char)(ch-'@')));controlNext=false;return true;}}
      return false;
    });
    input.setOnEditorActionListener((v,action,event)->{ if(action==EditorInfo.IME_ACTION_DONE){submit();return true;} return false; });
    setContentView(page);
    refreshPermissionsAndButtons();
    showPrompt();
  }
  private void addKey(LinearLayout row,String label,Runnable action){
    Button b=new Button(this); b.setText(label); b.setTextSize(10); b.setAllCaps(false); b.setPadding(0,0,0,0); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,42,1); lp.setMargins(1,0,1,2); row.addView(b,lp); b.setOnClickListener(v->action.run());
  }
  private void startKeepAliveService(){
    Intent i=new Intent(this,TerminalService.class); i.setAction(TerminalService.ACTION_START);
    if(Build.VERSION.SDK_INT>=26) startForegroundService(i); else startService(i);
  }
  private File chooseStartDir(){ File shared=Environment.getExternalStorageDirectory(); if(hasFileAccess()&&shared!=null&&shared.isDirectory())return shared; return getFilesDir(); }
  private boolean hasFileAccess(){return Build.VERSION.SDK_INT<30||Environment.isExternalStorageManager();}
  private void refreshPermissionsAndButtons(){
    if(accessButton==null)return;
    accessButton.setVisibility(hasFileAccess()?android.view.View.GONE:android.view.View.VISIBLE);
    if(hasFileAccess())cwd=Environment.getExternalStorageDirectory();
    boolean locked=getSharedPreferences("ncrox",MODE_PRIVATE).getBoolean("wake_lock",false);
    lockButton.setText(locked?"Wake lock: ligado":"Wake lock: desligado");
  }
  private void requestFileAccess(){ if(Build.VERSION.SDK_INT>=30){try{startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}} }
  private void toggleWakeLock(){boolean old=getSharedPreferences("ncrox",MODE_PRIVATE).getBoolean("wake_lock",false); boolean now=!old;getSharedPreferences("ncrox",MODE_PRIVATE).edit().putBoolean("wake_lock",now).apply();Intent i=new Intent(this,TerminalService.class);i.setAction(now?TerminalService.ACTION_LOCK:TerminalService.ACTION_UNLOCK);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);refreshPermissionsAndButtons();}
  private void showPrompt(){output.append("0───────~$[ ");scroll.post(()->scroll.fullScroll(ScrollView.FOCUS_DOWN));input.requestFocus();}
  private void submit(){String cmd=input.getText().toString().trim();input.setText("");if(cmd.isEmpty())return;output.append(cmd+"\n");history.add(cmd);historyPos=history.size();worker.execute(()->{String result=run(cmd);runOnUiThread(()->{if(!result.isEmpty())output.append(result+"\n");showPrompt();});});}
  private void insert(String s){int at=Math.max(0,input.getSelectionStart());input.getText().insert(at,s);input.setSelection(at+s.length());input.requestFocus();}
  private void moveCursor(boolean end){input.setSelection(end?input.length():0);input.requestFocus();}
  private void moveBy(int delta){int at=Math.max(0,input.getSelectionStart());input.setSelection(Math.max(0,Math.min(input.length(),at+delta)));}
  private void scrollByPage(int sign){scroll.smoothScrollBy(0,sign*Math.max(100,scroll.getHeight()*3/4));input.requestFocus();}
  private void historyMove(int delta){if(history.isEmpty())return;historyPos=Math.max(0,Math.min(history.size(),historyPos+delta));input.setText(historyPos==history.size()?"":history.get(historyPos));input.setSelection(input.length());}
  private String run(String command){
    try{
      String[] a=command.trim().split("\\s+");String c=a[0];
      if(c.equals("exit")||c.equals("quit"))return "";
      if(c.equals("help")||c.equals("ajuda"))return "Atalhos: pwd, cd, make, tree, pick, net faces, acquire lock, release lock. Tab envia o comando. Outros comandos são do Android.";
      if(c.equals("clear")||c.equals("cls")){runOnUiThread(()->{output.setText("");showPrompt();});return "";}
      if(c.equals("pwd"))return cwd.getAbsolutePath();
      if(c.equals("cd")){File f=a.length>1?new File(cwd,a[1]):chooseStartDir();if(f.isDirectory()){cwd=f.getCanonicalFile();return "";}return "cd: diretório inválido";}
      if(c.equals("make")||c.equals("mkdir")){if(a.length<2)return "Uso: make [-p] pasta";boolean parents=false;String name=a[a.length-1];for(String x:a)if(x.equals("-p")||x.equals("--parents"))parents=true;File f=new File(cwd,name);boolean ok=parents?f.mkdirs():f.mkdir();return ok?"": "make: não foi possível criar diretório";}
      if(c.equals("tree")||c.equals("ls")){File f=a.length>1?new File(cwd,a[1]):cwd;if(!f.canRead())return "tree: sem acesso";StringBuilder b=new StringBuilder(f.getName().isEmpty()?f.getAbsolutePath():f.getName());tree(f,"",b,0);return b.toString();}
      if(c.equals("pick")){if(a.length<2)return "Uso: pick URL|usuario/repo";String url=a[1];if(!url.contains("://")&&!url.startsWith("git@"))url="https://github.com/"+url.replaceAll("\\.git$","")+".git";return shell("git clone "+quote(url));}
      if(c.equals("net")){if(a.length>1&&(a[1].equals("faces")||(a.length>2&&a[1].equals("-show"))))return shell("ip addr");return "Uso: net faces | net -show -all";}
      if(c.equals("acquire")&&a.length>1&&a[1].equals("lock")){setWakeLock(true);return "Wake lock ligado";}
      if(c.equals("release")&&a.length>1&&a[1].equals("lock")){setWakeLock(false);return "Wake lock desligado";}
      return shell(command);
    }catch(Exception e){return "Erro: "+e.getMessage();}
  }
  private void setWakeLock(boolean on){getSharedPreferences("ncrox",MODE_PRIVATE).edit().putBoolean("wake_lock",on).apply();Intent i=new Intent(this,TerminalService.class);i.setAction(on?TerminalService.ACTION_LOCK:TerminalService.ACTION_UNLOCK);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);runOnUiThread(this::refreshPermissionsAndButtons);}
  private String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
  private String shell(String cmd){try{Process p=new ProcessBuilder("/system/bin/sh","-c",cmd).directory(cwd).redirectErrorStream(true).start();ByteArrayOutputStream buf=new ByteArrayOutputStream();InputStream in=p.getInputStream();byte[] x=new byte[2048];int n,total=0;while((n=in.read(x))>0&&total<65536){buf.write(x,0,n);total+=n;}int code=p.waitFor();String out=buf.toString("UTF-8").trim();if(out.isEmpty())out=code==0?"":"Código de saída: "+code;if(total>=65536)out+="\n[saída truncada]";return out;}catch(Exception e){return "Erro: "+e.getMessage();}}
  private void tree(File dir,String prefix,StringBuilder b,int depth){if(depth>=7)return;File[] files=dir.listFiles();if(files==null)return;java.util.Arrays.sort(files,(x,y)->Boolean.compare(!x.isDirectory(),!y.isDirectory()));for(int i=0;i<files.length&&i<150;i++){File f=files[i];boolean last=i==files.length-1;b.append("\n").append(prefix).append(last?"╰── ":"──╮─ ").append(f.getName()).append(f.isDirectory()?"/":"");if(f.isDirectory())tree(f,prefix+(last?"    ":"│   "),b,depth+1);}}
  @Override protected void onResume(){super.onResume();refreshPermissionsAndButtons();}
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
