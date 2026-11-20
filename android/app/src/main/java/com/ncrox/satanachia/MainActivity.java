package com.ncrox.satanachia;

import android.app.*;
import android.os.*;
import android.os.Environment;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.hardware.camera2.CameraManager;
import android.location.*;
import android.content.pm.*;
import android.speech.tts.TextToSpeech;
import android.bluetooth.*;
import android.app.ActivityManager;
import android.net.wifi.WifiManager;
import org.json.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
  private TextView output; private EditText input; private Button accessButton;
  private ScrollView scroll; private FrameLayout root; private LinearLayout page, drawer, keyRow1, keyRow2;
  private View shade; private float downX,downY; private boolean drawerOpen=false;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final ArrayList<Session> tabs=new ArrayList<>(); private int activeTab=0;
  private final ArrayList<String> history=new ArrayList<>(); private int historyPos=0; private boolean controlNext=false;
  private int bg,fg,accent,keyBg; private boolean lastFileAccess; private String prefix; private CameraManager cameraManager; private String cameraId; private TextToSpeech tts; private boolean pendingTaskConfirm=false; private String pendingInstall=""; private boolean waitingForLocation=false; private final Handler mainHandler=new Handler(Looper.getMainLooper());
  private static class Session { File cwd; SpannableStringBuilder text=new SpannableStringBuilder(); Session(File d,String p){cwd=d;text.append(p);} }

  @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setStatusBarColor(Color.BLACK);getWindow().setNavigationBarColor(Color.BLACK);cameraManager=(CameraManager)getSystemService(CAMERA_SERVICE);try{String[] ids=cameraManager.getCameraIdList();for(String id:ids){android.hardware.camera2.CameraCharacteristics ch=cameraManager.getCameraCharacteristics(id);Boolean flash=ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);Integer facing=ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);if(Boolean.TRUE.equals(flash)&&facing!=null&&facing==android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK){cameraId=id;break;}}}catch(Exception ignored){}tts=new TextToSpeech(this,status->{});loadPrefs();tabs.add(new Session(chooseStartDir(),""));lastFileAccess=hasFileAccess();startKeepAliveService();buildUi();}
  private void loadPrefs(){
    android.content.SharedPreferences p=getSharedPreferences("ncrox",MODE_PRIVATE);
    prefix=p.getString("prefix","0───────~$[");
    String theme=p.getString("theme","mono");
    if("amber".equals(theme)){bg=Color.rgb(12,10,6);fg=Color.rgb(255,190,70);accent=fg;keyBg=bg;}
    else if("light".equals(theme)){bg=Color.rgb(239,241,237);fg=Color.rgb(25,35,26);accent=Color.rgb(25,110,54);keyBg=bg;}
    else if("green".equals(theme)){bg=Color.BLACK;fg=Color.rgb(125,255,99);accent=fg;keyBg=bg;}
    else{bg=Color.BLACK;fg=Color.rgb(235,235,235);accent=fg;keyBg=bg;}
  }
  private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
  private Button flatButton(String label, String description){
    Button b=new Button(this);b.setText(label);b.setContentDescription(description);
    b.setAllCaps(false);b.setTextColor(fg);b.setTypeface(Typeface.MONOSPACE);
    b.setTextSize(14);b.setBackgroundColor(keyBg);b.setElevation(0);
    b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
    b.setPadding(dp(8),0,dp(8),0);return b;
  }
  private void buildUi(){
    root=new FrameLayout(this);page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(bg);page.setPadding(dp(4),0,dp(4),0);
    LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);
    Button menu=flatButton("☰","Abrir abas");menu.setOnClickListener(v->showDrawer());top.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(48)));
    TextView name=new TextView(this);name.setText("NCROX");name.setTextColor(accent);name.setTypeface(Typeface.MONOSPACE);name.setTextSize(14);name.setGravity(Gravity.CENTER_VERTICAL);top.addView(name,new LinearLayout.LayoutParams(0,dp(48),1));
    Button settings=flatButton("⚙","Abrir configurações");settings.setOnClickListener(v->showSettings());top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(48)));page.addView(top);
    scroll=new ScrollView(this);HorizontalScrollView horizontal=new HorizontalScrollView(this);horizontal.setFillViewport(true);horizontal.setHorizontalScrollBarEnabled(true);output=new TextView(this);output.setTextColor(fg);output.setTextSize(14);output.setTypeface(Typeface.MONOSPACE);output.setTextIsSelectable(true);output.setLinksClickable(true);output.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());output.setHorizontallyScrolling(true);output.setPadding(dp(4),dp(2),dp(4),dp(4));horizontal.addView(output,new HorizontalScrollView.LayoutParams(-2,-2));scroll.addView(horizontal);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    accessButton=flatButton("Conceder acesso aos arquivos","Conceder acesso aos arquivos");accessButton.setOnClickListener(v->requestFileAccess());page.addView(accessButton);
    HorizontalScrollView keyScroll=new HorizontalScrollView(this);keyScroll.setFillViewport(false);keyScroll.setHorizontalScrollBarEnabled(true);
    keyRow1=new LinearLayout(this);keyRow1.setOrientation(LinearLayout.HORIZONTAL);
    addKey(keyRow1,"↑","Comando anterior",()->historyMove(-1));addKey(keyRow1,"↓","Próximo comando",()->historyMove(1));
    addKey(keyRow1,"←","Mover cursor para a esquerda",()->moveBy(-1));addKey(keyRow1,"→","Mover cursor para a direita",()->moveBy(1));
    addKey(keyRow1,"HOME","Início da linha",()->moveCursor(false));addKey(keyRow1,"END","Fim da linha",()->moveCursor(true));
    addKey(keyRow1,"PGUP","Rolar para cima",()->scrollByPage(-1));addKey(keyRow1,"PGDN","Rolar para baixo",()->scrollByPage(1));
    addKey(keyRow1,"LOCK","Alternar wake lock",this::toggleWakeLock);keyScroll.addView(keyRow1);page.addView(keyScroll);
    LinearLayout commandLine=new LinearLayout(this);commandLine.setOrientation(LinearLayout.HORIZONTAL);
    input=new EditText(this);input.setSingleLine(true);input.setTextColor(fg);input.setHintTextColor(Color.GRAY);input.setHint("Comando");input.setContentDescription("Comando");input.setTypeface(Typeface.MONOSPACE);input.setTextSize(14);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);input.setImeOptions(EditorInfo.IME_ACTION_DONE|EditorInfo.IME_FLAG_NO_EXTRACT_UI);input.setBackgroundColor(bg);input.setPadding(dp(4),0,dp(4),0);input.setMinHeight(dp(48));commandLine.addView(input,new LinearLayout.LayoutParams(0,dp(48),1));
    Button send=flatButton("ENVIAR","Executar comando");send.setOnClickListener(v->submit());commandLine.addView(send,new LinearLayout.LayoutParams(-2,dp(48)));page.addView(commandLine);
    input.setOnKeyListener((v,k,e)->{
      if(k==KeyEvent.KEYCODE_ENTER){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)submit();return true;}
      if(k==KeyEvent.KEYCODE_DPAD_UP&&e.getAction()==KeyEvent.ACTION_DOWN){historyMove(-1);return true;}
      if(k==KeyEvent.KEYCODE_DPAD_DOWN&&e.getAction()==KeyEvent.ACTION_DOWN){historyMove(1);return true;}
      return false;
    });
    input.setOnEditorActionListener((v,a,e)->{if(a==EditorInfo.IME_ACTION_DONE){submit();return true;}return false;});
    root.addView(page,new FrameLayout.LayoutParams(-1,-1));shade=null;drawer=null;drawerOpen=false;buildDrawer();setContentView(root);refreshPermissions();renderSession();
  }
  private void buildDrawer(){
    if(shade!=null)root.removeView(shade);if(drawer!=null)root.removeView(drawer);
    shade=new View(this);shade.setBackgroundColor(0x99000000);shade.setVisibility(View.GONE);shade.setOnClickListener(v->hideDrawer());root.addView(shade,new FrameLayout.LayoutParams(-1,-1));
    drawer=new LinearLayout(this);drawer.setOrientation(LinearLayout.VERTICAL);drawer.setPadding(14,28,14,12);drawer.setBackgroundColor(bg);FrameLayout.LayoutParams dp=new FrameLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.82),-1,Gravity.LEFT);drawer.setTranslationX(-dp.width);root.addView(drawer,dp);
    TextView title=new TextView(this);title.setText("NCROX  ·  ABAS");title.setTextColor(accent);title.setTextSize(18);title.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);title.setPadding(8,4,4,18);drawer.addView(title);
    Button newTab=flatButton("+ Nova aba","Nova aba");newTab.setOnClickListener(v->{hideDrawer();saveSession();openNewTab();});drawer.addView(newTab);
    for(int i=0;i<tabs.size();i++){final int idx=i;Button tab=flatButton("","Alternar para aba "+(idx+1));tab.setTextColor(idx==activeTab?accent:fg);String label=(idx==activeTab?"● ":"○ ")+"Aba "+(idx+1)+"   "+shortPath(tabs.get(i).cwd);tab.setText(label);tab.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);tab.setOnClickListener(v->{hideDrawer();saveSession();activeTab=idx;history.clear();historyPos=0;renderSession();});drawer.addView(tab);}
    View spacer=new View(this);drawer.addView(spacer,new LinearLayout.LayoutParams(1,0,1));Button cfg=flatButton("⚙  Configurações","Abrir configurações");cfg.setOnClickListener(v->{hideDrawer();showSettings();});drawer.addView(cfg);
  }
  private String shortPath(File f){String s=f.getName();return s.isEmpty()?"/":s;}
  private void saveSession(){if(activeTab<tabs.size()){tabs.get(activeTab).cwd=cwd();tabs.get(activeTab).text=new SpannableStringBuilder(output.getText());}}
  private File cwd(){return tabs.get(activeTab).cwd;}
  private void renderSession(){Session s=tabs.get(activeTab);output.setText(s.text);if(s.text.length()==0)output.append(prefix+" ");else if(s.text.charAt(s.text.length()-1)!=' ')output.append("\n"+prefix+" ");scroll.post(()->scroll.fullScroll(ScrollView.FOCUS_DOWN));input.requestFocus();}
  private void showDrawer(){buildDrawer();shade.setVisibility(View.VISIBLE);drawer.animate().translationX(0).setDuration(180).start();drawerOpen=true;}
  private void hideDrawer(){if(!drawerOpen)return;drawer.animate().translationX(-drawer.getWidth()).setDuration(160).withEndAction(()->shade.setVisibility(View.GONE)).start();drawerOpen=false;}
  @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();}else if(e.getAction()==MotionEvent.ACTION_UP){float dx=e.getX()-downX,dy=e.getY()-downY;if(Math.abs(dx)>90&&Math.abs(dx)>Math.abs(dy)*1.3){if(!drawerOpen&&downX<32&&dx>0){showDrawer();return true;}if(drawerOpen&&dx<0){hideDrawer();return true;}}}return super.dispatchTouchEvent(e);}
  private void showSettings(){
    LinearLayout box=new LinearLayout(this);box.setPadding(dp(22),dp(8),dp(22),0);box.setOrientation(LinearLayout.VERTICAL);
    EditText pfx=new EditText(this);pfx.setSingleLine(true);pfx.setHint("Prefixo do prompt");pfx.setText(prefix);box.addView(pfx);
    Spinner themes=new Spinner(this);themes.setContentDescription("Tema");
    ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,new String[]{"Preto e branco","Verde escuro","Âmbar","Claro"});ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);themes.setAdapter(ad);
    String current=getSharedPreferences("ncrox",MODE_PRIVATE).getString("theme","mono");themes.setSelection(current.equals("green")?1:current.equals("amber")?2:current.equals("light")?3:0);box.addView(themes);
    new AlertDialog.Builder(this).setTitle("Configurações").setView(box).setNegativeButton("Cancelar",(d,w)->{}).setPositiveButton("Salvar",(d,w)->{
      String draft=input.getText().toString();int cursor=Math.max(0,input.getSelectionStart());saveSession();
      String val=pfx.getText().toString().trim();if(val.isEmpty())val="0───────~$[";
      String[] ids={"mono","green","amber","light"};String theme=ids[themes.getSelectedItemPosition()];
      getSharedPreferences("ncrox",MODE_PRIVATE).edit().putString("prefix",val).putString("theme",theme).apply();
      String oldPrefix=prefix;prefix=val;
      for(Session session:tabs){String suffix=oldPrefix+" ";if(session.text.toString().endsWith(suffix))session.text.replace(session.text.length()-suffix.length(),session.text.length(),prefix+" ");}
      loadPrefs();buildUi();input.setText(draft);input.setSelection(Math.min(cursor,draft.length()));
    }).show();
  }
  private void addKey(LinearLayout row,String label,String description,Runnable action){Button b=flatButton(label,description);b.setTextSize(12);row.addView(b,new LinearLayout.LayoutParams(-2,dp(48)));b.setOnClickListener(v->action.run());}
  private void openNewTab(){saveSession();tabs.add(new Session(chooseStartDir(),""));activeTab=tabs.size()-1;history.clear();historyPos=0;buildDrawer();renderSession();}
  private void closeTerminalApp(){
    try{setWakeLock(false);}catch(Exception ignored){}
    try{stopService(new Intent(this,TerminalService.class));}catch(Exception ignored){}
    try{if(tts!=null){tts.stop();tts.shutdown();tts=null;}}catch(Exception ignored){}
    try{if(cameraId!=null)torch(false);}catch(Exception ignored){}
    worker.shutdownNow();
    if(Build.VERSION.SDK_INT>=21)finishAndRemoveTask();else finish();
    mainHandler.postDelayed(()->android.os.Process.killProcess(android.os.Process.myPid()),300);
  }
  private void startKeepAliveService(){Intent i=new Intent(this,TerminalService.class);i.setAction(TerminalService.ACTION_START);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
  private File chooseStartDir(){File shared=Environment.getExternalStorageDirectory();if(hasFileAccess()&&shared!=null&&shared.isDirectory())return shared;return getFilesDir();}
  private boolean hasFileAccess(){return Build.VERSION.SDK_INT<30||Environment.isExternalStorageManager();}
  private void refreshPermissions(){boolean granted=hasFileAccess();if(accessButton!=null)accessButton.setVisibility(granted?View.GONE:View.VISIBLE);if(granted&&!lastFileAccess&&activeTab<tabs.size())tabs.get(activeTab).cwd=Environment.getExternalStorageDirectory();lastFileAccess=granted;}
  private void requestFileAccess(){if(Build.VERSION.SDK_INT>=30){try{startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,Uri.parse("package:"+getPackageName())));}catch(Exception e){startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));}}}
  private void toggleWakeLock(){boolean old=getSharedPreferences("ncrox",MODE_PRIVATE).getBoolean("wake_lock",false);setWakeLock(!old);Toast.makeText(this,old?"Wake lock desligado":"Wake lock solicitado",Toast.LENGTH_SHORT).show();}
  private void setWakeLock(boolean on){getSharedPreferences("ncrox",MODE_PRIVATE).edit().putBoolean("wake_lock",on).apply();Intent i=new Intent(this,TerminalService.class);i.setAction(on?TerminalService.ACTION_LOCK:TerminalService.ACTION_UNLOCK);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}
  private void showPrompt(){output.append(prefix+" ");scroll.post(()->scroll.fullScroll(ScrollView.FOCUS_DOWN));input.requestFocus();}
  private void submit(){String cmd=input.getText().toString().trim();input.setText("");if(cmd.isEmpty())return;
    if(cmd.equalsIgnoreCase("start")){output.append(cmd+"\n");history.add(cmd);historyPos=history.size();saveSession();openNewTab();return;}
    if(cmd.equalsIgnoreCase("exit")||cmd.equalsIgnoreCase("logout")||cmd.equalsIgnoreCase("quit")){output.append(cmd+"\nSaindo do Satanachia e parando o serviço do terminal…\n");saveSession();closeTerminalApp();return;}
if(pendingTaskConfirm){pendingTaskConfirm=false;if(cmd.equalsIgnoreCase("s")||cmd.equalsIgnoreCase("sim")||cmd.equalsIgnoreCase("y")||cmd.equalsIgnoreCase("yes")){output.append(cmd+"\n"+taskList()+"\n");showPrompt();return;}output.append(cmd+"\nOperação cancelada.\n");showPrompt();return;}if(!pendingInstall.isEmpty()){String name=pendingInstall;pendingInstall="";if(cmd.equalsIgnoreCase("s")||cmd.equalsIgnoreCase("sim")||cmd.equalsIgnoreCase("y")||cmd.equalsIgnoreCase("yes")){output.append(cmd+"\n");worker.execute(()->{String result=installPackage(name,true);runOnUiThread(()->{output.append(result+"\n");showPrompt();});});return;}output.append(cmd+"\nInstalação cancelada.\n");showPrompt();return;}output.append(cmd+"\n");history.add(cmd);historyPos=history.size();saveSession();worker.execute(()->{String result=run(cmd);String name=BannerArt.command(cmd);String artwork=loadBanner(name);runOnUiThread(()->{if(!artwork.isEmpty()){SpannableStringBuilder colored=new SpannableStringBuilder(artwork);colored.setSpan(new ForegroundColorSpan(BannerArt.color(name)),0,colored.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);output.append(colored);if(!artwork.endsWith("\n"))output.append("\n");}if(!result.isEmpty())output.append(result+"\n");android.text.util.Linkify.addLinks(output,android.text.util.Linkify.WEB_URLS);saveSession();showPrompt();saveSession();});});}
  private String loadBanner(String name){if(name.isEmpty())return "";try{return BannerArt.read(getAssets().open(name+".txt"));}catch(IOException e){return "";}}
  private void insert(String s){int at=Math.max(0,input.getSelectionStart());input.getText().insert(at,s);input.setSelection(at+s.length());input.requestFocus();}
  private void moveCursor(boolean end){input.setSelection(end?input.length():0);input.requestFocus();}private void moveBy(int d){int at=Math.max(0,input.getSelectionStart());input.setSelection(Math.max(0,Math.min(input.length(),at+d)));}private void scrollByPage(int n){scroll.smoothScrollBy(0,n*Math.max(100,scroll.getHeight()*3/4));input.requestFocus();}private void historyMove(int d){if(history.isEmpty())return;historyPos=Math.max(0,Math.min(history.size(),historyPos+d));input.setText(historyPos==history.size()?"":history.get(historyPos));input.setSelection(input.length());}
  private String run(String command){
    try {
      String[] a=command.trim().split("\\s+"); String c=a[0].toLowerCase(Locale.ROOT);
      if(c.equals("bash"))return runBashCommand(command);
      if(c.equals("exit")||c.equals("quit")||c.equals("logout"))return "Saindo do Satanachia.";
      if(c.equals("start")){runOnUiThread(this::openNewTab);return "Nova aba aberta.";}
      if(c.equals("help")||c.equals("ajuda"))return helpText();
      if(FerramentasSeguras.reconhece(c))return FerramentasSeguras.executar(command,cwd(),System.getenv("PATH"));
      if(c.equals("clear")||c.equals("cls")){runOnUiThread(()->output.setText(""));return "";}
      if(c.equals("pwd"))return cwd().getAbsolutePath();
      if(c.equals("readmd"))return readMarkdown(a);
      if(c.equals("cd")){File f=a.length>1?new File(cwd(),a[1]):chooseStartDir();if(f.isDirectory()){tabs.get(activeTab).cwd=f.getCanonicalFile();return "";}return "cd: diretório inválido";}
      if(c.equals("make")){if(a.length<2)return "Uso: make [-p] pasta";boolean parents=false;String name=a[a.length-1];for(String x:a)if(x.equals("-p")||x.equals("--parents"))parents=true;File f=new File(cwd(),name);boolean ok=parents?f.mkdirs():f.mkdir();return ok?"":"make: não foi possível criar diretório";}
      if(c.equals("tree")){File f=a.length>1?new File(cwd(),a[1]):cwd();if(!f.canRead())return "tree: sem acesso";StringBuilder b=new StringBuilder(f.getName().isEmpty()?f.getAbsolutePath():f.getName());tree(f,"",b,0);return b.toString();}
      if(c.equals("pick")){if(a.length<2)return "Uso: pick URL|usuario/repo";String url=a[1];if(!url.contains("://")&&!url.startsWith("git@"))url="https://github.com/"+url.replaceAll("\\.git$","")+".git";return shell("git clone -- "+quote(url));}
      if(c.equals("github"))return githubCommand(a);
      if(c.equals("net"))return netInfo();
      if(c.equals("device"))return deviceInfo();
      if(c.equals("battery"))return batteryInfo();
      if(c.equals("storage"))return storageInfo();
      if(c.equals("screen"))return screenInfo();
      if(c.equals("uptime"))return uptimeInfo();
      if(c.equals("locale"))return Locale.getDefault().toLanguageTag();
      if(c.equals("timezone"))return TimeZone.getDefault().getID()+" · "+TimeZone.getDefault().getDisplayName();
      if(c.equals("volume"))return volumeInfo();
      if(c.equals("ringer"))return ringerInfo();
      if(c.equals("network"))return networkInfo();
      if(c.equals("wifi-settings")){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)));return "Abri as configurações de Wi-Fi do Android.";}
      if(c.equals("sound-settings")){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS)));return "Abri as configurações de som do Android.";}
      if(c.equals("display-settings")){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)));return "Abri as configurações de tela do Android.";}
      if(c.equals("location-settings")){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));return "Abri as configurações de localização do Android.";}
      if(c.equals("app-settings")){if(a.length<2)return "Uso: app-settings <nome-ou-pacote>";return appDetails(a[1]);}
      if(c.equals("airplane-settings"))return openAndroidSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS,"modo avião");
      if(c.equals("bluetooth-settings"))return openAndroidSettings(Settings.ACTION_BLUETOOTH_SETTINGS,"Bluetooth");
      if(c.equals("nfc-settings"))return openAndroidSettings(Settings.ACTION_NFC_SETTINGS,"NFC");
      if(c.equals("keyboard-settings"))return openAndroidSettings(Settings.ACTION_INPUT_METHOD_SETTINGS,"teclado e métodos de entrada");
      if(c.equals("accessibility-settings"))return openAndroidSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS,"acessibilidade");
      if(c.equals("security-settings"))return openAndroidSettings(Settings.ACTION_SECURITY_SETTINGS,"segurança");
      if(c.equals("apps-settings"))return openAndroidSettings(Settings.ACTION_APPLICATION_SETTINGS,"aplicativos");
      if(c.equals("battery-settings"))return openAndroidSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS,"economia de bateria");
      if(c.equals("date-settings"))return openAndroidSettings(Settings.ACTION_DATE_SETTINGS,"data e hora");
      if(c.equals("language-settings"))return openAndroidSettings(Settings.ACTION_LOCALE_SETTINGS,"idioma e região");
      if(c.equals("search"))return webSearchCommand(command);
      if(c.equals("web"))return webCommand(a);
      if(c.equals("dns"))return a.length<2?"Uso: dns <domínio>":resolveHost(a[1]);
      if(c.equals("whois"))return a.length<2?"Uso: whois <domínio> — consulta RDAP público":rdapDomain(a[1]);
      if(c.equals("ip")&&a.length>1&&a[1].equals("a"))return netInfo();
      if(c.equals("ifconfig"))return netInfo();
      if(c.equals("flash"))return flashCommand(a);
      if(c.equals("tasklist")){if(a.length>1&&a[1].equalsIgnoreCase("-sudozi")){if(Global.getInt(getContentResolver(),Global.DEVELOPMENT_SETTINGS_ENABLED,0)==0)return "Ative Opções do desenvolvedor nas Configurações do Android. Isso não libera acesso a memória detalhada nem a processos de outros apps.";pendingTaskConfirm=true;return "Mostrar a lista limitada de processos visíveis ao Android? (s/n)";}return "Uso: tasklist -sudozi";}
      if(c.equals("taskkill")){if(a.length<3||!a[1].equalsIgnoreCase("-sudozi"))return "Uso: taskkill -sudozi <pacote-ou-app>";return appDetails(a[2]);}
      if(c.equals("avrcp"))return "Android não expõe ao app a versão negociada do protocolo AVRCP do Bluetooth; não é possível consultá-la com confiabilidade.";
      if(c.equals("ondeuto")){if(a.length<2)return "Uso: ondeuto -ip | ondeuto -info";if(a[1].equalsIgnoreCase("-ip"))return publicIpLocation();if(a[1].equalsIgnoreCase("-info"))return preciseLocation();return "Uso: ondeuto -ip | ondeuto -info";}
      if(c.equals("applist"))return appList(a);
      if(c.equals("docs"))return findFiles(a,false);
      if(c.equals("say")){if(a.length<2)return "Uso: say <texto>";String text=command.substring(command.indexOf(' ')+1);runOnUiThread(()->{if(tts!=null){tts.setLanguage(Locale.getDefault());tts.speak(text,TextToSpeech.QUEUE_FLUSH,null,"ncrox-say");}});return "";}
      if(c.equals("open"))return openApp(a);
      if(c.equals("satanachia"))return storeCommand(a);
      if(c.equals("acquire")&&a.length>1&&a[1].equals("lock")){setWakeLock(true);return "Wake lock ligado";}
      if(c.equals("release")&&a.length>1&&a[1].equals("lock")){setWakeLock(false);return "Wake lock desligado";}
      if(c.equals("tabs"))return "Abas abertas: "+tabs.size()+" (toque ☰ para alternar/criar)";
      if(c.equals("settings")){runOnUiThread(this::showSettings);return "";}
      return shell(command);
    }catch(Exception e){return "Erro: "+e.getMessage();}
  }
  private String helpText(){
    StringBuilder b=new StringBuilder("NCROX — comandos próprios: pwd, readmd, cd, make, tree, pick, github, pack, nano, edit, bash, search <termo>, search -o <termo>, web open|get|source|title|links|headers|status <url>, web dns|rdap <domínio>, dns, whois, device, battery, storage, screen, uptime, locale, timezone, volume, ringer, network, wifi-settings, sound-settings, display-settings, location-settings, app-settings, airplane-settings, bluetooth-settings, nfc-settings, keyboard-settings, accessibility-settings, security-settings, apps-settings, battery-settings, date-settings, language-settings, net, ip a, ifconfig, flash, tasklist, taskkill, avrcp, ondeuto, applist, docs, say, open, satanachia, acquire lock, release lock, tabs, start, exit, logout, settings, clear.\n\nExecutáveis do sistema visíveis no aparelho (variáveis conforme fabricante/Android):\n");
    TreeSet<String> names=new TreeSet<>();String path=System.getenv("PATH");if(path!=null)for(String dir:path.split(":"))addExecutables(new File(dir),names);
    for(String dir:new String[]{"/system/bin","/system/xbin","/vendor/bin","/vendor/xbin","/product/bin","/apex/com.android.runtime/bin","/apex/com.android.art/bin"})addExecutables(new File(dir),names);
    int n=0;for(String name:names){if(n>0)b.append(n%6==0?"\n":"  ");b.append(name);n++;}
    b.append("\nTotal encontrado: ").append(n).append(" executáveis. A lista muda entre aparelhos; não significa que todos aceitem entrada interativa. `vi` pode existir no Android, mas sem PTY pode não funcionar como editor. Use `nano <arquivo>` para o editor simples integrado.\n\nBash embutido 5.3 roda comando por comando: sem sessão persistente, stdin interativo, serviços/pkg/apt do Termux ou root. Comando desconhecido? `pack search <nome>` consulta catálogo Termux; para instalar, abra Termux e use `pkg install <nome>`. `start` abre nova aba; `exit`, `logout` ou `quit` fecha o app e para o serviço.\n\n").append(FerramentasSeguras.ajuda());return b.toString();
  }
  private void addExecutables(File dir,Set<String> names){File[] fs=dir.listFiles();if(fs==null)return;for(File f:fs){String n=f.getName();if(!f.isFile()||!f.canExecute()||n.endsWith(".so")||n.startsWith("linker")||n.startsWith("crash_dump")||n.equals("oatdump")||n.equals("dexlist")||n.equals("dexdump"))continue;names.add(n);}}

  private String packCommand(String[] a){
    if(a.length<2||a[1].equalsIgnoreCase("help")||a[1].equalsIgnoreCase("--help")||a[1].equalsIgnoreCase("-h"))return "pack — consulta ao catálogo oficial do Termux (internet necessária).\nUso: pack search <termo> | pack show <pacote> | pack list-all\nExemplos: pack search python; pack show git; pack list-all\nEste comando não é o pkg do Termux: o APK é isolado e não pode instalar/remover programas do próprio sistema nem ler o banco de pacotes do Termux. Para instalar, abra o Termux e rode ali `pkg install <pacote>`. Fonte: packages.termux.dev/apt/termux-main; documentação: wiki.termux.com/wiki/Package_Management.";
    String op=a[1].toLowerCase(Locale.ROOT);
    if(op.equals("--version")||op.equals("-v"))return "pack do Satanachia 1.0 — consulta ao catálogo oficial Termux; não é o gerenciador pkg completo.";
    if(op.equals("list-installed")||op.equals("installed"))return "Não consigo ler a lista de pacotes instalados do Termux: esse banco fica privado no outro app. Abra o Termux e rode `pkg list-installed` lá.";
    if(op.equals("files"))return "A lista de arquivos instalados só existe no banco local do Termux. Abra o Termux e rode `pkg files <pacote>` lá.";
    if(op.equals("install")||op.equals("in")||op.equals("uninstall")||op.equals("remove")||op.equals("upgrade")||op.equals("update")||op.equals("reinstall")||op.equals("full-upgrade")||op.equals("autoremove")||op.equals("clean"))return "No APK Satanachia, `pack "+op+"` não pode instalar nem alterar pacotes: o Android isola o app do Termux e não temos o ambiente pkg dentro do APK. Para isso, abra o Termux oficial e rode `pkg "+op+(a.length>2?" "+a[2]:"")+"` lá. `pack search/show/list-all` consulta o catálogo Termux sem instalar nada.";
    if(op.equals("search")||op.equals("list-all")||op.equals("list")){
      String q="";if(op.equals("search")){if(a.length<3)return "Uso: pack search <termo>";StringBuilder b=new StringBuilder();for(int i=2;i<a.length;i++){if(i>2)b.append(' ');b.append(a[i]);}q=b.toString().toLowerCase(Locale.ROOT);}
      try{return queryTermuxPackages(q,op.equals("list-all")||op.equals("list")?100:50);}catch(Exception e){return "Não consegui consultar o índice oficial do Termux: "+e.getMessage()+"\nConfere sua conexão e tenta de novo. A fonte é packages.termux.dev; nada foi instalado.";}
    }
    if(op.equals("show")||op.equals("info")){
      if(a.length<3)return "Uso: pack show <nome-do-pacote>";
      try{return showTermuxPackage(a[2].toLowerCase(Locale.ROOT));}catch(Exception e){return "Não consegui consultar o pacote no índice oficial: "+e.getMessage()+". Nada foi instalado.";}
    }
    return "Ação não suportada por `pack`: "+a[1]+". Este APK só consulta o catálogo; use `pack help`. Instalação e administração têm de ser feitas dentro do Termux com `pkg`.";
  }
  private String termuxArch(){
    String[] abis=Build.SUPPORTED_ABIS;for(String abi:abis){if(abi.equals("arm64-v8a"))return "aarch64";if(abi.equals("armeabi-v7a")||abi.equals("armeabi"))return "arm";if(abi.equals("x86_64"))return "x86_64";if(abi.equals("x86"))return "i686";}return "aarch64";
  }
  private String termuxPackagesUrl(){return "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-"+termuxArch()+"/Packages.gz";}
  private String queryTermuxPackages(String query,int limit)throws Exception{
    java.net.HttpURLConnection c=(java.net.HttpURLConnection)new URL(termuxPackagesUrl()).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","Satanachia-ncrox-pack/1.0");
    StringBuilder out=new StringBuilder("Catálogo oficial do Termux · "+termuxArch()+" · "+(query.isEmpty()?"lista inicial de pacotes":"busca: "+query)+"\n");int found=0;String pkg=null,ver=null,desc=null;long count=0;
    try(java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(c.getInputStream()),"UTF-8"))){
      String line;while((line=r.readLine())!=null){if(line.isEmpty()){if(pkg!=null&&(query.isEmpty()||pkg.toLowerCase(Locale.ROOT).contains(query)||(desc!=null&&desc.toLowerCase(Locale.ROOT).contains(query)))){out.append("• ").append(pkg).append(ver==null?"":" — "+ver).append(desc==null?"":"\n  "+desc).append('\n');if(++found>=limit)break;}pkg=null;ver=null;desc=null;count++;continue;}if(line.startsWith("Package: "))pkg=line.substring(9).trim();else if(line.startsWith("Version: "))ver=line.substring(9).trim();else if(line.startsWith("Description: "))desc=line.substring(13).trim();else if(line.startsWith(" ")&&desc!=null)desc+=" "+line.trim();}
    }finally{c.disconnect();}
    if(found==0)return "Não achei pacote "+(query.isEmpty()?"":("que combine com `"+query+"` "))+"no índice Termux consultado. Confere o nome, a arquitetura e os repositórios oficiais. Nada foi instalado.";
    out.append("\nMostrando ").append(found);if(found>=limit)out.append(" resultados (limite da consulta)");out.append(". Consulta somente leitura; para instalar no Termux, execute ali `pkg install <nome>`.\nFonte: ").append(termuxPackagesUrl());return out.toString();
  }
  private String showTermuxPackage(String name)throws Exception{
    java.net.HttpURLConnection c=(java.net.HttpURLConnection)new URL(termuxPackagesUrl()).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(15000);c.setRequestProperty("User-Agent","Satanachia-ncrox-pack/1.0");StringBuilder record=new StringBuilder();boolean match=false;
    try(java.io.BufferedReader r=new java.io.BufferedReader(new java.io.InputStreamReader(new java.util.zip.GZIPInputStream(c.getInputStream()),"UTF-8"))){String line;while((line=r.readLine())!=null){if(line.isEmpty()){if(match)return "Pacote do repositório oficial Termux\n"+record.toString().trim()+"\n\nPara instalar no Termux: `pkg install "+name+"`. `pack show` só consulta, não instala.\nFonte: "+termuxPackagesUrl();record.setLength(0);match=false;continue;}if(line.startsWith("Package: "))match=line.substring(9).trim().equals(name);if(match)record.append(line).append('\n');}}finally{c.disconnect();}
    return "Não achei `"+name+"` no índice Termux para arquitetura "+termuxArch()+". Tenta `pack search "+name+"`.";
  }

  private String deviceInfo(){return "Dispositivo: "+Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\nABI: "+Arrays.toString(Build.SUPPORTED_ABIS)+"\nApp: "+getPackageName();}
  private String batteryInfo(){Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(i==null)return "Android não forneceu estado da bateria.";int level=i.getIntExtra(BatteryManager.EXTRA_LEVEL,-1),scale=i.getIntExtra(BatteryManager.EXTRA_SCALE,100),status=i.getIntExtra(BatteryManager.EXTRA_STATUS,-1),plug=i.getIntExtra(BatteryManager.EXTRA_PLUGGED,0);String state=status==BatteryManager.BATTERY_STATUS_CHARGING?"carregando":status==BatteryManager.BATTERY_STATUS_FULL?"cheia":status==BatteryManager.BATTERY_STATUS_DISCHARGING?"descarregando":"estado desconhecido";return "Bateria: "+(level<0?"?":(level*100/Math.max(scale,1))+"%")+" · "+state+(plug!=0?" · conectada à energia":"");}
  private String storageInfo(){try{android.os.StatFs s=new android.os.StatFs(Environment.getDataDirectory().getAbsolutePath());long total=s.getTotalBytes(),free=s.getAvailableBytes();return "Armazenamento do app/sistema\nTotal: "+android.text.format.Formatter.formatFileSize(this,total)+"\nLivre: "+android.text.format.Formatter.formatFileSize(this,free)+"\nUso aproximado: "+android.text.format.Formatter.formatFileSize(this,total-free);}catch(Exception e){return "Não consegui consultar armazenamento: "+e.getMessage();}}
  private String screenInfo(){android.util.DisplayMetrics m=getResources().getDisplayMetrics();return m.widthPixels+"x"+m.heightPixels+" px · "+m.densityDpi+" dpi · escala "+m.density;}
  private String uptimeInfo(){long ms=android.os.SystemClock.elapsedRealtime();return "Tempo ligado: "+(ms/3600000)+" h "+((ms/60000)%60)+" min (desde a última inicialização, segundo o Android).";}
  private String volumeInfo(){android.media.AudioManager am=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);String[] labels={"música", "alarme", "toque", "notificações", "voz"};int[] streams={android.media.AudioManager.STREAM_MUSIC,android.media.AudioManager.STREAM_ALARM,android.media.AudioManager.STREAM_RING,android.media.AudioManager.STREAM_NOTIFICATION,android.media.AudioManager.STREAM_VOICE_CALL};StringBuilder b=new StringBuilder();for(int i=0;i<streams.length;i++){if(i>0)b.append('\n');b.append(labels[i]).append(": ").append(am.getStreamVolume(streams[i])).append('/').append(am.getStreamMaxVolume(streams[i]));}return b.toString();}
  private String ringerInfo(){android.media.AudioManager am=(android.media.AudioManager)getSystemService(AUDIO_SERVICE);int m=am.getRingerMode();return "Modo de toque: "+(m==android.media.AudioManager.RINGER_MODE_SILENT?"silencioso":m==android.media.AudioManager.RINGER_MODE_VIBRATE?"vibração":"normal");}
  private String networkInfo(){try{android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);android.net.Network n=cm.getActiveNetwork();if(n==null)return "Sem rede ativa visível ao app.";android.net.NetworkCapabilities c=cm.getNetworkCapabilities(n);if(c==null)return "Rede ativa sem detalhes disponíveis.";String kind=c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)?"dados móveis":c.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)?"Ethernet":"outra rede";return kind+(c.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)?" · internet validada":" · acesso à internet não confirmado");}catch(Exception e){return "Não consegui consultar a rede: "+e.getMessage();}}
  private String webSearchCommand(String command){String q=command.substring("search".length()).trim();if(q.length()>1&&q.startsWith("_")&&q.endsWith("_")&&!q.startsWith("-o "))q=q.substring(1,q.length()-1).replace('_',' ').trim();if(q.isEmpty())return "Uso: search <termo> | search -o <termo> (abre a busca no navegador padrão).";if(q.startsWith("-o ")){String query=q.substring(3).trim();if(query.length()>1&&query.startsWith("_")&&query.endsWith("_"))query=query.substring(1,query.length()-1).replace('_',' ').trim();if(query.isEmpty())return "Uso: search -o <termo>";openExternal("https://www.google.com/search?q="+urlEncode(query));return "Abri a busca no navegador padrão do aparelho.";}if(q.startsWith("_ "))q=q.substring(2);try{return fetchSearchResults(q);}catch(Exception e){return "Não consegui buscar na web agora: "+e.getMessage()+"\nTenta `search -o "+q+"` para abrir a busca no navegador padrão.";}}
  private String fetchSearchResults(String q)throws Exception{String url="https://www.bing.com/search?format=rss&q="+urlEncode(q);String xml=httpGet(url);org.xmlpull.v1.XmlPullParser p=android.util.Xml.newPullParser();p.setInput(new java.io.StringReader(xml));String tag="",title="",link="",desc="";ArrayList<String[]> items=new ArrayList<>();int event=p.getEventType();boolean inItem=false;while(event!=org.xmlpull.v1.XmlPullParser.END_DOCUMENT&&items.size()<3){if(event==org.xmlpull.v1.XmlPullParser.START_TAG){tag=p.getName();if(tag.equalsIgnoreCase("item")){inItem=true;title="";link="";desc="";}}else if(event==org.xmlpull.v1.XmlPullParser.TEXT&&inItem){if(tag.equalsIgnoreCase("title"))title=p.getText();else if(tag.equalsIgnoreCase("link"))link=p.getText();else if(tag.equalsIgnoreCase("description"))desc=p.getText();}else if(event==org.xmlpull.v1.XmlPullParser.END_TAG&&p.getName().equalsIgnoreCase("item")){inItem=false;if(!link.isEmpty())items.add(new String[]{title,link,android.text.Html.fromHtml(desc,android.text.Html.FROM_HTML_MODE_LEGACY).toString()});}event=p.next();}if(items.isEmpty())return "A busca não retornou resultados em formato legível. Usa `search -o "+q+"` pra abrir no navegador.";StringBuilder b=new StringBuilder("Busca: "+q+"\nResultados da busca do Bing (resumo dos 3 primeiros): ");for(int i=0;i<items.size();i++){if(i>0)b.append(" ");b.append(items.get(i)[0]);if(!items.get(i)[2].isEmpty())b.append(" — ").append(items.get(i)[2]);}b.append("\n\nLinks (toque para abrir):\n");for(int i=0;i<items.size();i++)b.append(i+1).append(". ").append(items.get(i)[0]).append("\n   ").append(items.get(i)[1]).append("\n");return b.toString();}
  private String webCommand(String[] a){
    if(a.length<3)return "Uso: web open|get|source|title|links|headers|status <url> | web dns|rdap <domínio>";
    String op=a[1].toLowerCase(Locale.ROOT),url=a[2];
    if(op.equals("dns"))return resolveHost(url);
    if(op.equals("rdap"))return rdapDomain(url);
    if(op.equals("open")){openExternal(url);return "Abri no navegador padrão.";}
    try{
      String safe=normalizeWebUrl(url);
      if(op.equals("get")||op.equals("source"))return httpGet(safe);
      if(op.equals("title")){String html=httpGet(safe);java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(html);return m.find()?android.text.Html.fromHtml(m.group(1),android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim():"Não achei a tag <title> na página.";}
      if(op.equals("links")){String html=httpGet(safe);java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?is)<a[^>]+href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>").matcher(html);StringBuilder b=new StringBuilder("Links encontrados (até 20):\n");int n=0;while(m.find()&&n<20){String href=m.group(1);if(href.startsWith("http://")||href.startsWith("https://")){String title=android.text.Html.fromHtml(m.group(2),android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim().replaceAll("\\s+"," ");b.append(++n).append(". ").append(title.isEmpty()?href:title).append("\n   ").append(href).append('\n');}}return n==0?"Não achei links absolutos http(s) nesta página.":b.toString();}
      if(op.equals("headers")||op.equals("status")){HttpURLConnection c=(HttpURLConnection)new URL(safe).openConnection();c.setRequestMethod("HEAD");c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("User-Agent","Satanachia-ncrox");try{int status=c.getResponseCode();if(op.equals("status"))return "HTTP "+status+" "+c.getResponseMessage();StringBuilder b=new StringBuilder("HTTP "+status+"\n");for(Map.Entry<String,List<String>> h:c.getHeaderFields().entrySet())if(h.getKey()!=null)b.append(h.getKey()).append(": ").append(String.join(", ",h.getValue())).append('\n');return b.toString();}finally{c.disconnect();}}
    }catch(Exception e){return "Não consegui consultar a página: "+e.getMessage();}
    return "Uso: web open|get|source|title|links|headers|status <url> | web dns|rdap <domínio>";
  }
  private String resolveHost(String host){try{if(host.contains("/")||host.contains("://"))return "Informe só o nome do domínio, sem caminho.";java.net.InetAddress[] ips=java.net.InetAddress.getAllByName(host);StringBuilder b=new StringBuilder("DNS do resolvedor do aparelho · ").append(host).append('\n');for(java.net.InetAddress ip:ips)b.append("• ").append(ip.getHostAddress()).append('\n');return b.toString().trim();}catch(Exception e){return "Não consegui resolver "+host+": "+e.getMessage();}}
  private String rdapDomain(String domain){try{if(!domain.matches("(?i)[a-z0-9][a-z0-9.-]{0,250}\\.[a-z]{2,63}"))return "Informe um domínio, por exemplo example.com.";return httpGet("https://rdap.org/domain/"+URLEncoder.encode(domain,"UTF-8"));}catch(Exception e){return "Consulta RDAP falhou: "+e.getMessage();}}
  private String normalizeWebUrl(String raw)throws Exception{String x=raw.matches("(?i)^https?://.*")?raw:"https://"+raw;URL u=new URL(x);if(!u.getProtocol().equalsIgnoreCase("http")&&!u.getProtocol().equalsIgnoreCase("https"))throw new IOException("só aceito HTTP e HTTPS");return x;}
  private String openAndroidSettings(String action,String label){runOnUiThread(()->{try{startActivity(new Intent(action));}catch(Exception e){Toast.makeText(this,"Essa tela não existe nesta versão do Android.",Toast.LENGTH_LONG).show();}});return "Abri as configurações de "+label+" do Android, se o aparelho oferecer essa tela.";}
  private void openExternal(String raw){try{String u=raw;if(!u.matches("(?i)^https?://.*"))u="https://"+u;Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(u));i.addCategory(Intent.CATEGORY_BROWSABLE);runOnUiThread(()->{try{startActivity(i);}catch(Exception e){Toast.makeText(this,"Não achei navegador padrão.",Toast.LENGTH_LONG).show();}});}catch(Exception ignored){}}
  private String urlEncode(String q){try{return URLEncoder.encode(q,"UTF-8");}catch(Exception e){return q;}}
  private String readMarkdown(String[] a){if(a.length<2)return "Uso: readmd <arquivo.md>";File f=new File(cwd(),a[1]);if(!f.getName().toLowerCase(Locale.ROOT).endsWith(".md"))return "readmd: precisa de extensão .md";if(!f.isFile()||!f.canRead())return "readmd: arquivo não encontrado ou sem acesso";try{BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));StringBuilder b=new StringBuilder();String line;int n=0;while((line=r.readLine())!=null&&n++<20000){if(b.length()>0)b.append('\n');b.append(line);}r.close();if(n>=20000)b.append("\n[arquivo truncado]");return b.toString();}catch(Exception e){return "readmd: erro ao ler arquivo";}}
  private String netInfo(){try{String s=shell("ip addr");if(!s.startsWith("Erro")&&!s.contains("not found")&&!s.contains("[Código de saída:")&&!s.contains("[tempo limite"))return s;}catch(Exception ignored){}StringBuilder b=new StringBuilder();try{Enumeration<java.net.NetworkInterface> e=java.net.NetworkInterface.getNetworkInterfaces();while(e.hasMoreElements()){java.net.NetworkInterface n=e.nextElement();b.append(n.getName()).append(n.isUp()?" UP":" DOWN");Enumeration<java.net.InetAddress> ips=n.getInetAddresses();while(ips.hasMoreElements())b.append("\n  ").append(ips.nextElement().getHostAddress());b.append('\n');}}catch(Exception e){return "Não foi possível listar interfaces: "+e.getMessage();}return b.length()==0?"Nenhuma interface visível.":b.toString();}
  private String flashCommand(String[] a){if(a.length<2)return "Uso: flash -on | -off | -blink | -os -time 1..300";if(cameraId==null)return "Este aparelho não expõe flash compatível.";if(checkSelfPermission("android.permission.CAMERA")!=PackageManager.PERMISSION_GRANTED){runOnUiThread(()->requestPermissions(new String[]{"android.permission.CAMERA"},410));return "Autorize a câmera para usar o flash e repita o comando.";}if(a[1].equalsIgnoreCase("-on")){torch(true);return "Flash ligado";}if(a[1].equalsIgnoreCase("-off")){torch(false);return "Flash desligado";}if(a[1].equalsIgnoreCase("-blink")){for(int i=0;i<8;i++){final boolean on=i%2==0;mainHandler.postDelayed(()->torch(on),i*450L);}mainHandler.postDelayed(()->torch(false),8*450L);return "Flash: quatro piscadas, depois desligado";}if(a.length>=4&&a[1].equalsIgnoreCase("-os")&&a[2].equalsIgnoreCase("-time")){try{int sec=Integer.parseInt(a[3]);if(sec<1||sec>300)return "Tempo permitido: 1 a 300 segundos";torch(true);mainHandler.postDelayed(()->torch(false),sec*1000L);return "Flash ligado por "+sec+" s";}catch(NumberFormatException e){return "Informe o tempo em segundos (1–300)";}}return "Uso: flash -on | -off | -blink | -os -time 1..300";}
  private void torch(boolean on){try{cameraManager.setTorchMode(cameraId,on);}catch(Exception ignored){}}
  private String taskList(){ActivityManager am=(ActivityManager)getSystemService(ACTIVITY_SERVICE);ActivityManager.MemoryInfo mi=new ActivityManager.MemoryInfo();am.getMemoryInfo(mi);StringBuilder b=new StringBuilder("Processos que Android expõe a este app (não representa todos os serviços nem memória por app):\nMemória total: "+(mi.totalMem/1048576)+" MB; disponível: "+(mi.availMem/1048576)+" MB\n");List<ActivityManager.RunningAppProcessInfo> ps=am.getRunningAppProcesses();if(ps==null||ps.isEmpty())return b.append("O Android não forneceu lista de processos.").toString();for(ActivityManager.RunningAppProcessInfo p:ps)b.append("╰── ").append(p.processName).append(" | pid ").append(p.pid).append(" | importance ").append(p.importance).append('\n');return b.toString();}
  private String appDetails(String token){String pkg=findPackage(token);if(pkg==null)return "Não achei o app/pacote instalado: "+token;runOnUiThread(()->{try{Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+pkg));startActivity(i);}catch(Exception ignored){}});return "Android não permite a este app encerrar outro processo. Abri as informações do app; use Forçar parada se quiser interrompê-lo.";}
  private String findPackage(String term){PackageManager pm=getPackageManager();for(ApplicationInfo ai:pm.getInstalledApplications(PackageManager.GET_META_DATA)){String label=String.valueOf(pm.getApplicationLabel(ai));if(ai.packageName.equalsIgnoreCase(term)||label.equalsIgnoreCase(term)||ai.packageName.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT)))return ai.packageName;}return null;}
  private String appList(String[] a){boolean system=a.length>1&&a[1].equalsIgnoreCase("-sudozi");boolean onlyApk=a.length>1&&a[1].equalsIgnoreCase("-apk");if(onlyApk)return findFiles(new String[]{"docs",".apk"},true);PackageManager pm=getPackageManager();List<ApplicationInfo> list=pm.getInstalledApplications(PackageManager.GET_META_DATA);Collections.sort(list,(x,y)->String.valueOf(pm.getApplicationLabel(x)).compareToIgnoreCase(String.valueOf(pm.getApplicationLabel(y))));StringBuilder b=new StringBuilder();int n=0;for(ApplicationInfo ai:list){boolean sys=(ai.flags&ApplicationInfo.FLAG_SYSTEM)!=0;if(!system&&sys)continue;b.append(n++==0?"":"\n").append("╰── ").append(pm.getApplicationLabel(ai)).append(" (").append(ai.packageName).append(")").append(sys?" [sistema]":"");}return n==0?"Nenhum app visível para PackageManager.":b.toString();}
  private String findFiles(String[] a,boolean forceApk){if(a.length<2)return "Uso: docs -<extensão>";String ext=forceApk?".apk":a[1].toLowerCase(Locale.ROOT);if(ext.startsWith("-"))ext=ext.substring(1);if(!ext.startsWith("."))ext="."+ext;if(!hasFileAccess())return "Conceda acesso aos arquivos compartilhados para buscar por extensão.";File rootDir=Environment.getExternalStorageDirectory();StringBuilder b=new StringBuilder();int[] count={0};walkExt(rootDir,ext,b,count,0);return count[0]==0?"Nenhum arquivo *"+ext+" encontrado nas pastas compartilhadas.":b.toString();}
  private void walkExt(File d,String ext,StringBuilder out,int[] count,int depth){if(depth>12||count[0]>=1000)return;File[] fs=d.listFiles();if(fs==null)return;for(File f:fs){if(f.isDirectory()){if(!f.isHidden())walkExt(f,ext,out,count,depth+1);}else if(f.getName().toLowerCase(Locale.ROOT).endsWith(ext)){out.append(count[0]++==0?"":"\n").append("╰── ").append(f.getAbsolutePath());}}}
  private String openApp(String[] a){if(a.length<2)return "Uso: open -a <app> | open .<app>";String name=a[a.length-1];if(name.startsWith("."))name=name.substring(1);if(name.equalsIgnoreCase("-a")&&a.length>2)name=a[2];String pkg=findPackage(name);if(pkg==null)return "App não encontrado ou não listado pelo Android: "+name;Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null)return "O pacote existe, mas o Android não oferece uma tela inicializável para ele.";runOnUiThread(()->startActivity(i));return "Abrindo "+name;}
  // Consultas somente de leitura; nenhum código é baixado ou executado.
  private String githubCommand(String[] a){
    String usage="Uso: github repo <dono/repo> | user <usuário> | releases <dono/repo> | issues <dono/repo> | search <termos>";
    if(a.length<3)return usage;
    try {
      String op=a[1].toLowerCase(Locale.ROOT), value=a[2], base="https://api.github.com/";
      if(op.equals("search")){
        StringBuilder q=new StringBuilder();for(int i=2;i<a.length;i++){if(i>2)q.append(' ');q.append(a[i]);}
        JSONObject result=new JSONObject(httpGet(base+"search/repositories?per_page=10&q="+URLEncoder.encode(q.toString(),"UTF-8")));
        JSONArray items=result.getJSONArray("items");StringBuilder out=new StringBuilder("Repositórios públicos (até 10):");
        for(int i=0;i<items.length();i++){JSONObject o=items.getJSONObject(i);out.append("\n").append(o.optString("full_name")).append(" | estrelas: ").append(o.optInt("stargazers_count")).append(" | ").append(o.optString("html_url"));}
        return items.length()==0?"Nenhum repositório encontrado.":out.toString();
      }
      if(a.length!=3)return usage;
      if(op.equals("user")){
        if(!value.matches("[A-Za-z0-9][A-Za-z0-9-]{0,38}"))return "Usuário GitHub inválido.";
        JSONObject o=new JSONObject(httpGet(base+"users/"+value));
        return o.optString("login")+" | "+o.optString("html_url")+"\nNome: "+o.optString("name","")+"\nRepositórios públicos: "+o.optInt("public_repos")+" | seguidores: "+o.optInt("followers");
      }
      if(!op.equals("repo")&&!op.equals("releases")&&!op.equals("issues"))return usage;
      if(!value.matches("[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}")||value.endsWith("/.")||value.endsWith("/.."))return "Repositório inválido; use dono/repo.";
      String url=base+"repos/"+value;
      if(op.equals("repo")){
        JSONObject o=new JSONObject(httpGet(url));
        return o.optString("full_name")+" | "+o.optString("html_url")+"\n"+o.optString("description","")+"\nLinguagem: "+o.optString("language","")+" | estrelas: "+o.optInt("stargazers_count")+" | forks: "+o.optInt("forks_count")+"\nBranch padrão: "+o.optString("default_branch");
      }
      JSONArray items=new JSONArray(httpGet(url+(op.equals("releases")?"/releases?per_page=10":"/issues?state=open&per_page=10")));
      StringBuilder out=new StringBuilder(op.equals("releases")?"Releases públicas (até 10):":"Issues abertas (até 10 resultados; PRs omitidos):");int shown=0;
      for(int i=0;i<items.length();i++){JSONObject o=items.getJSONObject(i);if(op.equals("issues")&&o.has("pull_request"))continue;shown++;out.append("\n").append(op.equals("releases")?o.optString("tag_name"):"#"+o.optInt("number")).append(" | ").append(o.optString("name",o.optString("title"))).append(" | ").append(o.optString("html_url"));}
      return shown==0?"Nenhum resultado público nesta página.":out.toString();
    }catch(Exception e){return "GitHub indisponível ou consulta recusada: "+e.getMessage()+" (API pública sem autenticação tem limites).";}
  }
  private String storeCommand(String[] a){if(a.length<2)return "Uso: satanachia -store | -pick <nome> | -st <nome>";if(a[1].equalsIgnoreCase("-store"))return storeList();if(a[1].equalsIgnoreCase("-pick")&&a.length>2)return downloadTool(a[2]);if(a[1].equalsIgnoreCase("-st")&&a.length>2)return installPackage(a[2],false);return "Uso: satanachia -store | -pick <nome> | -st <nome>";}
  private JSONArray getStore()throws Exception{String body=httpGet("https://api.github.com/repos/N3CR0-X/ncrox/contents/store");return new JSONArray(body);}
  private String storeList(){try{JSONArray arr=getStore();StringBuilder b=new StringBuilder("Ferramentas em store/\n");int n=0;for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);if(o.optString("type").equals("file")&&o.optString("name").toLowerCase(Locale.ROOT).endsWith(".zip")){b.append("╰── ").append(o.getString("name")).append('\n');n++;}}return n==0?"loja vazia":b.toString();}catch(Exception e){return "loja vazia (store/ ausente ou indisponível)";}}
  private String downloadTool(String name){try{String target=name.toLowerCase(Locale.ROOT);if(!target.endsWith(".zip"))target+=".zip";JSONArray arr=getStore();for(int i=0;i<arr.length();i++){JSONObject o=arr.getJSONObject(i);if(o.optString("name").equalsIgnoreCase(target)&&o.optString("name").toLowerCase(Locale.ROOT).endsWith(".zip")){String url=o.optString("download_url");if(url.isEmpty())return "Pacote sem URL de download.";File dest=new File(cwd(),o.getString("name"));download(url,dest);return "Baixado: "+dest.getAbsolutePath()+" (SHA-256 "+sha256(dest)+")";}}return arr.length()==0?"loja vazia":"Ferramenta não encontrada na loja: "+name;}catch(Exception e){return "Download falhou: "+e.getMessage();}}
  private String installPackage(String name,boolean replace){String zipName=name.toLowerCase(Locale.ROOT).endsWith(".zip")?name:name+".zip";File zip=findChildIgnoreCase(zipName);if(zip==null){String fetched=downloadTool(name);if(fetched.startsWith("Ferramenta não encontrada")||fetched.startsWith("loja vazia")||fetched.startsWith("Download falhou"))return fetched;zip=findChildIgnoreCase(zipName);if(zip==null)return "Não encontrei o ZIP baixado: "+zipName;}String dirName=zip.getName().substring(0,zip.getName().length()-4);File target=findChildIgnoreCase(dirName);if(target==null)target=new File(cwd(),dirName);if(target.exists())return "Já existe "+dirName+". Por segurança, renomeie ou faça backup da pasta antes de instalar novamente.";File stage=new File(cwd(),".ncrox-stage-"+System.nanoTime());try{net.lingala.zip4j.ZipFile zf=new net.lingala.zip4j.ZipFile(zip,(dirName+"ncrox").toCharArray());for(net.lingala.zip4j.model.FileHeader h:zf.getFileHeaders()){File out=new File(stage,h.getFileName());String rootPath=stage.getCanonicalPath()+File.separator;if(!out.getCanonicalPath().equals(stage.getCanonicalPath())&&!out.getCanonicalPath().startsWith(rootPath)){deleteRec(stage);return "ZIP recusado: contém caminho fora da pasta de instalação.";}}stage.mkdirs();zf.extractAll(stage.getCanonicalPath());if(target.exists()){deleteRec(stage);return "Destino apareceu durante a extração; instalação cancelada para preservar os arquivos.";}if(!stage.renameTo(target)){deleteRec(stage);return "Extração verificada, mas não consegui mover a pasta para o destino.";}return "Ferramenta extraída em "+target.getAbsolutePath()+". Nenhum arquivo foi executado.";}catch(Exception e){deleteRec(stage);return "Não foi possível habilitar "+dirName+": senha inválida, ZIP incompatível ou erro de extração; arquivos existentes foram mantidos.";}}
  private File findChildIgnoreCase(String name){File[] fs=cwd().listFiles();if(fs==null)return null;for(File f:fs)if(f.getName().equalsIgnoreCase(name))return f;return null;}
  private void deleteRec(File f){File[] fs=f.listFiles();if(fs!=null)for(File x:fs)deleteRec(x);f.delete();}
  private String httpGet(String url)throws Exception{
    HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
    c.setConnectTimeout(7000);c.setReadTimeout(7000);c.setRequestProperty("User-Agent","Satanachia-ncrox");
    try{
      int code=c.getResponseCode();if(code>=400)throw new IOException("HTTP "+code);
      try(InputStream in=c.getInputStream();ByteArrayOutputStream b=new ByteArrayOutputStream()){
        byte[] x=new byte[4096];int n;while((n=in.read(x))!=-1){if(b.size()+n>1000000)throw new IOException("resposta maior que 1 MB");b.write(x,0,n);}return b.toString("UTF-8");
      }
    }finally{c.disconnect();}
  }
  private String sha256(File f){try{java.security.MessageDigest d=java.security.MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte x:d.digest())s.append(String.format(Locale.ROOT,"%02x",x));return s.toString();}catch(Exception e){return "indisponível";}}
  private void download(String url,File dest)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(10000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent","Satanachia-ncrox");try(InputStream in=c.getInputStream();FileOutputStream out=new FileOutputStream(dest)){byte[] b=new byte[8192];int n;long total=0;while((n=in.read(b))>0){total+=n;if(total>100L*1024*1024)throw new IOException("arquivo maior que 100 MB");out.write(b,0,n);}}finally{c.disconnect();}}
  private String publicIpLocation(){try{JSONObject o=new JSONObject(httpGet("https://ipapi.co/json/"));return "Localização aproximada pelo IP público (consulta enviada ao serviço ipapi.co; pode estar errada e não confirma VPN):\nIP: "+o.optString("ip","?")+"\nCidade/região: "+o.optString("city","?")+", "+o.optString("region","?")+"\nPaís: "+o.optString("country_name","?");}catch(Exception e){return "Não consegui consultar a geolocalização aproximada do IP: "+e.getMessage();}}
  private String preciseLocation(){if(checkSelfPermission("android.permission.ACCESS_FINE_LOCATION")!=PackageManager.PERMISSION_GRANTED){waitingForLocation=true;runOnUiThread(()->requestPermissions(new String[]{"android.permission.ACCESS_FINE_LOCATION","android.permission.ACCESS_COARSE_LOCATION"},411));return "Preciso da permissão de localização; autorize na janela do Android e repita ondeuto -info.";}LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);try{Location best=null;for(String provider:lm.getProviders(true)){Location l=lm.getLastKnownLocation(provider);if(l!=null&&(best==null||l.getTime()>best.getTime()))best=l;}if(best!=null&&System.currentTimeMillis()-best.getTime()<120000)return "Localização do aparelho (estimativa do provedor; precisão ±"+(int)best.getAccuracy()+" m):\nLat: "+best.getLatitude()+"\nLon: "+best.getLongitude();String provider=lm.isProviderEnabled(LocationManager.GPS_PROVIDER)?LocationManager.GPS_PROVIDER:(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)?LocationManager.NETWORK_PROVIDER:null);if(provider==null){runOnUiThread(()->startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)));return "Localização desligada. Abri as configurações do Android; habilite e repita ondeuto -info.";}runOnUiThread(()->output.append("\nBuscando localização atual…\n"));final LocationManager manager=lm;LocationListener listener=new LocationListener(){public void onLocationChanged(Location l){manager.removeUpdates(this);runOnUiThread(()->{output.append("Localização do aparelho (precisão ±"+(int)l.getAccuracy()+" m):\nLat: "+l.getLatitude()+"\nLon: "+l.getLongitude()+"\n"+prefix+" ");scroll.fullScroll(ScrollView.FOCUS_DOWN);});}public void onProviderEnabled(String p){}public void onProviderDisabled(String p){}public void onStatusChanged(String p,int status,Bundle extras){}};lm.requestSingleUpdate(provider,listener,Looper.getMainLooper());mainHandler.postDelayed(()->{try{lm.removeUpdates(listener);}catch(Exception ignored){}},20000);return "Aguardando uma localização atual do sistema…";}catch(SecurityException e){return "Permissão de localização não concedida.";}}
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==410&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)output.append("\nPermissão de câmera concedida. Repita o comando flash.\n"+prefix+" ");if(requestCode==411){output.append(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED?"\nPermissão concedida. Repita ondeuto -info.\n":"\nPermissão de localização recusada.\n");}}
  private String quote(String s){return "'"+s.replace("'","'\\''")+"'";}
  private File bundledBash(){return new File(getApplicationInfo().nativeLibraryDir,"libbash.so");}
  private String runBashCommand(String command){
    File bash=bundledBash();if(!bash.isFile()||!bash.canExecute())return "GNU Bash não está disponível para a arquitetura deste aparelho.";
    String rest=command.length()>4?command.substring(4).trim():"";
    if(rest.isEmpty()||rest.equals("--version")||rest.equals("-version"))return ShellRunner.runDirect(new String[]{bash.getAbsolutePath(),"--version"},cwd(),30000);
    if(rest.startsWith("-c ")){
      String script=rest.substring(3).trim();
      if(script.length()>=2&&((script.charAt(0)=='\''&&script.charAt(script.length()-1)=='\'')||(script.charAt(0)=='\"'&&script.charAt(script.length()-1)=='\"')))script=script.substring(1,script.length()-1);
      return ShellRunner.runDirect(new String[]{bash.getAbsolutePath(),"-c",script},cwd(),30000);
    }
    String file=rest;
    if(file.length()>=2&&((file.charAt(0)=='\''&&file.charAt(file.length()-1)=='\'')||(file.charAt(0)=='\"'&&file.charAt(file.length()-1)=='\"')))file=file.substring(1,file.length()-1);
    return ShellRunner.runDirect(new String[]{bash.getAbsolutePath(),file},cwd(),30000);
  }
  private String openTextEditor(String command){
    String arg=command.length()>4?command.substring(4).trim():"";
    if(arg.isEmpty())return "Uso: nano <arquivo> — abre o editor simples integrado do Satanachia; não é o GNU nano.";
    if(arg.length()>1&&((arg.startsWith("\'")&&arg.endsWith("\'"))||(arg.startsWith("\"")&&arg.endsWith("\""))))arg=arg.substring(1,arg.length()-1);
    File file=new File(cwd(),arg);
    if(file.isDirectory())return "nano: esse caminho é uma pasta, não um arquivo.";
    if(file.exists()&&!file.isFile())return "nano: não é um arquivo normal.";
    if(file.exists()&&file.length()>1024L*1024L)return "nano: por segurança, o editor integrado abre arquivos de até 1 MiB.";
    runOnUiThread(()->showTextEditor(file));
    return "Editor simples aberto para "+file.getPath()+". Toque em Salvar para gravar.";
  }
  private void showTextEditor(File file){
    try{
      String initial="";
      if(file.exists())try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
        byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1){if(out.size()+n>1024*1024){Toast.makeText(this,"Arquivo maior que 1 MiB.",Toast.LENGTH_LONG).show();return;}out.write(buf,0,n);}initial=out.toString("UTF-8");
      }
      EditText editor=new EditText(this);editor.setText(initial);editor.setTextColor(fg);editor.setHintTextColor(Color.GRAY);editor.setTypeface(Typeface.MONOSPACE);editor.setTextSize(14);editor.setGravity(Gravity.TOP|Gravity.LEFT);editor.setSingleLine(false);editor.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);editor.setPadding(dp(12),dp(10),dp(12),dp(10));editor.setMinHeight(dp(300));
      ScrollView sc=new ScrollView(this);sc.addView(editor);LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);TextView note=new TextView(this);note.setText("Editor simples integrado · não é o GNU nano · limite de 1 MiB");note.setTextColor(fg);note.setPadding(dp(12),dp(8),dp(12),dp(8));box.addView(note);box.addView(sc,new LinearLayout.LayoutParams(-1,dp(360)));
      AlertDialog dialog=new AlertDialog.Builder(this).setTitle(file.getName().isEmpty()?"Novo arquivo":file.getName()).setView(box).setNegativeButton("Cancelar",(d,w)->{}).setPositiveButton("Salvar",null).create();
      dialog.setOnShowListener(v->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b->{try{
        if(editor.length()>1024*1024){Toast.makeText(this,"O texto excede o limite de 1 MiB.",Toast.LENGTH_LONG).show();return;}
        File parent=file.getCanonicalFile().getParentFile();if(parent==null||!parent.isDirectory()){Toast.makeText(this,"A pasta de destino não existe ou não está acessível.",Toast.LENGTH_LONG).show();return;}
        try(FileOutputStream out=new FileOutputStream(file)){out.write(editor.getText().toString().getBytes(StandardCharsets.UTF_8));}
        dialog.dismiss();Toast.makeText(this,"Arquivo salvo: "+file.getName(),Toast.LENGTH_SHORT).show();
      }catch(Exception e){Toast.makeText(this,"Não consegui salvar: "+e.getMessage(),Toast.LENGTH_LONG).show();}}));
      dialog.show();
    }catch(Exception e){Toast.makeText(this,"Não consegui abrir o arquivo: "+e.getMessage(),Toast.LENGTH_LONG).show();}
  }
  private String shell(String cmd){
    File bash=bundledBash();
    String userCmd="function command_not_found_handle(){ printf 'Comando não encontrado: %s. ZIP extraído não instala ferramenta no APK; para usar o pacote, rode o instalador no Termux.\\n' \"$1\" >&2; return 127; }; "+cmd;
    if(bash.isFile()&&bash.canExecute())return ShellRunner.run(bash.getAbsolutePath(),userCmd,cwd(),30000);
    return "GNU Bash deste APK não está disponível para esta arquitetura; usando o shell do Android.\n"+ShellRunner.run("/system/bin/sh",cmd,cwd(),30000);
  }

  private void tree(File dir,String prefix,StringBuilder b,int depth){if(depth>=7)return;File[] fs=dir.listFiles();if(fs==null)return;Arrays.sort(fs,(x,y)->Boolean.compare(!x.isDirectory(),!y.isDirectory()));for(int i=0;i<fs.length&&i<150;i++){File f=fs[i];boolean last=i==fs.length-1;b.append("\n").append(prefix).append(last?"╰── ":"──╮─ ").append(f.getName()).append(f.isDirectory()?"/":"");if(f.isDirectory())tree(f,prefix+(last?"    ":"│   "),b,depth+1);}}
  @Override protected void onResume(){super.onResume();refreshPermissions();}
  @Override protected void onDestroy(){if(isFinishing()){try{stopService(new Intent(this,TerminalService.class));}catch(Exception ignored){}try{if(tts!=null){tts.stop();tts.shutdown();}}catch(Exception ignored){}}worker.shutdownNow();super.onDestroy();}
}
