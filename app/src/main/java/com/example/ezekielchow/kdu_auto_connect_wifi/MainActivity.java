package com.example.ezekielchow.kdu_auto_connect_wifi;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity {

    private String MyPreferencesName = "SavedUsernameID";
    private String MyUsernameStored = "usernameStored";
    private String MyPasswordStored = "passwordStored";
    private EditText usernameEditText;
    private EditText passwordEditText;
    private String WifiLog = "";
    private String WifiStatus = "";
    private String KDUStudentSSID = "Kdu-Student";
    private WifiManager wifiManager;
    private WifiScanReceiver wifiScanReceiver;
    private String testSSID = "WiFi@McD";
    private WifiConfiguration wifiConfiguration;
    private WifiInfo wifiInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initializing variables
        usernameEditText = (EditText)findViewById(R.id.usernameEditText);
        passwordEditText = (EditText)findViewById(R.id.passwordEditText);
        wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        SharedPreferences sharedPreference = getSharedPreferences(MyPreferencesName, MODE_PRIVATE);

        //username and password existed
        if(sharedPreference.contains(MyUsernameStored) && sharedPreference.contains(MyPasswordStored))
        {
            String username = sharedPreference.getString(MyUsernameStored, null);
            String password = sharedPreference.getString(MyPasswordStored, null);

            usernameEditText.setText(username);
            passwordEditText.setText(password);
        }
        else //No old username and password
        {}

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onDestroy()
    {
        super.onDestroy();

        SharedPreferences.Editor sharedPrefEditor = getSharedPreferences(MyPreferencesName, MODE_PRIVATE).edit();

        sharedPrefEditor.putString(MyUsernameStored, usernameEditText.getText().toString());
        sharedPrefEditor.putString(MyPasswordStored, passwordEditText.getText().toString());

        sharedPrefEditor.commit();
    }

    public void loginBtnClicked(View view)
    {
        //if details are missing
        if(usernameEditText.length() == 0 || passwordEditText.length() == 0)
        {
            Toast.makeText(MainActivity.this, "Please Complete The Form", Toast.LENGTH_SHORT).show();
        }
        else
        {
            SharedPreferences.Editor sharedPrefEditor = getSharedPreferences(MyPreferencesName, MODE_PRIVATE).edit();

            sharedPrefEditor.putString(MyUsernameStored, usernameEditText.getText().toString());
            sharedPrefEditor.putString(MyPasswordStored, passwordEditText.getText().toString());

            sharedPrefEditor.commit();

            if (!checkWifiStatus()) //wifi is disabled
            {
                Toast.makeText(MainActivity.this, WifiStatus, Toast.LENGTH_SHORT).show();
            }
            else //wifi is enabled
            {
                //KDU wifi not found
                if (!scanForKDUWifi())
                {
                    Log.d(WifiLog, "Unable to Scan for wifi or KDU wifi doesnt exist");
                    Toast.makeText(MainActivity.this, "KDU wifi not found", Toast.LENGTH_SHORT).show();
                }
                else //KDU wifi found
                {
                    ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                    NetworkInfo theWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                    if (theWifi.isConnectedOrConnecting()) //if wifi already connected to kdu network
                    {
                        String extraInfo = theWifi.getExtraInfo();
                        String ssidWithQuotes = "\"" + testSSID + "\"";

                        if (!(extraInfo.equals(ssidWithQuotes))) //connected to wrong wifi
                        {
                            Toast.makeText(MainActivity.this, "Connected To Wrong Wifi", Toast.LENGTH_SHORT).show();
                        }
                        else //Connected to KDU already. Login into the page displayed
                        {
                            Toast.makeText(MainActivity.this, "Connected To KDU Wifi", Toast.LENGTH_SHORT).show();

                            //get redirected url and login
                            new getRedirectedURL().execute();
                            Toast.makeText(MainActivity.this, "Success! You Are Logged In!", Toast.LENGTH_SHORT).show();

                        }
                    }
                    else //connect to KDU Wifi
                    {
                        boolean connectionToKDUWIfi = connectToKDUWifi();

                        if (connectionToKDUWIfi) //Connected to KDU. Login into page
                        {
                            //get redirected url and login
                            new getRedirectedURL().execute();
                            Toast.makeText(MainActivity.this, "Success! You Are Logged In!", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            Toast.makeText(MainActivity.this, "Something Went Wrong. Please contact administrator", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
            }
        }
    }

    //check whether wifi is enabled
    public boolean checkWifiStatus()
    {
        if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED)
        {
            WifiStatus = "WiFi Enabled";
            return true;
        }
        else
        {
            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLING)
            {
                WifiStatus = "Enabling WiFi";
            }
            else if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED)
            {
                WifiStatus = "Please Enable Your WiFi";
            }
            else if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLING)
            {
                WifiStatus = "You Are Disabling Your WiFi";
            }
            else
            {
                Log.d(WifiLog, "Something Went Wrong in checkWifiStatus");
            }
            return false;
        }
    }

    //
    public boolean scanForKDUWifi()
    {
        wifiScanReceiver = new WifiScanReceiver();
        registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (wifiManager.startScan())
        {
            List<android.net.wifi.ScanResult> wifiScanList = wifiManager.getScanResults();

            //search for network
            int foundNetwork = 0;
            for (int i = 0;i < wifiScanList.size(); i++)
            {
                String concurrentNetwork = wifiScanList.get(i).SSID;

                if (concurrentNetwork.equals(testSSID))
                {
                    Log.d(WifiLog, "comparison success");
                    foundNetwork = 1;
                }
            }

            if (foundNetwork == 1) //if network found
            {
                return true;
            }
            else //if network not found
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public boolean connectToKDUWifi()
    {
        Log.d(WifiLog, "Network FOund!, Next Step");
        wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + testSSID + "\"";
        //wifiConfiguration.preSharedKey = "\"" + "getyourassbackhome" + "\"";
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        wifiManager.addNetwork(wifiConfiguration);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + testSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }

        wifiInfo = wifiManager.getConnectionInfo();
        String ssidOfWifiConnected = wifiInfo.getSSID();
        Log.d(WifiLog, "the connected wifi" + ssidOfWifiConnected);
        String ssidWithQuotes = "\"" + testSSID + "\"";

        if (!(ssidOfWifiConnected.equals(ssidWithQuotes)))
        {
            Log.d(WifiLog, "Failed to connect");
            return false;
        }
        else
        {
            Log.d(WifiLog, "Succesfully connected");
            return true;
        }
    }

    class getRedirectedURL extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params)
        {
            try{
                //get redirected url
                String myURL = "http://173.194.120.105/generate_204";
                URL url = new URL("http://173.194.120.105/generate_204");
                URLConnection con = url.openConnection();
                System.out.println("orignal url: " + con.getURL());
                con.connect();
                System.out.println("connected url: " + con.getURL());
                InputStream is = con.getInputStream();
                Log.d(WifiLog, "redirected url: " + con.getURL());
                String redirectedURL = con.getURL().toString();
                is.close();

                //make http request to login
                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(redirectedURL);

                SharedPreferences sharedPreference = getSharedPreferences(MyPreferencesName, MODE_PRIVATE);

                String username = "";
                String password = "";
                //username and password existed
                if(sharedPreference.contains(MyUsernameStored) && sharedPreference.contains(MyPasswordStored))
                {
                    username = sharedPreference.getString(MyUsernameStored, null);
                    password = sharedPreference.getString(MyPasswordStored, null);
                }
                else //Obvoiusly something went wronggg. BUGSSS
                {
                    Toast.makeText(MainActivity.this, "U&P not detected, please contact administrator", Toast.LENGTH_SHORT).show();
                }
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("user", username));
                nameValuePairs.add(new BasicNameValuePair("password", password));

                //Encoding POST data
                try {
                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                } catch (UnsupportedEncodingException e) {
                    // log exception
                    e.printStackTrace();
                }

                //making POST request
                try {
                    HttpResponse response = httpClient.execute(httpPost);
                    // write response to log
                    Log.d("Http Post Response:", response.toString());
                } catch (ClientProtocolException e) {
                    // Log exception
                    e.printStackTrace();
                } catch (IOException e) {
                    // Log exception
                    e.printStackTrace();
                }

            } catch (Exception e) {
                Log.d(WifiLog, "Error getting url: ", e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result)
        {
            super.onPostExecute(result);
        }
    }

    class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
        }
    }

}
