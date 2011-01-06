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

interface ISonetService {
	void setCallback(in IBinder mSonetUIBinder);
	void setIntSetting(int appWidgetId, String column, int value);
	void getSettings(int appWidgetId);
	void deleteAccount(int account);
	void addAccount(String username, String token, String secret, int expiry, int service, int timezone, int appWidgetId);
	void addAccountGetTimezone(String username, String token, String secret, int expiry, int service, int timezone, int appWidgetId);
	void getAuth(int account);
	void addTimezone(int account, int timezone);
	void listAccounts();
	void getWidgetSettings(int appWidgetId);
	void widgetOnClick(int appWidgetId, int statusId);
}