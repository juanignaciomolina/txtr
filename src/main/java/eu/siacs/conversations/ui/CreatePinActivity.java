package eu.siacs.conversations.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.R;
import eu.siacs.conversations.api.ApiAsyncTask;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.utils.Validator;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;

public class CreatePinActivity extends EditAccountActivity implements ApiAsyncTask.TaskCallbacks {

    private TextView mPin;
    private TextView mAssignedPin;
    private TextView mNoInternet;
    private TextView mTryAnother;
    private TextView mRequestingPin;
    private RelativeLayout mLoadingPanel;
    private LinearLayout mReloadLayout;
    private ImageButton mReloadButton;
    private Button mSaveButton;
    private Button mCancelButton;

    private boolean waitingForJSON = false;
    private JSONObject jsonPin;
    private boolean pinSelected = false;
    private int nAttempts = 0;
    static final int MAX_ATTEMPTS = 3;

    private static final String TAG_TASK_FRAGMENT = "task_api_createpin";
    private ApiAsyncTask mTaskFragment;

    //SavedInstanceState keys
    static final String STATE_WAITINGFORJSON = "waitingForJson";
    static final String STATE_JSONPIN_PINCODE = "jsonPincode";
    static final String STATE_JSONPIN_TOKEN = "jsonToken";
    static final String STATE_PINSELECTED = "pinSelected";

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private void startJSONRequest (String url) {
        // call AsynTask to perform network operation on separate thread
        if (!waitingForJSON && isConnected()){
            if (pinSelected) {
                //Lock the screen on it's current orientation to prevent UI errors with EditAccountActivity log in
                int currentOrientation = getResources().getConfiguration().orientation;
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                }
                else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                }
            }
            Log.d("TXTR", "startJSONRequest: " + url);
            mTaskFragment.startTask(url);
            waitingForJSON = true;
            }
        else {
            Log.d("TXTR", "startJSONRequest: No internet access");
            waitingForJSON = false;
        }
        this.updateLayout();
    }

    private void CreateAndLogIn (String pinCode, String pinPassword, String pinHost, String pinToken) {

        String pinJidString = pinCode+"@"+pinHost;

        //Check point: Should never happen
        if (!Validator.isValidJid(pinJidString)) {
            Toast toast = Toast.makeText(
                    getApplicationContext(),
                    "Error: Not a valid PIN",
                    Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        Jid jid;
        try {
            jid = Jid.fromString(pinJidString);
        } catch (final InvalidJidException e) {
            // TODO: Handle this error?
            return;
        }
        try {
            //Check point: Should never happen
            if (xmppConnectionService.findAccountByJid(Jid.fromString(pinJidString)) != null) {
                Toast toast = Toast.makeText(
                        getApplicationContext(),
                        "Error: This PIN is already in your device",
                        Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        } catch (InvalidJidException e) {
            return;
        }
        //Create local account
        mAccount = new Account(jid.toBareJid(), pinPassword);
        mAccount.setOption(Account.OPTION_USETLS, true);
        mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
        mAccount.setOption(Account.OPTION_REGISTER, false);
        mAccount.setPintoken(pinToken); //TXTR CUSTOM FIELD
        xmppConnectionService.createAccount(mAccount);
    }

    private OnClickListener mReloadButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (!waitingForJSON) {
                if (!pinSelected) {
                    startJSONRequest(Config.APIURL + "?method=pinRequest&output=json");
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
                    String pintoken = URLEncoder.encode(jsonPin.getString("pintoken"), "utf-8");
                    String url =
                                    Config.APIURL
                                    +"?method=pinRegister&output=json"
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
            if (pinSelected) {
                mCancelButton.setEnabled(false);
                mCancelButton.setTextColor(getSecondaryTextColor());
            }
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
            this.mPin.setVisibility(View.VISIBLE);
            this.mRequestingPin.setVisibility(View.GONE);
            if (!pinSelected) this.mReloadLayout.setVisibility(View.VISIBLE);
        }
        else {
            this.mAssignedPin.setVisibility(View.GONE);
            this.mPin.setVisibility(View.GONE);
            this.mReloadLayout.setVisibility(View.GONE);
            mSaveButton.setTextColor(getSecondaryTextColor());
            mSaveButton.setEnabled(false);
        }

        //Check for connection availability to display accurate message
        if (!isConnected()) {
            mNoInternet.setVisibility(View.VISIBLE);
            mRequestingPin.setVisibility(View.GONE);
            if (jsonPin != null && pinSelected) {
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
            if ( jsonPin != null) mPin.setVisibility(View.VISIBLE);
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
        this.mRequestingPin = (TextView) findViewById(R.id.account_request_pin);
        this.mAssignedPin = (TextView) findViewById(R.id.info_assigned_pin);
        this.mNoInternet = (TextView) findViewById(R.id.account_no_internet);
        this.mTryAnother = (TextView) findViewById(R.id.info_tryanother);
        this.mReloadButton = (ImageButton) findViewById(R.id.reload_button);
        this.mReloadButton.setOnClickListener(this.mReloadButtonClickListener);
        this.mSaveButton = (Button) findViewById(R.id.save_button);
        this.mCancelButton = (Button) findViewById(R.id.cancel_button);
        this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
        this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);

        FragmentManager fm = getFragmentManager();
        mTaskFragment = (ApiAsyncTask) fm.findFragmentByTag(TAG_TASK_FRAGMENT);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mTaskFragment == null) {
            mTaskFragment = new ApiAsyncTask();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }

        if (savedInstanceState != null) {
            //Restore values of the previous instance (ie: before rotating the screen)
            this.waitingForJSON = savedInstanceState.getBoolean(STATE_WAITINGFORJSON);
            this.pinSelected = savedInstanceState.getBoolean(STATE_PINSELECTED);
            if (    savedInstanceState.containsKey(STATE_JSONPIN_PINCODE)
                    && savedInstanceState.containsKey(STATE_JSONPIN_TOKEN)
                    ) {
                this.jsonPin = new JSONObject();
                try {
                    this.jsonPin.put("pincode", savedInstanceState.getString(STATE_JSONPIN_PINCODE));
                    this.jsonPin.put("pintoken", savedInstanceState.getString(STATE_JSONPIN_TOKEN));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                this.mPin.setText(savedInstanceState.getString(STATE_JSONPIN_PINCODE));
                this.mPin.setTextSize(getResources().getDimension(R.dimen.TextBig));
                this.updateLayout();
            }
        }
        else {
            //Request a new PIN to the API
            this.startJSONRequest(Config.APIURL + "?method=pinRequest&output=json");
        }

	}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current activity state
        savedInstanceState.putBoolean(STATE_PINSELECTED, pinSelected);
        savedInstanceState.putBoolean(STATE_WAITINGFORJSON, waitingForJSON);

        try {
            if (jsonPin != null) {
                savedInstanceState.putString(STATE_JSONPIN_PINCODE, URLEncoder.encode(jsonPin.getString("pincode"), "utf-8"));
                savedInstanceState.putString(STATE_JSONPIN_TOKEN, URLEncoder.encode(jsonPin.getString("pintoken"), "utf-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    //**Overrides for the ApiAsyncTask interface**//
    @Override
    public void onPreExecute() { }

    @Override //Called when the Task requested to ApiAsyncTask is completed
    public void onPostExecute(String result) {
        Log.d("TXTR", "onPostExecute: Something received");
        try {
            jsonPin = new JSONObject(result);
            Log.d("TXTR", "onPostExecute: Result converted to JSON");

            //mPin.setText(jsonPin.toString(2)); Show the whole object with this to debug
            if (jsonPin.has("state") && jsonPin.getInt("state") == 1) { //State 1: OK
                mPin.setText(jsonPin.getString("pincode"));
                mPin.setTextSize(getResources().getDimension(R.dimen.TextBig));
                nAttempts = 0; //Reset attempts

                if(pinSelected) {
                    CreateAndLogIn(
                            jsonPin.getString("pincode"),
                            jsonPin.getString("password"),
                            jsonPin.getString("host"),
                            jsonPin.getString("pintoken"));
                }
            }
            else if (nAttempts < MAX_ATTEMPTS) { //Check that the same PIN is not being tried too many times
                nAttempts++;
                mSaveButton.performClick();
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
