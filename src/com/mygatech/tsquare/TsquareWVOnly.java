
package com.mygatech.tsquare;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.Fields;
import com.google.analytics.tracking.android.GoogleAnalytics;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.mygatech.LoginChecker;
import com.mygatech.LoginTask;
import com.mygatech.MainActivity;
import com.mygatech.R;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebBackForwardList;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class TsquareWVOnly extends Activity {
	@SuppressWarnings("unused")
	private String subjectName, boardName, contentLink;
	private CookieManager manager;
	private WebView wv;
	private MenuItem menuItem;

	public boolean isNetworkConnected() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}
	
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

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.tsquare_main);
		overridePendingTransition(R.anim.fade, R.anim.hold);
		EasyTracker.getInstance(this).activityStart(this);
		GoogleAnalytics.getInstance(this).getTracker(MainActivity.TRACKING);
		Tracker tracker = GoogleAnalytics.getInstance(this).getTracker(	MainActivity.TRACKING);
		tracker.send(MapBuilder.createAppView().set(Fields.SCREEN_NAME, "T-Square webview only").build());
	}

	class ZoomJSInterface
	{
		String style = "<style> li {font-size:175%;} </style>";
		@JavascriptInterface
		public void processHTML(String html) {
			final String replacedHtml = html.replace("</head>", style+"</head>");
			
			//calling wv in ui thread
			runOnUiThread(new Runnable() {
		           @Override
		           public void run() {
						wv.loadData("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" + replacedHtml + "</html>", "text/html", "utf-8");
						WebBackForwardList wf = wv.copyBackForwardList();
						for(int i=0; i < wf.getSize(); i++) {
							Log.i("Url in history is ", wf.getItemAtIndex(i).getUrl() + "" );
						}
		           }
		    });
		}
	}
	String isLogedInJS = "javascript:var x = function(){if(document.getElementsByClassName('loginLink')) return \"true\"; else return \"false\";}; x();";
	
	class LogInJSInterface
	{
		String JS = "javascript:" + "document.getElementsByName('username').item(0).value='"+ 
				getSharedPreferences(MainActivity.getUserDetails(), 0).getString("username", "") + "';"+
				"document.getElementsByName('password').item(0).value='" + 
				getSharedPreferences(MainActivity.getUserDetails(), 0).getString("password", "") + "';"+
				"var inputs = document.getElementsByTagName('input');"+
				"for(var i=0; i<inputs.length; i++){" +
					"if(inputs[i].getAttribute('type')=='submit'){" +
						"inputs[i].click();" +
					"}" +
				"};";
		@JavascriptInterface
		public void login() {
			Log.i("Login injection", "begins :) ");
			//calling wv in ui thread
			runOnUiThread(new Runnable() {
		           @Override
		           public void run() {
		        	   wv.loadUrl(JS);
		        	   
		           }
		    });
		}
	}

	@Override
	protected void onResume(){
		super.onResume();
		
		setTitle("T-Square");
		if (isNetworkConnected()) {
			CookieSyncManager.createInstance(getApplicationContext());
			CookieSyncManager.getInstance().startSync();
			manager = CookieManager.getInstance();
			wv = (WebView) findViewById(R.id.webView);
			wv.getSettings().setJavaScriptEnabled(true);
			wv.addJavascriptInterface(new ZoomJSInterface(), "ZOOM");
			wv.addJavascriptInterface(new LogInJSInterface(), "LOGIN");
			
			wv.setWebViewClient(new WebViewClient() {
				@Override
				public void onPageFinished(WebView view, String url) {
					super.onPageFinished(view, url);
					if (menuItem != null) {
						menuItem.collapseActionView();
						menuItem.setActionView(null);
						new ToastRun("Done :)").run();
					}
					Log.d("Current URL is ", url);

					if (url.startsWith("https://t-square.gatech.edu/portal/pda") || url.equals("https://t-square.gatech.edu/portal/pda?force.login=yes")){
						wv.loadUrl("javascript:window.ZOOM.processHTML(document.getElementsByTagName('html')[0].innerHTML);");
//						wv.loadData("<html xmlns=\"http://www.w3.org/1999/xhtml\" lang=\"en\" xml:lang=\"en\">" + htmlOuter + "</html>", "text/", "utf-8");
					}
					if (url.equals("https://login.gatech.edu/cas/login?service=https%3A%2F%2Ft-square.gatech.edu%2Fsakai-login-tool%2Fcontainer")) {
						wv.loadUrl("javascript:window.LOGIN.login();");
					}
				}
			});
			wv.setPadding(5, 0, 5, 0);
			wv.loadUrl("https://t-square.gatech.edu/portal/pda");
			
		} else if (!((WifiManager) getSystemService(Context.WIFI_SERVICE))
				.isWifiEnabled()) {
			new AlertDialog.Builder(this)
					.setTitle("Network status alert")
					.setMessage(
							"Network is not connected.\nWould you like to enable the WI-FI?")
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
									WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
									wifiManager.setWifiEnabled(true);
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).show();
		} else { // WIFI is on and network is not connected
			new AlertDialog.Builder(this)
					.setTitle("Network status alert")
					.setMessage(
							"Network is not connected with WI-FI.\nPlease check your network.")
					.setPositiveButton("Ok",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									finish();
								}
							}).show();
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		EasyTracker.getInstance(this).activityStart(this); // Add this method.
	}

	@Override
	public void onStop() {
		super.onStop();
		EasyTracker.getInstance(this).activityStop(this); // Add this method.
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_load:
			menuItem = item;
			menuItem.setActionView(R.layout.progressbar);
			menuItem.expandActionView();
			wv.reload();
			Toast.makeText(this, "Refreshing :)", Toast.LENGTH_LONG).show();
			break;
		case android.R.id.home:
			onBackPressed();
//			NavUtils.navigateUpFromSameTask(this);
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.tsquare_menu, menu);
		return true;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack()) {
			wv.goBack();
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}



}
