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
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.silodec.wifirits.MainActivity;
import com.silodec.wifirits.R;


public class CustomiseFragment extends Fragment {

    public static final String FAVOURITE_KEY = "favourite_key";
    private boolean mFavourite;

    private CustomFragmentListener mListener;

    public WebView webView;

    public CustomiseFragment() {
    }



    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (!(context instanceof CustomFragmentListener)) throw new AssertionError();
        mListener = (CustomFragmentListener) context;
    }

    @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View customView = inflater.inflate(R.layout.fragment_customise, container, false);

        // get arguments from main activity as parcelable?
        Bundle arguments = getArguments();


        TextView tvMenu = customView.findViewById(R.id.customMenuMode);
        tvMenu.setText(MainActivity.mMenuMode.toString());
        TextView tvConnect = customView.findViewById(R.id.customConnectMode);
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
        TextView tvFavourite = customView.findViewById(R.id.customFavouriteMode);
        String sFavourite = (MainActivity.mRitsHasFavourite) ? getString(R.string.mode_favourite) : getString(R.string.mode_unassigned);
        tvFavourite.setText(sFavourite);

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

    Button btnCustomize = customView.findViewById(R.id.button_customize_link);
        btnCustomize.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            doAccessLink();

        }
    });

        WebView webView = (WebView) customView.findViewById(R.id.customise_webview);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                // Handle the error
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        webView.loadUrl("http://192.168.4.1/access.htm");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        return customView;


}

    private void doAccessLink(){
        try {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("http://192.168.4.1/access.htm"));
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

    private void doSave() {
        if (mListener == null) {
            throw new AssertionError();
        }
        mListener.onCustomFinishSave(mFavourite);
    }

    public interface CustomFragmentListener {
        void onCustomFinishSave(boolean favourite);
        void onCustomFinishQuit();
    }


}
