package com.cm.idcapture.nativecamera;

import android.content.*;
import androidx.annotation.NonNull;
import androidx.work.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UploadWorker extends Worker {
 public UploadWorker(@NonNull Context c,@NonNull WorkerParameters p){super(c,p);}
 @NonNull @Override public Result doWork(){Context ctx=getApplicationContext();String url=ctx.getSharedPreferences(MainActivity.PREF,0).getString("url","");String secret=ctx.getSharedPreferences(MainActivity.PREF,0).getString("secret","");if(url.isEmpty()||secret.isEmpty())return Result.failure();
  File dir=new File(ctx.getFilesDir(),"pending");File[] fs=dir.listFiles((d,n)->n.endsWith(".jpg"));if(fs==null||fs.length==0)return Result.success();Arrays.sort(fs,Comparator.comparing(File::getName));
  for(File f:fs){if(isStopped())return Result.retry();try{String filename=f.getName();String stamp=filename.substring(0,19);String id=filename.substring(20,filename.length()-4);
   byte[] bytes=read(f);String data=Base64.getEncoder().encodeToString(bytes);
   String body="secret="+enc(secret)+"&id="+enc(id)+"&stamp="+enc(stamp)+"&mime="+enc("image/jpeg")+"&data="+enc(data);
   JSONObject result=post(url,body);
   if(!result.optBoolean("ok",false))return Result.retry();
   if(!f.delete())return Result.retry();
   android.content.SharedPreferences pref=ctx.getSharedPreferences(MainActivity.PREF,0);pref.edit().putInt("synced",pref.getInt("synced",0)+1).apply();
  }catch(Exception e){android.util.Log.e("IDCapture","Upload failed; will retry",e);return Result.retry();}}
  return Result.success();
 }
 private String enc(String s)throws Exception{return URLEncoder.encode(s,"UTF-8");}
 private byte[] read(File f)throws IOException{try(FileInputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
 private JSONObject post(String url,String body)throws Exception{URL target=new URL(url);boolean usePost=true;for(int redirects=0;redirects<6;redirects++){
  HttpURLConnection c=(HttpURLConnection)target.openConnection();c.setConnectTimeout(25000);c.setReadTimeout(90000);c.setInstanceFollowRedirects(false);c.setRequestMethod(usePost?"POST":"GET");if(usePost){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");byte[] bytes=body.getBytes(StandardCharsets.UTF_8);try(OutputStream os=c.getOutputStream()){os.write(bytes);}}int status=c.getResponseCode();if(status==301||status==302||status==303||status==307||status==308){String next=c.getHeaderField("Location");c.disconnect();if(next==null||!next.startsWith("https://"))throw new IOException("Bad redirect");target=new URL(next);if(status==301||status==302||status==303)usePost=false;continue;}
  try(InputStream in=status<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);String response=out.toString("UTF-8");c.disconnect();if(status>=400)throw new IOException("HTTP "+status);return new JSONObject(response);}
 }throw new IOException("Too many redirects");}
}
