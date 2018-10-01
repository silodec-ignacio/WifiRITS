package com.silodec.wifirits;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.silodec.wifirits.model.RitsData;


import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;


import layout.AboutFragment;
import layout.ConnectFragment;
import layout.CustomiseFragment;
import layout.FindFragment;
import layout.IdleFragment;
import layout.RitsFragment;

import static com.silodec.wifirits.BasicImageDownloader.savedDiff;
import static android.os.Build.VERSION_CODES.M;


public class MainActivity extends AppCompatActivity implements
        FindFragment.FindFragmentListener,
        CustomiseFragment.CustomFragmentListener,
        ConnectFragment.ConnectFragmentListener {

    public static final String WIFI_RITS = "WIFI_RITS";
    public static byte[] bteGetSavedDiff;

    BroadcastReceiver wifi_receiver;

    public enum MenuMode {
        MENU_MODE_IDLE, MENU_MODE_CONNECT, MENU_MODE_RITS, MENU_MODE_FIND, MENU_MODE_ABOUT, MENU_MODE_CUSTOM
    }

    public enum ConnectMode {
        CONNECT_DISCONNECTED, CONNECT_CONNECTING, CONNECT_CONNECTED
    }

    public static WifiManager wifi;
    public static ConnectivityManager network;

    private static RitsData mRits;

    private boolean mPreAppWifiConnected;
    private int mPreAppWifiNetworkId;

    private static final int REQUEST_ACCESS_LOCATION = 0x1234;

    private boolean permissionGranted = false;

    private static final int MENU_ABOUT = 1001;
    private static final int MENU_CUSTOM = 1002;
    private static final int MENU_FAVOURITE = 1003;
    private static final int MENU_BACK = 1004;
    private static final int MENU_VERSION = 1005;

    public static MenuMode mMenuMode;
    public static ConnectMode mConnectMode;

    public static final String FRAGMENT_IDLE = "fragment_idle";
    public static final String FRAGMENT_RITS = "fragment_rits";
    public static final String FRAGMENT_FIND = "fragment_find";
    public static final String FRAGMENT_CONNECT = "fragment_connect";
    public static final String FRAGMENT_CUSTOMISE = "fragment_customise";

    public static boolean mRitsHasFavourite = false;
    public static boolean ConnectionFailed = false;
    public static boolean isDevMode = false;

    private int tickerCounter;

    private Toolbar mToolbar;

    @RequiresApi(api = M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (!permissionGranted) {
            checkAccessLocationPermission();
        }
        setDiffNumber();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (mMenuMode == null) {
            mMenuMode = MenuMode.MENU_MODE_IDLE;
        }

        String filepath ="/storage/emulated/0/Android/data/com.silodec.wifirits/files/favSSID.txt";
        FileInputStream fis;

        try {
            fis = new FileInputStream(filepath);
            int length = (int) new File(filepath).length();
            if (length > 0){
                mRitsHasFavourite = true;
            }
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mConnectMode = ConnectMode.CONNECT_DISCONNECTED;

        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        showConnectedRitsId(false);

        IdleFragment fragment = new IdleFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragmentFrame, fragment, FRAGMENT_IDLE)
                .commit();

        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        network = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        mPreAppWifiConnected = wifi.isWifiEnabled();
        mPreAppWifiNetworkId = wifi.getConnectionInfo().getNetworkId();

        Log.i(WIFI_RITS, "onCreate: preConnect=" + mPreAppWifiConnected + " preNetworkId=" + mPreAppWifiNetworkId);

        if(mRitsHasFavourite){
            getFavouriteRits();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(WIFI_RITS, "onRestart: preConnect=" + mPreAppWifiConnected + " preNetworkId=" + mPreAppWifiNetworkId);
    }

    @Override
    public void onCustomFinishSave(boolean favourite) {
        mRitsHasFavourite = favourite;
        goBack();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(WIFI_RITS, "onStart");
        getDiffFromFile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(WIFI_RITS, "onResume");
//        Check if wifi connected each time we resume
        tickerCounter = 0;
        tickHandler.postDelayed(ticker, 1000);
        if (!wifi.isWifiEnabled()) wifi.setWifiEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        tickHandler.removeCallbacks(ticker);
    }



    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        String menuModeStr = "" + mMenuMode;
        Log.i("menuMode", menuModeStr);

        if (mMenuMode == MenuMode.MENU_MODE_IDLE) {
            try {
                FrameLayout idleFrameLayout = findViewById(R.id.idleFragmentFrame);

                idleFrameLayout.setVisibility(View.VISIBLE);

                if (idleFrameLayout.getVisibility() != View.INVISIBLE){
                    TextView noFavRitsText = findViewById(R.id.noFavRitsText);
                    TextView FavRitsTextFound = findViewById(R.id.FavRitsTextFound);
                    try {
                        if(mRitsHasFavourite){
                            noFavRitsText.setVisibility(View.INVISIBLE);
                            FavRitsTextFound.setVisibility(View.VISIBLE);
                            getFavouriteRits();
                        }else{
                            noFavRitsText.setVisibility(View.VISIBLE);
                            FavRitsTextFound.setVisibility(View.INVISIBLE);
                        }
                    }catch (NullPointerException e){
                        e.printStackTrace();
                    }
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }else{
            try {
                FrameLayout idleFrameLayout = findViewById(R.id.idleFragmentFrame);
                idleFrameLayout.setVisibility(View.INVISIBLE);
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
        Log.i(WIFI_RITS, "onResumeFragments");
    }



    @Override
    public void onStop() {
        super.onStop();
        Log.i("WIFI_RITS", "onStop");
    }

    @Override
    protected void onDestroy() {
//        Before app closes, ensure any RITS units are disconnected from Wifi network and forget
        int networkId = wifi.getConnectionInfo().getNetworkId();
        wifi.disconnect();
        if(networkId != -1) wifi.removeNetwork(networkId);

//        If device was connected to a Wifi network before app was started, restore network connection
        if(mPreAppWifiConnected) {
            wifi.enableNetwork(mPreAppWifiNetworkId, true);
        } else {
            wifi.setWifiEnabled(false);
        }
        if (wifi_receiver != null) {
            unregisterReceiver(wifi_receiver);
        }
        Log.i(WIFI_RITS, "onDestroy");
        super.onDestroy();
    }

    private void showConnectedRitsId(boolean hasRits) {
        int tbColor = 0;
        if (!hasRits) {
            tbColor = ContextCompat.getColor(this, R.color.colorPrimary);
        } else {
            switch (mConnectMode) {
                case CONNECT_DISCONNECTED:
                    tbColor = ContextCompat.getColor(this, R.color.colorToolbarDisconnectedText);
                    break;
                case CONNECT_CONNECTING:
                    tbColor = ContextCompat.getColor(this, R.color.colorToolbarConnectingText);
                    break;
                case CONNECT_CONNECTED:
                    tbColor = ContextCompat.getColor(this, R.color.colorToolbarConnectedText);
                    break;
            }
        }
//BM
        TextView tvDriverId = mToolbar.findViewById(R.id.toolbar_driver_id);
        TextView tvTruckId = mToolbar.findViewById(R.id.toolbar_truck_id);

        tvDriverId.setTextColor(tbColor);
        tvTruckId.setTextColor(tbColor);

        if (hasRits) {
            tvDriverId.setText(mRits.getRitsDriverName());
            tvTruckId.setText(mRits.getRitsTruckNumber());
        } else {
            tvDriverId.setText("");
            tvTruckId.setText("");
        }
    }

    @RequiresApi(api = M)
    private boolean checkAccessLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ACCESS_LOCATION);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ACCESS_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionGranted = true;
                    Toast.makeText(this, "Yay, we can spy on you!\nJust kidding!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Sorry, but the Location service is required\nto search for WifiRITS units", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
//BM
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
// hideStatusBar doesn't work here
    try{
        switch (mConnectMode) {
            case CONNECT_DISCONNECTED:
                menu.findItem(R.id.action_connect).setTitle(R.string.menu_connect);
                break;
            case CONNECT_CONNECTING:
                menu.findItem(R.id.action_connect).setTitle(R.string.menu_connecting);
                try {
                    Button scanBTN = findViewById(R.id.buttonScan);
                    ConstraintLayout txtCont = findViewById(R.id.scanTextContainer);
                    menu.findItem(R.id.action_connect).setTitle(R.string.menu_connecting);
                    scanBTN.setText(R.string.mode_connecting_b);
                    scanBTN.setBackgroundColor(getResources().getColor(R.color.PrimaryDark));
                    scanBTN.setEnabled(false);
                    txtCont.setVisibility(View.GONE);
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                break;
            case CONNECT_CONNECTED:
                menu.findItem(R.id.action_connect).setTitle(R.string.menu_connected);
                break;
        }
    }catch(NullPointerException e){
        e.printStackTrace();
    }

        showConnectedRitsId(mRits != null);

// set drop down menus.
        switch (mMenuMode) {//when its not connected to rits

            case MENU_MODE_IDLE:
                onResumeFragments();

                if(!isDevMode){
                    try {
                        ProgressBar devHeartBeat = findViewById(R.id.heartBeat);
                        devHeartBeat.setVisibility(View.GONE);
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }

                if(!mRitsHasFavourite) {//Favorite is !set
                    if (menu.findItem(R.id.action_connect) != null) {
                        menu.removeItem(R.id.action_connect);
                    }
                }else{
                    if (menu.findItem(R.id.action_connect) != null) {
                        menu.removeItem(R.id.action_connect);
                    }
                }


                if (menu.findItem(MENU_ABOUT) == null) {
                    menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about);
                }
                if (menu.findItem(MENU_BACK) != null) {
                    menu.removeItem(MENU_BACK);
                }
                if (menu.findItem(MENU_FAVOURITE) != null) {
                    menu.removeItem(MENU_FAVOURITE);
                }
                if (menu.findItem(MENU_VERSION) == null) {
                    menu.add(Menu.NONE, MENU_VERSION, Menu.NONE, R.string.menu_version);
                }

                break;
            case MENU_MODE_RITS://when its connected to the Rits
                AppCompatTextView toolbarDriverId = findViewById(R.id.toolbar_driver_id);
                AppCompatTextView toolbarTruckId = findViewById(R.id.toolbar_truck_id);
                toolbarDriverId.setVisibility(View.VISIBLE);
                toolbarTruckId.setVisibility(View.VISIBLE);

                if(isDevMode){
                    try {
                        ProgressBar devHeartBeat = findViewById(R.id.heartBeat);
                        devHeartBeat.setVisibility(View.VISIBLE);
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }

                if (menu.findItem(R.id.action_find) != null) {
                    menu.removeItem(R.id.action_find);
                }
                if (menu.findItem(R.id.action_find) == null) {
                    menu.add(Menu.NONE, R.id.action_find, Menu.NONE, "Find");
                }
                if (menu.findItem(MENU_ABOUT) == null) {
                    menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, R.string.menu_about);
                }
                if (menu.findItem(MENU_BACK) != null) {
                    menu.removeItem(MENU_BACK);
                }
                if (menu.findItem(MENU_CUSTOM) == null) {
                    menu.add(Menu.NONE, MENU_CUSTOM, Menu.NONE, R.string.menu_custom);
                }
                if (!mRitsHasFavourite) {
                    if (menu.findItem(MENU_FAVOURITE) == null) {
                        menu.add(Menu.NONE, MENU_FAVOURITE, Menu.NONE, R.string.menu_favourite);
                        menu.removeItem(MENU_FAVOURITE);
                    }
                } else {
                    if (menu.findItem(MENU_FAVOURITE) != null) {
                        menu.removeItem(MENU_FAVOURITE);
                    }
                }
                if (menu.findItem(MENU_VERSION) == null) {
                    menu.add(Menu.NONE, MENU_VERSION, Menu.NONE, R.string.menu_version);
                }
                break;
            case MENU_MODE_CONNECT:
                if(!isDevMode){
                    try {
                        ProgressBar devHeartBeat = findViewById(R.id.heartBeat);
                        devHeartBeat.setVisibility(View.GONE);
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }
            case MENU_MODE_FIND:
                AppCompatTextView toolbarDriverIdB = findViewById(R.id.toolbar_driver_id);
                AppCompatTextView toolbarTruckIdB = findViewById(R.id.toolbar_truck_id);

                if(!isDevMode){
                    try {
                        ProgressBar devHeartBeat = findViewById(R.id.heartBeat);
                        devHeartBeat.setVisibility(View.GONE);
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }

                try{
                    toolbarDriverIdB.setVisibility(View.GONE);
                    toolbarTruckIdB.setVisibility(View.GONE);

                }catch (NullPointerException e){
                    e.printStackTrace();
                }

                if (menu.findItem(R.id.action_connect) != null) {
                    menu.removeItem(R.id.action_connect);
                }
                if (menu.findItem(R.id.action_find) != null) {
                    menu.removeItem(R.id.action_find);
                }
            case MENU_MODE_CUSTOM:

                if(!isDevMode){
                    try {
                        ProgressBar devHeartBeat = findViewById(R.id.heartBeat);
                        devHeartBeat.setVisibility(View.GONE);
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }

                if (menu.findItem(R.id.action_connect) != null) {
                    menu.removeItem(R.id.action_connect);
                }
                if (menu.findItem(R.id.action_find) != null) {
                    menu.removeItem(R.id.action_find);
                }
            case MENU_MODE_ABOUT:

                if(!isDevMode){
                    try {
                        ProgressBar devHeartBeat = findViewById(R.id.heartBeat);
                        devHeartBeat.setVisibility(View.GONE);
                    }catch(NullPointerException e){
                        e.printStackTrace();
                    }
                }

                if (menu.findItem(R.id.action_connect) != null) {
                    menu.removeItem(R.id.action_connect);
                }
                if (menu.findItem(R.id.action_find) != null) {
                    menu.removeItem(R.id.action_find);
                }
                if (menu.findItem(MENU_FAVOURITE) != null) {
                    menu.removeItem(MENU_FAVOURITE);
                }
                if (menu.findItem(MENU_CUSTOM) != null) {
                    menu.removeItem(MENU_CUSTOM);
                }
                if (menu.findItem(MENU_ABOUT) != null) {
                    menu.removeItem(MENU_ABOUT);
                }
                break;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_connect:
                if (mMenuMode == MenuMode.MENU_MODE_IDLE) {
                    if (mRits == null) {
                        if (mRitsHasFavourite) {
                            getFavouriteRits();
                            connectToRits();
                        } else {
                            findAvailableRitsUnits();
                        }
                    } else {
                        connectToRits();
                    }
                }
                if (mMenuMode == MenuMode.MENU_MODE_RITS) {
                    mConnectMode = ConnectMode.CONNECT_DISCONNECTED;
                    mMenuMode = MenuMode.MENU_MODE_IDLE;
                    WifiInfo wifiInfo = wifi.getConnectionInfo();
                    wifi.disconnect();
                    wifi.removeNetwork(wifiInfo.getNetworkId());
                    showConnectedRitsId(true);
                    IdleFragment idleFragment = new IdleFragment();
                    getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragmentFrame, idleFragment, FRAGMENT_IDLE)
                            .commit();
                }

                break;
            case R.id.action_find:
                if (mMenuMode == MenuMode.MENU_MODE_RITS || mMenuMode == MenuMode.MENU_MODE_IDLE) {
                    findAvailableRitsUnits();
                    mConnectMode = ConnectMode.CONNECT_DISCONNECTED;
                }
                break;
            case MENU_ABOUT:
                try {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("http://192.168.4.1/help.htm"));
                        startActivity(intent);
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }catch (ActivityNotFoundException e){
                    e.printStackTrace();
                }
            case MENU_CUSTOM:
                mMenuMode = MenuMode.MENU_MODE_CUSTOM;
                Bundle arguments = new Bundle();
                arguments.putBoolean(CustomiseFragment.FAVOURITE_KEY, mRitsHasFavourite);

                CustomiseFragment customiseFragment = new CustomiseFragment();
                customiseFragment.setArguments(arguments);

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentFrame, customiseFragment, FRAGMENT_CUSTOMISE)
                        .commit();

                break;
            case MENU_FAVOURITE:
                mRitsHasFavourite = true;
                Toast.makeText(this, "Favourite is set", Toast.LENGTH_SHORT).show();
                setFavouriteRits();
                break;
            case MENU_BACK:
                goBack();
                break;
        }

        if (id != MENU_BACK) invalidateOptionsMenu();

        return super.onOptionsItemSelected(item);
    }
//BM

    public void setFavouriteRits(){

        String filepath ="/storage/emulated/0/Android/data/com.silodec.wifirits/files/favSSID.txt";
        FileOutputStream fos;
        try {
            fos = new  FileOutputStream(filepath);
        //declare bytes to wright
            byte[] ritsSSID = mRits.getRitsSSID().getBytes();
        //wright declared bytes
            fos.write(ritsSSID, 0, ritsSSID.length);
            fos.close();
            setDiffNumber();
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public void setDiffNumber(){
        String filepath = "/storage/emulated/0/Android/data/com.silodec.wifirits/files/picDiff.txt";
        FileOutputStream fos;
        String strSavedDiff = "" + savedDiff;
        try {
            fos = new FileOutputStream(filepath);
        //declare bytes to wright
            byte[] byteSavedDiff = strSavedDiff.getBytes();
        //wright declared bytes
            fos.write(byteSavedDiff,0 , byteSavedDiff.length);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        getDiffFromFile();
    }

    private void getFavouriteRits() {

        Log.i("FIS", "getFavouriteRitsReached");

        String filepath = "/storage/emulated/0/Android/data/com.silodec.wifirits/files/favSSID.txt";
        FileInputStream fis;

        try {
            fis = new FileInputStream(filepath);
            int length = (int) new File(filepath).length();
            byte[] buffer = new byte[length];
            fis.read(buffer, 0, length);

            String stringSSID = "" + buffer;
            mRits.setRitsSSID(stringSSID);

            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getDiffFromFile() {

        Log.i("FIS", "getDiffReached");

        String filepath ="/storage/emulated/0/Android/data/com.silodec.wifirits/files/picDiff.txt";
        FileInputStream fis;

        try {
            fis = new FileInputStream(filepath);

            int length = (int) new File(filepath).length();
            byte[] buffer = new byte[length];
            fis.read(buffer, 0, length);
            bteGetSavedDiff = buffer;
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String strTestA = "" + bteGetSavedDiff;
        Log.i("FIS", strTestA);
    }

    @Override
    public void onBackPressed() {
        if (mMenuMode == MenuMode.MENU_MODE_IDLE) {
            super.onBackPressed();
        } else {
            goBack();
        }
    }
//BM: BUG: reconnection bug
    private void goBack() {
        // TODO fix this when reconnecting with known rits from idle

        if (mMenuMode == MenuMode.MENU_MODE_RITS || mMenuMode == MenuMode.MENU_MODE_IDLE) return;
        if (mConnectMode == ConnectMode.CONNECT_CONNECTED) {
            showConnectedRitsId(true);
            toggleBTN(null,false);
            RitsFragment ritsFragment = RitsFragment.newInstance(mRits);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentFrame, ritsFragment, FRAGMENT_RITS)
                    .commit();
            mMenuMode = MenuMode.MENU_MODE_RITS;
        } else {
            IdleFragment idleFragment = new IdleFragment();
            toggleBTN(null,true);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentFrame, idleFragment, FRAGMENT_IDLE)
                    .commit();
            mMenuMode = MenuMode.MENU_MODE_IDLE;
        }
        invalidateOptionsMenu();
    }

    private void connectToRits() {
        mConnectMode = ConnectMode.CONNECT_CONNECTING;
        mMenuMode = MenuMode.MENU_MODE_CONNECT;
        ConnectFragment connectFragment = ConnectFragment.newInstance(mRits);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentFrame, connectFragment, FRAGMENT_CONNECT)
                .commit();
    }

    private void findAvailableRitsUnits() {
        mMenuMode = MenuMode.MENU_MODE_FIND;
        FindFragment findFragment = new FindFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentFrame, findFragment, FRAGMENT_FIND)
                .commit();
    }


//BM: this could be useful for when it fails to connect, possibly add a message on screen saying, this it failed to connect

    public void toggleBTN(Menu menu, boolean mode){

        try {
            MenuItem findBTN = menu.getItem(R.id.action_find);
            findBTN.setVisible(mode);
        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }



    @Override
    public void onCustomFinishQuit() {
        goBack();
    }


    @Override
    public void onConnectFinished(boolean connectOk) {
        if (connectOk) {
            mConnectMode = ConnectMode.CONNECT_CONNECTED;
        } else {
            mConnectMode = ConnectMode.CONNECT_DISCONNECTED;
            mRits = null;
            ConnectionFailed = true;
        }
        goBack();
    }

    @Override
    public void onFindFinishConnect(RitsData rits) {
        mRits = rits;
        connectToRits();
        invalidateOptionsMenu();
    }

    @Override
    public void onFindFinishQuit() {
        mConnectMode = ConnectMode.CONNECT_DISCONNECTED;
        goBack();
    }

    public static void updateRitsFromFragment(RitsData rits) {
        mRits = rits;
    }

    private final Handler tickHandler = new Handler();
    private Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tickerCounter++;
            if(tickerCounter >= 2){
                try {
                    if(ConnectionFailed) {
                        TextView txtScan = findViewById(R.id.idleScanningText);
                        txtScan.setText(R.string.error_connectionFailed);
                        findViewById(R.id.idleScanTextContainer).setVisibility(View.VISIBLE);
                    }
                }catch(NullPointerException e){
                    e.printStackTrace();
                }
            }
            tickHandler.postDelayed(this, 1000);
        }
    };
}
