package org.tsanie.galacg.fragments;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;

import org.tsanie.galacg.MainActivity;
import org.tsanie.galacg.PlaceholderFragment;
import org.tsanie.galacg.R;
import org.tsanie.galacg.utils.HttpHelper;
import org.tsanie.galacg.utils.HttpTasks;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class LoginFragment extends PlaceholderFragment {

	public static final String USER_COOKIE = "cookie";

	private static final String PREF_USER_INFO = "pref_user_info";

	private TextView textResult;
	private ProgressBar loading;
	private String cookie;
	private String username;

	private EditText editUser;
	private EditText editPass;
	private View layout_login;
	private View layout_login_detail;

	private void setCookie(String cookie) {
		this.cookie = cookie;
		MainActivity.setCookie(cookie);
	}

	private String getCookie() {
		return this.cookie;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_login, container, false);

		layout_login_detail = rootView.findViewById(R.id.layoutLoginDetail);
		Button buttonLogout = (Button) rootView.findViewById(R.id.buttonLogout);
		buttonLogout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 登出
				setCookie(null);
				setUserPreference(null);
				layout_login_detail.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						layout_login_detail.setVisibility(View.GONE);
					}
				});
				layout_login.setAlpha(0);
				layout_login.setVisibility(View.VISIBLE);
				layout_login.animate().alpha(1);
			}
		});

		textResult = (TextView) rootView.findViewById(R.id.section_label);
		editUser = (EditText) rootView.findViewById(R.id.editUsername);
		editPass = (EditText) rootView.findViewById(R.id.editPassword);

		// TODO
		editUser.setText("tsanie");

		layout_login = rootView.findViewById(R.id.layoutLogin);
		loading = (ProgressBar) rootView.findViewById(R.id.progressBar);
		Button buttonLogin = (Button) rootView.findViewById(R.id.buttonLogin);
		buttonLogin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// 登入
				InputMethodManager input = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
				View currentFocus = getActivity().getCurrentFocus();
				if (currentFocus != null) {
					input.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
				}

				layout_login.animate().alpha(0);

				loading.setAlpha(0);
				loading.setVisibility(View.VISIBLE);
				loading.animate().alpha(1).setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						String user = editUser.getText().toString();
						String pass = editPass.getText().toString();

						new LoginTask().execute(user, pass);
					}
				});
			}
		});

		layout_login_detail.setVisibility(View.INVISIBLE);
		layout_login.setVisibility(View.INVISIBLE);
		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		new PreferenceTask().execute();
	}

	private void setUserPreference(String cookie) {
		SharedPreferences userInfo = getActivity().getSharedPreferences(PREF_USER_INFO, 0);
		Editor editor = userInfo.edit();
		if (cookie == null) {
			editor.remove(USER_COOKIE);
		} else {
			editor.putString(USER_COOKIE, cookie);
		}
		editor.commit();
	}

	/**
	 * 判断用户设置的任务
	 * 
	 * @author Tsanie
	 */
	class PreferenceTask extends AsyncTask<Void, Void, Boolean> {
		@Override
		protected Boolean doInBackground(Void... params) {
			SharedPreferences userInfo = getActivity().getSharedPreferences(PREF_USER_INFO, 0);
			setCookie(userInfo.getString(USER_COOKIE, null));
			if (getCookie() != null) {
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				layout_login_detail.setVisibility(View.VISIBLE);
				loading.setAlpha(0);
				loading.setVisibility(View.VISIBLE);
				loading.animate().alpha(1);

				new UserTask().execute(getCookie());
			} else {
				layout_login.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * 获取用户名的任务
	 * 
	 * @author Tsanie
	 */
	class UserTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			if (params == null || params.length != 1) {
				return null;
			}
			String cookie = params[0];
			return new HttpTasks().setCookie(cookie).getUsername(getActivity());
		}

		@Override
		protected void onPostExecute(String result) {
			username = result;
			loading.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					loading.setVisibility(View.GONE);
					textResult.setText(username);
				}
			});
		}
	}

	/**
	 * 登录任务
	 * 
	 * @author Tsanie
	 */
	class LoginTask extends AsyncTask<String, Void, Boolean> {
		@Override
		protected Boolean doInBackground(String... params) {
			if (params == null || params.length != 2) {
				return false;
			}
			String user = params[0];
			String password = params[1];
			try {
				user = URLEncoder.encode(user, Charset.defaultCharset().name());
				password = URLEncoder.encode(password, Charset.defaultCharset().name());
			} catch (UnsupportedEncodingException e) {
				Log.e("LoginTask.doInBackground", e.getMessage(), e);
				return false;
			}

			HttpHelper http = new HttpHelper().setUrl("http://www.galacg.me/wp-login.php").addHeader("Cache-Control", "max-age=0")
					.addHeader("Origin", "http://www.galacg.me").addHeader("Referer", "http://www.galacg.me/wp-login.php")
					.addHeader("Cookie", "wordpress_test_cookie=WP+Cookie+check").addMobile();
			http.postBytes("log=" + user + "&pwd=" + password + "&testcookie=1");
			setCookie(http.getCookie().toString());

			if (http.getCookie().containsKey("duoshuo_token")) {
				username = new HttpTasks().setCookie(getCookie()).getUsername(getActivity());
				setUserPreference(getCookie());
				return true;
			}
			return false;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result) {
				layout_login.setVisibility(View.GONE);

				textResult.setText(username);
				layout_login_detail.setAlpha(0);
				layout_login_detail.setVisibility(View.VISIBLE);
				layout_login_detail.animate().alpha(1);

				loading.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						loading.setVisibility(View.GONE);
					}
				});
			} else {
				Toast.makeText(getActivity(), "登录失败", Toast.LENGTH_LONG).show();
				layout_login.animate().alpha(1);

				loading.animate().alpha(0).setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						loading.setVisibility(View.GONE);
					}
				});
			}
		}
	}
}
