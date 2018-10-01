package layout;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.silodec.wifirits.MainActivity;
import com.silodec.wifirits.R;

public class AboutFragment extends Fragment {

    private CustomiseFragment.CustomFragmentListener mListener;

    public AboutFragment() {
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof CustomiseFragment.CustomFragmentListener)) throw new AssertionError();
        mListener = (CustomiseFragment.CustomFragmentListener) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View aboutView = inflater.inflate(R.layout.fragment_about, container, false);

        TextView tvMenu = aboutView.findViewById(R.id.aboutMenuMode);
        tvMenu.setText(MainActivity.mMenuMode.toString());
        TextView tvConnect = aboutView.findViewById(R.id.aboutConnectMode);
        String sConnect = "";
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
        TextView tvFavourite = aboutView.findViewById(R.id.aboutFavouriteMode);
        String sFavourite = (MainActivity.mRitsHasFavourite) ? getString(R.string.mode_favourite) : getString(R.string.mode_unassigned);
        tvFavourite.setText(sFavourite);

        Button buttonBack = aboutView.findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCancel();
            }
        });

        Button aboutPageBTN = aboutView.findViewById(R.id.aboutPageBTN);
        aboutPageBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCustimizeLink();
            }
        });

        return aboutView;
    }

    private void doCustimizeLink(){
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
    }

    private void doCancel() {
        if (mListener == null) {
            throw new AssertionError();
        }
        mListener.onCustomFinishQuit();
    }

}
