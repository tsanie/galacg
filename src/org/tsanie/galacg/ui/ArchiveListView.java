package org.tsanie.galacg.ui;

import org.tsanie.galacg.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ArchiveListView extends ListView {

	private static final int STATE_DONE = 0;
	private static final int STATE_REFRESH_ABLE = 1;
	private static final int STATE_START_REFRESH = 2;
	private static final int STATE_TO_REFRESH = 3;
	private static final int STATE_REFRESHING = 4;

	private View refresher;
	private int refresherHeight = -1;
	private ArchiveListAdapter adapter;
	private OnListViewRefresh onRefresh;
	private OnListViewPreload onPreload;

	private float touchY;
	private int state;
	private int lastState;

	public ArchiveListView(Context context) {
		this(context, null);
	}

	public ArchiveListView(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.listViewStyle);
	}

	public ArchiveListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setOnListViewRefresh(OnListViewRefresh onRefresh) {
		this.onRefresh = onRefresh;
	}

	public void setOnListViewPreload(OnListViewPreload onPreload) {
		this.onPreload = onPreload;
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		if (refresherHeight < 0 && refresher != null) {
			measureChild(refresher, widthMeasureSpec, heightMeasureSpec);
			refresherHeight = refresher.getMeasuredHeight();
			refresher.setPadding(0, -refresherHeight, 0, 0);
		}
	}

	@Override
	public void setAdapter(ListAdapter adapter) {
		this.adapter = (ArchiveListAdapter) adapter;
		if (getHeaderViewsCount() == 0) {
			// header
			refresher = LayoutInflater.from(getContext()).inflate(
					R.layout.listview_header, this, false);
			this.addHeaderView(refresher);
		}
		super.setAdapter(adapter);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (adapter != null) {
			float y = ev.getY();
			int offset;
			switch (ev.getAction()) {
			case MotionEvent.ACTION_DOWN:
				touchY = y;
				if (state != STATE_REFRESHING
						&& adapter.getFirstItemIndex() == 0) {
					state = STATE_REFRESH_ABLE;
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (state == STATE_REFRESHING || state == STATE_DONE) {
					break;
				}
				offset = (int) ((y - touchY) / 3);
				if (offset > 0) {
					if (offset > refresherHeight) {
						setMessage(STATE_TO_REFRESH);
						state = STATE_TO_REFRESH;
					} else {
						setMessage(STATE_START_REFRESH);
						state = STATE_START_REFRESH;
					}
					refresher.setPadding(0, -refresherHeight + offset, 0, 0);
					ev.setAction(MotionEvent.ACTION_CANCEL);
				}
				break;

			case MotionEvent.ACTION_UP:
				// preload
				if (onPreload != null) {
					onPreload.onPreload(adapter.getFirstItemIndex(),
							adapter.getVisibleItemCount());
				}

				// refresh
				if (state == STATE_REFRESH_ABLE || state == STATE_DONE) {
					state = STATE_DONE;
					break;
				}
				offset = (int) ((y - touchY) / 3);
				if (state == STATE_START_REFRESH) {
					animateToY(-offset, -refresherHeight, STATE_DONE);
					break;
				}
				if (state == STATE_TO_REFRESH) {
					setMessage(STATE_REFRESHING);
					animateToY(refresherHeight - offset, 0, STATE_REFRESHING);

					// Ë¢ÐÂ
					if (onRefresh == null) {
						refreshOver();
					} else {
						new RefreshTask().execute();
					}
				}
				break;
			}
		}

		return super.onTouchEvent(ev);
	}

	private void refreshOver() {
		adapter.clearLoadingBitmap();
		animateToY(-refresherHeight, -refresherHeight, STATE_DONE);
		setSelection(0);
		// state = STATE_DONE;
		if (onRefresh != null) {
			onRefresh.refreshed();
		}
	}

	private void setMessage(int state) {
		if (state == lastState) {
			return;
		}
		lastState = state;
		int shortAnimateTime = getResources().getInteger(
				android.R.integer.config_shortAnimTime);
		View imv = refresher.findViewById(R.id.imageHeaderRefresher);
		TextView textView = (TextView) refresher
				.findViewById(R.id.textViewHeaderRefresher);

		if (state == STATE_REFRESHING) {
			imv.setVisibility(GONE);
			refresher.findViewById(R.id.progressHeaderRefresher).setVisibility(
					VISIBLE);
			textView.setText(getResources().getString(
					R.string.header_refreshing));
		} else {
			imv.setVisibility(VISIBLE);
			refresher.findViewById(R.id.progressHeaderRefresher).setVisibility(
					GONE);
			if (state == STATE_START_REFRESH) {
				imv.animate().setDuration(shortAnimateTime).rotation(0);
				textView.setText(getResources().getString(
						R.string.header_torefresh));
			} else if (state == STATE_TO_REFRESH) {
				imv.animate().setDuration(shortAnimateTime).rotation(180);
				textView.setText(getResources().getString(
						R.string.header_willrefresh));
			}
		}
	}

	private void animateToY(int offset, final int y, final int state) {
		int shortAnimateTime = getResources().getInteger(
				android.R.integer.config_shortAnimTime);
		this.animate().setDuration(shortAnimateTime).y(offset)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						setY(0);
						refresher.setPadding(0, y, 0, 0);
						ArchiveListView.this.state = state;
					}
				});
	}

	class RefreshTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			onRefresh.refreshing();
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			refreshOver();
		}
	}
}
