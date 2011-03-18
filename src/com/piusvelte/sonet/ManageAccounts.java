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

import static com.piusvelte.sonet.Sonet.ACTION_REFRESH;
import static com.piusvelte.sonet.Sonet.FACEBOOK_PERMISSIONS;
import static com.piusvelte.sonet.Sonet.TWITTER;
import static com.piusvelte.sonet.Sonet.FACEBOOK;
import static com.piusvelte.sonet.Sonet.MYSPACE;

import static com.piusvelte.sonet.Tokens.FACEBOOK_ID;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Statuses;
import com.piusvelte.sonet.Sonet.Widgets;

import static com.piusvelte.sonet.Sonet.BUZZ;
import static com.piusvelte.sonet.Sonet.SALESFORCE;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.FacebookError;
import com.facebook.android.Util;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.Facebook.DialogListener;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class ManageAccounts extends ListActivity implements OnClickListener, DialogInterface.OnClickListener, DialogListener/*, IMSSessionCallback*/ {
	private static final String TAG = "ManageAccounts";
	private static final int REAUTH_ID = Menu.FIRST;
	private static final int SETTINGS_ID = Menu.FIRST + 1;
	private static final int DELETE_ID = Menu.FIRST + 2;
	private Facebook mFacebook;
	private AsyncFacebookRunner mAsyncRunner;
	protected static int sAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	protected static boolean sUpdateWidget = false;
	private boolean mHasAccounts = false;
	protected static long sAccountId = Sonet.INVALID_ACCOUNT_ID;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setResult(RESULT_CANCELED);

		Intent intent = getIntent();
		if (intent != null) {
			Bundle extras = intent.getExtras();
			if (extras != null) sAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
			// if called from widget, the id is set in the action, as pendingintents must have a unique action
			else if ((intent.getAction() != null) && (!intent.getAction().equals(ACTION_REFRESH)) && (!intent.getAction().equals(Intent.ACTION_VIEW))) sAppWidgetId = Integer.parseInt(intent.getAction());
		}

		setContentView(R.layout.accounts);
		registerForContextMenu(getListView());
		((Button) findViewById(R.id.default_widget_settings)).setOnClickListener(this);
		((Button) findViewById(R.id.button_add_account)).setOnClickListener(this);
	}

	@Override
	protected void onListItemClick(ListView list, View view, int position, long id) {
		super.onListItemClick(list, view, position, id);
		final long item = id;
		final CharSequence[] items = {getString(R.string.re_authenticate), getString(R.string.account_settings)};
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				which++; //fix indexing
				switch (which) {
				case REAUTH_ID:
					// need the account id if reauthenticating
					sAccountId = item;
					Cursor c = getContentResolver().query(Accounts.CONTENT_URI, new String[]{Accounts._ID, Accounts.SERVICE}, Accounts._ID + "=" + item, null, null);
					if (c.moveToFirst()) getAuth(c.getInt(c.getColumnIndex(Accounts.SERVICE)));
					c.close();
					break;
				case SETTINGS_ID:
					startActivity(new Intent(ManageAccounts.this, AccountSettings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, sAppWidgetId).putExtra(Sonet.EXTRA_ACCOUNT_ID, item));
					break;
				}
				dialog.cancel();
			}
		}).show();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, view, menuInfo);
		menu.add(0, DELETE_ID, 0, R.string.delete_account);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == DELETE_ID) {
			sUpdateWidget = true;
			getContentResolver().delete(Accounts.CONTENT_URI, Accounts._ID + "=" + ((AdapterContextMenuInfo) item.getMenuInfo()).id, null);
			// need to delete the statuses and settings for this account
			getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=? and " + Widgets.ACCOUNT + "=?", new String[]{Integer.toString(sAppWidgetId), Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id)});
			getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=? and " + Statuses.ACCOUNT + "=?", new String[]{Integer.toString(sAppWidgetId), Long.toString(((AdapterContextMenuInfo) item.getMenuInfo()).id)});
		}
		return super.onContextItemSelected(item);
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_add_account:
			// add a new account
			String[] services = getResources().getStringArray(R.array.service_entries);
			(new AlertDialog.Builder(this))
			.setItems(services, this)
			.show();
			break;
		case R.id.default_widget_settings:
			startActivity(new Intent(this, Settings.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, sAppWidgetId));
			break;
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		listAccounts();
		// returning from twitter login, setresult needs to be called
		if (sUpdateWidget && mHasAccounts) setResultOK();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (sUpdateWidget) startService(new Intent(this, SonetService.class).setAction(ACTION_REFRESH).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{sAppWidgetId}));
		else if (!mHasAccounts) {
			// clean up any setup for this widget
			getContentResolver().delete(Widgets.CONTENT_URI, Widgets.WIDGET + "=" + sAppWidgetId, null);
			getContentResolver().delete(Statuses.CONTENT_URI, Statuses.WIDGET + "=" + sAppWidgetId, null);
		}
	}

	// convenience method
	private void setResultOK() {
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, sAppWidgetId);
		setResult(RESULT_OK, resultValue);		
	}

	private void listAccounts() {
		// prepend service name to username
		Cursor c = this.managedQuery(Accounts.CONTENT_URI, new String[]{Accounts._ID, "(case when " + Accounts.SERVICE + "='" + TWITTER + "' then 'Twitter: ' when " + Accounts.SERVICE + "='" + FACEBOOK + "' then 'Facebook: ' when " + Accounts.SERVICE + "='" + MYSPACE + "' then 'MySpace: ' when " + Accounts.SERVICE + "='" + BUZZ + "' then 'Buzz: ' else '' end)||" + Accounts.USERNAME + " as " + Accounts.USERNAME, Accounts.SERVICE}, Accounts.WIDGET + "=?", new String[]{Integer.toString(sAppWidgetId)}, null);
		mHasAccounts = c.getCount() != 0;
		setListAdapter(new SimpleCursorAdapter(ManageAccounts.this, R.layout.accounts_row, c, new String[] {Accounts.USERNAME}, new int[] {R.id.account_username}));
	}

	private void getAuth(int service) {
		switch (service) {
		case TWITTER:
			startActivity(new Intent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, service));
			break;
		case FACEBOOK:
//			mFacebook = new Facebook();
//			mAsyncRunner = new AsyncFacebookRunner(mFacebook);
//			mFacebook.setAccessToken(null);
//			mFacebook.setAccessExpires(0);
//			mFacebook.authorize(this, FACEBOOK_ID, FACEBOOK_PERMISSIONS, this);
			startActivity(new Intent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, service));
			break;
		case MYSPACE:
			startActivity(new Intent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, service));
			break;
		case BUZZ:
			startActivity(new Intent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, service));
			break;
		case SALESFORCE:
			startActivity(new Intent(this, OAuthLogin.class).putExtra(Accounts.SERVICE, service));
			break;
		}
	}
	

	public void onClick(DialogInterface dialog, int which) {
		getAuth(which);
		dialog.cancel();
	}

	// facebook
	public void onComplete(Bundle values) {
		mAsyncRunner.request("me", new RequestListener() {
			@Override
			public void onComplete(String response) {
				try {
					JSONObject json = Util.parseJson(response);
					final String username = json.getString("name");
					final double timezone = Double.parseDouble(json.getString(Accounts.TIMEZONE));
					ManageAccounts.this.runOnUiThread(new Runnable() {
						public void run() {
							sUpdateWidget = true;
							setResultOK();
							ContentValues values = new ContentValues();
							values.put(Accounts.USERNAME, username);
							values.put(Accounts.TOKEN, mFacebook.getAccessToken());
							values.put(Accounts.SECRET, "");
							values.put(Accounts.EXPIRY, (int) mFacebook.getAccessExpires());
							values.put(Accounts.SERVICE, FACEBOOK);
							values.put(Accounts.TIMEZONE, timezone);
							values.put(Accounts.WIDGET, sAppWidgetId);
							if (sAccountId != Sonet.INVALID_ACCOUNT_ID) {
								getContentResolver().update(Accounts.CONTENT_URI, values, Accounts._ID + "=?", new String[]{Long.toString(sAccountId)});
								sAccountId = Sonet.INVALID_ACCOUNT_ID;
							} else getContentResolver().insert(Accounts.CONTENT_URI, values);
						}
					});
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				} catch (FacebookError e) {
					Log.e(TAG, e.toString());
				}
			}

			@Override
			public void onFacebookError(FacebookError e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onFileNotFoundException(FileNotFoundException e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onIOException(IOException e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onMalformedURLException(MalformedURLException e) {
				Log.e(TAG, e.toString());
			}
		});
	}

	public void onFacebookError(FacebookError error) {
		Toast.makeText(ManageAccounts.this, error.getMessage(), Toast.LENGTH_LONG).show();
	}

	public void onError(DialogError error) {
		Toast.makeText(ManageAccounts.this, error.getMessage(), Toast.LENGTH_LONG).show();
	}

	public void onCancel() {
		Toast.makeText(ManageAccounts.this, "Authorization canceled", Toast.LENGTH_LONG).show();
	}
}
