package com.mygatech;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.mygatech.R;
import com.mygatech.forfutureuse.TsquareHandler;
import com.mygatech.tsquare.TsquareArrays;
import com.mygatech.tsquare.TsquareMain;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

public class FirstTimeTsquareService extends IntentService {
	static CookieManager manager;
	static HttpsURLConnection connection,connection2;
	private final SharedPreferences mainSp;
	private Intent mIntent;
	private int percent;
	
	
	public FirstTimeTsquareService(){
		super("T-Square Update Service");	
		//getSharedPreferences should be only used in Activity
		if(MainActivity.sp.getBoolean("startActivityInUse", false)){
			setIntentRedelivery(true);
			mIntent = new Intent(StartActivity.TSQUARE_LOAD);
			mIntent.putExtra("percent", 0);
			LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
		}
		mainSp =  MainActivity.sp;

	}
	

	public String parseJsession(String mCookie){
		String[] split = mCookie.split(";", -1);
		String mReturn = "";
		for(String temp : split){
			if(temp.toLowerCase(Locale.US).contains("jsessionid")){
				String[] arrayTemp = temp.split("=",2);
				for(String js : arrayTemp){
					if(js.toLowerCase(Locale.US).contains("jsessionid")){
						mReturn = ";"+js.toLowerCase(Locale.getDefault());
					}else{
						mReturn+="="+js;
					}
				}
			}
		}
		return mReturn;
	}
	
	public String jsoupParser(String titleName, String prev, int step) {
		if (titleName.toLowerCase(Locale.US).equals("announcements")) {
			Log.e("jsoup", "announcement lists");
			return "h4 a[href], td[headers=date]";
		} else if (titleName.equals("Gradebook")) {
			return "tr";
		} else if (titleName.toLowerCase(Locale.US).contains("assignments")) {
			return "td[headers=openDate], td[headers=dueDate], h4 a[href]";
		} else if (titleName.toLowerCase(Locale.US).contains("piazza")) {
			return "iframe";
		}
		return null;
	}
	

	@SuppressWarnings("unused")
	private class ToastRun implements Runnable{
		String mText;
	    public ToastRun(String text) {
	        mText = text;
	    }
	    
	    @Override
	    public void run(){
	         Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
	    }
	}
	
	
	@Override
	protected void onHandleIntent(Intent intent)  {
		//For t-square
		setIntentRedelivery(true);
		try{
		CookieSyncManager.createInstance(getApplicationContext());
	    CookieSyncManager.getInstance().resetSync();
	    CookieSyncManager.getInstance().startSync();
	    manager = CookieManager.getInstance();
	   	manager.acceptCookie();
	   	manager.setAcceptCookie(true);
		manager.setCookie("https://login.gatech.edu/cas/login", "Set-Cookie");
		manager.setCookie("https://t-square.gatech.edu/portal/pda", "Set-Cookie");
		manager.setCookie("https://t-square.gatech.edu/sakai-login-tool/container", "Set-Cookie");
	    HttpURLConnection.setFollowRedirects(true);

	    String login = "https://login.gatech.edu/cas/login";
		URL urlForLogin = new URL(login);		
		connection = (HttpsURLConnection) urlForLogin.openConnection();
	    connection.setRequestProperty("Cookie","utmccn=(referral)");
	    connection.setRequestProperty("Accept", "text/html, application/xhtml+xml, */*");
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("User-Agent", "Mozilla");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("Host", "login.gatech.edu");
        connection.setRequestProperty("DNT", "1");
        connection.setRequestProperty("Connection","Keep-Alive");
        String cookie = connection.getHeaderField("Set-Cookie"); // Real cookie usage
		String js = parseJsession(cookie); // URL usage
		cookie = (cookie.split(";",2)[0]);
		Log.e("check","Cookie1 outgoing: " + cookie);

        //Response header fields
        Log.e("check", "1111Response header fields==========================================");
        for (String header : connection.getHeaderFields().keySet()) {
			if (header != null) {
				for (String value : connection.getHeaderFields()
						.get(header)) {
					Log.e("check", header + ": " + value);
				}
			}else{
				Log.e("check", "Header is null");
			}
		}
        
        //Take lt value for login
        String lt = "";
        Document doc =  Jsoup.parse(connection.getInputStream(),null, login);
		Elements links = doc.select("input[name=lt]");
		for(Element elem : links){
			lt = elem.attr("value");
		}
		
		Log.d("LT = " , lt);

		String strForHtml = login + js;
        URL urlForHtml = new URL(strForHtml);
        connection = (HttpsURLConnection)urlForHtml.openConnection();	    
        connection.setRequestMethod("POST");
	    connection.setRequestProperty("Cookie",cookie+";utmccn=(referral)");
	    connection.setRequestProperty("Accept", "text/html, application/xhtml+xml, */*");
        connection.setRequestProperty("Accept-Language", "en-US");
        connection.setRequestProperty("User-Agent", "Mozilla");
        connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        connection.setRequestProperty("Host", "login.gatech.edu");
        connection.setRequestProperty("DNT", "1");
        connection.setRequestProperty("Connection","Keep-Alive");
        connection.setRequestProperty("Referer", "https://login.gatech.edu/cas/login?service=https://t-square.gatech.edu/portal/pda");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setFollowRedirects(false);

        final String user = mainSp.getString("username", null);       
	    final String pass = mainSp.getString("password", null); 
        String urlParameters = "username="+user+"&password="+pass+"&lt="+lt+"&_eventId=submit&submit=LOGIN";
        connection.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
        wr.writeBytes(urlParameters);
        wr.flush();
        wr.close();

        Log.e("check", "After Post response==========================================");
        String ticket = "";
        Log.e("check",""+connection.getResponseCode());
	    Log.e("check",""+connection.getResponseMessage());
        for (String header : connection.getHeaderFields().keySet()) {
			if (header != null) {
				for (String value : connection.getHeaderFields()
						.get(header)) {
					Log.e("check", header + ": " + value);
					if(value.contains("CASTGC")){
						value=value.split(";",2)[0];
						ticket+=value.split("-",2)[1];
					}
				}
			}
		}
        
	    
	    if(connection.getResponseCode()==200 && ticket!=null){
	    	String tsquare = "https://t-square.gatech.edu/portal/pda";
			URL urlTsquare = new URL(tsquare);		
			connection = (HttpsURLConnection) urlTsquare.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html, application/xhtml+xml, */*");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setRequestProperty("User-Agent", "Mozilla");
			connection.setRequestProperty("Accept-Encoding", "gzip, deflate");
			connection.setRequestProperty("Host", "t-square.gatech.edu");
			connection.setRequestProperty("DNT", "1");
			connection.setRequestProperty("Referer", "https://login.gatech.edu/cas/login?service=https://t-square.gatech.edu/portal/pda");
			connection.setRequestProperty("Cookie", cookie);
			connection.setRequestProperty("Connection","Keep-Alive");
	       
		    String tsquareJS = "";
		    String bigip = "";
		    Log.e("check", "=========="+tsquare+"============");
		    Log.e("check",""+connection.getResponseCode());
		    Log.e("check",""+connection.getResponseMessage());
		    for (String header : connection.getHeaderFields().keySet()) {
				if (header != null) {
					for (String value : connection.getHeaderFields().get(header)) {
						Log.e("check", header + " " + value);
						if(value.contains("BIGip")){
							bigip = value.split(";",2)[0];
						}
						if(value.toLowerCase(Locale.US).contains("jsessionid")){
							tsquareJS = (value.split(";",2)[0]);
						}
					}
				}
			}
		    
		    String tF = "https://t-square.gatech.edu/portal/pda/?force.login=yes";
			URL uF = new URL(tF);		
			connection = (HttpsURLConnection) uF.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "login.gatech.edu");
			connection.setRequestProperty("DNT", "1");
			connection.setRequestProperty("Referer", " https://t-square.gatech.edu/portal/pda");
			connection.setRequestProperty("Cookie",cookie + "; " + ticket + ";utmccn=(referral)"); // tsquareJS+"; "+bigip + "; " + 
			connection.setRequestProperty("Connection","Keep-Alive");
	        Log.e("check", "=====================================================================");
	        Log.e("cookie", "COOKIE: " + cookie);
	        Log.e("cookie", "Ticket: " + ticket);
	        Log.e("cookie", "TsquareJS: " + tsquareJS);
	        Log.e("cookie", "BIGip: " + bigip);
	        Log.e("check",""+connection.getResponseCode());
		    Log.e("check",""+connection.getResponseMessage());
	        for (String header : connection.getHeaderFields().keySet()) {
				if (header != null) {
					for (String value : connection.getHeaderFields()
							.get(header)) {
							Log.e("check", header + ": " + value );
							if (value.toLowerCase(Locale.getDefault()).contains("jsessionid"))
								tsquareJS = (value.split(";", 2)[0]);
							if(value.contains("BIGip"))
								bigip = value.split(";",2)[0];
					}
				}else
					Log.e("check", "Header is null");
			}
	        
	        Log.e("cookie", "TsquareJS: " + tsquareJS);
	        Log.e("cookie", "BIGip: " + bigip);
	        String rdSakia = "https://t-square.gatech.edu/sakai-login-tool/container?force.login=yes";
	        URL urdSakia = new URL(rdSakia);
			connection = (HttpsURLConnection) urdSakia.openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "login.gatech.edu");
			connection.setRequestProperty("DNT", "1");
			connection.setRequestProperty("Referer", " https://t-square.gatech.edu/portal/pda");
			connection.setRequestProperty("Cookie",tsquareJS + "; " + bigip + ";utmccn=(referral)"); // tsquareJS+"; "+bigip + "; " + 
			connection.setRequestProperty("Connection","Keep-Alive");
	        Log.e("check", "rdSakia====================================================================");
	        Log.e("check",""+connection.getResponseCode());
		    Log.e("check",""+connection.getResponseMessage());
	        for (String header : connection.getHeaderFields().keySet()) {
				if (header != null) {
					for (String value : connection.getHeaderFields()
							.get(header)) {
							Log.e("check", header + ": " + value );
							
					}
				}else{
					Log.e("check", "Header is null");
				}
			}
	        
	        String sakia = "https://login.gatech.edu/cas/login?service=https://t-square.gatech.edu/sakai-login-tool/container";
	        URL uSakia = new URL(sakia);
	        connection = (HttpsURLConnection) uSakia.openConnection();
	        connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "login.gatech.edu");
			connection.setRequestProperty("DNT", "1");
	        connection.setRequestProperty("Referer", "https://t-square.gatech.edu/portal/pda");//?ticket=ST-"+ticket);
	        connection.setRequestProperty("Cookie", cookie+"; CASTGC=TGT-"+ticket);
	        connection.setRequestProperty("Connection","Keep-Alive");
	        
	        Log.e("check", "sakia====================================================================");
	        Log.e("check",""+connection.getResponseCode());
		    Log.e("check",""+connection.getResponseMessage());
		    String nextURL = "";
	        for (String header : connection.getHeaderFields().keySet()) {
				if (header != null) {
					for (String value : connection.getHeaderFields()
							.get(header)) {
							Log.e("check", header + ": " + value );
						if(header.contains("Loca")){
							nextURL = value;
						}
					}
				}else{
					Log.e("check", "Header is null");
				}
			}
	        String finalTicket = nextURL.split("=", -1)[1];
	        Log.e("cookie", finalTicket);
	        
	        URL uSakiaContainer = new URL("https://t-square.gatech.edu/sakai-login-tool/container?ticket="+finalTicket);
	        connection = (HttpsURLConnection) uSakiaContainer.openConnection();
	        connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "t-square.gatech.edu");
			connection.setRequestProperty("DNT", "1");
	        connection.setRequestProperty("Referer", "https://t-square.gatech.edu/portal/pda?ticket=ST-"+ticket+"?force.login=yes");
	        connection.setRequestProperty("Cookie", tsquareJS+"; "+ bigip);
	        connection.setRequestProperty("Connection","Keep-Alive");
	        
	        Log.e("check", "sakia====================================================================");
	        Log.e("check",""+connection.getResponseCode());
		    Log.e("check",""+connection.getResponseMessage());

	        for (String header : connection.getHeaderFields().keySet()) {
				if (header != null) {
					for (String value : connection.getHeaderFields()
							.get(header)) {
							Log.e("check", header + ": " + value );
					}
				}else{
					Log.e("check", "Header is null");
				}
			}
	        
	        URL tsquareFinal = new URL("https://t-square.gatech.edu/portal/pda/?force.login=yes");
	        connection = (HttpsURLConnection) tsquareFinal.openConnection();
	        connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "t-square.gatech.edu");
			connection.setRequestProperty("DNT", "1");
	        connection.setRequestProperty("Referer", "https://t-square.gatech.edu/portal/pda?ticket=ST-"+ticket+"?force.login=yes");
	        connection.setRequestProperty("Cookie", tsquareJS+"; "+ bigip);
	        connection.setRequestProperty("Connection","Keep-Alive");
	        
	        Log.e("check", "tsquareFinal===============================================");
	        Log.e("check",""+connection.getResponseCode());
		    Log.e("check",""+connection.getResponseMessage());

	        for (String header : connection.getHeaderFields().keySet()) {
				if (header != null) {
					for (String value : connection.getHeaderFields()
							.get(header)) {
							Log.e("check", header + ": " + value );
					}
				}else{
					Log.e("check", "Header is null");
				}
			}
	        
	        
	        Document docSakia =  Jsoup.parse(connection.getInputStream(),null, tsquareFinal.toString());
		    Editor classEditor = getSharedPreferences(TsquareHandler.CLASS_LIST, 0).edit();
		    ArrayList<TsquareArrays> aList = new ArrayList<TsquareArrays>();
	        Elements sakiaLinks = docSakia.select("ul#pda-portlet-site-menu li a[href]");
    		int numClass = 0;
			for(Element link: sakiaLinks) {
				if(numClass != 0 && !link.attr("title").contains("FALL") && !link.attr("title").contains("SPRING")
					&& !link.attr("title").contains("SUMMER")){ // get rid of not now classes
					TsquareArrays temp = new TsquareArrays(link.attr("title"), link.attr("href"));
					Log.e("connection", link.attr("title"));
					aList.add(temp);
				}
				numClass++;
			}
			numClass--;
			
			classEditor.putString("username", mainSp.getString("username", null));
			classEditor.putString("password", mainSp.getString("password",null));
			classEditor.putBoolean("upToDate", true);
			classEditor.commit();
			
			String state = Environment.getExternalStorageState();
			String dir = Environment.getExternalStorageDirectory() + "/MyGaTech/";
			String TSQUARE = "tsquare.txt";
			String TSQUARE_SUB = "tsquaresub.txt";
			
			if (Environment.MEDIA_MOUNTED.equals(state)) {
			    // We can read and write the media
			    Log.e("has", dir);
			    File file = new File(dir);
				if(!file.exists()){
					file.mkdir();
				}
				
				//Got to create file first for the first time user****************************
				FileOutputStream fos = new FileOutputStream(dir + TSQUARE);
				fos.close();
				file = new File(dir, TSQUARE);
				
				fos = new FileOutputStream(dir + TSQUARE_SUB);
				fos.close();
				file = new File(dir, TSQUARE_SUB);
			} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			    // We can only read the media
			} else {
			    // Something else is wrong. It may be one of many other states, but all we need
			    //  to know is we can neither read nor write
			}
			
			
			//Saving as a text file in emulator/0/MyGaTech/tsquare.txt
			//Saving format
			//Assignment
			//Subject;Board;Title;Due date;Posted date;Link;
			//Gradebook
			//Subject;Board;Title;Grade;Posted date;Commnet;
			//Announcement
			//Subject;Board;Title;NaN;Posted date;Link;
			//Piazza
			//Subject;Board;NaN;NaN;NaN;Link;
			
			FileInputStream fis = new FileInputStream(dir + TSQUARE_SUB);
			StringBuilder builder = new StringBuilder();
			int c;
			while((c = fis.read()) != - 1){
				builder.append((char)c);
			}
			fis.close();
			Log.i("Tquare Subject:", builder.toString());
			
			builder.delete(0, builder.length());
			fis = new FileInputStream(dir + TSQUARE);
			while((c = fis.read()) != - 1){
				builder.append((char)c);
			}
			fis.close();
			Log.i("Tquare:", (char)c +"");

			
			aList = getLists(aList, ticket, tsquareJS, bigip); // Get a list of class
			FileOutputStream fos_sub = new FileOutputStream(dir + TSQUARE_SUB, false);
			for(TsquareArrays ts: aList){
				fos_sub.write(ts.getName().getBytes());
				fos_sub.write(";".getBytes());
				fos_sub.write(ts.getBoard().getBytes());
				fos_sub.write(";".getBytes());
				fos_sub.write(ts.getLink().getBytes());
				fos_sub.write(System.getProperty("line.separator").getBytes());
			}
			fos_sub.close();
			
			
			aList = getMenus(aList, ticket, tsquareJS, bigip); //Get details of class
			FileOutputStream fos = new FileOutputStream(dir + TSQUARE, false);
			for(TsquareArrays ts : aList){
				Log.e("check", "Written" + ts.getName());
				fos.write((ts.getName() + ";").getBytes() );
				fos.write((ts.getBoard()+ ";").getBytes());
				fos.write((ts.getTitle()+ ";").getBytes());
				fos.write((ts.getContent()+ ";").getBytes());
				fos.write((ts.getDate()+ ";").getBytes());
				fos.write((ts.getLink()+ ";").getBytes());
				fos.write(System.getProperty("line.separator").getBytes());
			}
			fos.close();
			mIntent = new Intent(StartActivity.TSQUARE_LOAD);
			mIntent.putExtra("percent", 10000);
			LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
	    }
		}catch(SSLException e){ // Network is disconnected (= isConnected())
			Log.e(e.getMessage(), "Network is disconnected");
		}catch(IOException e){
			try{
				FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/MyGaTech/log.txt", true);
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				fos.write((new String("From log alarm : ")).getBytes());
				fos.write(e.toString().getBytes());
				fos.write((new String("\n")).getBytes());
				fos.close();
			}catch(IOException err){
					Log.d("error", "Could not write to the txt");
			}	
			NotificationManager notificationManager =
	    		    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	    	Intent loadStartActivity = new Intent(getApplicationContext(), StartActivity.class);
	    	PendingIntent pendingIntent =PendingIntent.getActivity(getApplicationContext(), 0, loadStartActivity, 0);

	    	CharSequence notiText = "MyGatech recognized that your password is outdated.\nPlease change in My GaTech Login settings :)\n" +
	    			"Have a nice day ;=>";
	    	long[] pattern = {200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0,200,0};
	    	Notification notification = new Notification.Builder(getApplicationContext())
	    									.setContentTitle("Password is outdated!")
	    									.setContentIntent(pendingIntent)
	    									.setStyle(new Notification.BigTextStyle()) 
	    									.setContentText(notiText)
	    									.setSmallIcon(R.drawable.ic_launcher)
	    									.setLights(0xffff00ff,200,200)
	    									.setVibrate(pattern)
	    									.setAutoCancel(true)
	    									.setWhen(System.currentTimeMillis())
	    									.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.FLAG_SHOW_LIGHTS)
	    									.build();
			notificationManager.notify(0, notification);
		}catch(NullPointerException e){
			Log.e(e.getMessage(), "It's null pointer exception!");
		}catch(IllegalArgumentException e){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			Log.e(e.toString(), "Object should not be null");
		}catch(Throwable e){
			try{
				FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/MyGaTech/log.txt", true);
				StringWriter errors = new StringWriter();
				e.printStackTrace(new PrintWriter(errors));
				fos.write(e.toString().getBytes());
				fos.close();
			}catch(IOException err){
					Log.d("error", "Could not write to the txt");
			}			
		}
	}
	
	public ArrayList<TsquareArrays> getLists(ArrayList<TsquareArrays> preList, String ticket, String tsquareJS, String bigip) throws Throwable{
		ArrayList<TsquareArrays> curList = new ArrayList<TsquareArrays>();
        Editor menuEditor = getSharedPreferences(TsquareMain.MENU_LIST, 0).edit();
        menuEditor.clear().commit();
        menuEditor.putString("TsquareJS", tsquareJS);
        menuEditor.putString("Bigip", bigip);
        menuEditor.putString("Ticket", ticket);
		menuEditor.commit();
		percent = 0;
		int standard = 1000 / preList.size();
		for(TsquareArrays curTemp : preList){
			percent+= standard;
			mIntent = new Intent(StartActivity.TSQUARE_LOAD);
			mIntent.putExtra("percent", percent);
			LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
			
			URL menuURL = new URL(curTemp.getLink());
	        connection = (HttpsURLConnection) menuURL.openConnection();
	        connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "t-square.gatech.edu");
			connection.setRequestProperty("DNT", "1");
	        connection.setRequestProperty("Referer", "https://t-square.gatech.edu/portal/pda?ticket=ST-"+ticket+"?force.login=yes");
	        connection.setRequestProperty("Cookie", tsquareJS+"; "+ bigip);
	        connection.setRequestProperty("Connection","Keep-Alive");
	        Document menuDoc =  Jsoup.parse(connection.getInputStream(),null, curTemp.getLink());
	        Elements menuElems = menuDoc.select("ul#pda-portlet-page-menu li a[href]");
	        for(Element menuElem : menuElems){
	        	String tempTitle = menuElem.attr("title");
	        	if(tempTitle.equals("Announcements") || tempTitle.equals("Assignments") || tempTitle.equals("Gradebook")
	        			|| tempTitle.equals("Piazza") || tempTitle.equals("Syllabus") || tempTitle.equals("Wiki")
	        			|| tempTitle.equals("Section Info") || tempTitle.equals("Forums") || tempTitle.equals("Chat Room")
	        			|| tempTitle.equals("PostFun")){
		        	curList.add(new TsquareArrays(curTemp.getName(), tempTitle, menuElem.attr("href")));
					Log.e("cookie",curList.get(curList.size()-1).getName() + " ~~~~ " +  curList.get(curList.size()-1).getBoard()
							+ " ~~~~ "  + curList.get(curList.size()-1).getLink());
	        	}
	        }
		}
        return curList;
	}
	
	public ArrayList<TsquareArrays> getMenus(ArrayList<TsquareArrays> preList, String ticket, String tsquareJS, String bigip) throws Throwable{
		ArrayList<TsquareArrays> curList = new ArrayList<TsquareArrays>();
       	int standard = 9000 / preList.size();
		for(TsquareArrays curTemp : preList){
			String subject = curTemp.getName();
			String board = curTemp.getBoard();
			String tempLink = curTemp.getLink();
			Log.e("check", subject + " ^^^^ " + board  + " ^^^^ " + tempLink);
			
			percent += standard;
			mIntent = new Intent(StartActivity.TSQUARE_LOAD);
			mIntent.putExtra("percent", percent);
			LocalBroadcastManager.getInstance(this).sendBroadcast(mIntent);
			
			URL menuURL = new URL(curTemp.getLink());
			//Log.e("check", menuURL.toString());
	        connection = (HttpsURLConnection) menuURL.openConnection();
	        connection.setInstanceFollowRedirects(false);
			connection.setRequestMethod("GET");
	        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
			connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
			connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
			connection.setRequestProperty("Host", "t-square.gatech.edu");
			connection.setRequestProperty("DNT", "1");
	        connection.setRequestProperty("Referer", tempLink);
	        connection.setRequestProperty("Cookie", tsquareJS+"; "+ bigip);
	        connection.setRequestProperty("Connection","Keep-Alive");

	        if(connection.getResponseCode()== 302){
//	        	Log.e("check " + board + " " + subject, menuURL.toString());
	        	menuURL = new URL(connection.getHeaderField("Location"));
		        if(board.equalsIgnoreCase("gradebook"))
		        	menuURL = new URL(menuURL.toString()+"/studentView.jsf");
		        else if (board.equalsIgnoreCase("announcements"))
		        	menuURL = new URL(menuURL.toString()+"?panel=Main");
		        else if (board.equalsIgnoreCase("syllabus"))
		        	menuURL = new URL(menuURL.toString()+"/main");
		        else if (board.equalsIgnoreCase("section info"))
		        	menuURL = new URL(menuURL.toString()+"/studentView.jsf");
		        else if (board.equalsIgnoreCase("forums"))
		        	menuURL = new URL(returnForumAddress(menuURL.toString(), tempLink, ticket, tsquareJS, bigip));
		        else if (board.equalsIgnoreCase("chat room"))
		        	menuURL = new URL(menuURL.toString()+"/room");
//		        	System.out.println(menuDoc);
	        	connection = (HttpsURLConnection) menuURL.openConnection();
		        connection.setInstanceFollowRedirects(false);
				connection.setRequestMethod("GET");
				connection.setRequestProperty("Transfer-Encoding", "chunked");
		        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
				connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
				connection.setRequestProperty("Accept-Encoding", "gzip,deflate");
				connection.setRequestProperty("Host", "t-square.gatech.edu");
				connection.setRequestProperty("DNT", "1");
		        connection.setRequestProperty("Referer", "https://t-square.gatech.edu/portal/pda?ticket=ST-"+ticket+"?force.login=yes");
		        connection.setRequestProperty("Cookie", tsquareJS+"; "+ bigip);
		        connection.setRequestProperty("Connection","Keep-Alive");
		       
		        BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
				String inputLine;
				StringBuffer html = new StringBuffer();
				
				while ((inputLine = in.readLine()) != null) {
					html.append(inputLine);
				}
				in.close();
//				System.out.println(html);

		        Log.e("jsoup", "-->"+ board + " " + subject);

	        	
	        	if(board.toLowerCase(Locale.getDefault()).contains("announcements")){
			        Document menuDoc =  Jsoup.parse(html.toString());
			        Elements menuElems = menuDoc.select(jsoupParser(board, subject, 1));

	        		String stemp = null;
    				TsquareArrays temp = null;
    				int index = 0;
	        		for(Element menuElem : menuElems){
	        			if(index%2 == 0){
		    				if(menuElem.attr("title").contains("View announcement")){
		    					stemp = menuElem.attr("title").substring(18);
		    				}
		    				
		    				if(stemp==null){
		    					temp= new TsquareArrays(subject, board, menuElem.attr("title"), menuElem.attr("href"));
		    				}else{
		    					temp = new TsquareArrays(subject, board, stemp, menuElem.attr("href"));
		    				}
		    				
	        			}else{
	    				//curTemp; title; content; date
		    				String date = menuElem.select("td[headers=date]").text();
		    				temp = new TsquareArrays(temp.getName(), temp.getBoard(), temp.getTitle(), "NaN", date , temp.getLink());
				        	curList.add(temp);
	        			}
	        			index++;
			        	Log.e("jsoupAnn",">" + temp.getName() + " for " + temp.getBoard() + ": " + temp.getTitle());// + ":" + temp.getLink());
	        		}
	        	}
	        	
	        	if(board.toLowerCase(Locale.US).equals("assignments")){
			        Document menuDoc =  Jsoup.parse(html.toString());
			        Elements menuElems = menuDoc.select(jsoupParser(board, subject, 1));

					TsquareArrays temp = null;
					String date = null;
					int index = 0;
					String title = null;
					for(Element menuElem : menuElems){
						if(index%3==0){
							title = menuElem.select("a").text();
							temp = new TsquareArrays(title, menuElem.attr("href"));
						}else if(index%3 == 1){
							date = menuElem.select("td").text(); // Open
						}else{
							String content = menuElem.select("td").text(); // Due
							temp = new TsquareArrays(subject, board, title, content, date, temp.getLink());
							curList.add(temp);
				        	Log.e("jsoupAss",">" + temp.getName() + temp.getTitle());// + ":" + temp.getLink());							
						}
						index++;
					}
				}
	        	
	        	if (board.contains("Gradebook")){
			        Document menuDoc =  Jsoup.parse(html.toString());
			        Elements menuElems = menuDoc.select(jsoupParser(board, subject, 1));

					for(Element menuElem : menuElems){
	    				TsquareArrays temp;
						if(menuElem.children().size()>=4 && menuElem.hasAttr("id")){
							String itemName = menuElem.select(".left").text();
							String itemDate = menuElem.select(".center").first().text();
							String itemGrade = "";
							if(menuElem.select(".center").get(1).hasText()){
								itemGrade = menuElem.select(".center").get(1).text();
							}
							if(itemDate.equals("-")){
								String dateTemp = "(No date)";
								itemDate = dateTemp;
							}
							if(itemGrade.equals("-")){
								String gradeTemp = "(No grade)";
								itemGrade = gradeTemp;
							}
							//else{
							//	itemGrade = link.select(".center").get(1).select(".Not counted towards course grade").text();
							//}
							temp = new TsquareArrays(subject, board, itemName, itemGrade, itemDate, "NaN");
							curList.add(temp);
							Log.e("jsoupGra", itemName + " " + itemDate + " " + itemGrade);
							
						}
					}
				}
	        	
	        	//Directs to piazza app
	        	if (board.contains("Piazza")){
//			        Document menuDoc =  Jsoup.parse(html.toString());
//			        Elements menuElems = menuDoc.select(jsoupParser(board, subject, 1));
//					for(Element menuElem : menuElems){
						String linkToPiazza = tempLink;
//						String linkToPiazza = "https://t-square.gatech.edu" + menuElem.attr("src");
	    				TsquareArrays temp = new TsquareArrays(subject, board, "NaN", "NaN", "NaN", linkToPiazza);
	    				curList.add(temp);
						Log.e("jsoupPiazza", linkToPiazza);
//						}
					}
	        	}
	        
	        	//Support only webview
	        	if(board.contains("Syllabus") || board.contains("Section Info")
	        			|| board.contains("Forums") || board.contains("Chat Room")
	        			|| board.contains("Wiki")){
    				curList.add(new TsquareArrays(subject, board, "NaN", "NaN", "NaN", menuURL.toString()));
	        	}

	        
		}
       
        return curList;
	}

	private String returnForumAddress(String url, String referer, String ticket, String tsquareJS, String bigip) throws Exception{
		HttpsURLConnection connection = (HttpsURLConnection) (new URL(url)).openConnection();
        connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		connection.setRequestProperty("Accept-Language", "en-US;q=0.6,en;q=0.4");
		connection.setRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; U; CPU iPhone OS 4_3_2 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8H7 Safari/6533.18.5");
		connection.setRequestProperty("Accept-Encoding", "gzip,deflate,sdch");
		connection.setRequestProperty("Host", "t-square.gatech.edu");
		connection.setRequestProperty("DNT", "1");
        connection.setRequestProperty("Referer", referer);
        connection.setRequestProperty("Cookie", tsquareJS+"; "+ bigip);
        connection.setRequestProperty("Connection","Keep-Alive");
        if(connection.getResponseCode()== 302){
        	return connection.getHeaderField("Location");
        }
        return "/discussionForum/forumsOnly/dfForums";
	}
}
