package com.microsoft.felixrieseberg.azuretweets;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

// Azure Mobile App Service
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceJsonTable;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;

import static com.microsoft.windowsazure.mobileservices.table.query.QueryOperations.*;

public class TimelineActivity extends Activity {

    private ArrayList<Tweet> tweetList = new ArrayList<Tweet>();
    private TweetAdapter tweetAdapter;

    private MobileServiceClient mobileServiceClient;
    private MobileServiceTable<Tweet> mobileServiceTable;

    private EditText tweetInput;
    private EditText authorInput;
    private ListView tweetsListView;
    private Button tweetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        // Setup the client
        try {
            mobileServiceClient = new MobileServiceClient(
                    "https://droidcon.azure-mobile.net/",
                    "AnWuiRDhlJkkUCXyAICtIpQCfmzxKL53",
                    this
            );
            mobileServiceTable = mobileServiceClient.getTable(Tweet.class);
        } catch (java.net.MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // Get our input elements
        tweetInput = (EditText) findViewById(R.id.tweetInput);
        authorInput = (EditText) findViewById(R.id.authorInput);
        tweetsListView = (ListView) findViewById(R.id.tweetListView);

        // Bind the list of tweets to the adapter
        tweetAdapter = new TweetAdapter(this, tweetList);
        tweetsListView.setAdapter(tweetAdapter);

        // Get some tweets, yo
        GetTweets();
    }

    public void AddTweet() {
        if (mobileServiceClient == null) {
            return;
        }

        // Create the Tweet object
        final Tweet item = new Tweet();
        item.text = tweetInput.getText().toString();
        item.author = authorInput.getText().toString();

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final Tweet entity = (Tweet) mobileServiceTable.insert(item).get();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tweetAdapter.add(entity);
                        }
                    });
                } catch (Exception e){
                    createAndShowDialog(e, "Error");
                }

                return null;
            }
        }.execute();

        tweetInput.setText("");
    }

    public void AddTweet(View view) {
        AddTweet();
    }

    public void GetTweets() {
        if (mobileServiceClient == null) {
            return;
        }

        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    final List<Tweet> results = mobileServiceTable.execute().get();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tweetAdapter.clear();

                            for(Tweet tweet : results){
                                tweetAdapter.add(tweet);
                            }
                        }
                    });
                } catch (Exception e){
                    createAndShowDialog(e, "Error");
                }

                return null;
            }
        }.execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_timeline, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {
            GetTweets();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param exception
     *            The exception to show in the dialog
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    /**
     * Creates a dialog and shows it
     *
     * @param message
     *            The dialog message
     * @param title
     *            The dialog title
     */
    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }
}