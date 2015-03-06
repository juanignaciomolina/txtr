package eu.siacs.conversations.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.makeramen.RoundedImageView;

import java.util.List;

import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Downloadable;
import eu.siacs.conversations.entities.DownloadableFile;
import eu.siacs.conversations.entities.Message;
import eu.siacs.conversations.ui.ConversationActivity;
import eu.siacs.conversations.ui.XmppActivity;
import eu.siacs.conversations.utils.UIHelper;

public class ConversationAdapter extends ArrayAdapter<Conversation> {

	private XmppActivity activity;

	public ConversationAdapter(XmppActivity activity,
			List<Conversation> conversations) {
		super(activity, 0, conversations);
		this.activity = activity;
	}

	@Override
	public View getView(int position, View view, ViewGroup parent) {
		if (view == null) {
			LayoutInflater inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.conversation_list_row,
					parent, false);
		}
		Conversation conversation = getItem(position);
		if (this.activity instanceof ConversationActivity) {
			ConversationActivity activity = (ConversationActivity) this.activity;
			if (!activity.isConversationsOverviewHideable()) {
				if (conversation == activity.getSelectedConversation()) {
					view.setBackgroundColor(activity
							.getSecondaryBackgroundColor());
				} else {
					view.setBackgroundColor(Color.TRANSPARENT);
				}
			} else {
				view.setBackgroundColor(Color.TRANSPARENT);
			}
		}
		TextView convName = (TextView) view
			.findViewById(R.id.conversation_name);
		if (conversation.getMode() == Conversation.MODE_SINGLE
				|| activity.useSubjectToIdentifyConference()) {
			convName.setText(conversation.getName());
		} else {
			convName.setText(conversation.getJid().toBareJid().toString());
		}
		TextView mLastMessage = (TextView) view
			.findViewById(R.id.conversation_lastmsg);
		TextView mTimestamp = (TextView) view
			.findViewById(R.id.conversation_lastupdate);
		ImageView imagePreview = (ImageView) view
			.findViewById(R.id.conversation_lastimage);
        ImageView msgIcon = (ImageView) view
                .findViewById(R.id.conversation_status_icon);
        RoundedImageView msgIconBg = (RoundedImageView) view
                .findViewById(R.id.conversation_status_bg);


		Message message = conversation.getLatestMessage();

		if (!conversation.isRead()) {
			convName.setTypeface(null, Typeface.BOLD);
            activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_received);
		} else {
			convName.setTypeface(null, Typeface.NORMAL);
		}

        //TODO TXTR CUSTOM: Status icon for the latest message
        switch (conversation.getLatestMessage().getStatus()) {
            case 0: //STATUS_RECEIVED
                if (conversation.isRead()) {
                    msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_received_read));
                    msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.primary_bg));
                }else {
                    msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_received));
                    msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.brightred_bg));
                }
                break;
            case 1: //STATUS_UNSEND
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_unsend));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.purple_bg));
                break;
            case 2: //STATUS_SEND
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_send));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.darkblue_bg));
                break;
            case 3: //STATUS_SEND_FAILED
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_error));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.asphalt_bg));
                break;
            case 5: //STATUS_WAITING
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_waiting));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.orange_bg));
                break;
            case 6: //STATUS_OFFERED
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_offered));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.orange_bg));
                break;
            case 7: //STATUS_SEND_RECEIVED
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_send_received));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.blue_bg));
                break;
            case 8: //STATUS_SEND_DISPLAYED
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_send_displayed));
                msgIconBg.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.emerald_bg));
                break;
            default:
                msgIcon.setImageDrawable(activity.getApplicationContext().getResources().getDrawable(R.drawable.ic_msg_received_read));
                break;
        }

		if (message.getType() == Message.TYPE_IMAGE || message.getType() == Message.TYPE_FILE
				|| message.getDownloadable() != null) {
			Downloadable d = message.getDownloadable();
			if (conversation.isRead()) {
				mLastMessage.setTypeface(null, Typeface.ITALIC);
			} else {
				mLastMessage.setTypeface(null, Typeface.BOLD_ITALIC);
			}
			if (d != null) {
				mLastMessage.setVisibility(View.VISIBLE);
				imagePreview.setVisibility(View.GONE);
				if (d.getStatus() == Downloadable.STATUS_CHECKING) {
					mLastMessage.setText(R.string.checking_image);
				} else if (d.getStatus() == Downloadable.STATUS_DOWNLOADING) {
					if (message.getType() == Message.TYPE_FILE) {
						mLastMessage.setText(getContext().getString(R.string.receiving_file,d.getMimeType(), d.getProgress()));
					} else {
						mLastMessage.setText(getContext().getString(R.string.receiving_image, d.getProgress()));
					}
				} else if (d.getStatus() == Downloadable.STATUS_OFFER) {
					if (message.getType() == Message.TYPE_FILE) {
						mLastMessage.setText(R.string.file_offered_for_download);
					} else {
						mLastMessage.setText(R.string.image_offered_for_download);
					}
				} else if (d.getStatus() == Downloadable.STATUS_OFFER_CHECK_FILESIZE) {
					mLastMessage.setText(R.string.image_offered_for_download);
				} else if (d.getStatus() == Downloadable.STATUS_DELETED) {
					if (message.getType() == Message.TYPE_FILE) {
						mLastMessage.setText(R.string.file_deleted);
					} else {
						mLastMessage.setText(R.string.image_file_deleted);
					}
				} else if (d.getStatus() == Downloadable.STATUS_FAILED) {
					if (message.getType() == Message.TYPE_FILE) {
						mLastMessage.setText(R.string.file_transmission_failed);
					} else {
						mLastMessage.setText(R.string.image_transmission_failed);
					}
				} else if (message.getImageParams().width > 0) {
					mLastMessage.setVisibility(View.GONE);
					imagePreview.setVisibility(View.VISIBLE);
					activity.loadBitmap(message, imagePreview);
				} else {
					mLastMessage.setText("");
				}
			} else if (message.getEncryption() == Message.ENCRYPTION_PGP) {
				imagePreview.setVisibility(View.GONE);
				mLastMessage.setVisibility(View.VISIBLE);
				mLastMessage.setText(R.string.encrypted_message_received);
			} else if (message.getType() == Message.TYPE_FILE && message.getImageParams().width <= 0) {
				DownloadableFile file = activity.xmppConnectionService.getFileBackend().getFile(message);
				mLastMessage.setVisibility(View.VISIBLE);
				imagePreview.setVisibility(View.GONE);
				mLastMessage.setText(getContext().getString(R.string.file,file.getMimeType()));
			} else {
				mLastMessage.setVisibility(View.GONE);
				imagePreview.setVisibility(View.VISIBLE);
				activity.loadBitmap(message, imagePreview);
			}
		} else {
			if ((message.getEncryption() != Message.ENCRYPTION_PGP)
					&& (message.getEncryption() != Message.ENCRYPTION_DECRYPTION_FAILED)) {
				mLastMessage.setText(message.getBody());
			} else {
				mLastMessage.setText(R.string.encrypted_message_received);
			}
			if (!conversation.isRead()) {
				mLastMessage.setTypeface(null, Typeface.BOLD);
			} else {
				mLastMessage.setTypeface(null, Typeface.NORMAL);
			}
			mLastMessage.setVisibility(View.VISIBLE);
			imagePreview.setVisibility(View.GONE);
		}
		mTimestamp.setText(UIHelper.readableTimeDifference(getContext(),
					conversation.getLatestMessage().getTimeSent()));


        //TODO: TXTR CUSTOM
        RoundedImageView profilePicture = (RoundedImageView) view
			.findViewById(R.id.conversation_image);
		profilePicture.setImageBitmap(activity.avatarService().get(
					conversation, activity.getPixel(56)));

        RoundedImageView accountPicture = (RoundedImageView) view
                .findViewById(R.id.conversation_account_image);
        accountPicture.setImageBitmap(activity.avatarService().get(
                conversation.getAccount(), activity.getPixel(24)));



        //Determine a border color for the account image (to prevent confusion when two accounts have the same image)
        profilePicture.setBorderColor(conversation.getAccount().getAccountColor());
        accountPicture.setBorderColor(conversation.getAccount().getAccountColor());


		return view;
	}

}
