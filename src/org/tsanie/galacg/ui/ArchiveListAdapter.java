package org.tsanie.galacg.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;

import org.tsanie.galacg.R;
import org.tsanie.galacg.utils.BitmapCache;
import org.tsanie.galacg.utils.HttpHelper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ArchiveListAdapter extends BaseAdapter implements OnScrollListener {

	// private static final int CACHE_WIDTH = 258;
	// private static final int CACHE_HEIGHT = 160;

	private int firstItemIndex;
	private int visibleItemCount;
	private int scrollState;
	private LayoutInflater inflater;
	private ArchiveListView listView;

	private ArrayList<ArchiveItem> list;
	private BitmapCache bitmapCache;
	private OnTaskHandle onLoadMore;
	private boolean isLoadingMore;

	public ArchiveListAdapter(ArchiveListView listView) {
		inflater = LayoutInflater.from(listView.getContext());
		this.listView = listView;
		list = new ArrayList<ArchiveItem>();
		bitmapCache = new BitmapCache();
	}

	public void setOnLoadMore(OnTaskHandle runner) {
		this.onLoadMore = runner;
	}

	public void clear() {
		list.clear();
		bitmapCache.clear();
		this.notifyDataSetChanged();
	}

	public void clearSecond() {
		bitmapCache.clearSecond();
	}

	public void add(ArchiveItem item) {
		list.add(item);
		this.notifyDataSetChanged();
	}

	public void addAll(Collection<? extends ArchiveItem> collection) {
		list.addAll(collection);
		this.notifyDataSetChanged();
	}

	public void init(Collection<? extends ArchiveItem> collection) {
		list = new ArrayList<ArchiveItem>(collection);
		this.notifyDataSetChanged();
	}

	public synchronized void clearLoadingBitmap() {
		for (ArchiveItem item : list) {
			item.setLoading(false);
		}
	}

	public int getFirstItemIndex() {
		return firstItemIndex;
	}

	public int getVisibleItemCount() {
		return visibleItemCount;
	}

	public int getScrollState() {
		return scrollState;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return list.get(position).getId();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Holder holder;
		if (convertView == null) {
			convertView = inflater.inflate(R.layout.item_archive, null);
			holder = new Holder();
			holder.textTitle = (TextView) convertView
					.findViewById(R.id.textItemTitle);
			holder.textDetail = (TextView) convertView
					.findViewById(R.id.textItemDetail);
			holder.textAuthor = (TextView) convertView
					.findViewById(R.id.textItemAuthor);
			holder.imagePreview = (ImageView) convertView
					.findViewById(R.id.imageItemPreview);
			holder.progress = (ProgressBar) convertView
					.findViewById(R.id.progressItemPreview);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		final ArchiveItem item = list.get(position);
		holder.textTitle.setText(item.getTitle());
		holder.textDetail.setText(item.getClicks());
		holder.textAuthor.setText(item.getAuthor());

		holder.imagePreview.setVisibility(View.GONE);
		holder.progress.setVisibility(View.GONE);

		final String fUrl = item.getPreview();
		final long fId = item.getId();

		if (fUrl != null) {
			holder.progress.setVisibility(View.VISIBLE);
			holder.imagePreview.setImageBitmap(null);
			holder.imagePreview.setVisibility(View.VISIBLE);

			// 判断是否正在载入
			if (!item.isLoading()) {

				// 试图读取缓存
				Bitmap bitmap = bitmapCache.getBitmapFromCache(fId);
				if (bitmap != null) {
					holder.progress.setVisibility(View.GONE);
					holder.imagePreview.setImageBitmap(bitmap);

				} else if (this.scrollState != OnScrollListener.SCROLL_STATE_FLING) {
					// 不在滑动动画时获取预览图
					// 开始获取预览图
					final View fView = convertView;
					new Thread(new Runnable() {
						@Override
						public void run() {
							loadPreview(item, fView);
						}
					}).start();
				}
			}
		}

		return convertView;
	}

	private void loadPreview(final ArchiveItem fItem, View fView) {

		if (fItem.isLoading()) {
			Log.e("loadPreview", "isLoading, (fId = " + fItem.getId() + ")");
			return;
		}

		fItem.setLoading(true);
		// Log.w("loadPreview(fId = " + fItem.getId() + ")", fItem.getTitle());

		// 先检查缓存是否有
		String md5 = getMd5(fItem.getPreview());
		Bitmap bmp = getCachePreview(md5);

		if (bmp == null) {
			// 下载
			try {
				byte[] data = new HttpHelper().setUrl(fItem.getPreview())
						.getBytes(null);
				// BitmapFactory.Options opts = new BitmapFactory.Options();
				// opts.inJustDecodeBounds = true;
				// BitmapFactory.decodeByteArray(data, 0, data.length, opts);
				//
				// opts.inSampleSize = computeSampleSize(opts, -1,
				// getMaxNumOfPixels(opts, CACHE_WIDTH, CACHE_HEIGHT));
				// opts.inJustDecodeBounds = false;

				bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
				if (bmp != null) {
					// 缓存
					setCachePreview(md5, data);
				}
			} catch (Exception ex) {
				Log.e("ArchiveListAdapter.loadPreview", ex.getMessage(), ex);
			}
		}
		// Log.w("bmp(" + fItem.getPreview() + ")", String.valueOf(bmp));

		if (bmp == null) {
			// 最后刷新之后1000ms后重试
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				Log.e("ArchiveListAdapter.loadPreview", e.getMessage(), e);
			}
		} else {
			bitmapCache.put(fItem.getId(), bmp);
		}

		fItem.setLoading(false);
		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			fView.post(new Runnable() {
				@Override
				public void run() {
					notifyDataSetChanged();
				}
			});
		}
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		// Log.w("onScrollStateChanged", String.valueOf(scrollState));

		if (scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			// String msg = "first: " + this.firstItemIndex + "\nvisible: "
			// + this.visibleItemCount + "\ncount: " + getCount();
			// Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

			// 通知更新
			notifyDataSetChanged();

			if (!isLoadingMore) {
				// 滑动动画停止
				int count = getCount();
				if (count > 0
						&& (this.firstItemIndex + this.visibleItemCount >= count)) {
					if (onLoadMore != null) {
						isLoadingMore = true;
						listView.setFooterVisible(true);
						new LoadMoreTask().execute();
					}
				}
			}
		}
		this.scrollState = scrollState;
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		this.firstItemIndex = firstVisibleItem;
		this.visibleItemCount = visibleItemCount;
	}

	private Bitmap getCachePreview(String md5) {
		File cache = new File(listView.getContext().getCacheDir().getAbsolutePath()
				+ "/previews/" + md5);
		if (cache.exists()) {
			String file = cache.getAbsolutePath();

			Bitmap bitmap = null;
			// BitmapFactory.Options opts = new BitmapFactory.Options();
			// opts.inJustDecodeBounds = true;
			// BitmapFactory.decodeFile(file, opts);
			//
			// opts.inSampleSize = computeSampleSize(opts, -1,
			// getMaxNumOfPixels(opts, CACHE_WIDTH, CACHE_HEIGHT));
			// opts.inJustDecodeBounds = false;

			try {
				bitmap = BitmapFactory.decodeFile(file);
			} catch (Exception ex) {
				Log.e("ArchiveListAdapter.getCachePreview", ex.getMessage(), ex);
			}
			return bitmap;
		}

		return null;
	}

	private void setCachePreview(String md5, byte[] data) {
		File cache = new File(listView.getContext().getCacheDir().getAbsolutePath()
				+ "/previews");
		cache.mkdirs();
		try {
			FileOutputStream fos = new FileOutputStream(cache.getAbsolutePath()
					+ "/" + md5);
			fos.write(data);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			Log.e("MainFragment.setCachePreview", e.getMessage(), e);
		}
	}

	private static String getMd5(String str) {
		MessageDigest md5 = null;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (Exception e) {
			Log.e("MainFragment.md5", e.getMessage(), e);
			return "";
		}

		byte[] data = str.getBytes();
		data = md5.digest(data);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < data.length; i++) {
			int val = data[i] & 0xff;
			if (val < 16) {
				sb.append("0");
			}
			sb.append(Integer.toHexString(val));
		}
		return sb.toString();
	}

	// private static int getMaxNumOfPixels(BitmapFactory.Options opts, int
	// width,
	// int height) {
	// int w = opts.outWidth;
	// int h = opts.outHeight;
	// if (w * height > h * width) {
	// return width * (h * width / w);
	// }
	// return (w * height / h) * height;
	// }
	//
	// private static int computeSampleSize(BitmapFactory.Options options,
	// int minSideLength, int maxNumOfPixels) {
	// int initialSize = computeInitialSampleSize(options, minSideLength,
	// maxNumOfPixels);
	// int roundedSize;
	// if (initialSize <= 8) {
	// roundedSize = 1;
	// while (roundedSize < initialSize) {
	// roundedSize <<= 1;
	// }
	// } else {
	// roundedSize = (initialSize + 7) / 8 * 8;
	// }
	// return roundedSize;
	// }
	//
	// private static int computeInitialSampleSize(BitmapFactory.Options
	// options,
	// int minSideLength, int maxNumOfPixels) {
	// double w = options.outWidth;
	// double h = options.outHeight;
	// int lowerBound = (maxNumOfPixels == -1) ? 1 : (int) Math.ceil(Math
	// .sqrt(w * h / maxNumOfPixels));
	// int upperBound = (minSideLength == -1) ? 128 : (int) Math.min(
	// Math.floor(w / minSideLength), Math.floor(h / minSideLength));
	// if (upperBound < lowerBound) {
	// // return the larger one when there is no overlapping zone.
	// return lowerBound;
	// }
	// if ((maxNumOfPixels == -1) && (minSideLength == -1)) {
	// return 1;
	// } else if (minSideLength == -1) {
	// return lowerBound;
	// } else {
	// return upperBound;
	// }
	// }

	/**
	 * 载入更多任务
	 * 
	 * @author Tsanie
	 */
	class LoadMoreTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			if (onLoadMore != null) {
				return onLoadMore.run();
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result && onLoadMore != null) {
				onLoadMore.post();
			}
			listView.setFooterVisible(false);
			isLoadingMore = false;
		}
	}

	/**
	 * ListViewItem 支持类
	 * 
	 * @author Tsanie
	 */
	class Holder {
		public TextView textTitle;
		public TextView textDetail;
		public TextView textAuthor;
		public ImageView imagePreview;
		public ProgressBar progress;
	}
}
