package design.scheduler;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Interpolator;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;

public class Map extends Fragment implements OnMapReadyCallback{

    //region Attributes
    //Map
    MapView mapView;
    GoogleMap mGoogleMap;
    ArrayList<Marker> motorMarkers;
    ArrayList<Marker> siteMarkers;
    Marker site, motor;
    ArrayList<LatLng> locArray;
    Boolean isMapLoaded = false;

    //View
    View mView;
    EditText txtFindClient;
    int pos;
    Boolean isClicked = false;

    //Data
    ArrayList<HashMap<String, String>> data;
    ArrayList<HashMap<String, String>> motorData;

    //Task
    AsyncTask myTask;
    //endregion

    //region Handler
    final android.os.Handler handler = new android.os.Handler();
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            UpdateMarkers();
        }
    };
    //endregion

    //region Views
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.map, container, false);
        txtFindClient = (EditText) mView.findViewById(R.id.txtFindClient);

        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView = (MapView) view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.onResume();
        mapView.getMapAsync(this);

        txtFindClient.setInputType(InputType.TYPE_NULL);
        txtFindClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FindClient();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        GetAllSchedule();
        runnable.run();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        if(myTask!= null) myTask.cancel(true);
    }
    //endregion

    //region Methods
    public void UpdateMarkers() {
        if (isMapLoaded) {
            data = new ArrayList<>();
            myTask = new MapTask(getActivity(), 2, data).execute();
        }
        handler.postDelayed(runnable, 1000);
    }

    public void GetAllSchedule(){
        data = new ArrayList<>();
        locArray = new ArrayList<>();
        motorMarkers = new ArrayList<>();
        siteMarkers = new ArrayList<>();

        mGoogleMap.clear();
        myTask = new MapTask(getActivity(), 1, data).execute();
    }

    public void FindClient() {

        //Create View
        LayoutInflater li = LayoutInflater.from(getActivity());
        View lstDialog = li.inflate(R.layout.list_dialog, null);

        //Create List
        final ListView lstView = lstDialog.findViewById(R.id.lstView);
        String[] itemStr = new String[]{"MotorName", "Num"};
        int[] itemID = new int[]{R.id.txtMotorName, R.id.txtMotorNum};
        ListAdapter adapter = new SimpleAdapter(getActivity(), data, R.layout.list_item_motor, itemStr, itemID);
        lstView.setAdapter(adapter);

        //Create Dialog
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Find Client");
        alertDialog.setView(lstDialog);
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.setPositiveButton("Show All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                isClicked = false;
                GetAllSchedule();
                dialog.dismiss();
            }
        });
        final AlertDialog alert = alertDialog.create();
        alert.show();

        //Click Listener
        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                pos = position;
                isClicked = true;
                String motorName = data.get(position).get("MotorName");
                LatLng motorLoc = new LatLng(Double.parseDouble(data.get(position).get("MotorLat")), Double.parseDouble(data.get(position).get("MotorLng")));
                String siteName = data.get(position).get("MotorName") + " destination.";
                LatLng siteLoc = new LatLng(Double.parseDouble(data.get(position).get("SchedLat")), Double.parseDouble(data.get(position).get("SchedLng")));
                locArray = new ArrayList<>();

                //Set Marker Here
                mGoogleMap.clear();
                motor = mGoogleMap.addMarker(new MarkerOptions()
                        .position(motorLoc)
                        .title(motorName)
                        .snippet(null)
                        .icon(BitmapDescriptorFactory.fromBitmap(MotorIcon())));
                site = mGoogleMap.addMarker(new MarkerOptions().
                        position(siteLoc)
                        .title(siteName));
                locArray.add(motorLoc);
                locArray.add(siteLoc);
                CameraPosition(mGoogleMap,locArray);
                alert.dismiss();

            }
        });
    }

    public void AddMarkers() {
        for (int i = 0; i < data.size(); i++) {
            //Set Motor markers
            String motorName = data.get(i).get("MotorName");
            LatLng motorLoc = new LatLng(Double.parseDouble(data.get(i).get("MotorLat")), Double.parseDouble(data.get(i).get("MotorLng")));
            Marker motorMark = mGoogleMap.addMarker(new MarkerOptions()
                    .position(motorLoc)
                    .title(motorName)
                    .snippet(null)
                    .icon(BitmapDescriptorFactory.fromBitmap(MotorIcon())));

            //Set Sched Markers
            String siteName = data.get(i).get("MotorName") + " destination.";
            LatLng siteLoc = new LatLng(Double.parseDouble(data.get(i).get("SchedLat")), Double.parseDouble(data.get(i).get("SchedLng")));
            Marker siteMark = mGoogleMap.addMarker(new MarkerOptions().
                    position(siteLoc)
                    .title(siteName));

            motorMarkers.add(motorMark);
            siteMarkers.add(siteMark);
            locArray.add(motorLoc);
            locArray.add(siteLoc);
        }
        CameraPosition(mGoogleMap,locArray);
    }

    public void CameraPosition(GoogleMap gMap, ArrayList<LatLng> la) {

        final GoogleMap mGMap = gMap;

        LatLngBounds.Builder bld = new LatLngBounds.Builder();
        for (int i = 0; i < la.size(); i++) {
            LatLng ll = new LatLng(la.get(i).latitude, la.get(i).longitude);
            bld.include(ll);
        }
        LatLngBounds bounds = bld.build();
        mGMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 300));
    }

    public Bitmap MotorIcon() {

        int height = 125;
        int width = 125;
        BitmapDrawable bitmapdraw = (BitmapDrawable) getResources().getDrawable(R.mipmap.ic_motor_foreground);
        Bitmap b = bitmapdraw.getBitmap();
        return android.graphics.Bitmap.createScaledBitmap(b, width, height, false);
    }
    //endregion

    //region Map Task Class
    private class MapTask extends AsyncTask<String, Void, String> {

        //region Attributes
        Context context;
        Integer activityFlag;
        String ipAddress;
        String domain;

        Boolean hasError = false;
        Boolean isScheduleEmpty = true;
        ArrayList<HashMap<String, String>> data;
        //endregion

        //region Constructor
        public MapTask(Context context, Integer activityFlag, Object... objects) {
            this.context = context;
            this.activityFlag = activityFlag;
            this.data = (ArrayList<HashMap<String, String>>) objects[0];
            this.ipAddress = ((MyApplication) getActivity().getApplication()).GetWebSerAddress();
            this.domain = "http://" + ipAddress + "/schedulerService/";
        }
        //endregion

        //region Task
        @Override
        protected String doInBackground(String... objects) {
            String result = "";
            result = ViewMotorSchedulesService();
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                if (hasError) throw new Exception(s);
                switch (activityFlag) {
                    case 1:
                        AddMarkers();
                        break;
                    case 2:
                        if(isScheduleEmpty) return;
                       LatLng motorLoc;
                        if (!isClicked) {
                            for (int i = 0; i < data.size(); i++) {
                                motorLoc = new LatLng(Double.parseDouble(data.get(i).get("MotorLat")), Double.parseDouble(data.get(i).get("MotorLng")));
                                motorMarkers.get(i).setPosition(motorLoc);
                            }
                        }
                        if (isClicked) {
                            motorLoc = new LatLng(Double.parseDouble(data.get(pos).get("MotorLat")), Double.parseDouble(data.get(pos).get("MotorLng")));
                            motor.setPosition(motorLoc);
                        }
                        break;
                }
                if(data.size() > 0) isMapLoaded = true;
            } catch (Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
        //endregion

        //region Methods

        public String ViewMotorSchedulesService() {

            try{

                String link = domain + "ViewMotorSchedules.php";

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                if(!result.equals("")) {
                    isScheduleEmpty = false;
                    //Convert JSON to HashMap
                    JSONArray jarray = new JSONArray(result);
                    for (int i = 0; i < jarray.length(); i++) {
                        JSONObject item = jarray.getJSONObject(i);
                        HashMap<String, String> hashMap = new HashMap<>();
                        hashMap.put("MotorName", item.getString("MotorName"));
                        hashMap.put("Num", item.getString("Num"));
                        hashMap.put("MotorLat", item.getString("MotorLat"));
                        hashMap.put("MotorLng", item.getString("MotorLng"));
                        hashMap.put("SchedLat", item.getString("SchedLat"));
                        hashMap.put("SchedLng", item.getString("SchedLng"));
                        data.add(hashMap);
                    }
                }
                else isScheduleEmpty = true;
                hasError = false;
                return result;
            } catch(Exception e){
                hasError = true;
                return new String("Exception: " + e.getMessage());
            }
        }

        //endregion
    }
    //endregion
}
