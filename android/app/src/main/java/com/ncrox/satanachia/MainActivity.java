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
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
  private LinearLayout layout;
  private TextView output;
  private EditText input;
  private Button accessButton;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private File cwd;
  private final int green = Color.rgb(125,255,99);
  private final String intro = "Satanachia ncrox — shell Android sem root\nDigite help para ajuda.\n";

  @Override public void onCreate(Bundle saved) {
    super.onCreate(saved);
    getWindow().setStatusBarColor(Color.rgb(8,11,8));
    getWindow().setNavigationBarColor(Color.rgb(8,11,8));
    cwd = chooseStartDir();
    layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setBackgroundColor(Color.rgb(8,11,8)); layout.setPadding(14,10,14,10);
    TextView title = new TextView(this); title.setText("SATANACHIA  •  NCROX"); title.setTextColor(green); title.setTextSize(17); title.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); title.setPadding(4,8,4,8); layout.addView(title);
    accessButton = new Button(this); accessButton.setText("Permitir acesso ao armazenamento compartilhado"); accessButton.setTextAllCaps(false); accessButton.setOnClickListener(v -> requestFileAccess()); layout.addView(accessButton);
    ScrollView scroll = new ScrollView(this); output = new TextView(this); output.setText(intro + accessStatus() + "\n\n"); output.setTextColor(green); output.setTextSize(14); output.setTypeface(Typeface.MONOSPACE); output.setTextIsSelectable(true); scroll.addView(output); layout.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    input = new EditText(this); input.setSingleLine(true); input.setTextColor(green); input.setHintTextColor(Color.GRAY); input.setHint("comando…"); input.setTypeface(Typeface.MONOSPACE); input.setImeOptions(EditorInfo.IME_ACTION_DONE); input.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green)); layout.addView(input,new LinearLayout.LayoutParams(-1,-2));
    input.setOnEditorActionListener((v,action,event)->{ submit(); return true; });
    setContentView(layout); refreshAccessButton();
  }
  private File chooseStartDir() {
    File shared=Environment.getExternalStorageDirectory();
    if (hasFileAccess() && shared!=null && shared.isDirectory()) return shared;
    return getFilesDir();
  }
  private boolean hasFileAccess() {
    return Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager();
  }
  private String accessStatus() {
    return hasFileAccess()?"Armazenamento compartilhado: liberado ("+Environment.getExternalStorageDirectory()+").":
      "Armazenamento compartilhado: sem permissão; usando pasta privada do app. Toque no botão para abrir a configuração Android.";
  }
  private void refreshAccessButton() {
    if (accessButton==null) return;
    boolean granted=hasFileAccess();
    accessButton.setText(granted?"Acesso ao armazenamento concedido — abrir configurações":"Permitir acesso ao armazenamento compartilhado");
    if(granted) cwd=Environment.getExternalStorageDirectory();
  }
  private void requestFileAccess() {
    if(Build.VERSION.SDK_INT>=30) {
      try { Intent i=new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:"+getPackageName())); startActivity(i); }
      catch(Exception e) { startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)); }
    }
  }
  @Override protected void onResume(){super.onResume();refreshAccessButton();}
  private void submit() {
    String cmd=input.getText().toString().trim(); input.setText(""); if(cmd.isEmpty()) return;
    output.append(cwd.getAbsolutePath()+" $ "+cmd+"\n");
    worker.execute(()->{ String result=run(cmd); runOnUiThread(()->{ if(!result.isEmpty()) output.append(result+"\n"); ((ScrollView)output.getParent()).post(()->((ScrollView)output.getParent()).fullScroll(ScrollView.FOCUS_DOWN)); }); });
  }
  private String run(String command) {
    try {
      String[] a=command.trim().split("\\s+"); String c=a[0];
      if(c.equals("exit")||c.equals("quit")) return "Use o botão voltar para sair.";
      if(c.equals("help")||c.equals("ajuda")) return "Atalhos: help, pwd, cd [pasta], make [-p] pasta, tree [pasta], pick <URL|usuario/repo>, net faces, net faces -root, net -show -all, clear, exit.\n"+
        "Os outros comandos vão para /system/bin/sh. Acesso amplo a arquivos compartilhados só funciona após você conceder a permissão nas Configurações do Android.\n"+
        "Sem root, sem acesso a dados privados de outros apps e sem ambiente Linux/Termux instalado: só comandos incluídos no Android estão disponíveis.\n"+
        "Aviso: comandos shell podem modificar ou apagar arquivos. Confira cada comando antes de executar.";
      if(c.equals("clear")||c.equals("cls")) { runOnUiThread(()->output.setText(intro+accessStatus()+"\n\n")); return ""; }
      if(c.equals("pwd")) return cwd.getAbsolutePath();
      if(c.equals("cd")) { File f=a.length>1?new File(cwd,a[1]):chooseStartDir(); if(f.isDirectory()){cwd=f.getCanonicalFile();return cwd.getAbsolutePath();} return "cd: diretório não encontrado ou sem acesso"; }
      if(c.equals("make")||c.equals("mkdir")) { if(a.length<2)return "Uso: make [-p] pasta"; boolean parents=false; String name=a[a.length-1]; for(String x:a) if(x.equals("-p")||x.equals("--parents"))parents=true; File f=new File(cwd,name); boolean ok=parents?f.mkdirs():f.mkdir(); return ok?"Pasta criada: "+f.getAbsolutePath():"Não foi possível criar pasta (já existe ou acesso negado)."; }
      if(c.equals("tree")||c.equals("ls")) { File f=a.length>1?new File(cwd,a[1]):cwd; if(!f.canRead())return "tree: sem acesso. Confira a permissão de armazenamento e o caminho."; StringBuilder b=new StringBuilder(f.getAbsolutePath()); tree(f,"",b,0); return b.toString(); }
      if(c.equals("pick")) { if(a.length<2)return "Uso: pick <URL GitHub ou usuario/repo>"; String url=a[1]; if(!url.contains("://")&&!url.startsWith("git@"))url="https://github.com/"+url.replaceAll("\\.git$","")+".git"; return shell("git clone "+quote(url)); }
      if(c.equals("net")) { if(a.length>1&&(a[1].equals("faces")||(a.length>2&&a[1].equals("-show"))))return shell("ip addr"); return "Uso: net faces | net faces -root | net -show -all. Não mostra chaves Wi-Fi."; }
      return shell(command);
    } catch(Exception e){ return "Erro: "+e.getMessage(); }
  }
  private String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
  private String shell(String cmd) {
    try {
      Process p=new ProcessBuilder("/system/bin/sh","-c",cmd).directory(cwd).redirectErrorStream(true).start();
      ByteArrayOutputStream buf=new ByteArrayOutputStream(); InputStream in=p.getInputStream(); byte[] x=new byte[2048]; int n; int total=0; while((n=in.read(x))>0 && total<65536){buf.write(x,0,n);total+=n;} int code=p.waitFor(); String out=buf.toString("UTF-8").trim(); if(out.isEmpty())out=code==0?"Concluído.":"Código de saída: "+code; if(total>=65536)out+="\n[saída truncada]"; return out;
    } catch(Exception e){return "Erro ao executar comando: "+e.getMessage();}
  }
  private void tree(File dir,String prefix,StringBuilder b,int depth) {
    if(depth>=7)return; File[] files=dir.listFiles(); if(files==null)return; java.util.Arrays.sort(files,(x,y)->Boolean.compare(!x.isDirectory(),!y.isDirectory()));
    for(int i=0;i<files.length&&i<150;i++){File f=files[i];boolean last=i==files.length-1;b.append("\n").append(prefix).append(last?"└── ":"├── ").append(f.getName()).append(f.isDirectory()?"/":""); if(f.isDirectory())tree(f,prefix+(last?"    ":"│   "),b,depth+1);}
  }
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
