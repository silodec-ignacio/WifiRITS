package com.silodec.wifirits.model;

import android.os.Parcel;
import android.os.Parcelable;

public class RitsData implements Parcelable {

    public static final String PARCEL_RITS_KEY = "PARCEL_RITS_KEY";

    private int ritsId;
    private String ritsSSID;
    private String ritsFriendlySSID;
    private String ritsDriverName;
    private String ritsTruckNumber;
    private String ritsTruckImage;



    public static final int RITS_SSID_ID_STOP = 0;


    public RitsData() {
        ritsId = 0;
        ritsSSID = "";
        ritsFriendlySSID = "";
        ritsDriverName = "";
        ritsTruckNumber = "";
        ritsTruckImage = "";


    }

    public RitsData(int ritsId, String ritsSSID, String ritsDriverName, String ritsTruckNumber, String ritsTruckImage) {
        this.ritsId = ritsId;
        formatSSIDs(ritsSSID);
        this.ritsDriverName = ritsDriverName;
        this.ritsTruckNumber = ritsTruckNumber;
        this.ritsTruckImage = ritsTruckImage;

    }

    private void formatSSIDs(String ssid) {
        ritsFriendlySSID = ssid.substring(RITS_SSID_ID_STOP+1);
        ritsSSID = String.format("\"%s\"", ssid);
    }


    //set
    public void setRitsDriverName(String ritsDriverName) {
        this.ritsDriverName = ritsDriverName;
    }

    public void setRitsTruckNumber(String ritsTruckNumber) {this.ritsTruckNumber = ritsTruckNumber;}

    public void setRitsSSID(String ritsSSID) {formatSSIDs(ritsSSID);}

    public void setRitsTruckImage(String ritsTruckImage) {this.ritsTruckNumber = ritsTruckImage;}

    public void setRitsId(int ritsId) {this.ritsId = ritsId;}

    //get
    public String getRitsDriverName() {
        return ritsDriverName;
    }

    public String getRitsTruckNumber() {
        return ritsTruckNumber;
    }

    public int getRitsId() {
        return ritsId;
    }

    public String getRitsSSID() {
        return ritsSSID;
    }

    public String getFriendlyRitsSSID() {return ritsFriendlySSID;}

    @Override
    public String toString() {
        return "RitsData{" +
                "ritsId='" + ritsId + '\'' +
                ", ritsSSID='" + ritsSSID + '\'' +
                ", ritsFriendlySSID='" + ritsFriendlySSID + '\'' +
                ", ritsDriverName='" + ritsDriverName + '\'' +
                ", ritsTruckNumber='" + ritsTruckNumber + '\'' +
                ", ritsTruckImage='" + ritsTruckImage + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.ritsId);
        dest.writeString(this.ritsSSID);
        dest.writeString(this.ritsFriendlySSID);
        dest.writeString(this.ritsDriverName);
        dest.writeString(this.ritsTruckNumber);
        dest.writeString(this.ritsTruckImage);
    }

    protected RitsData(Parcel in) {
        this.ritsId = in.readInt();
        this.ritsSSID = in.readString();
        this.ritsFriendlySSID = in.readString();
        this.ritsDriverName = in.readString();
        this.ritsTruckNumber = in.readString();
        this.ritsTruckImage = in.readString();
    }

    public static final Parcelable.Creator<RitsData> CREATOR = new Parcelable.Creator<RitsData>() {
        @Override
        public RitsData createFromParcel(Parcel source) {
            return new RitsData(source);
        }

        @Override
        public RitsData[] newArray(int size) {
            return new RitsData[size];
        }
    };


}
