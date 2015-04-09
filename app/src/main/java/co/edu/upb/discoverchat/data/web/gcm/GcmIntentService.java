package co.edu.upb.discoverchat.data.web.gcm;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import co.edu.upb.discoverchat.data.db.base.DbBase;
import co.edu.upb.discoverchat.data.web.MessageWeb;
import co.edu.upb.discoverchat.views.message.MessageActivity;
import co.edu.upb.discoverchat.R;

/**
 * Created by hatsumora on 3/04/15.
 * This class handles the incoming data from the Google servers
 */
public class GcmIntentService extends IntentService {

    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private static Messenger mMessenger;
    private String TAG = "GcmIntent";
    public GcmIntentService() {
        super("GcmIntentService");
    }

    public static void setmMessenger(Messenger _mMessenger) {
        mMessenger = _mMessenger;
        Log.e("Service: ", "Binded a new Messenger"+_mMessenger.toString());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("Service: ", "Stoped");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("Service: ", "Running");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work
                // En los extras: {receiver: 3142946469, content: "Un mensaje bien chingon", type: text}.
                MessageWeb web = new MessageWeb(this);
                long id = web.receiveMessage(extras);
                // Post notification of received message.
                extras.putLong(DbBase.KEY_MESSAGE_ID, id);
                if(updateGUI(extras))
                    sendNotification(extras.getString("content"));
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private boolean updateGUI(Bundle extras) {
        if(extras.getLong(DbBase.KEY_MESSAGE_ID)>0) {
            if(mMessenger!=null){
                Messenger messenger = mMessenger;
                Message message = Message.obtain();
                message.setData(extras);
                try {
                    messenger.send(message);
                    return false;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            Object o = extras.get(MessageActivity.MESSENGER);
        }
        return true;
    }

    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MessageActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_action_chat)
                        .setContentTitle("DiscoverChat")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}
