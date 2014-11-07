package eu.siacs.conversations.ui;

import android.app.Activity;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.utils.Validator;

public class CreatePinActivity extends EditAccountActivity {

    private TextView mPin;
    private TextView mAssignedPin;
    private TextView mNoInternet;
    private TextView mTryAnother;
    private RelativeLayout mLoadingPanel;
    private LinearLayout mReloadLayout;
    private ImageButton mReloadButton;
    private Button mSaveButton;
    private Button mCancelButton;

    private boolean waitingForJSON = false;
    private JSONObject jsonPin;
    private boolean pinSelected = false;
    private boolean internetAccess = true;


    public static String GET(String url){
        InputStream inputStream = null;
        String result = "";
        try {

            // create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // make GET request to the given URL
            HttpResponse httpResponse = httpclient.execute(new HttpGet(url));

            // receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            // convert inputstream to string
            if(inputStream != null)
                result = convertInputStreamToString(inputStream);
            else
                result = "Did not work!";

        } catch (Exception e) {
            Log.d("InputStream", e.getLocalizedMessage());
        }

        return result;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;

    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private class HttpAsyncTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {

            return GET(urls[0]);
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            Log.d("TXTR API", "HtppAsyncTask: JSON Received");
            try {
                jsonPin = new JSONObject(result);

                //mPin.setText(jsonPin.toString(2)); Show the whole object with this to debug
                if (jsonPin.has("state") && jsonPin.getInt("state") == 1) { //State 1: OK
                    mPin.setText(jsonPin.getString("pincode"));
                    mPin.setTextSize(getResources().getDimension(R.dimen.TextBig));

                    if(pinSelected) {
                        loadPINforLogin(
                                jsonPin.getString("pincode"),
                                jsonPin.getString("password"),
                                jsonPin.getString("host"));
                    }
                }
                else {
                    mPin.setText("Couldn't get a PIN, try again later");
                    mPin.setTextSize(getResources().getDimension(R.dimen.TextMedium));
                }

            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            waitingForJSON = false;
            updateLayout();
        }
    }

    private void loadPINforLogin (String pincode, String password, String host) {
        //Simulate user loaded data on edit texts to keep compatibility
        super.mAccountJid.setText(pincode+"@"+host);
        mPassword.setText(password);
        mSaveButton.performClick(); //Simulate a click on the Save (Next, Connecting...) button
    }

    private void startJSONRequest (String url) {
        // call AsynTask to perform network operation on separate thread
        if (!waitingForJSON && isConnected()){
            Log.d("TXTR API", "startJSONRequest: " + url);
            new HttpAsyncTask().execute(url);
            waitingForJSON = true;
            }
        else {
            Log.d("TXTR API", "startJSONRequest: No internet access");
            waitingForJSON = false;
        }
        this.updateLayout();
    }

    private OnClickListener mReloadButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!waitingForJSON) {
                if (!pinSelected) {
                    startJSONRequest("http://api.droidko.com/?method=pinRequest&output=json");
                }
                else {
                    mSaveButton.performClick();
                }
            }
        }
    };

    public OnClickListener mCancelButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            finish();
        }
    };

	private OnClickListener mSaveButtonClickListener = new OnClickListener() {

		@Override
		public void onClick(View v) {

            //In case we have already request a PIN and the user wants it
            if (jsonPin != null || pinSelected) {
                try {
                    String pincode = URLEncoder.encode(jsonPin.getString("pincode"), "utf-8");
                    String pintoken = URLEncoder.encode(jsonPin.getString("token"), "utf-8");
                    String url =
                            "http://api.droidko.com/?method=pinRegister&output=json"
                            + "&pincode=" + pincode
                            + "&pintoken=" + pintoken;
                    pinSelected = true; //This means that the user is commited with this PIN
                    startJSONRequest(url);

                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

			if (mAccount != null
					&& mAccount.getStatus() == Account.STATUS_DISABLED) {
				mAccount.setOption(Account.OPTION_DISABLED, false);
				xmppConnectionService.updateAccount(mAccount);
				return;
			}
			if (!Validator.isValidJid(mAccountJid.getText().toString())) {
				mAccountJid.setError(getString(R.string.invalid_jid));
				mAccountJid.requestFocus();
				return;
			}
			boolean registerNewAccount = mRegisterNew.isChecked();
			String[] jidParts = mAccountJid.getText().toString().split("@");
			String username = jidParts[0];
			String server;
			if (jidParts.length >= 2) {
				server = jidParts[1];
			} else {
				server = "";
			}
			String password = mPassword.getText().toString();
			String passwordConfirm = mPasswordConfirm.getText().toString();
			if (registerNewAccount) {
				if (!password.equals(passwordConfirm)) {
					mPasswordConfirm
							.setError(getString(R.string.passwords_do_not_match));
					mPasswordConfirm.requestFocus();
					return;
				}
			}
			if (mAccount != null) {
				mAccount.setPassword(password);
				mAccount.setUsername(username);
				mAccount.setServer(server);
				mAccount.setOption(Account.OPTION_REGISTER, registerNewAccount);
				xmppConnectionService.updateAccount(mAccount);
			} else {
				if (xmppConnectionService.findAccountByJid(mAccountJid
						.getText().toString()) != null) {
					mAccountJid
							.setError(getString(R.string.account_already_exists));
					mAccountJid.requestFocus();
					return;
				}
				mAccount = new Account(username, server, password);
				mAccount.setOption(Account.OPTION_USETLS, true);
				mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
				mAccount.setOption(Account.OPTION_REGISTER, registerNewAccount);
				xmppConnectionService.createAccount(mAccount);
			}
			if (jidToEdit != null) {
				finish();
			} else {
				updateSaveButton();
				updateAccountInformation();
			}

		}
	};

    protected void updateLayout() {


        //Update logic for JSON object retrieval
        if (waitingForJSON) {
            this.mLoadingPanel.setVisibility(View.VISIBLE);
            this.mReloadLayout.setVisibility(View.GONE);

            mSaveButton.setEnabled(false);
            mSaveButton.setTextColor(getSecondaryTextColor());
            mSaveButton.setText(R.string.account_status_connecting);
        }
        else {
            if (pinSelected) {
                this.mLoadingPanel.setVisibility(View.VISIBLE);
                this.mReloadLayout.setVisibility(View.GONE);
            }
            else {
                this.mLoadingPanel.setVisibility(View.GONE);
                this.mReloadButton.setVisibility(View.VISIBLE);
            }
            if (!pinSelected && isConnected()) {
                mSaveButton.setEnabled(true);
                mSaveButton.setTextColor(getPrimaryTextColor());
                mSaveButton.setText(R.string.next);
            }
        }

        if (jsonPin != null) {
            this.mAssignedPin.setVisibility(View.VISIBLE);
            if (!pinSelected) this.mReloadLayout.setVisibility(View.VISIBLE);
        }
        else {
            this.mAssignedPin.setVisibility(View.GONE);
            this.mReloadLayout.setVisibility(View.GONE);
            mSaveButton.setTextColor(getSecondaryTextColor());
            mSaveButton.setEnabled(false);
        }

        //Check for connection availability to display accurate message
        if (!isConnected()) {
            mNoInternet.setVisibility(View.VISIBLE);
            if (pinSelected) {
                mPin.setVisibility(View.VISIBLE);
                mPin.setTextColor(getSecondaryTextColor());
            }
            else {
                mPin.setVisibility(View.GONE);
                mPin.setTextColor(getPrimaryTextColor());
            }
            mLoadingPanel.setVisibility(View.GONE);
            mAssignedPin.setVisibility(View.GONE);
            mReloadLayout.setVisibility(View.VISIBLE);
            mTryAnother.setText(R.string.createPin_tryagainnow);
            mTryAnother.setTextColor(getSecondaryTextColor());
            mSaveButton.setEnabled(false);
            mSaveButton.setTextColor(getSecondaryTextColor());
            mSaveButton.setText(R.string.next);
        }
        else {
            mNoInternet.setVisibility(View.GONE);
            mPin.setVisibility(View.VISIBLE);
            mTryAnother.setText(R.string.createPin_tryanotherpin);
            mTryAnother.setTextColor(getSecondaryTextColor());
            mPin.setTextColor(getPrimaryTextColor());
        }

    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_pin);

        this.mLoadingPanel = (RelativeLayout) findViewById(R.id.loadingPanel);
        this.mReloadLayout = (LinearLayout) findViewById(R.id.reload_layout);
        this.mPin = (TextView) findViewById(R.id.account_pin);
        this.mAssignedPin = (TextView) findViewById(R.id.info_assigned_pin);
        this.mNoInternet = (TextView) findViewById(R.id.account_no_internet);
        this.mTryAnother = (TextView) findViewById(R.id.info_tryanother);
        this.mReloadButton = (ImageButton) findViewById(R.id.reload_button);
        this.mReloadButton.setOnClickListener(this.mReloadButtonClickListener);
        this.mSaveButton = (Button) findViewById(R.id.save_button);
        this.mCancelButton = (Button) findViewById(R.id.cancel_button);
        this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
        this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);


        //Request a new PIN to the API
        this.startJSONRequest("http://api.droidko.com/?method=pinRequest&output=json");

	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	protected void onBackendConnected() {
        super.onBackendConnected();

        updateLayout();
	}

}
