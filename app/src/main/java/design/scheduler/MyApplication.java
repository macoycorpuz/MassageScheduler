package design.scheduler;

import android.app.Application;

import com.google.android.gms.maps.model.LatLng;

public class MyApplication extends Application {
    private String webSerAddress = "http://daryl-kiel-mora.000webhostapp.com";
    private LatLng remoteLocation = new LatLng(14.5905251, 120.9781245);
    private double radius = 50; //meters

    public double GetRadius() {
        return radius;
    }

    public void SetRadius(double radius) {
        this.radius = radius;
    }

    public String GetWebSerAddress() {
        return webSerAddress;
    }

    public void SetWebSerAddress(String webSerAddress) {
        this.webSerAddress = webSerAddress;
    }

    public LatLng GetRemoteLocation() {
        return remoteLocation;
    }

    public void SetRemoteLocation(LatLng remoteLocation) {
        this.remoteLocation = remoteLocation;
    }
}
