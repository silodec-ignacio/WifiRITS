package layout;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowId;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.silodec.wifirits.BasicImageDownloader;
import com.silodec.wifirits.JSONParser;
import com.silodec.wifirits.MainActivity;
import com.silodec.wifirits.R;
import com.silodec.wifirits.model.RitsData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import static com.silodec.wifirits.MainActivity.ConnectMode.CONNECT_CONNECTED;
import static com.silodec.wifirits.MainActivity.ConnectMode.CONNECT_DISCONNECTED;
import static com.silodec.wifirits.MainActivity.FRAGMENT_IDLE;
import static com.silodec.wifirits.MainActivity.WIFI_RITS;
import static com.silodec.wifirits.MainActivity.mConnectMode;
import static com.silodec.wifirits.MainActivity.mMenuMode;
import static com.silodec.wifirits.model.RitsData.PARCEL_RITS_KEY;

public class RitsFragment extends Fragment {

    private final String TAG = this.getClass().getSimpleName();

    private String mDriver;
    private String mFleet;
    private int mChannelInfo[];

    public String mFavWifiRITS = MainActivity.wifi.getConnectionInfo().getSSID();

    RitsData rits = new RitsData();

    public String mChannel1;
    public String mChannel2;
    public String mChannel3;
    public String mChannel4;

    private int tickerCounter;
    private ImageView mImageView;
    private RitsData mRits = null;


    String captureChan1;
    String captureChan2;
    String captureChan3;
    String captureChan4;

    int stagNationDoubler1;
    int stagNationDoubler2;
    int stagNationDoubler3;
    int stagNationDoubler4;

    String savedData1 = "0 kg";
    String savedData2 = "0 kg";
    String savedData3 = "0 kg";
    String savedData4 = "0 kg";

    FrameLayout idleFrameLayout;

    boolean saveData1;
    boolean saveData2;
    boolean saveData3;
    boolean saveData4;

    boolean isFavouriteRits;

    String[] mChan = {mChannel1, mChannel2, mChannel3, mChannel4};
    String[] captureChan = {captureChan1, captureChan2, captureChan3, captureChan4};
    String[] savedData = {savedData1, savedData2, savedData3, savedData4};
    String[] getChan = {"chan1", "chan2", "chan3", "chan4"};
    boolean[] saveData = {saveData1,saveData2,saveData3,saveData4};
    int[] stagNationDoubler = {stagNationDoubler1,stagNationDoubler2,stagNationDoubler3,stagNationDoubler4};
    int checkDataDoubler = 0;

    public String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FavRITS";

    private FindFragment.FindFragmentListener mListener;


    public RitsFragment() {
    }

    public static RitsFragment newInstance(RitsData rits) {

        Bundle args = new Bundle();
        args.putParcelable(PARCEL_RITS_KEY, rits);

        RitsFragment fragment = new RitsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof FindFragment.FindFragmentListener)) throw new AssertionError();
        mListener = (FindFragment.FindFragmentListener) context;
    }


    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View ritsView = inflater.inflate(R.layout.fragment_rits, container, false);

        mRits = getArguments().getParcelable(PARCEL_RITS_KEY);

        mImageView = ritsView.findViewById(R.id.imageRits);

        FrameLayout idleFrameLayout =  ritsView.findViewById(R.id.idleFragmentFrame);

        getRitsConfig();

        getRitsImage();
        getRitsData();
        isFavouriteRits();

        mRits.setRitsDriverName(mDriver);
        mRits.setRitsTruckNumber(mFleet);

        MainActivity.updateRitsFromFragment(mRits);
        ActivityCompat.invalidateOptionsMenu(getActivity());
        return ritsView;



    }



    public boolean isFavouriteRits(){
        if(mDriver == null && mFleet == null){

            isFavouriteRits = false;

            mMenuMode = MainActivity.MenuMode.MENU_MODE_IDLE;
            mConnectMode = MainActivity.ConnectMode.CONNECT_DISCONNECTED;

            IdleFragment idleFragment = new IdleFragment();
            Fragment newFragment = new Fragment();
            FragmentTransaction transaction = getFragmentManager().beginTransaction();

            transaction.replace(R.id.fragmentFrame, idleFragment, FRAGMENT_IDLE);
            transaction.addToBackStack(null);

            transaction.commit();

        }
        else{
            isFavouriteRits = true;
        }
        return isFavouriteRits;
    }

    public void quitScanning() {
        if (mListener == null) {
            throw new AssertionError();
        }
        //getActivity().unregisterReceiver(wifi_receiver);
        mListener.onFindFinishQuit();
    }


    public String FavGetRitsSSID() {
        rits.setRitsSSID(mFavWifiRITS);
        return mFavWifiRITS;
    }

    public String FavGetRitsDriverName() {
        rits.setRitsDriverName(mDriver);
        return mDriver;
    }

    public String FavGetRitsTruckNumber() {
        rits.setRitsTruckNumber(mFleet);
        return mFleet;
    }

    public String FavGetFriendlyRitsSSID() {
        String ritsFriendlySSID = mFavWifiRITS.substring(1);
        return ritsFriendlySSID;
    }



    @Override
    public void onResume() {
        super.onResume();
        tickerCounter = 0;
        tickHandler.postDelayed(ticker, 1000);
        Log.i("SSID<","onResume");
        Log.i("mFavWifiRITS<", mFavWifiRITS);
    }

    @Override
    public void onPause() {
        super.onPause();
        tickHandler.removeCallbacks(ticker);
    }

    private void getRitsConfig() {
        JSONParser jsonParser = new JSONParser();
        JSONObject json = jsonParser.getJSONFromUrl(getString(R.string.url_rits_config));
        mChannelInfo = new int[6];

        try {
            mDriver = json.getString("owner");
            mFleet = json.getString("fleet");

            JSONArray channelConfig = json.getJSONArray("ch_settings");
            for (int i = 1; i < 5; i++){
                int loc = 1;
                while(loc < 8) {
                    if (channelConfig.getInt(loc) == i) {
                        mChannelInfo[i] = loc;
                        break;
                    }
                    loc++;
                }
                if(loc > 7) mChannelInfo[i] = 0;
            }
            mChannelInfo[0] = channelConfig.getInt(0);
            mChannelInfo[5] = channelConfig.getInt(8);
        } catch (JSONException e){
            e.printStackTrace();
        }


    }

    private void getRitsImage() {

        BasicImageDownloader imageDownloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {
            @Override
            public void onError(BasicImageDownloader.ImageError error) {
                error.printStackTrace();
            }

            @Override
            public void onProgressChange(int percent) {

            }

            @Override
            public void onComplete(Bitmap result) {
                try{
                    final Bitmap.CompressFormat bmpFormat = Bitmap.CompressFormat.PNG;

                    String imageFilename = String.format(Locale.getDefault(),"rits%d.png", mRits.getRitsId());
                    File path = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

                    File imageFile = new File(path, imageFilename);
                    Log.d(TAG, "onComplete: imageFile=" + imageFile);

                    BasicImageDownloader.writeToDisk(imageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                        @Override
                        public void onBitmapSaved() {

                        }

                        @Override
                        public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                            error.printStackTrace();
                        }
                    }, bmpFormat, false);       // shouldOverwrite set to false here for now

                    mImageView.setImageBitmap(result);
                }catch(NullPointerException e){
                    e.printStackTrace();
                }
            }
        });

        String imageUrl = getString(R.string.url_rits_image);
        imageDownloader.download(imageUrl, true);
    }

    private void getRitsData() {
        JSONParser jsonParser = new JSONParser();
        JSONObject json = jsonParser.getJSONFromUrl(getString(R.string.url_rits_data));

        try{
            TextView dataChan0 = getView().findViewById(R.id.dataBlank);
            TextView dataChan1 = getView().findViewById(R.id.dataChannel1);
            TextView dataChan2 = getView().findViewById(R.id.dataChannel2);
            TextView dataChan3 = getView().findViewById(R.id.dataChannel3);
            TextView dataChan4 = getView().findViewById(R.id.dataChannel4);
            TextView dataChan5 = getView().findViewById(R.id.dataChannel5);
            TextView dataChan6 = getView().findViewById(R.id.dataChannel6);
            TextView dataChan7 = getView().findViewById(R.id.dataChannel7);
            TextView[] dataChan = {dataChan0, dataChan1, dataChan2, dataChan3, dataChan4, dataChan5, dataChan6,dataChan7};
            try {
                try {
                    try{

                        Boolean captureDatEna1 = false;
                        Boolean captureDatEna2 = false;
                        Boolean captureDatEna3 = false;
                        Boolean captureDatEna4 = false;

                        Boolean[] captureDatEna = {captureDatEna1, captureDatEna2, captureDatEna3, captureDatEna4};
                        for (int i = 0; i < 4; i++) {

                            if (tickerCounter < 2 && json.getString(getChan[i]).contains("~")){// for the first Few ticks
                                captureChan[i] = "0 kg";//set all the channels to 0 kgs
                                updateRitsData();//then update the rits
                            }

                            if (json.getString(getChan[i]).contains("~")){// if channel is disconnected
                                captureDatEna[i] = false;// set capture from meter off
                            }else{// if channel is connected
                                captureDatEna[i] = true;// capture data from meter is true
                            }


                            if (captureDatEna[i]) {// if capture is enable for each of the channels
                                mChan[i] = json.getString(getChan[i]);// set the data going to the screen to the incoming data from the meter
                                captureChan[i] = json.getString(getChan[i]);// capture the data from the meter
                                saveData[i] = true;// set the second save data for the "buffer to true"
                            }else{
                                mChan[i] = captureChan[i];// the data to send to the screen will be the captured data
                                saveData[i] = false;// set the second save data for the "buffer to false"
                            }

                            if (!captureChan[i].equals(savedData[i])) {// if the captured total no longer equals the total added together
                                checkDataDoubler++;
                                if (checkDataDoubler == 3) {
                                    updateRitsData();//update the data
                                    checkDataDoubler = 0;
                                    saveData[i] = true;
                                } else {
                                    saveData[i] = false;
                                }
                            }

                            //Capture Total for Calling updateRits
                            if (saveData[i]) {// if capture is enable for each of the channels
                                savedData[i] = captureChan[i];
                            } else {
                                captureChan[i] = savedData[i];
                            }

                            if (json.getString(getChan[i]).contains("~")) {  //if any channels contains ~
                                stagNationDoubler[i]++;// increase the stagnation count by 1(every 0.5 seconds)
                                if (stagNationDoubler[i] < 9){
                                    dataChan[mChannelInfo[i+1]].setTextColor(0xFFd1f1ff);
                                }else if(stagNationDoubler[i] > 10 && stagNationDoubler[i] < 20){
                                    dataChan[mChannelInfo[i+1]].setTextColor(0xFFb7d6e3);
                                }else if(stagNationDoubler[i] > 20 && stagNationDoubler[i] < 30){
                                    dataChan[mChannelInfo[i+1]].setTextColor(0xFFabc6d2);
                                }else if(stagNationDoubler[i] > 30 && stagNationDoubler[i] < 40){
                                    dataChan[mChannelInfo[i+1]].setTextColor(0xFF99b0ba);
                                }else if(stagNationDoubler[i] > 40){
                                    dataChan[mChannelInfo[i+1]].setTextColor(0xFF7b9099);
                                    stagNationDoubler[i] = 45;
                                }
                            }else{
                                stagNationDoubler[i] = 0;// then set the stagnation number to 0
                                dataChan[mChannelInfo[i+1]].setTextColor(0xFFd1f1ff);// and reset the color
                            }
                        }
                    }catch (ArrayIndexOutOfBoundsException e){
                        e.printStackTrace();
                    }
                }catch (NullPointerException e){
                    e.printStackTrace();
                }
            } catch (JSONException e){
                e.printStackTrace();
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }


    }

    public void updateRitsData(){
        try{
            TextView dataChan0 = getView().findViewById(R.id.dataBlank);
            TextView dataChan1 = getView().findViewById(R.id.dataChannel1);
            TextView dataChan2 = getView().findViewById(R.id.dataChannel2);
            TextView dataChan3 = getView().findViewById(R.id.dataChannel3);
            TextView dataChan4 = getView().findViewById(R.id.dataChannel4);
            TextView dataChan5 = getView().findViewById(R.id.dataChannel5);
            TextView dataChan6 = getView().findViewById(R.id.dataChannel6);
            TextView dataChan7 = getView().findViewById(R.id.dataChannel7);
            TextView GroupChanel1 = getView().findViewById(R.id.groupChannel1);
            TextView GroupChanel2 = getView().findViewById(R.id.groupChannel2);
            TextView dataTotal = getView().findViewById(R.id.dataTotal);

            TextView positionChan1 = getView().findViewById(R.id.positionChannel1);
            TextView positionChan2 = getView().findViewById(R.id.positionChannel2);
            TextView positionChan3 = getView().findViewById(R.id.positionChannel3);
            TextView positionChan4 = getView().findViewById(R.id.positionChannel4);
            TextView infoGroup = getView().findViewById(R.id.infoGroup);
            TextView infoTotal = getView().findViewById(R.id.infoTotal);
            TextView infoDriver = getView().findViewById(R.id.infoDriver);
            TextView infoFleet = getView().findViewById(R.id.infoFleet);

            infoDriver.setText(mDriver);
            infoFleet.setText(mFleet);
            positionChan1.setText("Chan 1: " + Integer.toString(mChannelInfo[1]) + " ");
            positionChan2.setText("Chan 2: " + Integer.toString(mChannelInfo[2]) + " ");
            positionChan3.setText("Chan 3: " + Integer.toString(mChannelInfo[3]) + " ");
            positionChan4.setText("Chan 4: " + Integer.toString(mChannelInfo[4]) + " ");
            infoGroup.setText("Grouping: " + Integer.toString(mChannelInfo[0]) + " ");
            infoTotal.setText("Total: " + Integer.toString(mChannelInfo[5]) + " ");



            TextView[] dataChan = {dataChan0, dataChan1, dataChan2, dataChan3, dataChan4, dataChan5, dataChan6, dataChan7};

            try {
                try{
                    for (int i = 1; i < 5; i++) {
                        dataChan[mChannelInfo[i]].setText(mChan[i-1]);
                    }
                } catch (ArrayIndexOutOfBoundsException e){
                    e.printStackTrace();
                }
            }catch(NullPointerException e){
                e.printStackTrace();
            }

            try {
                int numChan1 = Integer.parseInt(mChan[0].replaceAll("\\D", ""));
                int numChan2 = Integer.parseInt(mChan[1].replaceAll("\\D", ""));
                int numChan3 = Integer.parseInt(mChan[2].replaceAll("\\D", ""));
                int numChan4 = Integer.parseInt(mChan[3].replaceAll("\\D", ""));
                switch (mChannelInfo[0]) {
                    case 0:
                        GroupChanel1.setText("  ");
                        GroupChanel2.setText("  ");
                        break;
                    case 1:
                        GroupChanel1.setText(Integer.toString(numChan1 + numChan2) + " kg");
                        GroupChanel2.setText("  ");
                        break;
                    case 2:
                        GroupChanel1.setText("  ");
                        GroupChanel2.setText(Integer.toString(numChan3 + numChan4) + " kg");
                        break;
                    case 3:
                        GroupChanel1.setText(Integer.toString(numChan1 + numChan2) + " kg");
                        GroupChanel2.setText(Integer.toString(numChan3 + numChan4) + " kg");
                        break;
                }
                if (mChannelInfo[5] == 1) {
                    dataTotal.setText(Integer.toString(numChan1 + numChan2 + numChan3 + numChan4) + " kg");
                } else {
                    dataTotal.setText("  ");
                }
            }catch (NumberFormatException e){
                e.printStackTrace();
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }
    private final Handler tickHandler = new Handler();
    private Runnable ticker = new Runnable() {
        @Override
        public void run() {
            tickerCounter++;
            if(tickerCounter >= 64){
                tickerCounter = 8;
            }
            getRitsData();
            tickHandler.postDelayed(this, 500);
        }
    };

}
