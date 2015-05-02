package co.edu.upb.discoverchat.data.web;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.loopj.android.http.RequestParams;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import co.edu.upb.discoverchat.data.db.ChatsManager;
import co.edu.upb.discoverchat.data.db.ImageMessagesManager;
import co.edu.upb.discoverchat.data.db.ReceiversManager;
import co.edu.upb.discoverchat.data.db.TextMessagesManager;
import co.edu.upb.discoverchat.data.db.UserManager;
import co.edu.upb.discoverchat.data.db.base.DbBase;
import co.edu.upb.discoverchat.data.provider.ContactProvider;
import co.edu.upb.discoverchat.data.web.base.HandlerJsonRequest;
import co.edu.upb.discoverchat.data.web.base.RestClient;
import co.edu.upb.discoverchat.models.Chat;
import co.edu.upb.discoverchat.models.Image;
import co.edu.upb.discoverchat.models.ImageMessage;
import co.edu.upb.discoverchat.models.Message;
import co.edu.upb.discoverchat.models.Receiver;
import co.edu.upb.discoverchat.models.TextMessage;

/**
 * Created by hatsumora on 4/04/15.
 * Allows recive and send data to the Drake server
 */
public class MessageWeb extends RestClient {
    private Context context;
    ReceiversManager receiversManager;
    ChatsManager chatsManager;
    public MessageWeb(Context context){
        this.context = context;
    }

    public long receiveMessage(Bundle extras){
        chatsManager = new ChatsManager(context);
        receiversManager = new ReceiversManager(context);

        Receiver receiver;
        receiver = receiversManager.findBy(ReceiversManager.FIELD_PHONE, extras.get("receiver"));
        if(receiver==null){
            receiver = receiverFromBundle(extras);
        }
        Chat chat = chatsManager.getOneChatFor(receiver);
        HashMap<String, Object> udpateNew = new HashMap<>();
        udpateNew.put(DbBase.FIELD_READED, true);
        chatsManager.update(udpateNew, DbBase.KEY_ID + " = ?", ""+chat.getId());
        chat.setHasNewMessages(true);

        if(extras.getString("type").equals("text")){
            TextMessagesManager textMessagesManager = new TextMessagesManager(context);
            TextMessage textMessage = new TextMessage();
            textMessage.setSent(true);
            textMessage.setContent(extras.getString("content"));
            if(extras.containsKey("group_id")){
                //ToDO
                extras.getString("group_id");
            }else{
                textMessage.setReceiver(receiver);
                textMessage.setChat_id(chat.getId());
                textMessagesManager.add(textMessage);
                return textMessage.getId();
            }
        }else{
            ImageMessage imageMessage = new ImageMessage();
            ImageMessagesManager imageMessagesManager = new ImageMessagesManager(context);
            imageMessage.setSent(true);
            imageMessage.getImage().setUrl(extras.getString("image_url"));
            imageMessage.setReceiver(receiver);
            imageMessage.setChat_id(chat.getId());
            imageMessagesManager.add(imageMessage);
            return imageMessage.getId();
        }
        return -1;
    }
    /*Example of expected json request to /messages/ship
    {
        authentication_token: 'UxdNQfwsgZDW3S4vbFBw',
                messages: [
        {message: {to: 3142946469, content: "Mensaje de Luis David para aldo :)", type: "text"}}
        ]
    }*/
    public synchronized void sendTextMessage(Chat chat, Message message, final HandlerJsonRequest handlerJsonRequest){
        JSONObject request = new JSONObject();
        JSONArray messages = new JSONArray();
        ArrayList<Message> pendingMessages = new ArrayList<>();
        StringEntity entity = null;
        TextMessagesManager textMessagesManager = null;
        pendingMessages.add(message);
        if(message.getType() == Message.Type.TEXT){
            textMessagesManager = new TextMessagesManager(context);
            pendingMessages.addAll(textMessagesManager.getAllNotSend());
        }
        try {
            for(Message m: pendingMessages)
                messages.put(new JSONObject().put("message",m.toJson().put(FIELD_DESTINATION,textMessagesManager.phoneDestination(m))));

            addAuthenticate(request);
            request.put("messages",messages);
            entity = new StringEntity(request.toString());
            entity.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        }catch (Exception e){
            e.printStackTrace();
        }

        post(getShipMessagePath(),entity,handlerJsonRequest);
    }
    public synchronized void sendImageMessage(Chat chat, ImageMessage message, final HandlerJsonRequest handlerJsonRequest){
        UserManager userManager = new UserManager(context);

        ImageMessagesManager imageMessagesManager = new ImageMessagesManager(context);
        Image image = message.getImage();

        RequestParams params = new RequestParams();

        File imageFile = new File(image.getPath());
        try {
            RequestParams imageParam = new RequestParams();

            params.put(FIELD_IMAGE,imageFile);
            params.put(DbBase.FIELD_LONGITUDE,image.getLongitude());
            params.put(DbBase.FIELD_LATITUDE,image.getLatitude());
            params.put(FIELD_DESTINATION,imageMessagesManager.phoneDestination(message));
            params.put(FIELD_TYPE,Message.Type.IMAGE.name());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        params.put(DbBase.KEY_AUTHENTICATION_TOKEN,userManager.getToken());
        post(RestClient.getImageMessagePath(),params,handlerJsonRequest);
    }

    private Receiver receiverFromBundle(Bundle extras){
        Receiver receiver = new Receiver();
        ContactProvider contactProvider = new ContactProvider(context);
        receiver.setPhone(extras.get("receiver").toString());
        receiver.setName(contactProvider.nameOfPhone(receiver.getPhone()));
        receiversManager.add(receiver);
        return receiver;
    }
    private void addAuthenticate(JSONObject request) throws JSONException {
        UserManager userManager = new UserManager(context);
        request.put(DbBase.KEY_AUTHENTICATION_TOKEN, userManager.getToken());
    }
}
