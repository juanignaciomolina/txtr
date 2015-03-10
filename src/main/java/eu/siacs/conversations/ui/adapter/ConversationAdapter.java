package eu.siacs.conversations.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.makeramen.RoundedImageView;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import eu.siacs.conversations.R;
import eu.siacs.conversations.entities.Conversation;
import eu.siacs.conversations.entities.Downloadable;
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
			LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			view = inflater.inflate(R.layout.conversation_list_row,parent, false);
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
		TextView convName = (TextView) view.findViewById(R.id.conversation_name);
		if (conversation.getMode() == Conversation.MODE_SINGLE || activity.useSubjectToIdentifyConference()) {
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

        if (message.getImageParams().width > 0
				&& (message.getDownloadable() == null
				|| message.getDownloadable().getStatus() != Downloadable.STATUS_DELETED)) {
			mLastMessage.setVisibility(View.GONE);
			imagePreview.setVisibility(View.VISIBLE);
			activity.loadBitmap(message, imagePreview);
		} else {
			Pair<String,Boolean> preview = UIHelper.getMessagePreview(activity,message);
			mLastMessage.setVisibility(View.VISIBLE);
			imagePreview.setVisibility(View.GONE);
			mLastMessage.setText(preview.first);
			if (preview.second) {
				if (conversation.isRead()) {
					mLastMessage.setTypeface(null, Typeface.ITALIC);
				} else {
					mLastMessage.setTypeface(null,Typeface.BOLD_ITALIC);
				}
			} else {
				if (conversation.isRead()) {
					mLastMessage.setTypeface(null,Typeface.NORMAL);
				} else {
					mLastMessage.setTypeface(null,Typeface.BOLD);
				}
			}
		}

		mTimestamp.setText(UIHelper.readableTimeDifference(activity,conversation.getLatestMessage().getTimeSent()));
		RoundedImageView profilePicture = (RoundedImageView) view.findViewById(R.id.conversation_image);
		loadAvatar(conversation,profilePicture);

        //TODO: TXTR CUSTOM
        RoundedImageView accountPicture = (RoundedImageView) view
                .findViewById(R.id.conversation_account_image);
        accountPicture.setImageBitmap(activity.avatarService().get(
                conversation.getAccount(), activity.getPixel(24)));

        //Determine a border color for the account image (to prevent confusion when two accounts have the same image)
        profilePicture.setBorderColor(conversation.getAccount().getAccountColor());
        accountPicture.setBorderColor(conversation.getAccount().getAccountColor());

		return view;
	}

	class BitmapWorkerTask extends AsyncTask<Conversation, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private Conversation conversation = null;

		public BitmapWorkerTask(ImageView imageView) {
			imageViewReference = new WeakReference<>(imageView);
		}

		@Override
		protected Bitmap doInBackground(Conversation... params) {
			return activity.avatarService().get(params[0], activity.getPixel(56));
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					imageView.setBackgroundColor(0x00000000);
				}
			}
		}
	}

	public void loadAvatar(Conversation conversation, ImageView imageView) {
		if (cancelPotentialWork(conversation, imageView)) {
			final Bitmap bm = activity.avatarService().get(conversation, activity.getPixel(56), true);
			if (bm != null) {
				imageView.setImageBitmap(bm);
				imageView.setBackgroundColor(0x00000000);
			} else {
				imageView.setBackgroundColor(UIHelper.getColorForName(conversation.getName()));
				imageView.setImageDrawable(null);
				final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
				final AsyncDrawable asyncDrawable = new AsyncDrawable(activity.getResources(), null, task);
				imageView.setImageDrawable(asyncDrawable);
				try {
					task.execute(conversation);
				} catch (final RejectedExecutionException ignored) {
				}
			}
		}
	}

	public static boolean cancelPotentialWork(Conversation conversation, ImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final Conversation oldConversation = bitmapWorkerTask.conversation;
			if (oldConversation == null || conversation != oldConversation) {
				bitmapWorkerTask.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}
}

