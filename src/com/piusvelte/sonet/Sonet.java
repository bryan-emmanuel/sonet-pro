package com.piusvelte.sonet;

public class Sonet {
	public static final int TWITTER = 0;
	public static final int FACEBOOK = 1;
	public static final int MYSPACE = 2;
	public static final String TWITTER_URL_REQUEST = "http://api.twitter.com/oauth/request_token";
	public static final String TWITTER_URL_ACCESS = "http://api.twitter.com/oauth/access_token";
	public static final String TWITTER_URL_AUTHORIZE = "http://api.twitter.com/oauth/authorize";
    public static final String[] FACEBOOK_PERMISSIONS = new String[] {"offline_access"};
    public static final String ACTION_REFRESH = "com.piusvelte.sonet.Sonet.REFRESH";
    public static final String DONATE = "http://www.piusvelte.com?p=donate-sonet";
    public static final String ACTION_BUILD_SCROLL = "com.piusvelte.sonet.Sonet.BUILD_SCROLL";
    public static final String ACTION_DELETE = "com.piusvelte.sonet.Sonet.DELETE";
    public static final String EXTRA_ACCOUNTID = "com.piusvelte.sonet.Sonet.ACCOUNTID";
}
