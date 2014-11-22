package eu.siacs.conversations.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.R;
import eu.siacs.conversations.api.ApiAsyncTask;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.entities.Contact;
import eu.siacs.conversations.entities.ListItem;
import eu.siacs.conversations.ui.adapter.ListItemAdapter;
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
    private TextView mOnlyLocalMessage;
    private TextView mWarningMessage;
    private TextView mErrorMessage;
    private TextView mState1Message;
    private TextView mState203Message;
    private TextView mStateUnknown;
    private TextView mStateOnlyLocal;
    private TextView mErrorCodeMessage;
    private TextView mNoContacts;
    private TextView mYesContacts;
    private ImageView mOkIcon;
    private ImageView mErrorIcon;
    private ImageView mWarningIcon;
    private ImageView mAvatar;
    private RelativeLayout mLoadingPanel;
    private Button mSaveButton;
    private Button mCancelButton;
    private ListView mListView;
    private ArrayList<ListItem> contacts = new ArrayList<>();
    private ArrayAdapter<ListItem> mContactsAdapter;

    private boolean waitingForJSON = false;
    private boolean pinEliminated = false;
    private boolean mOnlyLocalDismiss = true;
    private boolean mBackEndConnected = false;
    private boolean mCountdownFinished = false;
    private Bitmap mAvatarBitMap;
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
    static final String STATE_AVATARBITMAP = "avatarBitMap";

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

    protected void loadContacts (Account account) {
        this.contacts.clear();
        for (Contact contact : account.getRoster().getContacts()) {
            if (contact.showInRoster()) {
                this.contacts.add(contact);
            }
        }
        Collections.sort(this.contacts);
        mContactsAdapter.notifyDataSetChanged();
    }

    private void startCountdown() {
        mCountdownFinished = false;
        new CountDownTimer(Config.DISMISSCOUNTDOWNLOCK, 1000) {

            public void onTick(long millisUntilFinished) {
                mSaveButton.setText(getString(R.string.waiting) + " " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                mCountdownFinished = true;
                updateLayout();
            }
        }.start();
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

            if (mBackEndConnected) {
                if (pinEliminated) {
                    finish();
                }
                else if (!waitingForJSON && mPincode != null && mPintoken != null) {
                    //Request the delete of a desired PIN
                    startJSONRequest(Config.APIURL + "?method=pinDismiss&output=json&pincode=" + mPincode + "&pintoken=" + mPintoken);
                }
                else if (mOnlyLocalDismiss) {
                    xmppConnectionService.deleteAccount(mAccount);
                }
            }

        }
    };

    protected void updateLayout() {

        if (waitingForJSON) {
            mLoadingPanel.setVisibility(View.VISIBLE);
            this.mListView.smoothScrollToPosition(0);
            mCancelButton.setEnabled(false);
            mCancelButton.setTextColor(getSecondaryTextColor());
            if (mCountdownFinished) {
                mSaveButton.setEnabled(false);
                mSaveButton.setTextColor(getSecondaryTextColor());
                mSaveButton.setText(R.string.account_status_connecting);
            }
        }
        else {
            mLoadingPanel.setVisibility(View.GONE);
            mCancelButton.setEnabled(true);
            mCancelButton.setTextColor(getPrimaryTextColor());
            if (mCountdownFinished) {
                mSaveButton.setEnabled(true);
                mSaveButton.setTextColor(getPrimaryTextColor());
                mSaveButton.setText(R.string.confirm);
            }
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

        if (mOnlyLocalDismiss) {
            mOnlyLocalMessage.setVisibility(View.VISIBLE);
        }
        else {
            mOnlyLocalMessage.setVisibility(View.GONE);
        }

        if (contacts.isEmpty()) {
            mNoContacts.setVisibility(View.VISIBLE);
            mYesContacts.setVisibility(View.GONE);
        }
        else {
            mNoContacts.setVisibility(View.GONE);
            mYesContacts.setVisibility(View.VISIBLE);
        }

        if (pinEliminated) {
            mDisclaimer.setVisibility(View.GONE);
            mWarningMessage.setVisibility(View.GONE);
            mSaveButton.setText(R.string.finish);
            mCancelButton.setEnabled(false);
            mCancelButton.setTextColor(getSecondaryTextColor());
            if (mOnlyLocalDismiss) {mStateOnlyLocal.setVisibility(View.VISIBLE);}
            else {mStateOnlyLocal.setVisibility(View.GONE);}
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
                mOkIcon.setVisibility(View.GONE);
                mErrorIcon.setVisibility(View.GONE);
                mWarningIcon.setVisibility(View.VISIBLE);
            break;
            case 1:
                mPinDeletedSuccessfully.setVisibility(View.VISIBLE);
                mState1Message.setVisibility(View.VISIBLE);
                mState203Message.setVisibility(View.GONE);
                mStateUnknown.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);
                mErrorCodeMessage.setVisibility(View.GONE);
                mOkIcon.setVisibility(View.VISIBLE);
                mErrorIcon.setVisibility(View.GONE);
                mWarningIcon.setVisibility(View.GONE);
                this.mListView.smoothScrollToPosition(0);
            break;
            case 203:
                mPinDeletedSuccessfully.setVisibility(View.VISIBLE);
                mState1Message.setVisibility(View.GONE);
                mState203Message.setVisibility(View.VISIBLE);
                mStateUnknown.setVisibility(View.GONE);
                mErrorMessage.setVisibility(View.GONE);
                mErrorCodeMessage.setVisibility(View.GONE);
                mOkIcon.setVisibility(View.VISIBLE);
                mErrorIcon.setVisibility(View.GONE);
                mWarningIcon.setVisibility(View.GONE);
                this.mListView.smoothScrollToPosition(0);
            break;
            default: //Other error cases
                mPinDeletedSuccessfully.setVisibility(View.GONE);
                mState1Message.setVisibility(View.GONE);
                mState203Message.setVisibility(View.GONE);
                mWarningMessage.setVisibility(View.GONE);
                mDisclaimer.setVisibility(View.GONE);
                mStateUnknown.setVisibility(View.VISIBLE);
                mErrorMessage.setVisibility(View.VISIBLE);
                mErrorCodeMessage.setText(getString(R.string.pinDismiss_error_code) + " " + mState);
                mErrorCodeMessage.setVisibility(View.VISIBLE);
                mOkIcon.setVisibility(View.GONE);
                mErrorIcon.setVisibility(View.VISIBLE);
                mWarningIcon.setVisibility(View.GONE);
                this.mListView.smoothScrollToPosition(0);
        }

    }

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_dismiss_pin);

        //Important: the XML layout was splitted in two files, activity_dismiss_pin.xml
        //and dismiss_header.xml because you can't have two scrollables nested.
        //So it's 'impossible' to have a ListView inside a ScrollView.
        //Therefore it's important to inflate the header file before making references to its views.
        mListView = (ListView) findViewById(R.id.dismiss_contact_list);
        mListView.setFastScrollEnabled(true);
        mContactsAdapter = new ListItemAdapter(this, contacts);
        mListView.setAdapter(mContactsAdapter);
        View header = getLayoutInflater().inflate(R.layout.dismiss_header, null);
        mListView.addHeaderView(header);

        this.mLoadingPanel = (RelativeLayout) findViewById(R.id.loadingPanel);
        this.mPin = (TextView) findViewById(R.id.account_pin);
        this.mDeletingPin = (TextView) findViewById(R.id.account_request_pin);
        this.mAssignedPin = (TextView) findViewById(R.id.info_assigned_pin);
        this.mNoInternet = (TextView) findViewById(R.id.info_no_internet);
        this.mTryAgain = (TextView) findViewById(R.id.info_tryanother);
        this.mDisclaimer = (TextView) findViewById(R.id.dismiss_disclaimer);
        this.mOnlyLocalMessage = (TextView) findViewById(R.id.dismiss_info_onlylocalpin);
        this.mState1Message = (TextView) findViewById(R.id.dismiss_result_1);
        this.mState203Message = (TextView) findViewById(R.id.dismiss_result_203);
        this.mStateUnknown = (TextView) findViewById(R.id.dismiss_result_unknown);
        this.mStateOnlyLocal = (TextView) findViewById(R.id.dismiss_result_onlylocal);
        this.mWarningMessage = (TextView) findViewById(R.id.dismiss_warning);
        this.mErrorMessage = (TextView) findViewById(R.id.info_error);
        this.mErrorCodeMessage = (TextView) findViewById(R.id.dismiss_error_code);
        this.mPinDeletedSuccessfully = (TextView) findViewById(R.id.info_pin_eliminated_successfully);
        this.mNoContacts = (TextView) findViewById(R.id.info_pin_no_contacts);
        this.mYesContacts = (TextView) findViewById(R.id.info_pin_contacts);
        this.mOkIcon = (ImageView) findViewById(R.id.ok_icon);
        this.mErrorIcon = (ImageView) findViewById(R.id.error_icon);
        this.mWarningIcon = (ImageView) findViewById(R.id.warning_icon);
        this.mAvatar = (ImageView) findViewById(R.id.dismiss_avatar);
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
            this.mAvatarBitMap = savedInstanceState.getParcelable(STATE_AVATARBITMAP);
        }

        this.startCountdown(); //Save button countdown lock
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
        savedInstanceState.putParcelable(STATE_AVATARBITMAP, mAvatarBitMap);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

	@Override
	protected void onBackendConnected() {

        mBackEndConnected = true;
        try {
            receivedJid = Jid.fromString(getIntent().getExtras().getString("jidString"));
            mAccount = xmppConnectionService.findAccountByJid(receivedJid);
        } catch (InvalidJidException e) {
            e.printStackTrace();
        }
        mPincode = receivedJid.getLocalpart().toUpperCase();
        if (mAccount != null) {
            mAvatarBitMap = avatarService().get(this.mAccount, getPixel(72)); //If necessary to prevent NPE
            mPintoken = mAccount.getPintoken();
            //This loads the PIN's contacts
            loadContacts(mAccount);
        }
        if (mAvatarBitMap != null) mAvatar.setImageBitmap(mAvatarBitMap);
        mPin.setText(mPincode);
        mPin.setTextSize(getResources().getDimension(R.dimen.TextMedium));
        //If there is no pintoken in the account, then it doesn't have 'admin' privileges and cannot
        //delete the pin from the server, only from the device.
        mOnlyLocalDismiss = (mPintoken == null);

        updateLayout();
	}

}
