package eu.siacs.conversations.ui;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import eu.siacs.conversations.Config;
import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.Account;
import eu.siacs.conversations.ui.adapter.KnownHostsAdapter;
import eu.siacs.conversations.ui.cards.AddPinExpandCard;
import eu.siacs.conversations.xmpp.jid.InvalidJidException;
import eu.siacs.conversations.xmpp.jid.Jid;
import eu.siacs.conversations.xmpp.pep.Avatar;
import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.ViewToClickToExpand;
import it.gmariotti.cardslib.library.view.CardViewNative;

/**
 * Created by JuanIgnacio on 22/01/2015.
 */
public class AddPinActivity extends EditAccountActivity  {

    protected EditText mAccountJid;
    private EditText mPassword;
    private Button mCancelButton;
    private Button mSaveButton;

    private Jid jidToEdit;
    protected Account mAccount;

    private Card card;

    private boolean mFetchingAvatar = false;
    private boolean mWaitingForLogin = false;

    private final View.OnClickListener mSaveButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {

            //If the password has not been introduced yet, we change the focus to its editText.
            if (mAccountJid.hasFocus() && mPassword.getText().toString().equals("")) {
                mPassword.requestFocus();
                return;
            }

            final Jid jid;
            if (!mWaitingForLogin) {
                try {
                    //Check that a domain has not been inputted, if it has, remove it. The 'official' domain is concatenated latter
                    if (mAccountJid.getText().toString().contains("@")) mAccountJid.setText(mAccountJid.getText().toString().split("@",2)[0]);
                    jid = Jid.fromString(mAccountJid.getText().toString() + "@" + Config.PINDOMAIN);
                } catch (final InvalidJidException e) {
                    mAccountJid.setError(getString(R.string.invalid_jid));
                    mAccountJid.requestFocus();
                    card.doCollapse();
                    return;
                }
                if (jid.isDomainJid()) {
                    mAccountJid.setError(getString(R.string.invalid_jid));
                    mAccountJid.requestFocus();
                    card.doCollapse();
                    return;
                }
                final String password = mPassword.getText().toString();
                if (mAccount != null) {
                    try {
                        mAccount.setUsername(jid.hasLocalpart() ? jid.getLocalpart() : "");
                        mAccount.setServer(jid.getDomainpart());
                    } catch (final InvalidJidException ignored) {
                        return;
                    }
                    mAccount.setPassword(password);
                    mAccount.setOption(Account.OPTION_REGISTER, false);
                    xmppConnectionService.updateAccount(mAccount);
                    mWaitingForLogin = true;
                    //Expand card with the selected PIN's information
                    card.doExpand();
                    hideKeyboard(); //This is a XmppActivity method
                } else {
                    try {
                        if (xmppConnectionService.findAccountByJid(Jid.fromString(mAccountJid.getText().toString())) != null) {
                            mAccountJid.setError(getString(R.string.account_already_exists));
                            mAccountJid.requestFocus();
                            card.doCollapse();
                            return;
                        }
                    } catch (final InvalidJidException e) {
                        return;
                    }
                    mAccount = new Account(jid.toBareJid(), password);
                    mAccount.setOption(Account.OPTION_USETLS, true);
                    mAccount.setOption(Account.OPTION_USECOMPRESSION, true);
                    mAccount.setOption(Account.OPTION_REGISTER, false);
                    xmppConnectionService.createAccount(mAccount);
                    mWaitingForLogin = true;
                    //Expand card with the selected PIN's information
                    card.doExpand();
                    hideKeyboard(); //This is a XmppActivity method
                }
            }

            if (jidToEdit != null) {
                finish();
            } else {
                updateSaveButton();
                updateAccountInformation();
            }
            updateSaveButton();

        }
    };
    private final View.OnClickListener mCancelButtonClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View v) {
            finish();
        }
    };
    @Override
    public void onAccountUpdate() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                invalidateOptionsMenu();
                if (mAccount != null
                        && mAccount.getStatus() != Account.State.ONLINE
                        && mFetchingAvatar) {
                    startActivity(new Intent(getApplicationContext(),
                            ManageAccountActivity.class));
                    finish();
                } else if (jidToEdit == null && mAccount != null
                        && mAccount.getStatus() == Account.State.ONLINE) {
                    if (!mFetchingAvatar) {
                        mFetchingAvatar = true;
                        xmppConnectionService.checkForAvatar(mAccount,
                                mAvatarFetchCallback);
                    }
                } else {
                    updateSaveButton();
                }
                if (mAccount != null) {
                    updateAccountInformation();
                }
            }
        });
    }
    private final UiCallback<Avatar> mAvatarFetchCallback = new UiCallback<Avatar>() {

        @Override
        public void userInputRequried(final PendingIntent pi, final Avatar avatar) {
            finishInitialSetup(avatar);
        }

        @Override
        public void success(final Avatar avatar) {
            finishInitialSetup(avatar);
        }

        @Override
        public void error(final int errorCode, final Avatar avatar) {
            finishInitialSetup(avatar);
        }
    };
    private final TextWatcher mTextWatcher = new TextWatcher() {

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            updateSaveButton();
        }

        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
        }

        @Override
        public void afterTextChanged(final Editable s) {

        }
    };

    protected void finishInitialSetup(final Avatar avatar) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                final Intent intent;
                if (avatar != null) {
                    intent = new Intent(getApplicationContext(),
                            StartConversationActivity.class);
                } else {
                    intent = new Intent(getApplicationContext(),
                            PublishProfilePictureActivity.class);
                    intent.putExtra("account", mAccount.getJid().toBareJid().toString());
                    intent.putExtra("setup", true);
                }
                startActivity(intent);
                finish();
            }
        });
    }

    protected void updateSaveButton() {
        if (!mWaitingForLogin) {
            this.mSaveButton.setEnabled(true);
            this.mSaveButton.setTextColor(getPrimaryTextColor());
            this.mSaveButton.setText(R.string.next);
        } else {
            this.mSaveButton.setEnabled(false);
            this.mSaveButton.setTextColor(getSecondaryTextColor());
            this.mSaveButton.setText(R.string.account_status_connecting);
        }
    }

    protected boolean accountInfoEdited() {
        return (!this.mAccount.getJid().toBareJid().toString().equals(
                this.mAccountJid.getText().toString()))
                || (!this.mAccount.getPassword().equals(
                this.mPassword.getText().toString()));
    }

    @Override
    protected String getShareableUri() {
        if (mAccount!=null) {
            return mAccount.getShareableUri();
        } else {
            return "";
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pin);

        //Create a Card
        card = new Card(getApplicationContext());

        //This provide a custom CreatePinExpandCard expand area
        AddPinExpandCard expand = new AddPinExpandCard(getApplicationContext());

        //Add expand to a card
        card.addCardExpand(expand);

        ViewToClickToExpand viewToClickToExpand = ViewToClickToExpand.builder().enableForExpandAction();
        card.setViewToClickToExpand(viewToClickToExpand);

        //Set card in the cardView
        CardViewNative cardView = (CardViewNative) findViewById(R.id.card_addpin);
        cardView.setCard(card);

        this.mAccountJid = (EditText) findViewById(R.id.account_jid);
        this.mAccountJid.addTextChangedListener(this.mTextWatcher);
        this.mPassword = (EditText) findViewById(R.id.account_password);
        this.mPassword.addTextChangedListener(this.mTextWatcher);
        this.mSaveButton = (Button) findViewById(R.id.save_button);
        this.mCancelButton = (Button) findViewById(R.id.cancel_button);
        this.mSaveButton.setOnClickListener(this.mSaveButtonClickListener);
        this.mCancelButton.setOnClickListener(this.mCancelButtonClickListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getActionBar() != null) {
            getActionBar().setTitle(R.string.action_add_account);
        }
    }

    @Override
    protected void onBackendConnected() {
        final KnownHostsAdapter mKnownHostsAdapter = new KnownHostsAdapter(this,
                android.R.layout.simple_list_item_1,
                xmppConnectionService.getKnownHosts());

        if (this.xmppConnectionService.getAccounts().size() == 0) {
            if (getActionBar() != null) {
                getActionBar().setDisplayHomeAsUpEnabled(false);
                getActionBar().setDisplayShowHomeEnabled(false);
            }
            this.mCancelButton.setEnabled(false);
            this.mCancelButton.setTextColor(getSecondaryTextColor());
        }
        updateSaveButton();
    }

    private void updateAccountInformation() {
        this.mAccountJid.setText(this.mAccount.getJid().toBareJid().toString());
        this.mPassword.setText(this.mAccount.getPassword());
        if (this.mAccount.errorStatus()) {
            this.mAccountJid.setError(getString(this.mAccount.getStatus().getReadableId()));
            this.mAccountJid.requestFocus();
            this.mWaitingForLogin = false;
            card.doCollapse();
            updateSaveButton();
        }
    }

}
