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

import java.util.HashMap;

import com.piusvelte.sonet.Sonet.Accounts;
import com.piusvelte.sonet.Sonet.Widgets;
import com.piusvelte.sonet.Sonet.Statuses;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class SonetProvider extends ContentProvider {

	public static final String AUTHORITY = "com.piusvelte.sonet.SonetProvider";

	private static final UriMatcher sUriMatcher;

	private static final int ACCOUNTS = 0;
	private static final int WIDGETS = 1;
	private static final int STATUSES = 2;

	private static final String DATABASE_NAME = "sonet.db";
	private static final int DATABASE_VERSION = 6;

	private static final String TABLE_ACCOUNTS = "accounts";
	private static HashMap<String, String> accountsProjectionMap;

	private static final String TABLE_WIDGETS = "widgets";
	private static HashMap<String, String> widgetsProjectionMap;

	private static final String TABLE_STATUSES = "statuses";
	private static HashMap<String, String> statusesProjectionMap;

	private DatabaseHelper mDatabaseHelper;

	public static final String[] PROJECTION_APPWIDGETS = new String[] {
		SonetProviderColumns._id.toString(),
		SonetProviderColumns.created.toString(),
		SonetProviderColumns.link.toString(),
		SonetProviderColumns.friend.toString(),
		SonetProviderColumns.profile.toString(),
		SonetProviderColumns.message.toString(),
		SonetProviderColumns.service.toString(),
		SonetProviderColumns.createdtext.toString(),
		SonetProviderColumns.widget.toString()};

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		sUriMatcher.addURI(AUTHORITY, TABLE_ACCOUNTS, ACCOUNTS);

		accountsProjectionMap = new HashMap<String, String>();
		accountsProjectionMap.put(Accounts._ID, Accounts._ID);
		accountsProjectionMap.put(Accounts.USERNAME, Accounts.USERNAME);
		accountsProjectionMap.put(Accounts.TOKEN, Accounts.TOKEN);
		accountsProjectionMap.put(Accounts.SECRET, Accounts.SECRET);
		accountsProjectionMap.put(Accounts.SERVICE, Accounts.SERVICE);
		accountsProjectionMap.put(Accounts.EXPIRY, Accounts.EXPIRY);
		accountsProjectionMap.put(Accounts.TIMEZONE, Accounts.TIMEZONE);
		accountsProjectionMap.put(Accounts.WIDGET, Accounts.WIDGET);

		sUriMatcher.addURI(AUTHORITY, TABLE_WIDGETS, WIDGETS);

		widgetsProjectionMap = new HashMap<String, String>();
		widgetsProjectionMap.put(Widgets._ID, Widgets._ID);
		widgetsProjectionMap.put(Widgets.WIDGET, Widgets.WIDGET);
		widgetsProjectionMap.put(Widgets.INTERVAL, Widgets.INTERVAL);
		widgetsProjectionMap.put(Widgets.HASBUTTONS, Widgets.HASBUTTONS);
		widgetsProjectionMap.put(Widgets.BUTTONS_BG_COLOR, Widgets.BUTTONS_BG_COLOR);
		widgetsProjectionMap.put(Widgets.BUTTONS_COLOR, Widgets.BUTTONS_COLOR);
		widgetsProjectionMap.put(Widgets.MESSAGES_BG_COLOR, Widgets.MESSAGES_BG_COLOR);
		widgetsProjectionMap.put(Widgets.MESSAGES_COLOR, Widgets.MESSAGES_COLOR);
		widgetsProjectionMap.put(Widgets.TIME24HR, Widgets.TIME24HR);
		widgetsProjectionMap.put(Widgets.FRIEND_COLOR, Widgets.FRIEND_COLOR);
		widgetsProjectionMap.put(Widgets.CREATED_COLOR, Widgets.CREATED_COLOR);
		widgetsProjectionMap.put(Widgets.SCROLLABLE, Widgets.SCROLLABLE);
		widgetsProjectionMap.put(Widgets.BUTTONS_TEXTSIZE, Widgets.BUTTONS_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.MESSAGES_TEXTSIZE, Widgets.MESSAGES_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.FRIEND_TEXTSIZE, Widgets.FRIEND_TEXTSIZE);
		widgetsProjectionMap.put(Widgets.CREATED_TEXTSIZE, Widgets.CREATED_TEXTSIZE);

		sUriMatcher.addURI(AUTHORITY, TABLE_STATUSES, STATUSES);

		statusesProjectionMap = new HashMap<String, String>();
		statusesProjectionMap.put(Statuses._ID, Statuses._ID);
		statusesProjectionMap.put(Statuses.CREATED, Statuses.CREATED);
		statusesProjectionMap.put(Statuses.LINK, Statuses.LINK);
		statusesProjectionMap.put(Statuses.FRIEND, Statuses.FRIEND);
		statusesProjectionMap.put(Statuses.PROFILE, Statuses.PROFILE);
		statusesProjectionMap.put(Statuses.MESSAGE, Statuses.MESSAGE);
		statusesProjectionMap.put(Statuses.SERVICE, Statuses.SERVICE);
		statusesProjectionMap.put(Statuses.WIDGET, Statuses.WIDGET);
		statusesProjectionMap.put(Statuses.CREATEDTEXT, Statuses.CREATEDTEXT);
	}

	public enum SonetProviderColumns {
		_id, created, link, friend, profile, message, service, createdtext, widget
	}

	@Override
	public boolean onCreate() {
		mDatabaseHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			return Accounts.CONTENT_TYPE;
		case WIDGETS:
			return Widgets.CONTENT_TYPE;
		case STATUSES:
			return Statuses.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String whereClause, String[] whereArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			count = db.delete(TABLE_ACCOUNTS, whereClause, whereArgs);
			break;
		case WIDGETS:
			count = db.delete(TABLE_WIDGETS, whereClause, whereArgs);
			break;
		case STATUSES:
			count = db.delete(TABLE_STATUSES, whereClause, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
		long rowId;
		Uri returnUri;
		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			rowId = db.insert(TABLE_ACCOUNTS, Accounts._ID, values);
			returnUri = ContentUris.withAppendedId(Accounts.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			break;
		case WIDGETS:
			rowId = db.insert(TABLE_WIDGETS, Widgets._ID, values);
			returnUri = ContentUris.withAppendedId(Widgets.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			break;
		case STATUSES:
			rowId = db.insert(TABLE_STATUSES, Statuses._ID, values);
			returnUri = ContentUris.withAppendedId(Statuses.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(returnUri, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		return returnUri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			qb.setTables(TABLE_ACCOUNTS);
			qb.setProjectionMap(accountsProjectionMap);
			break;
		case WIDGETS:
			qb.setTables(TABLE_WIDGETS);
			qb.setProjectionMap(widgetsProjectionMap);
			break;
		case STATUSES:
			qb.setTables(TABLE_STATUSES);
			qb.setProjectionMap(statusesProjectionMap);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();

		int count;
		switch (sUriMatcher.match(uri)) {
		case ACCOUNTS:
			count = db.update(TABLE_ACCOUNTS, values, selection, selectionArgs);
			break;
		case WIDGETS:
			count = db.update(TABLE_WIDGETS, values, selection, selectionArgs);
			break;
		case STATUSES:
			count = db.update(TABLE_STATUSES, values, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("create table if not exists " + TABLE_ACCOUNTS
					+ " (" + Accounts._ID + " integer primary key autoincrement, "
					+ Accounts.USERNAME + " text, "
					+ Accounts.TOKEN + " text, "
					+ Accounts.SECRET + " text, "
					+ Accounts.SERVICE + " integer, "
					+ Accounts.EXPIRY + " integer, "
					+ Accounts.TIMEZONE + " integer, "
					+ Accounts.WIDGET + " integer);");
			db.execSQL("create table if not exists " + TABLE_WIDGETS
					+ " (" + Widgets._ID + " integer primary key autoincrement, "
					+ Widgets.WIDGET + " integer, "
					+ Widgets.INTERVAL + " integer, "
					+ Widgets.HASBUTTONS + " integer, "
					+ Widgets.BUTTONS_BG_COLOR + " integer, "
					+ Widgets.BUTTONS_COLOR + " integer, "
					+ Widgets.FRIEND_COLOR + " integer, "
					+ Widgets.CREATED_COLOR + " integer, "
					+ Widgets.MESSAGES_BG_COLOR + " integer, "
					+ Widgets.MESSAGES_COLOR + " integer, "
					+ Widgets.TIME24HR + " integer, "
					+ Widgets.SCROLLABLE + " integer, "
					+ Widgets.BUTTONS_TEXTSIZE + " integer, "
					+ Widgets.MESSAGES_TEXTSIZE + " integer, "
					+ Widgets.FRIEND_TEXTSIZE + " integer, "
					+ Widgets.CREATED_TEXTSIZE + " integer);");
			db.execSQL("create table if not exists " + TABLE_STATUSES
					+ " (" + Statuses._ID + " integer primary key autoincrement, "
					+ Statuses.CREATED + " integer, "
					+ Statuses.LINK + " text, "
					+ Statuses.FRIEND + " text, "
					+ Statuses.PROFILE + " blob, "
					+ Statuses.MESSAGE + " text, "
					+ Statuses.SERVICE + " integer, "
					+ Statuses.CREATEDTEXT + " text, "
					+ Statuses.WIDGET + " integer);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				// add column for expiry
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text not null, "
						+ Accounts.TOKEN + " text not null, "
						+ Accounts.SECRET + " text not null, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 3) {
				// remove not null constraints as facebook uses oauth2 and doesn't require a secret, add timezone
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ Accounts.TIMEZONE + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
			}
			if (oldVersion < 4) {
				// add column for widget
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("create temp table " + TABLE_ACCOUNTS + "_bkp as select * from " + TABLE_ACCOUNTS + ";");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + ";");
				db.execSQL("create table if not exists " + TABLE_ACCOUNTS
						+ " (" + Accounts._ID + " integer primary key autoincrement, "
						+ Accounts.USERNAME + " text, "
						+ Accounts.TOKEN + " text, "
						+ Accounts.SECRET + " text, "
						+ Accounts.SERVICE + " integer, "
						+ Accounts.EXPIRY + " integer, "
						+ Accounts.TIMEZONE + " integer, "
						+ Accounts.WIDGET + " integer);");
				db.execSQL("insert into " + TABLE_ACCOUNTS + " select " + Accounts._ID + "," + Accounts.USERNAME + "," + Accounts.TOKEN + "," + Accounts.SECRET + "," + Accounts.SERVICE + "," + Accounts.EXPIRY + "," + Accounts.TIMEZONE + ",\"\" from " + TABLE_ACCOUNTS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_ACCOUNTS + "_bkp;");
				// move preferences to db
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer);");
			}
			if (oldVersion < 5) {
				// cache for statuses
				db.execSQL("create table if not exists " + TABLE_STATUSES
						+ " (" + Statuses._ID + " integer primary key autoincrement, "
						+ Statuses.CREATED + " integer, "
						+ Statuses.LINK + " text, "
						+ Statuses.FRIEND + " text, "
						+ Statuses.PROFILE + " blob, "
						+ Statuses.MESSAGE + " text, "
						+ Statuses.SERVICE + " integer, "
						+ Statuses.CREATEDTEXT + " text, "
						+ Statuses.WIDGET + " integer);");
				// column for scrollable
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ",0 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
			if (oldVersion < 6) {
				// add columns for textsize
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("create temp table " + TABLE_WIDGETS + "_bkp as select * from " + TABLE_WIDGETS + ";");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + ";");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer);");
				db.execSQL("create table if not exists " + TABLE_WIDGETS
						+ " (" + Widgets._ID + " integer primary key autoincrement, "
						+ Widgets.WIDGET + " integer, "
						+ Widgets.INTERVAL + " integer, "
						+ Widgets.HASBUTTONS + " integer, "
						+ Widgets.BUTTONS_BG_COLOR + " integer, "
						+ Widgets.BUTTONS_COLOR + " integer, "
						+ Widgets.FRIEND_COLOR + " integer, "
						+ Widgets.CREATED_COLOR + " integer, "
						+ Widgets.MESSAGES_BG_COLOR + " integer, "
						+ Widgets.MESSAGES_COLOR + " integer, "
						+ Widgets.TIME24HR + " integer, "
						+ Widgets.SCROLLABLE + " integer, "
						+ Widgets.BUTTONS_TEXTSIZE + " integer, "
						+ Widgets.MESSAGES_TEXTSIZE + " integer, "
						+ Widgets.FRIEND_TEXTSIZE + " integer, "
						+ Widgets.CREATED_TEXTSIZE + " integer);");
				db.execSQL("insert into " + TABLE_WIDGETS
						+ " select "
						+ Widgets._ID + ","
						+ Widgets.WIDGET + ","
						+ Widgets.INTERVAL + ","
						+ Widgets.HASBUTTONS + ","
						+ Widgets.BUTTONS_BG_COLOR + ","
						+ Widgets.BUTTONS_COLOR + ","
						+ Widgets.FRIEND_COLOR + ","
						+ Widgets.CREATED_COLOR + ","
						+ Widgets.MESSAGES_BG_COLOR + ","
						+ Widgets.MESSAGES_COLOR + ","
						+ Widgets.TIME24HR + ","
						+ Widgets.SCROLLABLE + ",14,14,14,14 from " + TABLE_WIDGETS + "_bkp;");
				db.execSQL("drop table if exists " + TABLE_WIDGETS + "_bkp;");
			}
		}

	}	

}
