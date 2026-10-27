package com.cm.idcapture.nativecamera;

import android.Manifest;
import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.activity.ComponentActivity;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.work.*;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.*;
import java.nio.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends ComponentActivity {
 static final String PREF="idcapture_prefs";
 private final ExecutorService io=Executors.newSingleThreadExecutor();
 private final Handler ui=new Handler(Looper.getMainLooper());
 private PreviewView preview;
 private ImageCapture capture;
 private TextView shotCount,pendingCount,syncedCount,tip;
 private ImageButton shutter;
 private int captured=0;
 private int inflight=0;
 private int lastSaved=0;
 private boolean cameraVisible=false;
 private boolean pausedVisible=false;
 private ProcessCameraProvider cameraProvider;
 private final Runnable refresh=new Runnable(){public void run(){if(cameraVisible||pausedVisible){refreshCounts();ui.postDelayed(this,1000);}}};

 @Override public void onCreate(Bundle b){super.onCreate(b);showHome();}
 private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER);return t;}
 private LinearLayout column(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);return c;}
 private void showHome(){cameraVisible=false;pausedVisible=false;stopCamera();ui.removeCallbacks(refresh);
  LinearLayout root=column();root.setGravity(Gravity.CENTER);root.setPadding(dp(26),dp(20),dp(26),dp(20));root.setBackgroundColor(Color.WHITE);
  ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.logo);logo.setAdjustViewBounds(true);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);root.addView(logo,new LinearLayout.LayoutParams(dp(240),dp(155)));
  TextView title=text("Chụp ảnh Căn cước",25,Color.rgb(15,23,42));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,dp(88));root.addView(title,tp);
  Button begin=new Button(this);begin.setText("Bắt đầu chụp ảnh");begin.setTransformationMethod(null);begin.setTextColor(Color.WHITE);begin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(22,118,239)));
  root.addView(begin,new LinearLayout.LayoutParams(-1,dp(64)));begin.setOnClickListener(v->startFlow());setContentView(root);
 }
 private void startFlow(){if(getSharedPreferences(PREF,0).getString("url","").isEmpty()){showSetup();return;}if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.CAMERA},10);return;}showCamera();}
 @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] results){super.onRequestPermissionsResult(req,permissions,results);if(req==10){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)showCamera();else Toast.makeText(this,"Cần cấp quyền Camera trong Cài đặt Android",Toast.LENGTH_LONG).show();}}
 private void showSetup(){LinearLayout fields=column();fields.setPadding(dp(20),dp(12),dp(20),0);EditText url=new EditText(this);url.setSingleLine(true);url.setHint("Apps Script URL kết thúc /exec");url.setInputType(17);fields.addView(url);EditText secret=new EditText(this);secret.setSingleLine(true);secret.setHint("UPLOAD_SECRET");secret.setInputType(129);fields.addView(secret);
 new AlertDialog.Builder(this).setTitle("Cấu hình kết nối lần đầu").setView(fields).setPositiveButton("Lưu",(d,w)->{String u=url.getText().toString().trim();String s=secret.getText().toString().trim();if(!u.startsWith("https://script.google.com/macros/s/")||!u.endsWith("/exec")||s.isEmpty()){Toast.makeText(this,"URL /exec hoặc mật khẩu chưa hợp lệ",Toast.LENGTH_LONG).show();showSetup();return;}getSharedPreferences(PREF,0).edit().putString("url",u).putString("secret",s).apply();startFlow();}).setNegativeButton("Hủy",null).show();}
 private void showCamera(){cameraVisible=true;pausedVisible=false;LinearLayout root=column();root.setBackgroundColor(Color.rgb(16,23,34));
  preview=new PreviewView(this);preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout controls=column();controls.setGravity(Gravity.CENTER);controls.setPadding(dp(8),dp(10),dp(8),dp(6));
  shutter=new ImageButton(this);
  shutter.setImageResource(R.drawable.shutter_icon);
  shutter.setScaleType(ImageView.ScaleType.FIT_CENTER);
  shutter.setPadding(dp(13),dp(13),dp(13),dp(13));
  android.graphics.drawable.GradientDrawable shutterBg=new android.graphics.drawable.GradientDrawable();
  shutterBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
  shutterBg.setColor(Color.WHITE);
  shutter.setBackground(shutterBg);
  shutter.setContentDescription("Chụp ảnh");
  controls.addView(shutter,new LinearLayout.LayoutParams(dp(78),dp(78)));
  tip=text("Sẵn sàng chụp",12,Color.LTGRAY);controls.addView(tip,new LinearLayout.LayoutParams(-1,dp(26)));
  LinearLayout counters=new LinearLayout(this);counters.setOrientation(LinearLayout.HORIZONTAL);
  shotCount=counter(counters,"Đã chụp",Color.WHITE);pendingCount=counter(counters,"Chờ đồng bộ",Color.rgb(255,82,82));syncedCount=counter(counters,"Đã đồng bộ",Color.rgb(63,218,124));
  controls.addView(counters,new LinearLayout.LayoutParams(-1,dp(78)));
  Button pause=new Button(this);pause.setText("Tạm dừng");pause.setTransformationMethod(null);pause.setTextColor(Color.WHITE);pause.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(75,85,99)));
  controls.addView(pause,new LinearLayout.LayoutParams(-1,dp(52)));
  pause.setOnClickListener(v->showPaused());
  root.addView(controls);setContentView(root);
  shutter.setEnabled(false);shutter.setOnClickListener(v->takePhoto());startCamera();ui.post(refresh);enqueueUpload();
 }
 private TextView counter(LinearLayout parent,String label,int color){LinearLayout cell=column();cell.setGravity(Gravity.CENTER);TextView name=text(label,12,color);TextView val=text("0",24,color);cell.addView(name);cell.addView(val);parent.addView(cell,new LinearLayout.LayoutParams(0,-1,1));return val;}
 private void startCamera(){ListenableFuture<ProcessCameraProvider> future=ProcessCameraProvider.getInstance(this);future.addListener(()->{try{ProcessCameraProvider provider=future.get();if(!cameraVisible)return;cameraProvider=provider;provider.unbindAll();Preview p=new Preview.Builder().build();p.setSurfaceProvider(preview.getSurfaceProvider());capture=new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setJpegQuality(72).setTargetResolution(new android.util.Size(1600,1200)).build();provider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA,p,capture);shutter.setEnabled(true);}catch(Exception e){tip.setText("Lỗi camera: "+e.getMessage());}},ContextCompat.getMainExecutor(this));}
 private void takePhoto(){if(capture==null||inflight>=2){tip.setText("Đang xử lý ảnh, vui lòng đợi");return;}inflight++;shutter.setEnabled(inflight<2);tip.setText("Đang lưu ảnh…");
  final long when=System.currentTimeMillis();final String stamp=new SimpleDateFormat("yyyy.MM.dd_HH.mm.ss",Locale.US){{setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));}}.format(new Date(when));final String id=UUID.randomUUID().toString();
  capture.takePicture(io,new ImageCapture.OnImageCapturedCallback(){
   @Override public void onCaptureSuccess(ImageProxy image){try{if(image.getFormat()!=android.graphics.ImageFormat.JPEG)throw new IOException("Camera không trả về JPEG");ByteBuffer buffer=image.getPlanes()[0].getBuffer();byte[] bytes=new byte[buffer.remaining()];buffer.get(bytes);File dir=new File(getFilesDir(),"pending");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Không tạo được thư mục lưu");File tmp=new File(dir,stamp+"_"+id+".part");File target=new File(dir,stamp+"_"+id+".jpg");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(bytes);out.getFD().sync();}if(!tmp.renameTo(target))throw new IOException("Không hoàn tất lưu ảnh");captured++;ui.post(()->{tip.setText("Đã lưu ảnh, tiếp tục chụp");refreshCounts();});enqueueUpload();}catch(Exception e){ui.post(()->{tip.setText("LỖI LƯU ẢNH: "+e.getMessage());Toast.makeText(MainActivity.this,"Không lưu được ảnh",Toast.LENGTH_LONG).show();});}finally{image.close();finishCapture();}}
   @Override public void onError(ImageCaptureException e){ui.post(()->tip.setText("Lỗi chụp: "+e.getMessage()));finishCapture();}
  });
 }
 private void finishCapture(){ui.post(()->{inflight=Math.max(0,inflight-1);if(shutter!=null)shutter.setEnabled(inflight<2);});}
 private void refreshCounts(){if(shotCount==null||pendingCount==null||syncedCount==null)return;File dir=new File(getFilesDir(),"pending");File[] files=dir.listFiles((d,n)->n.endsWith(".jpg"));int waiting=files==null?0:files.length;shotCount.setText(String.valueOf(captured));pendingCount.setText(String.valueOf(waiting));int synced=getSharedPreferences(PREF,0).getInt("synced",0);syncedCount.setText(String.valueOf(synced));}
 private void enqueueUpload(){Constraints net=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(UploadWorker.class).setConstraints(net).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,TimeUnit.SECONDS).build();WorkManager.getInstance(this).enqueueUniqueWork("upload-idcapture",ExistingWorkPolicy.KEEP,request);}
 private void stopCamera(){capture=null;if(shutter!=null)shutter.setEnabled(false);if(cameraProvider!=null){cameraProvider.unbindAll();cameraProvider=null;}}
 private void showPaused(){
  cameraVisible=false;pausedVisible=true;stopCamera();ui.removeCallbacks(refresh);
  LinearLayout root=column();root.setBackgroundColor(Color.rgb(16,23,34));root.setPadding(dp(20),dp(25),dp(20),dp(22));root.setGravity(Gravity.CENTER);
  TextView title=text("Đã tạm dừng chụp ảnh",24,Color.WHITE);root.addView(title,new LinearLayout.LayoutParams(-1,dp(100)));
  TextView info=text("Camera đã đóng. Ảnh trong hàng đợi vẫn được đồng bộ khi có mạng.",14,Color.LTGRAY);root.addView(info,new LinearLayout.LayoutParams(-1,dp(75)));
  LinearLayout counters=new LinearLayout(this);counters.setOrientation(LinearLayout.HORIZONTAL);
  shotCount=counter(counters,"Đã chụp",Color.WHITE);pendingCount=counter(counters,"Chờ đồng bộ",Color.rgb(255,82,82));syncedCount=counter(counters,"Đã đồng bộ",Color.rgb(63,218,124));
  root.addView(counters,new LinearLayout.LayoutParams(-1,dp(115)));
  Button resume=new Button(this);resume.setText("Tiếp tục");resume.setTransformationMethod(null);resume.setTextColor(Color.WHITE);resume.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(22,118,239)));
  root.addView(resume,new LinearLayout.LayoutParams(-1,dp(62)));resume.setOnClickListener(v->showCamera());
  Button end=new Button(this);end.setText("Kết thúc");end.setTransformationMethod(null);end.setTextColor(Color.WHITE);end.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(75,85,99)));
  LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(62));ep.topMargin=dp(12);root.addView(end,ep);
  end.setOnClickListener(v->{enqueueUpload();showHome();});
  setContentView(root);refreshCounts();ui.post(refresh);enqueueUpload();
 }
 @Override public void onBackPressed(){if(cameraVisible){showPaused();}else if(pausedVisible){enqueueUpload();showHome();}else super.onBackPressed();}
 @Override protected void onDestroy(){stopCamera();io.shutdown();ui.removeCallbacks(refresh);super.onDestroy();}
}
