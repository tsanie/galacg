package org.tsanie.galacg.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.util.Log;

public class HttpTasks {

	private static final String HOME_CACHE = "home_html";
	private static final String HOME_URL = "http://www.galacg.me/";

	private String user;

	public interface IHandleLine {
		Boolean readLine(String line);
	}

	private String cookie;

	public HttpTasks setCookie(String cookie) {
		this.cookie = cookie;
		return this;
	}

	public String getHomePage(boolean refresh, int page, Context context) {
		if (!refresh && context.getFileStreamPath(HOME_CACHE + page).exists()) {
			// ”–ª∫¥Ê
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(context.openFileInput(HOME_CACHE
								+ page)));
				String line;
				StringBuilder sb = new StringBuilder();
				while ((line = reader.readLine()) != null) {
					sb.append(line);
					sb.append(System.getProperty("line.separator"));
				}
				return sb.toString();
			} catch (Exception e) {
				Log.e("HttpTasks.getHomePage", e.getMessage(), e);
			}
		}

		// œ¬‘ÿ
		String http = new HttpHelper().setUrl(HOME_URL + "page/" + page)
				.addMobile().setCookie(cookie).getLines(null);

		// ª∫¥Êhttp source
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					context.openFileOutput(HOME_CACHE + page,
							Context.MODE_PRIVATE)));
			writer.write(http);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			Log.e("HttpTasks.getUsername", e.getMessage(), e);
		}
		return http;
	}

	public String getUsername(Context context) {
		final Pattern p = Pattern
				.compile("<a class=\"ab-item\"[^>]*>([^<]*)<img");
		if (context.getFileStreamPath(HOME_CACHE).exists()) {
			// ”–ª∫¥Ê
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(context.openFileInput(HOME_CACHE)));
				String line;
				while ((line = reader.readLine()) != null) {
					if (checkUsername(line, p)) {
						break;
					}
				}
			} catch (Exception e) {
				Log.e("HttpTasks.getUsername", e.getMessage(), e);
			}
		} else {
			// œ¬‘ÿ
			String http = new HttpHelper().setUrl(HOME_URL).addMobile()
					.setCookie(cookie).getLines(new IHandleLine() {
						@Override
						public Boolean readLine(String line) {
							if (user == null) {
								checkUsername(line, p);
							}
							return true;
						}
					});

			// ª∫¥Êhttp source
			try {
				BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(context.openFileOutput(
								HOME_CACHE, Context.MODE_PRIVATE)));
				writer.write(http);
				writer.flush();
				writer.close();
			} catch (Exception e) {
				Log.e("HttpTasks.getUsername", e.getMessage(), e);
			}
		}

		return user;
	}

	private Boolean checkUsername(String line, Pattern p) {
		int index = line.indexOf("<li id=\"wp-admin-bar-my-account\"");
		if (index >= 0) {
			Matcher m = p.matcher(line);
			if (m.find()) {
				user = m.group(1);
				return true;
			}
		}
		return false;
	}
}
