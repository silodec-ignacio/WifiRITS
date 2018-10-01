package layout;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.silodec.wifirits.MainActivity;
import com.silodec.wifirits.R;
import com.silodec.wifirits.model.RitsData;

import static com.silodec.wifirits.model.RitsData.PARCEL_RITS_KEY;

public class ConnectFragment extends Fragment {

    public static final String RITS_PASSWORD = "\"12345678\"";
    public static final String WIFI_CONNECT = "WIFI_CONNECT";
    private ConnectFragmentListener mListener;
    private boolean mWaitingReconnect;

    public ConnectFragment() {
    }

    public static ConnectFragment newInstance(RitsData rits) {

        Bundle args = new Bundle();
        args.putParcelable(PARCEL_RITS_KEY, rits);

        ConnectFragment fragment = new ConnectFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof ConnectFragmentListener)) throw new AssertionError();
        mListener = (ConnectFragmentListener) context;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View connectView = inflater.inflate(R.layout.fragment_connect, container, false);

        RitsData rits = getArguments().getParcelable(PARCEL_RITS_KEY);
        assert rits != null;

        boolean ritsConnected = isRitsConnected(rits);
        mWaitingReconnect = false;
        if (!ritsConnected) {
            StringBuilder sbSSID = new StringBuilder();
            try {
                sbSSID.append(R.string.connecting_to)
                        .append(rits.getFriendlyRitsSSID());
            }catch (NullPointerException e){
                e.printStackTrace();
            }
            TextView tvConnection = connectView.findViewById(R.id.textConnectingSSID);
            tvConnection.setText(sbSSID);
            StringBuilder sbUnitName = new StringBuilder();
            sbUnitName.append(rits.getRitsDriverName())
                    .append(" ")
                    .append(rits.getRitsTruckNumber());

            TextView tvUnitName = connectView.findViewById(R.id.textUnitName);
            tvUnitName.setText(sbUnitName);


            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = rits.getRitsSSID();
            wifiConfig.preSharedKey = RITS_PASSWORD;

            int netId = MainActivity.wifi.addNetwork(wifiConfig);
            MainActivity.wifi.disconnect();
            MainActivity.wifi.enableNetwork(netId, true);

            getActivity().registerReceiver(wifi_receiver, new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
            MainActivity.wifi.reconnect();
            while (mWaitingReconnect) { /*...*/ }
            getActivity().unregisterReceiver(wifi_receiver);

            long i = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < i) { /* */ }

            ritsConnected = isRitsConnected(rits);
        }

        if (mListener == null) {
            throw new AssertionError();
        }


        mListener.onConnectFinished(ritsConnected);

        return connectView;
    }

    public BroadcastReceiver wifi_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            NetworkInfo info = MainActivity.network.getActiveNetworkInfo();
            mWaitingReconnect = (info.getState() == NetworkInfo.State.CONNECTED);
                mWaitingReconnect = (MainActivity.wifi.getConnectionInfo().getNetworkId() != -1);
        }
        }
    };

    private boolean isRitsConnected(RitsData rits) {
        try {
            WifiInfo wifiInfo = MainActivity.wifi.getConnectionInfo();
            Log.d(WIFI_CONNECT, "isRitsConnected: wifi=" + wifiInfo.getSSID() + " rits=" + rits.getRitsSSID());
            return (wifiInfo.getSSID().equals(rits.getRitsSSID()));
        }catch(NullPointerException e){
            e.printStackTrace();
        }

        return false;
    }


    public interface ConnectFragmentListener {
        void onConnectFinished(boolean connectOk);
    }


}
