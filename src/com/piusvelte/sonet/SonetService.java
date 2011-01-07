/*
 * Sonet - Android Social Networking Widget
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.sonet;

import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;
import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.ACTION_BUILD_SCROLL;

import static com.piusvelte.sonet.Tokens.TWITTER_KEY;
import static com.piusvelte.sonet.Tokens.TWITTER_SECRET;
import static com.piusvelte.sonet.Tokens.MYSPACE_KEY;
import static com.piusvelte.sonet.Tokens.MYSPACE_SECRET;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
//import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.signature.SignatureMethod;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service implements Runnable {
	private static final String TAG = "SonetService";
	private BroadcastReceiver mReceiver;
	private Thread sThread;

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		if (intent != null) {
			if (intent.getAction() != null) {
				if (intent.getAction().equals(ACTION_REFRESH)) {
					if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
					else SonetService.updateWidgets(getAppWidgetIds());
				} else SonetService.updateWidgets(new int[] {Integer.parseInt(intent.getAction())});
			} else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) SonetService.updateWidgets(intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
			else if (intent.hasExtra(AppWidgetManager.EXTRA_APPWIDGET_ID)) SonetService.updateWidgets(new int[]{intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)});
			else SonetService.updateWidgets(getAppWidgetIds());
		} else SonetService.updateWidgets(getAppWidgetIds());
		synchronized (sLock) {
			if ((sThread == null) || !sThread.isAlive()) (sThread = new Thread(this)).start();
		}
	}

	private int[] getAppWidgetIds() {
		int[] appWidgetIds = new int[0];
		Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.WIDGET}, null, null, null);
		if (accounts.moveToFirst()) {
			appWidgetIds = new int[accounts.getCount()];
			int iwidget = accounts.getColumnIndex(Accounts.WIDGET),
			counter = 0;
			while (!accounts.isAfterLast()) {
				appWidgetIds[counter] = accounts.getInt(iwidget);
				counter++;
				accounts.moveToNext();
			}
		} else appWidgetIds = new int[0];
		accounts.close();
		return appWidgetIds;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		onStart(intent, startId);
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if (mReceiver != null) {
			unregisterReceiver(mReceiver);
			mReceiver = null;
		}
		super.onDestroy();
	}

	private static Object sLock = new Object();
	private static Queue<Integer> sAppWidgetIds = new LinkedList<Integer>();

	public static void updateWidgets(int[] appWidgetIds) {
		synchronized (sLock) {
			for (int appWidgetId : appWidgetIds) sAppWidgetIds.add(appWidgetId);
		}
	}

	private static boolean updatesQueued() {
		synchronized (sLock) {
			return !sAppWidgetIds.isEmpty();
		}
	}

	private static int getNextUpdate() {
		synchronized (sLock) {
			if (sAppWidgetIds.peek() == null) return AppWidgetManager.INVALID_APPWIDGET_ID;
			else return sAppWidgetIds.poll();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private Date parseDate(String date, String format, int timezone) {
		SimpleDateFormat msformat = new SimpleDateFormat(format);
		Calendar cal = Calendar.getInstance();
		Date created;
		try {
			created = msformat.parse(date);
		} catch (ParseException e) {
			created = new Date();
			Log.e(TAG,e.toString());
		}
		cal.setTime(created);
		cal.add(Calendar.HOUR, timezone);
		return cal.getTime();
	}

	private ContentValues statusItem(long created, String link, String friend, byte[] profile, String message, int service, String createdText, int appWidgetId) {
		ContentValues values = new ContentValues();
		values.put(Statuses.CREATED, created);
		values.put(Statuses.LINK, link);
		values.put(Statuses.FRIEND, friend);
		values.put(Statuses.PROFILE, profile);
		values.put(Statuses.MESSAGE, message);
		values.put(Statuses.SERVICE, service);
		values.put(Statuses.CREATEDTEXT, createdText);
		values.put(Statuses.WIDGET, appWidgetId);
		return values;
	}

	private String getCreatedText(long now, Date created, boolean time24hr) {
		return now - created.getTime() < 86400000 ?
				(time24hr ?
						String.format("%d:%02d", created.getHours(), created.getMinutes())
						: String.format("%d:%02d%s", created.getHours() < 13 ? created.getHours() : created.getHours() - 12, created.getMinutes(), getString(created.getHours() < 13 ? R.string.am : R.string.pm)))
						: String.format("%s %d", getResources().getStringArray(R.array.months)[created.getMonth()], created.getDate());
	}

	private byte[] getProfile(String url) {
		ByteArrayOutputStream blob = new ByteArrayOutputStream();
		Bitmap profile = null;
		// get profile
		try {
			profile = BitmapFactory.decodeStream(new URL(url).openConnection().getInputStream());
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
		}
		if (profile != null) profile.compress(Bitmap.CompressFormat.PNG, 100, blob);
		return blob.toByteArray();
	}

	@Override
	public void run() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		boolean hasConnection = (cm.getActiveNetworkInfo() != null) && cm.getActiveNetworkInfo().isConnected();
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Boolean hasbuttons,
		time24hr,
		hasAccount = true;
		int interval,
		buttons_bg_color,
		buttons_color,
		messages_bg_color,
		messages_color,
		friend_color,
		created_color,
		buttons_textsize,
		messages_textsize,
		friend_textsize,
		created_textsize;
		SharedPreferences sp = null;
		while (updatesQueued()) {
			// first handle deletes, then scroll updates, finally regular updates
			int appWidgetId;
				appWidgetId = getNextUpdate();
				alarmManager.cancel(PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
				Cursor settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.INTERVAL, Widgets.HASBUTTONS, Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_COLOR, Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_COLOR, Widgets.FRIEND_COLOR, Widgets.CREATED_COLOR, Widgets.TIME24HR, Widgets.BUTTONS_TEXTSIZE, Widgets.MESSAGES_TEXTSIZE, Widgets.FRIEND_TEXTSIZE, Widgets.CREATED_TEXTSIZE}, Widgets.WIDGET + "=" + appWidgetId, null, null);
				if (settings.moveToFirst()) {
					interval = settings.getInt(settings.getColumnIndex(Widgets.INTERVAL));
					hasbuttons = settings.getInt(settings.getColumnIndex(Widgets.HASBUTTONS)) == 1;
					buttons_bg_color = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_BG_COLOR));
					buttons_color = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_COLOR));
					messages_bg_color = settings.getInt(settings.getColumnIndex(Widgets.MESSAGES_BG_COLOR));
					messages_color = settings.getInt(settings.getColumnIndex(Widgets.MESSAGES_COLOR));
					friend_color = settings.getInt(settings.getColumnIndex(Widgets.FRIEND_COLOR));
					created_color = settings.getInt(settings.getColumnIndex(Widgets.CREATED_COLOR));
					time24hr = settings.getInt(settings.getColumnIndex(Widgets.TIME24HR)) == 1;
					buttons_textsize = settings.getInt(settings.getColumnIndex(Widgets.BUTTONS_TEXTSIZE));
					messages_textsize = settings.getInt(settings.getColumnIndex(Widgets.MESSAGES_TEXTSIZE));
					friend_textsize = settings.getInt(settings.getColumnIndex(Widgets.FRIEND_TEXTSIZE));
					created_textsize = settings.getInt(settings.getColumnIndex(Widgets.CREATED_TEXTSIZE));
				} else {
					// upgrade, moving settings from sharedpreferences to db
					if (sp == null) sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
					interval = Integer.parseInt((String) sp.getString(getString(R.string.key_interval), getString(R.string.default_interval)));
					hasbuttons = sp.getBoolean(getString(R.string.key_display_buttons), true);
					buttons_bg_color =Integer.parseInt(sp.getString(getString(R.string.key_head_background), getString(R.string.default_buttons_bg_color)));
					buttons_color = Integer.parseInt(sp.getString(getString(R.string.key_head_text), getString(R.string.default_buttons_color)));
					messages_bg_color = Integer.parseInt(sp.getString(getString(R.string.key_body_background), getString(R.string.default_message_bg_color)));
					messages_color = Integer.parseInt(sp.getString(getString(R.string.key_body_text), getString(R.string.default_message_color)));
					friend_color = Integer.parseInt(sp.getString(getString(R.string.key_friend_text), getString(R.string.default_friend_color)));
					created_color = Integer.parseInt(sp.getString(getString(R.string.key_created_text), getString(R.string.default_created_color)));
					time24hr = sp.getBoolean(getString(R.string.key_time_12_24), false);
					buttons_textsize = Integer.parseInt(sp.getString(getString(R.string.key_buttons_textsize), getString(R.string.default_buttons_textsize)));
					messages_textsize = Integer.parseInt(sp.getString(getString(R.string.key_messages_textsize), getString(R.string.default_messages_textsize)));
					friend_textsize = Integer.parseInt(sp.getString(getString(R.string.key_friend_textsize), getString(R.string.default_friend_textsize)));
					created_textsize = Integer.parseInt(sp.getString(getString(R.string.key_created_textsize), getString(R.string.default_created_textsize)));
					ContentValues values = new ContentValues();
					values.put(Widgets.INTERVAL, interval);
					values.put(Widgets.HASBUTTONS, hasbuttons);
					values.put(Widgets.BUTTONS_BG_COLOR, buttons_bg_color);
					values.put(Widgets.BUTTONS_COLOR, buttons_color);
					values.put(Widgets.MESSAGES_BG_COLOR, messages_bg_color);
					values.put(Widgets.MESSAGES_COLOR, messages_color);
					values.put(Widgets.FRIEND_COLOR, friend_color);
					values.put(Widgets.CREATED_COLOR, created_color);
					values.put(Widgets.TIME24HR, time24hr);
					values.put(Widgets.BUTTONS_TEXTSIZE, buttons_textsize);
					values.put(Widgets.MESSAGES_TEXTSIZE, messages_textsize);
					values.put(Widgets.FRIEND_TEXTSIZE, friend_textsize);
					values.put(Widgets.CREATED_TEXTSIZE, created_textsize);
					values.put(Widgets.WIDGET, appWidgetId);
					this.getContentResolver().insert(Widgets.CONTENT_URI, values);
				}
				settings.close();
				if (hasConnection) {
					// query accounts
					/* get statuses for all accounts
					 * then sort them by datetime, descending
					 */
					Cursor accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY, Accounts.TIMEZONE}, Accounts.WIDGET + "=" + appWidgetId, null, null);
					if (accounts.getCount() == 0) {
						// check for old accounts without appwidgetid
						accounts.close();
						accounts = this.getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.USERNAME, Accounts.TOKEN, Accounts.SECRET, Accounts.SERVICE, Accounts.EXPIRY, Accounts.TIMEZONE}, Accounts.WIDGET + "=''", null, null);
						if (accounts.moveToFirst()) {
							// upgrade the accounts, adding the appwidgetid
							int username = accounts.getColumnIndex(Accounts.USERNAME),
							token = accounts.getColumnIndex(Accounts.TOKEN),
							secret = accounts.getColumnIndex(Accounts.SECRET),
							service = accounts.getColumnIndex(Accounts.SERVICE),
							expiry = accounts.getColumnIndex(Accounts.EXPIRY),
							timezone = accounts.getColumnIndex(Accounts.TIMEZONE);
							while (!accounts.isAfterLast()) {
								ContentValues values = new ContentValues();
								values.put(Accounts.USERNAME, accounts.getString(username));
								values.put(Accounts.TOKEN, accounts.getString(token));
								values.put(Accounts.SECRET, accounts.getString(secret));
								values.put(Accounts.SERVICE, accounts.getInt(service));
								values.put(Accounts.EXPIRY, accounts.getInt(expiry));
								values.put(Accounts.TIMEZONE, accounts.getInt(timezone));
								values.put(Accounts.WIDGET, appWidgetId);
								this.getContentResolver().insert(Accounts.CONTENT_URI, values);
								accounts.moveToNext();
							}
						} else hasAccount = false;
						this.getContentResolver().delete(Accounts.CONTENT_URI, Accounts._ID + "=''", null);
					}
					if (accounts.getCount() > 0) {
						// load the updates
						accounts.moveToFirst();
						int iservice = accounts.getColumnIndex(Accounts.SERVICE),
						itoken = accounts.getColumnIndex(Accounts.TOKEN),
						isecret = accounts.getColumnIndex(Accounts.SECRET),
						iexpiry = accounts.getColumnIndex(Accounts.EXPIRY),
						itimezone = accounts.getColumnIndex(Accounts.TIMEZONE);
						String name = "name",
						id = "id",
						status = "status";
						long now = new Date().getTime();
						while (!accounts.isAfterLast()) {
							int service = accounts.getInt(iservice);
							switch (service) {
							case TWITTER:
								String status_url = "http://twitter.com/%s/status/%s";
								try {
									List<Status> statuses = (new TwitterFactory().getOAuthAuthorizedInstance(TWITTER_KEY, TWITTER_SECRET, new AccessToken(accounts.getString(itoken), accounts.getString(isecret)))).getFriendsTimeline();
									// if there are updates, clear the cache
									if (!statuses.isEmpty()) this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + appWidgetId + " and " + Statuses.SERVICE + "=" + service, null);
									for (Status s : statuses) {
										String screenname = s.getUser().getScreenName();
										Date created = s.getCreatedAt();
										this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
												String.format(status_url, screenname, Long.toString(s.getId())),
												screenname,
												getProfile(s.getUser().getProfileImageURL().toString()),
												s.getText(),
												service,
												getCreatedText(now, created, time24hr),
												appWidgetId));
									}
								} catch (TwitterException te) {
									Log.e(TAG, te.toString());
								}
								break;
							case FACEBOOK:
								String created_time = "created_time",
								actions = "actions",
								link = "link",
								comment = "Comment",
								from = "from",
								type = "type",
								profile = "http://graph.facebook.com/%s/picture",
								message = "message",
								data = "data",
								to = "to",
								fburl = "http://www.facebook.com";
								Facebook facebook = new Facebook();
								facebook.setAccessToken(accounts.getString(itoken));
								facebook.setAccessExpires((long)accounts.getInt(iexpiry));
								try {
									// limit the returned fields
									Bundle parameters = new Bundle();
									parameters.putString("fields", "actions,link,type,from,message,created_time,to");
									JSONObject jobj = Util.parseJson(facebook.request("me/home", parameters));
									JSONArray jarr = jobj.getJSONArray(data);
									// if there are updates, clear the cache
									if (jarr.length() > 0)  this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + appWidgetId + " and " + Statuses.SERVICE + "=" + service, null);
									for (int d = 0; d < jarr.length(); d++) {
										JSONObject o = jarr.getJSONObject(d);
										// only parse status types, not photo, video or link
										if (o.has(type) && o.getString(type).equals(status) && o.has(from) && o.has(message)) {
											// parse the link
											String l = fburl;
											if (o.has(actions)) {											
												JSONArray action = o.getJSONArray(actions);
												for (int a = 0; a < action.length(); a++) {
													JSONObject n = action.getJSONObject(a);
													if (n.getString(name) == comment) {
														l = n.getString(link);
														break;
													}
												}
											}
											JSONObject f = o.getJSONObject(from);
											if (f.has(name) && f.has(id)) {
												String friend = f.getString(name);
												if (o.has(to)) {
													// handle wall messages from one friend to another
													JSONObject t = o.getJSONObject(to);
													if (t.has(data)) {
														JSONObject n = t.getJSONArray(data).getJSONObject(0);
														if (n.has(name)) friend += " > " + n.getString(name);
													}												
												}
												Date created = parseDate(o.getString(created_time), "yyyy-MM-dd'T'HH:mm:ss'+0000'", accounts.getInt(itimezone));
												this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(
														created.getTime(),
														l,
														friend,
														getProfile(String.format(profile, f.getString(id))),
														o.getString(message),
														service,
														getCreatedText(now, created, time24hr),
														appWidgetId));
											}
										}
									}
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
								} catch (FacebookError e) {
									Log.e(TAG, e.toString());
								} catch (IOException e) {
									Log.e(TAG, e.toString());
								}
								break;
							case MYSPACE:
								//								OAuthConsumer consumer = new DefaultOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET);
								String displayName = "displayName",
								moodStatusLastUpdated = "moodStatusLastUpdated",
								thumbnailUrl = "thumbnailUrl",
								source = "source",
								url = "url",
								author = "author";
								OAuthConsumer consumer = new CommonsHttpOAuthConsumer(MYSPACE_KEY, MYSPACE_SECRET, SignatureMethod.HMAC_SHA1);
								consumer.setTokenWithSecret(accounts.getString(itoken), accounts.getString(isecret));
								HttpClient client = new DefaultHttpClient();
								ResponseHandler <String> responseHandler = new BasicResponseHandler();
								HttpGet request = new HttpGet("http://opensocial.myspace.com/1.0/statusmood/@me/@friends/history?includeself=true&fields=author,source");
								try {
									consumer.sign(request);
									JSONObject jobj = new JSONObject(client.execute(request, responseHandler));
									JSONArray entries = jobj.getJSONArray("entry");
									// if there are updates, clear the cache
									if (entries.length() > 0)  this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + appWidgetId + " and " + Statuses.SERVICE + "=" + service, null);
									for (int e = 0; e < entries.length(); e++) {
										JSONObject entry = entries.getJSONObject(e);
										JSONObject authorObj = entry.getJSONObject(author);
										Date created = parseDate(entry.getString(moodStatusLastUpdated), "yyyy-MM-dd'T'HH:mm:ss'Z'", accounts.getInt(itimezone));
										this.getContentResolver().insert(Statuses.CONTENT_URI, statusItem(created.getTime(),
												entry.getJSONObject(source).getString(url),
												authorObj.getString(displayName),
												getProfile(authorObj.getString(thumbnailUrl)),
												entry.getString(status),
												service,
												getCreatedText(now, created, time24hr),
												appWidgetId));
									}
								} catch (ClientProtocolException e) {
									Log.e(TAG, e.toString());
								} catch (JSONException e) {
									Log.e(TAG, e.toString());
								} catch (IOException e) {
									Log.e(TAG, e.toString());
								} catch (OAuthMessageSignerException e) {
									Log.e(TAG, e.toString());
								} catch (OAuthExpectationFailedException e) {
									Log.e(TAG, e.toString());
								}
								//								} catch (OAuthCommunicationException e) {
								//									Log.e(TAG, e.toString());
								//								}
								break;
							}
							accounts.moveToNext();
						}
					} else this.getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + appWidgetId, null); // no accounts, clear cache
					accounts.close();
				}
				// race condition when finished configuring, the service starts.
				// meanwhile, the launcher broadcasts READY and the listview is created. it's at this point that the widget is marked scrollable
				// this run finishes after the listview is created, but is not flagged as scrollable and replaces the listview with the regular widget
				boolean scrollable = false;
				settings = this.getContentResolver().query(Widgets.CONTENT_URI, new String[]{Widgets._ID, Widgets.SCROLLABLE}, Widgets.WIDGET + "=" + appWidgetId, null, null);
				if (settings.getCount() > 0) {
					settings.moveToFirst();
					scrollable = settings.getInt(settings.getColumnIndex(Widgets.SCROLLABLE)) == 1;
				}
				settings.close();
				// Push update for this widget to the home screen
				// set messages background
				Bitmap messages_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				Canvas messages_bg_canvas = new Canvas(messages_bg);
				messages_bg_canvas.drawColor(messages_bg_color);
				int[] map_item = {R.id.item0, R.id.item1, R.id.item2, R.id.item3, R.id.item4, R.id.item5, R.id.item6},
				map_profile = {R.id.profile0, R.id.profile1, R.id.profile2, R.id.profile3, R.id.profile4, R.id.profile5, R.id.profile6},
				map_message = {R.id.message0, R.id.message1, R.id.message2, R.id.message3, R.id.message4, R.id.message5, R.id.message6},
				map_screenname = {R.id.friend0, R.id.friend1, R.id.friend2, R.id.friend3, R.id.friend4, R.id.friend5, R.id.friend6},
				map_created = {R.id.created0, R.id.created1, R.id.created2, R.id.created3, R.id.created4, R.id.created5, R.id.created6};
				RemoteViews views = new RemoteViews(getPackageName(), hasbuttons ? R.layout.widget : R.layout.widget_nobuttons);
				if (hasbuttons) {
					Bitmap buttons_bg = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
					Canvas buttons_bg_canvas = new Canvas(buttons_bg);
					buttons_bg_canvas.drawColor(buttons_bg_color);
					views.setImageViewBitmap(R.id.buttons_bg, buttons_bg);
					views.setTextColor(R.id.head_spacer, buttons_bg_color);
					views.setOnClickPendingIntent(R.id.button_post, PendingIntent.getActivity(this, 0, new Intent(this, PostDialog.class), 0));
					views.setTextColor(R.id.button_post, buttons_color);
					views.setFloat(R.id.button_post, "setTextSize", buttons_textsize);
					views.setOnClickPendingIntent(R.id.button_configure, PendingIntent.getActivity(this, 0, new Intent(this, UI.class).setAction(Integer.toString(appWidgetId)), 0));
					views.setTextColor(R.id.button_configure, buttons_color);
					views.setFloat(R.id.button_configure, "setTextSize", buttons_textsize);
					views.setOnClickPendingIntent(R.id.button_refresh, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
					views.setTextColor(R.id.button_refresh, buttons_color);
					views.setFloat(R.id.button_refresh, "setTextSize", buttons_textsize);
				}
				views.setImageViewBitmap(R.id.messages_bg, messages_bg);
				if (!scrollable) {
					Cursor statuses = this.getContentResolver().query(Statuses.CONTENT_URI, new String[]{Statuses._ID, Statuses.LINK, Statuses.FRIEND, Statuses.PROFILE, Statuses.MESSAGE, Statuses.SERVICE, Statuses.CREATEDTEXT}, Statuses.WIDGET + "=" + appWidgetId, null, Statuses.CREATED + " desc");
					if (statuses.getCount() > 0) {
						int count_status = 0;
						statuses.moveToFirst();
						int ilink = statuses.getColumnIndex(Statuses.LINK),
						iprofile = statuses.getColumnIndex(Statuses.PROFILE),
						ifriend = statuses.getColumnIndex(Statuses.FRIEND),
						imessage = statuses.getColumnIndex(Statuses.MESSAGE),
						iservice = statuses.getColumnIndex(Statuses.SERVICE),
						icreatedText = statuses.getColumnIndex(Statuses.CREATEDTEXT);
						while (!statuses.isAfterLast() && (count_status < map_item.length)) {
							views.setTextViewText(map_message[count_status], statuses.getString(imessage));
							views.setTextColor(map_message[count_status], messages_color);
							views.setFloat(map_message[count_status], "setTextSize", messages_textsize);
							// if no buttons, use StatusDialog.java with options for Config and Refresh
							if (hasbuttons) views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_VIEW, Uri.parse(statuses.getString(ilink))), 0));
							else views.setOnClickPendingIntent(map_item[count_status], PendingIntent.getActivity(this, 0, new Intent(this, StatusDialog.class).setAction(appWidgetId+"`"+statuses.getInt(iservice)+"`"+statuses.getString(ilink)), 0));
							views.setTextViewText(map_screenname[count_status], statuses.getString(ifriend));
							views.setTextColor(map_screenname[count_status], friend_color);
							views.setFloat(map_screenname[count_status], "setTextSize", friend_textsize);
							views.setTextViewText(map_created[count_status], statuses.getString(icreatedText));
							views.setTextColor(map_created[count_status], created_color);
							views.setFloat(map_created[count_status], "setTextSize", created_textsize);
							byte[] profile = statuses.getBlob(iprofile);
							views.setImageViewBitmap(map_profile[count_status], BitmapFactory.decodeByteArray(profile, 0, profile.length));						
							count_status++;
							statuses.moveToNext();
						}
					} else {
						// no connect or account
						views.setTextViewText(map_message[0], getString(hasAccount ? hasConnection ? R.string.no_updates : R.string.no_connection : R.string.no_accounts));
						views.setTextColor(map_message[0], messages_color);
						views.setFloat(map_message[0], "setTextSize", messages_textsize);
					}
					statuses.close();
				}
				appWidgetManager.updateAppWidget(appWidgetId, views);
				// replace with scrollable widget
				if (scrollable) sendBroadcast(new Intent(this, SonetWidget.class).setAction(ACTION_BUILD_SCROLL).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId));
				if (hasAccount && (interval > 0)) alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + interval, PendingIntent.getService(this, 0, new Intent(this, SonetService.class).setAction(Integer.toString(appWidgetId)), 0));
		}
		if (hasConnection) {
			if (mReceiver != null) {
				unregisterReceiver(mReceiver);
				mReceiver = null;
			}
		} else if (mReceiver == null) {
			// if there's no connection, listen for one
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
						if (((NetworkInfo) intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).isConnected()) {
							synchronized (sLock) {
								if ((sThread == null) || !sThread.isAlive()) (sThread = new Thread((Runnable) context)).start();
							}								
						}
					}
				}
			};
			IntentFilter f = new IntentFilter();
			f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
			registerReceiver(mReceiver, f);	
		}
	}
}