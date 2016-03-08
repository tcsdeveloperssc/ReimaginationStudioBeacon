package com.tcs.santaclara.reimaginationstudiobeacon;

import android.content.DialogInterface;
import android.content.IntentSender;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.NearbyMessagesStatusCodes;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.android.gms.nearby.messages.SubscribeCallback;
import com.google.android.gms.nearby.messages.SubscribeOptions;

public class HomeActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    public static final String TAG = "HomeActivity";
    private MessageListener beaconListener = null;
    private GoogleApiClient apiClient = null;
    private boolean mResolvingNearbyPermissionError = false;
    private boolean mResolvingError = false;
    private TextView displayTextView = null;
    private WebView beaconWebView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        displayTextView = (TextView) findViewById(R.id.beaconMessage);

        //create listener to process beacon messages
        beaconListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                // Do something with the message.
                Log.i(TAG, "Message found: " + message);
                Log.i(TAG, "Message string: " + new String(message.getContent()));
                Log.i(TAG, "Message namespaced type: " + message.getNamespace() +
                        "/" + message.getType());

                String payload = new String(message.getContent());
                displayTextView.setText(payload);

                beaconWebView = (WebView) findViewById(R.id.beaconWebView);
                beaconWebView.loadUrl(payload);

            }

            // Called when a message is no longer detectable nearby.
            public void onLost(Message message) {
                // Take appropriate action here (update UI, etc.)
            }
        };

        //create google api client
        apiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        // Connect the GoogleApiClient.
        if (!apiClient.isConnected()) {
            if (!apiClient.isConnecting()) {
                apiClient.connect();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
            Nearby.Messages.unsubscribe(apiClient, beaconListener);
            apiClient.disconnect();
        }
        catch(Exception e)
        {
            Log.i(TAG, "Error in onStop: " +e.getMessage());
        }

    }

    public void subscribeToBeacon(View view)
    {
        // Subscribe to receive messages.
        Log.i(TAG, "Trying to subscribe.");

//        else {
            SubscribeOptions options = new SubscribeOptions.Builder()
                    .setStrategy(Strategy.BLE_ONLY)
                    .setCallback(new SubscribeCallback() {
                        @Override
                        public void onExpired() {
                            Log.i(TAG, "No longer subscribing.");
                        }
                    }).build();

            Nearby.Messages.subscribe(apiClient, beaconListener, options)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                Log.i(TAG, "Subscribed successfully.");
                            } else {
                                Log.i(TAG, "Could not subscribe.");
                                // Check whether consent was given;
                                // if not, prompt the user for consent.
                                handleUnsuccessfulNearbyResult(status);
                            }
                        }
                    });
        }
//    }

    private void handleUnsuccessfulNearbyResult(Status status) {
        Log.i(TAG, "processing error, status = " + status);
        if (status.getStatusCode() == NearbyMessagesStatusCodes.APP_NOT_OPTED_IN) {
            if (!mResolvingNearbyPermissionError) {
                try {
                    mResolvingNearbyPermissionError = true;
                    status.startResolutionForResult(this,
                            Constants.REQUEST_RESOLVE_ERROR);

                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        } else {
            if (status.getStatusCode() == ConnectionResult.NETWORK_ERROR) {
                Toast.makeText(this.getApplicationContext(),
                        "No connectivity, cannot proceed. Fix in 'Settings' and try again.",
                        Toast.LENGTH_LONG).show();
//                resetToDefaultState();
            } else {
                // To keep things simple, pop a toast for all other error messages.
                Toast.makeText(this.getApplicationContext(), "Unsuccessful: " +
                        status.getStatusMessage(), Toast.LENGTH_LONG).show();
            }

        }
    }

    protected void finishedResolvingNearbyPermissionError() {
        mResolvingNearbyPermissionError = false;
    }

    public void unsubscribeFromBeacon(View view)
    {
        try
        {
            Nearby.Messages.unsubscribe(apiClient, beaconListener);
            if(beaconWebView  != null)
                beaconWebView.loadUrl("about:blank");
        }
        catch(Exception e)
        {
            Log.i(TAG, "error in unsubscribe: " +e.getMessage());
        }

    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "APICLIENT Connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "APICLIENT Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "APICLIENT Connection Failed");
    }



}
