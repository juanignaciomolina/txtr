package eu.siacs.conversations.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import eu.siacs.conversations.R;
import eu.siacs.conversations.api.ApiAsyncTask;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;

public class DismissPinActivity extends XmppActivity implements ApiAsyncTask.TaskCallbacks {

    private TextView mPin;
    private TextView mAssignedPin;
    private TextView mNoInternet;
    private TextView mTryAgain;
    private TextView mDeletingPin;
    private TextView mPinDeletedSuccessfully;
    private TextView mDisclaimer;
    private TextView mWarningMessage;
    private TextView mErrorMessage;
    private TextView mState1Message;
    private TextView mState203Message;
    private TextView mStateUnknown;
    private TextView mErrorCodeMessage;
    private RelativeLayout mLoadingPanel;
    private Button mSaveButton;
    private Button mCancelButton;

    private boolean waitingForJSON = false;
    private boolean pinEliminated = false;
    private JSONObject jsonPin;
    private Jid receivedJid;
    private String mPincode;
    private String mHost;
    private String mPintoken;
    private Account mAccount;
    private int nAttempts = 0;
    private int mState = 0;
    static final int MAX_ATTEMPTS = 3;

    private static final String TAG_TASK_FRAGMENT = "task_api_createpin";
    private ApiAsyncTask mTaskFragment;

    //SavedInstanceState keys
    static final String STATE_WAITINGFORJSON = "waitingForJson";
    static final String STATE_PINELIMINATED = "pinEliminated";
    static final String STATE_PINCODE = "pinCode";
    static final String STATE_HOST = "host";
    static final String STATE_PINTOKEN ="pinToken";
    static final String STATE_REQUESTSTATE = "requestState";

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

    public OnClickListener mCancelButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            finish();
        }
    };


    private OnClickListener mSaveButtonClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {

            if (pinEliminated) {
                finish();
            }
            else if (!waitingForJSON && mPincode != null) {
                //Request the delete of a desired PIN
                startJSONRequest("http://api.droidko.com/?method=pinDismiss&output=json&pincode="+mPincode);
            }

        }
    };

    protected void updateLayout() {


        if (waitingForJSON) {
            mLoadingPanel.setVisibility(View.VISIBLE);
            mSaveButton.setEnabled(false);
            mSaveButton.setTextColor(getSecondaryTextColor());
            mSaveButton.setText(R.string.account_status_connecting);
        }
        else {
            mLoadingPanel.setVisibility(View.GONE);
            mSaveButton.setEnabled(true);
            mSaveButton.setTextColor(getPrimaryTextColor());
            mSaveButton.setText(R.string.next);
        }

        if (!isConnected()) {
            mNoInternet.setVisibility(View.VISIBLE);
            mSaveButton.setEnabled(false);
            mSaveButton.setTextColor(getSecondaryTextColor());
            mSaveButton.setText(R.string.account_status_connecting);
        }
        else {
            mNoInternet.setVisibility(View.GONE);
        }

        if (pinEliminated) {
            mDisclaimer.setVisibility(View.GONE);
            mWarningMessage.setVisibility(View.GONE);
        }
        else {
            mDisclaimer.setVisibility(View.VISIBLE);
            mWarningMessage.setVisibility(View.VISIBLE);
        }

        switch (mState) {
            case 0:
                mPinDeletedSuccessfully.setVisibility(View.GONE);
                mState1Message.setVisibility(View.GONE);
                mState203Message.setVisibility(View.GONE);
                mStateUnknown.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);
                mErrorCodeMessage.setVisibility(View.GONE);
            break;
            case 1:
                mPinDeletedSuccessfully.setVisibility(View.VISIBLE);
                mState1Message.setVisibility(View.VISIBLE);
                mState203Message.setVisibility(View.GONE);
                mStateUnknown.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);
                mErrorCodeMessage.setVisibility(View.GONE);
            break;
            case 203:
                mPinDeletedSuccessfully.setVisibility(View.VISIBLE);
                mState1Message.setVisibility(View.GONE);
                mState203Message.setVisibility(View.VISIBLE);
                mStateUnknown.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);
                mErrorCodeMessage.setVisibility(View.GONE);
            break;
            default:
                mPinDeletedSuccessfully.setVisibility(View.GONE);
                mState1Message.setVisibility(View.GONE);
                mState203Message.setVisibility(View.GONE);
                mStateUnknown.setVisibility(View.VISIBLE);
                mErrorMessage.setVisibility(View.VISIBLE);
                mErrorCodeMessage.setText(getString(R.string.pinDismiss_error_code)+mState);
                mErrorCodeMessage.setVisibility(View.VISIBLE);
        }

    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_dismiss_pin);

        this.mLoadingPanel = (RelativeLayout) findViewById(R.id.loadingPanel);
        this.mPin = (TextView) findViewById(R.id.account_pin);
        this.mDeletingPin = (TextView) findViewById(R.id.account_request_pin);
        this.mAssignedPin = (TextView) findViewById(R.id.info_assigned_pin);
        this.mNoInternet = (TextView) findViewById(R.id.info_no_internet);
        this.mTryAgain = (TextView) findViewById(R.id.info_tryanother);
        this.mDisclaimer = (TextView) findViewById(R.id.dismiss_disclaimer);
        this.mState1Message = (TextView) findViewById(R.id.dismiss_result_1);
        this.mState203Message = (TextView) findViewById(R.id.dismiss_result_203);
        this.mStateUnknown = (TextView) findViewById(R.id.dismiss_result_unknown);
        this.mWarningMessage = (TextView) findViewById(R.id.dismiss_warning);
        this.mErrorMessage = (TextView) findViewById(R.id.info_error);
        this.mErrorCodeMessage = (TextView) findViewById(R.id.dismiss_error_code);
        this.mPinDeletedSuccessfully = (TextView) findViewById(R.id.info_pin_eliminated_successfully);
        this.mSaveButton = (Button) findViewById(R.id.save_button);
        this.mCancelButton = (Button) findViewById(R.id.cancel_button);
        this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
        this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);

        getActionBar().setTitle("Eliminate PIN");

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
            this.pinEliminated = savedInstanceState.getBoolean(STATE_PINELIMINATED);
            this.mPincode = savedInstanceState.getString(STATE_PINCODE);
            this.mHost = savedInstanceState.getString(STATE_HOST);
            this.mPintoken = savedInstanceState.getString(STATE_PINTOKEN);
            this.mState = savedInstanceState.getInt(STATE_REQUESTSTATE);
        }

        this.updateLayout();

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
            if (jsonPin.has("state")) {

                 switch (jsonPin.getInt("state")) {
                     //State 1: OK
                     case 1:
                         //Delete the account locally
                         xmppConnectionService.deleteAccount(mAccount);
                         pinEliminated = true;
                         mState = 1;
                         break;

                     //State 203: Already deleted or not even registered
                     case 203:
                         xmppConnectionService.deleteAccount(mAccount);
                         pinEliminated = true;
                         mState = 203;
                         break;

                     //In case of any other type of error
                     default:
                         pinEliminated = false;
                         mState = jsonPin.getInt("state");
                         break;
                 }

            }
            else if (nAttempts < MAX_ATTEMPTS) { //Server response unexpected, retry
                pinEliminated = false;
                nAttempts++;
                mSaveButton.performClick();
            }
            else { //Server response unexpected, no more tries
                pinEliminated = false;
                mState = 299; //State 299: Unknown
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
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current activity state
        savedInstanceState.putBoolean(STATE_PINELIMINATED, pinEliminated);
        savedInstanceState.putBoolean(STATE_WAITINGFORJSON, waitingForJSON);
        savedInstanceState.putString(STATE_PINCODE, mPincode);
        savedInstanceState.putString(STATE_PINTOKEN, mPintoken);
        savedInstanceState.putString(STATE_HOST, mHost);
        savedInstanceState.putInt(STATE_REQUESTSTATE, mState);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

	@Override
	protected void onBackendConnected() {

        //TODO unHardcode this
        //Split the jid received
        try {
            receivedJid = Jid.fromString(getIntent().getExtras().getString("jidString"));
            mAccount = xmppConnectionService.findAccountByJid(receivedJid);
        } catch (InvalidJidException e) {
            e.printStackTrace();
        }
        //String[] separated =  getIntent().getExtras().getString("pincode").split("@");
        //mPincode = separated[0]; //Retrieve the pincode send to the activity
        //mHost = separated[1];
        mPincode = receivedJid.getLocalpart().toUpperCase();
        mPin.setText(mPincode);
        mPin.setTextSize(getResources().getDimension(R.dimen.TextBig));

        updateLayout();
	}

}
