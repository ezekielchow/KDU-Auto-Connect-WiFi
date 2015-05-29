package com.example.ezekielchow.kdu_auto_connect_wifi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.net.wifi.WifiManager;

import java.lang.reflect.Array;
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
    private String testSSID = "optik";

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
            if (!checkWifiStatus()) //wifi is disabled
            {
                Toast.makeText(MainActivity.this, WifiStatus, Toast.LENGTH_SHORT).show();
            }
            else //wifi is enabled
            {
                Toast.makeText(MainActivity.this, WifiStatus, Toast.LENGTH_SHORT).show();

                //scan for available networks
                if (!scanForKDUWifi())
                {
                    Log.d(WifiLog, "Unable to Scan for wifi or KDU wifi doesnt exist");
                }
                else
                {
                    Log.d(WifiLog, "Network FOund!, Next Step");
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

    class WifiScanReceiver extends BroadcastReceiver
    {
        public void onReceive(Context c, Intent intent)
        {
        }
    }

}
