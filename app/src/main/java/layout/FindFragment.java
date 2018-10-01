package layout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.silodec.wifirits.JSONParser;
import com.silodec.wifirits.MainActivity;
import com.silodec.wifirits.R;
import com.silodec.wifirits.RitsDataAdapter;
import com.silodec.wifirits.model.RitsData;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FindFragment extends Fragment{

    public static final String WIFI_SCANNER = "WIFI_SCANNER";
    public static final String FAVOURITE_KEY = "favourite_key";

    private String mDriver;
    private String mFleet;

    private boolean mFavourite;

    private FindFragmentListener mListener;
    private List<RitsData> mRitsDataList = new ArrayList<>();
    private RitsDataAdapter mRitsAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof FindFragmentListener)) throw new AssertionError();
        mListener = (FindFragmentListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View findView = inflater.inflate(R.layout.fragment_find, container, false);

        final View customView = inflater.inflate(R.layout.fragment_customise, container, false);

        this.mRitsAdapter = new RitsDataAdapter(getContext(), mRitsDataList);

        // get arguments from main activity as parcelable?
        Bundle arguments = getArguments();
        if (arguments != null) {
            boolean favourite = arguments.getBoolean(FAVOURITE_KEY);
            CheckBox cbFavourite = customView.findViewById(R.id.cbFavourite);
            cbFavourite.setChecked(favourite);
        }

        final CheckBox cbFavourite = customView.findViewById(R.id.cbFavourite);
        cbFavourite.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                mFavourite = isChecked;
                Toast.makeText(customView.getContext(), "Favourite is " + mFavourite, Toast.LENGTH_SHORT).show();
            }
        });

        ListView wifiList = findView.findViewById(R.id.wifiList);
        wifiList.setAdapter(this.mRitsAdapter);

        Button btnScan = findView.findViewById(R.id.buttonScan);

        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRitsDataList.clear();
                mRitsAdapter.notifyDataSetChanged();
                scanWifiNetworks();


            }
        });

        Button btnQuit = findView.findViewById(R.id.buttonQuit);
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                quitScanning();
            }
        });

        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                RitsData rits = mRitsAdapter.getItem(i);
                connectToRits(rits);
            }
        });

        Button btnSave = customView.findViewById(R.id.button_ok);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doSave();
            }
        });

        Button btnCancel = customView.findViewById(R.id.button_cancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCancel();
            }
        });

        checkLocationEnabled();

        return findView;
    }


    private void checkLocationEnabled() {
        int locationMode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                locationMode = Settings.Secure.getInt(getContext().getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return;
            }

            if (locationMode == Settings.Secure.LOCATION_MODE_OFF) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setMessage(R.string.location_dialog);
                dialog.setPositiveButton(R.string.location_dialog_change, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                        //get gps
                    }
                });
                dialog.setNegativeButton(R.string.location_dialog_cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        // TODO Auto-generated method stub
                        quitScanning();
                    }
                });
                dialog.show();
            }
        }

    }

    public void getDriverInfo() {
        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject json = jsonParser.getJSONFromUrl(getString(R.string.url_rits_config));
            try {
                mDriver = json.getString("owner");
                mFleet = json.getString("fleet");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

    boolean permGrant = false;
    boolean emulate = false;

    private void scanWifiNetworks() {

        String SSIDCheck = MainActivity.wifi.getConnectionInfo().getSSID();
        Log.i("SSID<", SSIDCheck);//logs the ssid

        mRitsDataList.clear();
        getActivity().registerReceiver(wifi_receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        MainActivity.wifi.startScan();
        handleScanText(false, "Scanning...", " ", false);
        permGrant = true;

    }


    public BroadcastReceiver wifi_receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
        getDriverInfo();
            mRitsAdapter.clear();

            if (permGrant) {
                try {
                    List<ScanResult> results = MainActivity.wifi.getScanResults();
                    int size = results.size();
                    boolean listUpdated = false;
                    try {
                        while (size > 0) {
                            size--;
                            String ssid = results.get(size).SSID;
                            if (emulate) {
                                RitsData newRits = new RitsData();
                                newRits.setRitsSSID(ssid);
                                if (mDriver != null) {
                                    newRits.setRitsDriverName(mDriver);
                                    newRits.setRitsTruckNumber(mFleet);
                                } else {
                                    newRits.setRitsDriverName("");
                                    newRits.setRitsTruckNumber("");
                                }
                                mRitsDataList.add(newRits);
                                listUpdated = true;
                            }else{
                                if (ssid.startsWith("<SIL")) {

                                    RitsData newRits = new RitsData();
                                    newRits.setRitsSSID(ssid);
                                    if (mDriver != null) {
                                        newRits.setRitsDriverName(mDriver);
                                        newRits.setRitsTruckNumber(mFleet);
                                    } else {
                                        newRits.setRitsDriverName("");
                                        newRits.setRitsTruckNumber("");
                                    }
                                    mRitsDataList.add(newRits);
                                    listUpdated = true;

                                }
                            }
                        }
                        if (listUpdated) {

                            Log.i(WIFI_SCANNER, "List Updated");
                            mRitsAdapter.notifyDataSetChanged();
                            handleScanText(true, "Scan Again", "Done, Scanning Completed.", true);
                        }
                    } catch (Exception e) {
                        Log.w(WIFI_SCANNER, "exception " + e);
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    public void handleScanText(boolean toggle, String btnText, String scanTXT,boolean lightCToggle) {


        try {
            TextView txtScan = getView().findViewById(R.id.scanningTXT);
            Button btnScan = getView().findViewById(R.id.buttonScan);

            txtScan.setText(scanTXT);

            btnScan.setEnabled(toggle);
            btnScan.setText(btnText);

            if (lightCToggle) {
                btnScan.setBackgroundColor(getResources().getColor(R.color.colorAccent));//
                getView().findViewById(R.id.scanTextContainer).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.scanningAnimBar).setVisibility(View.GONE);
            } else {
                btnScan.setBackgroundColor(getResources().getColor(R.color.colorDarker));
                getView().findViewById(R.id.scanningAnimBar).setVisibility(View.VISIBLE);
                getView().findViewById(R.id.scanTextContainer).setVisibility(View.GONE);
            }

        }catch(NullPointerException e){
            e.printStackTrace();
        }
    }

    private void doSave() {
        if (mListener == null) {
            throw new AssertionError();
        }
        //getActivity().unregisterReceiver(wifi_receiver);
        mListener.onCustomFinishSave(mFavourite);
    }
    private void quitScanning() {
        if (mListener == null) {
            throw new AssertionError();
        }
        //getActivity().unregisterReceiver(wifi_receiver);
        mListener.onFindFinishQuit();
    }

    private void doCancel() {
        if (mListener == null) {
            throw new AssertionError();
        }
        //mListener.onCustomFinishQuit();
        //getActivity().unregisterReceiver(wifi_receiver);
    }

    private void connectToRits(RitsData rits) {
/*
        if (mListener == null) {
            throw new AssertionError();
        }
*/

        mListener.onFindFinishConnect(rits);
        //getActivity().unregisterReceiver(wifi_receiver);
    }


    public interface FindFragmentListener {
        void onFindFinishConnect(RitsData rits);
        void onFindFinishQuit();

        void onCustomFinishSave(boolean mFavourite);
    }
}