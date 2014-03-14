package org.tsanie.galacg.fragments;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tsanie.galacg.ArchiveDetailActivity;
import org.tsanie.galacg.MainActivity;
import org.tsanie.galacg.PlaceholderFragment;
import org.tsanie.galacg.R;
import org.tsanie.galacg.ui.ArchiveItem;
import org.tsanie.galacg.ui.ArchiveListAdapter;
import org.tsanie.galacg.ui.ArchiveListView;
import org.tsanie.galacg.ui.OnListViewRefresh;
import org.tsanie.galacg.ui.OnTaskHandle;
import org.tsanie.galacg.utils.HttpTasks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;

public class MainFragment extends PlaceholderFragment {

	private ArrayList<ArchiveItem> list;
	private ArchiveListAdapter adapter;
	private ArchiveListView listView;
	private ProgressBar progressBarLoading;

	private int currentPage;

	@Override
	public void onLowMemory() {
		if (adapter != null) {
			adapter.clearSecond();
			Log.w("MainFragment.onLowMemory", "clear second caches.");
		}
		super.onLowMemory();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		listView = (ArchiveListView) rootView.findViewById(R.id.archiveListView);
		progressBarLoading = (ProgressBar) rootView.findViewById(R.id.progressBarMainLoading);

		adapter = new ArchiveListAdapter(listView);

		listView.setDividerHeight(0);
		listView.setAdapter(adapter);
		listView.setOnScrollListener(adapter);

		listView.setOnListViewRefresh(new OnListViewRefresh() {
			@Override
			public void refreshing() {
				currentPage = 1;
				list = doGetArchives(true, currentPage);
			}

			@Override
			public void refreshed() {
				adapter.init(list);
			}
		});
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(getActivity(), ArchiveDetailActivity.class);
				Bundle bundle = new Bundle();
				bundle.putSerializable("item", (ArchiveItem) adapter.getItem(position));
				intent.putExtras(bundle);
				startActivity(intent);
			}
		});

		adapter.setOnLoadMore(new OnTaskHandle() {
			@Override
			public boolean run() {
				currentPage++;
				list = doGetArchives(true, currentPage);
				if (list.size() == 0) {
					currentPage--;
					return false;
				}
				return true;
			}

			@Override
			public void post() {
				adapter.addAll(list);
			}
		});

		return rootView;
	}

	@Override
	public void onDrawerClosed() {
		currentPage = 1;
		new RefreshArchivesTask().execute();
	}

	private ArrayList<ArchiveItem> doGetArchives(boolean refresh, int page) {
		ArrayList<ArchiveItem> result = new ArrayList<ArchiveItem>();
		try {
			String html = new HttpTasks().setCookie(MainActivity.getCookie()).getHomePage(refresh, page, getActivity());
			Pattern p = Pattern.compile("<div class=\"article well clearfix\">[\\s\\S]*?"
					// 是否有书签
					+ "(<i class=\"fa fa-bookmark article-stick visible-md visible-lg\"></i>[\\s\\S]*?)?"
					// 发布日期
					+ "<span class=\"month\">([^<]+?)</span>[\\s\\S]*?<span class=\"day\">([^<]+?)</span>[\\s\\S]*?"
					+ "<section class=\"visible-md visible-lg\">[^<]*?<div class=\"title-article\">[^<]*?"
					// Id, 标题
					+ "<h1><a href=\"http://www.galacg.me/archives/([0-9]+?)\">([^<]+?)</a></h1>[\\s\\S]*?"
					// 作者
					+ "<a [^>]*?rel=\"author\"[^>]*?>([^<]+?)</a>[\\s\\S]*?"
					// 点击数
					+ "<i class=\"fa fa-eye\"></i>([^<]+?)</span>[\\s\\S]*?<div class=\"alert alert-zan\">[^<]*?"
					// 图片预览(如果有的话)
					+ "<p[^>]*?>[\\s\\S]*?(<img[^>]*? src=\"([^\"]+?)\"[\\s\\S]*?)?</div>", Pattern.MULTILINE);
			Matcher m = p.matcher(html);
			while (m.find()) {
				// for (int i = 1; i < m.groupCount() + 1; i++) {
				// if (m.group(i) == null) {
				// Log.w("group " + i, "<null>");
				// } else {
				// Log.w("group " + i, m.group(i));
				// }
				// }
				ArchiveItem item = new ArchiveItem();
				item.setBookmark(m.group(1) != null);
				item.setMonth(m.group(2));
				item.setDay(m.group(3));
				item.setId(Long.parseLong(m.group(4)));
				item.setTitle(Html.fromHtml(m.group(5).trim()).toString());
				item.setAuthor(Html.fromHtml(m.group(6).trim()).toString());
				item.setClicks(m.group(7).trim());
				item.setPreview(m.group(9));
				result.add(item);
			}

		} catch (Exception e) {
			Log.e("RefreshArchivesTask.doInBackground", e.getMessage(), e);
		}
		return result;
	}

	class RefreshArchivesTask extends AsyncTask<Void, Void, ArrayList<ArchiveItem>> {

		@Override
		protected ArrayList<ArchiveItem> doInBackground(Void... params) {
			return doGetArchives(false, 1);
		}

		@Override
		protected void onPostExecute(ArrayList<ArchiveItem> result) {
			progressBarLoading.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					progressBarLoading.setVisibility(View.GONE);
				}
			});
			adapter.init(result);
		}

	}
}
