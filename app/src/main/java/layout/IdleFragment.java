package layout;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.silodec.wifirits.MainActivity;
import com.silodec.wifirits.R;

public class IdleFragment extends Fragment {


    public IdleFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View splashView = inflater.inflate(R.layout.fragment_idle, container, false);

        TextView tvMenu = splashView.findViewById(R.id.idleMenuMode);
        tvMenu.setText(MainActivity.mMenuMode.toString());
        TextView tvConnect = splashView.findViewById(R.id.idleConnectMode);
        String sConnect = "";
        //not the Button, but the text on the idle menu
        switch (MainActivity.mConnectMode) {
            case CONNECT_DISCONNECTED:
                sConnect = getString(R.string.mode_disconnected);
                break;
            case CONNECT_CONNECTING:
                sConnect = getString(R.string.mode_connecting);
                break;
            case CONNECT_CONNECTED:
                sConnect = getString(R.string.mode_connected);
                break;
        }
        tvConnect.setText(sConnect);
        TextView tvFavourite = splashView.findViewById(R.id.idleFavouriteMode);
        String sFavourite = (MainActivity.mRitsHasFavourite) ? getString(R.string.mode_favourite) : getString(R.string.mode_unassigned);
        tvFavourite.setText(sFavourite);

        return splashView;
    }

}
