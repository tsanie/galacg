package org.tsanie.galacg.utils;

import java.lang.ref.SoftReference;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;

public class BitmapCache {
	private static final int MAX_CAPACITY = 6;

	private LinkedHashMap<Long, Bitmap> firstCache;
	private ConcurrentHashMap<Long, SoftReference<Bitmap>> secondCache;

	public BitmapCache() {
		firstCache = new LinkedHashMap<Long, Bitmap>(MAX_CAPACITY / 2, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(java.util.Map.Entry<Long, Bitmap> eldest) {
				if (size() > MAX_CAPACITY) {
					secondCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
					// Log.w("removeEldestEntry",
					// String.valueOf(secondCache.size()));
					return true;
				}
				return false;
			}
		};
		secondCache = new ConcurrentHashMap<Long, SoftReference<Bitmap>>();
	}

	public void put(Long key, Bitmap bmp) {
		synchronized (firstCache) {
			firstCache.put(key, bmp);
		}
	}

	public void clear() {
		firstCache.clear();
		secondCache.clear();
	}

	public void clearSecond() {
		secondCache.clear();
	}

	public Bitmap getBitmapFromCache(Long url) {
		Bitmap bmp = fromFirstCache(url);
		if (bmp != null) {
			return bmp;
		}

		bmp = fromSecondCache(url);
		return bmp;
	}

	private Bitmap fromFirstCache(Long url) {
		Bitmap bmp = null;
		synchronized (firstCache) {
			bmp = firstCache.get(url);
			if (bmp != null) {
				// 最近访问的放到最前排
				firstCache.remove(url);
				firstCache.put(url, bmp);
			}
		}
		return bmp;
	}

	private Bitmap fromSecondCache(Long url) {
		Bitmap bmp = null;
		SoftReference<Bitmap> ref = secondCache.get(url);
		if (ref != null) {
			bmp = ref.get();
			if (bmp == null) {
				// 已经被gc回收
				secondCache.remove(url);
			}
		}
		return bmp;
	}
}
