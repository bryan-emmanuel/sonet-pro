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

import static com.piusvelte.sonet.SonetDatabaseHelper._ID;
import static com.piusvelte.sonet.SonetDatabaseHelper.USERNAME;
import static com.piusvelte.sonet.SonetDatabaseHelper.SECRET;
import static com.piusvelte.sonet.SonetDatabaseHelper.SERVICE;
import static com.piusvelte.sonet.SonetDatabaseHelper.TOKEN;
import static com.piusvelte.sonet.SonetDatabaseHelper.TABLE_ACCOUNTS;
import static com.piusvelte.sonet.SonetDatabaseHelper.TAG;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_KEY;
import static com.piusvelte.sonet.SonetDatabaseHelper.TWITTER_SECRET;

import java.util.List;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

public class SonetService extends Service {
	private static final String ACTION = "com.piusvelte.sonet.APPWIDGET_UPDATE";
	private static final String REFRESH = "com.piusvelte.sonet.REFRESH";
	private static final int TWITTER = 0;
	private static final int FACEBOOK = 1;


	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		init(intent);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStart(intent, startId);
		init(intent);
		return START_STICKY;
	}

	public void init(Intent intent) {
		// cancel the alarm on REFRESH, it's set again later
		if  ((intent != null) && (intent.getAction() != null) && intent.getAction().equals(REFRESH)) ((AlarmManager) getSystemService(Context.ALARM_SERVICE)).cancel(PendingIntent.getBroadcast(this, 0, new Intent(this, SonetService.class).setAction(ACTION), 0));
		// check backgroundData settings and connection
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		// Build an update that holds the updated widget contents
		SharedPreferences sp = (SharedPreferences) getSharedPreferences(getString(R.string.key_preferences), SonetService.MODE_PRIVATE);
		RemoteViews updateViews = new RemoteViews(getPackageName(), R.layout.widget);
		if (cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo().isConnected()) {
			updateViews.removeAllViews(R.id.body);
			// Build the widget update for today
			// sort results
			// update
			// Pick out month names from resources
			//			Resources res = context.getResources();

			SonetDatabaseHelper sonetDatabaseHelper = new SonetDatabaseHelper(this);
			SQLiteDatabase db = sonetDatabaseHelper.getWritableDatabase();

			// query accounts
			Cursor cursor = db.query(TABLE_ACCOUNTS, new String[]{_ID, USERNAME, TOKEN, SECRET, SERVICE}, null, null, null, null, null);
			if (cursor.getCount() > 0) {
				cursor.moveToFirst();
				int service = cursor.getColumnIndex(SERVICE),
				token = cursor.getColumnIndex(TOKEN),
				secret = cursor.getColumnIndex(SECRET);
				while (!cursor.isAfterLast()) {
					switch (cursor.getInt(service)) {
					case TWITTER:
						TwitterFactory factory = new TwitterFactory();
						Twitter twitter = factory.getInstance();
						twitter.setOAuthConsumer(TWITTER_KEY, TWITTER_SECRET);
						AccessToken accessToken = new AccessToken(cursor.getString(token), cursor.getString(secret));
						twitter.setOAuthAccessToken(accessToken);
						try {
							List<Status> statuses = twitter.getFriendsTimeline();
							for (Status status : statuses) {
								RemoteViews v = new RemoteViews(getPackageName(), R.layout.friend_status);
								v.setTextViewText(R.id.friend, status.getUser().getScreenName());
								v.setTextViewText(R.id.status, status.getText());
								updateViews.addView(R.id.body, v);
							}
						} catch (TwitterException te) {
							Log.e(TAG, te.toString());
						}
						break;
					case FACEBOOK:
						break;					
					}
					cursor.moveToNext();
				}
			}
			cursor.close();
			db.close();
			// list content
			// empty list will display "loading..."
			// apply styles
		} else {
			updateViews.removeAllViews(R.id.body);
			RemoteViews view = new RemoteViews(getPackageName(), R.layout.friend_status);
			view.setTextViewText(R.id.status, getString(R.string.no_connection));
			updateViews.addView(R.id.body, view);
		}
		// Push update for this widget to the home screen
		ComponentName thisWidget = new ComponentName(this, SonetWidget_4x2.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		manager.updateAppWidget(thisWidget, updateViews);
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + Integer.parseInt((String) sp.getString(getString(R.string.key_interval), getString(R.string.default_interval))), PendingIntent.getBroadcast(this, 0, new Intent(this, SonetService.class).setAction(ACTION), 0));
	}

	@Override
	public IBinder onBind(Intent intent) {
		// We don't need to bind to this service
		return null;
	}
}
