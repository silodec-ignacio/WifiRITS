//andr
package com.silodec.wifirits;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class JSONParser {

    private static final String JSON_PARSER = "JSON_PARSER";

    // constructor
    public JSONParser() {
    }

    public JSONObject getJSONFromUrl(String address) {

        JSONObject jsonObject = null;

        String urlString = String.format("http://%s", address);
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log.e(JSON_PARSER, "getJSONFromUrl: newURL(string) - ", e);
        }

        DownloadJson downloadJson = new DownloadJson();
        downloadJson.execute(url);
        try {
            jsonObject = downloadJson.get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(JSON_PARSER, "getJSONFromUrl: downloadJson.get() - ", e);
        }

        return jsonObject;

    }

    private class DownloadJson extends AsyncTask<URL, Void, JSONObject> {

        @Override

        protected JSONObject doInBackground(URL... urls) {

            JSONObject jsonObject = new JSONObject();
            String jsonString = "";

            HttpURLConnection urlConnection = null;
            try {
                assert urls[0] != null;
                urlConnection = (HttpURLConnection) urls[0].openConnection();
                urlConnection.setRequestMethod("GET");
            } catch (IOException e) {
                Log.e(JSON_PARSER, "DownloadJson: urlConnection - ", e);
            }

            try {
                assert urlConnection != null;
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                jsonString = convertStreamToString(in);
            } catch (IOException e) {
                Log.e(JSON_PARSER, "DownloadJson: readStream -", e);

            } finally {
                assert urlConnection != null;
                urlConnection.disconnect();

            }
            try {
                jsonObject = new JSONObject(jsonString);
            } catch (JSONException e) {
                Log.e(JSON_PARSER, "DownloadJson: new JSONObject(jsonString) - ", e);
            }
            return jsonObject;
        }
    }

    private String convertStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()){
            sb.append(line);
        }
        is.close();
        return sb.toString();
    }
}
