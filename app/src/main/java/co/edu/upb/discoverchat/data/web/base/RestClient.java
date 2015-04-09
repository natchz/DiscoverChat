package co.edu.upb.discoverchat.data.web.base;
import android.content.Context;
import android.util.Log;

import com.loopj.android.http.*;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.json.JSONObject;

/**
 * Created by hatsumora on 1/04/15.
 * Base for all rest clients
 */
public class RestClient {
    private static final String protocol = "https://";
    private static final String urlBase = "10.154.12.17";
    private static final String portNumber = ":443/";
    private static final String APPLICATION_JSON = "application/json";
    private static final String registrationPath = "users.json";
    private static String shipMessagePath = "messages/ship.json";

    public static String getRegistrationPath(){
        return registrationPath;}
    public static String getShipMessagePath(){
        return shipMessagePath;};

    private static AsyncHttpClient client = new AsyncHttpClient();
    private static SyncHttpClient syncHttpClient = new SyncHttpClient();

    public static void get(String url, RequestParams params, ResponseHandlerInterface responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }
    public static void post(Context context, String url, HttpEntity entity, final HandlerJsonRequest responseHandler){
        client.post(null,getAbsoluteUrl(url),entity, APPLICATION_JSON, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                if(responseHandler != null)
                    responseHandler.handleResponse(response);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.e("ERROR JSON:",throwable.getMessage());
                throwable.getCause().printStackTrace();
                if(responseHandler != null)
                    responseHandler.handleError(errorResponse.toString());
            }
        });


    }
//    public static void post(Context)
    private static String getAbsoluteUrl(String relativeUrl) {
        return protocol+urlBase+portNumber+relativeUrl;
    }
}
