package org.tsanie.galacg.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPInputStream;

import org.tsanie.galacg.utils.HttpTasks.IHandleLine;

import android.os.Build;

public class HttpHelper {
	private static final int BUFFER = 1024;

	private String url;
	private HashMap<String, String> headers;
	private CookieManager cookie;

	public HttpHelper() {
		headers = new HashMap<String, String>();
		cookie = new CookieManager();
		addHeader("Accept-Encoding", "gzip,deflate").addHeader(
				"Accept-Language",
				"zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4,zh-CN;q=0.2");
	}

	public CookieManager getCookie() {
		return cookie;
	}

	public HttpHelper setCookie(String cookie) {
		this.cookie = new CookieManager(cookie);
		this.headers.put("Cookie", cookie);
		return this;
	}

	public HttpHelper setUrl(String url) {
		this.url = url;
		return this;
	}

	public HttpHelper addHeader(String name, String value) {
		headers.put(name, value);
		return this;
	}

	public HttpHelper addMobile() {
		String userAgent = "Mozilla/5.0 (Linux; Android "
				+ Build.VERSION.RELEASE
				+ "; "
				+ Build.DEVICE
				+ " Build/"
				+ Build.ID
				+ ") AppleWebKit/537.36 (KHTML, like Gecko) Wallpaper/1.1 Mobile Safari/537.36";
		return addHeader("User-Agent", userAgent);
	}

	public byte[] getBytes(OnReading onReading) {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(this.url);
			conn = (HttpURLConnection) url.openConnection();
			// conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(6000);
			for (String kv : headers.keySet()) {
				conn.addRequestProperty(kv, headers.get(kv));
			}

			InputStream reader = conn.getInputStream();
			// this.cookie = new
			// CookieManager(conn.getHeaderFields().get("Set-Cookie"));
			return readFromResponse(reader, conn.getContentEncoding(),
					conn.getContentLength(), onReading);
		} catch (Exception e) {
			return getExceptions(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public String getLines(IHandleLine handler) {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(this.url);
			conn = (HttpURLConnection) url.openConnection();
			// conn.setInstanceFollowRedirects(false);
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(6000);
			for (String kv : headers.keySet()) {
				conn.addRequestProperty(kv, headers.get(kv));
			}

			InputStream ins = conn.getInputStream();
			String encoding = conn.getHeaderField("Content-Encoding");
			if (encoding != null) {
				switch (encoding) {
				case "gzip":
					ins = new GZIPInputStream(ins);
					break;
				case "deflate":
					ins = new DeflaterInputStream(ins);
					break;
				}
			}
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					ins));
			String line;
			StringBuilder result = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				result.append(line);
				result.append(System.getProperty("line.separator"));
				if (handler != null) {
					if (!handler.readLine(line)) {
						break;
					}
				}
			}
			return result.toString();
		} catch (Exception e) {
			return getExceptionString(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	public byte[] postBytes(String data) {
		HttpURLConnection conn = null;
		try {
			URL url = new URL(this.url);
			conn = (HttpURLConnection) url.openConnection();
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(6000);
			conn.setReadTimeout(6000);
			conn.setDoOutput(true);
			for (String kv : headers.keySet()) {
				conn.addRequestProperty(kv, headers.get(kv));
			}

			byte[] d = data.getBytes();
			OutputStream os = conn.getOutputStream();
			os.write(d, 0, d.length);
			os.flush();
			os.close();

			InputStream reader = conn.getInputStream();
			this.cookie.addCookies(conn.getHeaderFields().get("Set-Cookie"));
			return readFromResponse(reader,
					conn.getHeaderField("Content-Encoding"), 0, null);
		} catch (Exception e) {
			return getExceptions(e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private byte[] readFromResponse(InputStream reader, String encoding,
			int total, OnReading onReading) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		if (encoding != null) {
			switch (encoding) {
			case "gzip":
				reader = new GZIPInputStream(reader);
				break;
			case "deflate":
				reader = new DeflaterInputStream(reader);
				break;
			}
		}
		byte[] buffer = new byte[BUFFER];
		int count;
		while ((count = reader.read(buffer, 0, BUFFER)) > 0) {
			bos.write(buffer, 0, count);
			if (onReading != null) {
				onReading.reading(bos.size(), total);
			}
		}
		return bos.toByteArray();
	}

	private static byte[] getExceptions(Throwable e) {
		String result = getExceptionString(e);
		return result.getBytes(Charset.defaultCharset());
	}

	private static String getExceptionString(Throwable e) {
		String result = "{\"code\":-1,\"type\":\"" + e.getClass()
				+ "\",\"msg\":\"" + e.getMessage() + "\"";
		Throwable cause = e.getCause();
		if (cause != null) {
			result += ",\"cause\":" + getExceptionString(cause);
		}
		result += "}";
		return result;
	}
}
