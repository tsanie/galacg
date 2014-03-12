package org.tsanie.galacg.utils;

import java.util.HashMap;
import java.util.List;

public class CookieManager {
	private HashMap<String, String> cookies;

	public CookieManager() {
		cookies = new HashMap<>();
	}

	public CookieManager(String header) {
		this();
		if (header != null) {
			String[] headers = header.split(";");
			for (String cookie : headers) {
				int index = cookie.indexOf(';');
				if (index > 0) {
					addCookie(cookie.substring(0, index));
				} else {
					addCookie(cookie);
				}
			}
		}
	}

	public CookieManager addCookies(List<String> headers) {
		for (String cookie : headers) {
			int index = cookie.indexOf(';');
			if (index > 0) {
				addCookie(cookie.substring(0, index));
			} else {
				addCookie(cookie);
			}
		}
		return this;
	}

	public Boolean containsKey(String key) {
		return cookies.containsKey(key);
	}

	private void addCookie(String cookie) {
		String[] kv = cookie.split("=");
		if (kv.length == 2) {
			cookies.put(kv[0], kv[1]);
		}
	}

	@Override
	public String toString() {
		String result = "";
		for (String key : cookies.keySet()) {
			result += key + "=" + cookies.get(key) + "; ";
		}
		return result;
	}
}
