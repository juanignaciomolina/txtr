package eu.siacs.conversations.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
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

import org.json.JSONException;
import org.json.JSONObject;

import eu.siacs.conversations.R;
import eu.siacs.conversations.api.ApiAsyncTask;

public class DismissPinActivity extends EditAccountActivity implements ApiAsyncTask.TaskCallbacks {

    private TextView mPin;
    private TextView mAssignedPin;
    private TextView mNoInternet;
    private TextView mTryAgain;
    private TextView mDeletingPin;
    private RelativeLayout mLoadingPanel;
    private LinearLayout mReloadLayout;
    private ImageButton mReloadButton;
    private Button mSaveButton;
    private Button mCancelButton;

    private boolean waitingForJSON = false;
    private JSONObject jsonPin;
    private String mPincode;
    private String mPintoken;
    private int nAttempts = 0;
    static final int MAX_ATTEMPTS = 3;

    private static final String TAG_TASK_FRAGMENT = "task_api_createpin";
    private ApiAsyncTask mTaskFragment;

    //SavedInstanceState keys
    static final String STATE_WAITINGFORJSON = "waitingForJson";

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

            if (!waitingForJSON && mPincode != null) {
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
        this.mSaveButton = (Button) findViewById(R.id.save_button);
        this.mCancelButton = (Button) findViewById(R.id.cancel_button);
        this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
        this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);

        //Retrieve the pincode send to the activity
        mPincode = getIntent().getExtras().getString("pincode");
        mPin.setText(mPincode);

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
        }

        this.updateLayout();

	}

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current activity state
        savedInstanceState.putBoolean(STATE_WAITINGFORJSON, waitingForJSON);

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
                //AlertDialog for letting the user now that the pin has been deleted
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        DismissPinActivity.this);
                builder.setTitle("PIN deleted successfully");
                builder.setIconAttribute(android.R.attr.alertDialogIcon);
                builder.setMessage("PIN "+mPincode+" has been removed and cannot receive messages anymore. Contacts from this PIN has also been removed from your device.");
                builder.setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                builder.create().show();
            }
            else if (nAttempts < MAX_ATTEMPTS) { //Check that the same PIN is not being tried too many times
                nAttempts++;
                mSaveButton.performClick();
            }
            else {
                mPin.setText("Couldn't delete the PIN, try again later");
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
