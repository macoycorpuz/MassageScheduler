package design.scheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

public class History extends Fragment implements OnMapReadyCallback{

    //region Attributes
    //Map
    MapView mapView;
    GoogleMap mGoogleMap;
    ArrayList<LatLng> locArray;
    Marker site, motor, crumb;

    //View
    View view;
    ListView lstView;
    TextView txtClient, txtNum, txtMotor;
    ArrayList<HashMap<String, String>> data;
    ArrayList<HashMap<String, String>> crumbsData;

    //Task
    AsyncTask myTask;
    //endregion

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.history, container, false);
        lstView = (ListView) view.findViewById(R.id.lstViewHistory);
        return  view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable final Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        data = new ArrayList<>();

        myTask = new HistoryTask(getActivity(),1, data, lstView).execute();

        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                LayoutInflater li = LayoutInflater.from(getActivity());
                View viewSchedDialog = li.inflate(R.layout.view_history_dialog, null);
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                HistoryDetails(position, viewSchedDialog, savedInstanceState);


                alertDialog.setTitle("History Details");
                alertDialog.setView(viewSchedDialog);
                alertDialog.setNegativeButton("Close",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                final AlertDialog alert = alertDialog.create();
                alert.show();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        myTask.cancel(true);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
    }

    public void HistoryDetails(int position, View view, @Nullable final Bundle savedInstanceState) {
        try {
            txtMotor = (TextView) view.findViewById(R.id.txtHistoryMotor);
            txtClient = (TextView) view.findViewById(R.id.txtHistoryClient);
            txtNum = (TextView) view.findViewById(R.id.txtHistoryNum);
            mapView = (MapView) view.findViewById(R.id.mapView2);
            mapView.onCreate(savedInstanceState);
            mapView.onResume();
            mapView.getMapAsync(this);

            String Num = data.get(position).get("CNum");
            String ClientName = data.get(position).get("FName") + " " + data.get(position).get("LName");
            String MotorName = data.get(position).get("Name");

            txtMotor.setText(MotorName);
            txtClient.setText(ClientName);
            txtNum.setText(Num);

            //Set Markers
            GetBreadCrumbs(position);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void GetBreadCrumbs(int position){
        crumbsData = new ArrayList<>();
        locArray = new ArrayList<>();
        myTask = new HistoryTask(getActivity(), 2, crumbsData).execute(data.get(position).get("SchedID"), position);
    }

    public void AddMarkers(int position) {


        //Set Sched Marker
        String siteName = data.get(position).get("Name") + " destination.";
        LatLng siteLoc = new LatLng(Double.parseDouble(data.get(position).get("SchedLat")), Double.parseDouble(data.get(position).get("SchedLng")));
        site = mGoogleMap.addMarker(new MarkerOptions().
                position(siteLoc)
                .title(siteName));
        locArray.add(siteLoc);

        //Set crumb markers
        String motorName = data.get(position).get("Name");
        for (int i = 0; i < crumbsData.size(); i++) {
            LatLng crumbLoc = new LatLng(Double.parseDouble(crumbsData.get(i).get("Lat")), Double.parseDouble(crumbsData.get(i).get("Lng")));
            crumb = mGoogleMap.addMarker(new MarkerOptions().
                    position(crumbLoc)
                    .title(motorName)
                    .snippet(null)
                    .icon(BitmapDescriptorFactory.fromBitmap(MotorIcon())));
            locArray.add(crumbLoc);
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


    private class HistoryTask  extends AsyncTask<Object, Void, String> {

        //region Attributes
        Context context;
        Integer activityFlag;
        String ipAddress;
        String domain;

        Boolean hasError = false;
        ListView lstView;
        ArrayAdapter<String> adapter;
        ArrayList<String> arrayList;
        ArrayList<HashMap<String, String>> data;
        int position;
        //endregion

        public HistoryTask(Context context, Integer activityFlag, Object... objects) {
            this.context = context;
            this.activityFlag = activityFlag;
            this.data = (ArrayList<HashMap<String, String>>) objects[0];
            if(activityFlag == 1) this.lstView = (ListView) objects[1];

            this.ipAddress = ((MyApplication) getActivity().getApplication()).GetWebSerAddress();
            this.domain = "http://" + ipAddress + "/schedulerService/";
        }

        @Override
        protected String doInBackground(Object... objects) {

            String result = "";

            switch(activityFlag) {
                case 1:
                    result = ViewHistoryService();
                    break;
                case 2:
                    result = ViewBreadCrumbsService(objects);
                    break;
            }

            return result;
        }

        @Override
        protected void onPostExecute(String s) {

            if(hasError) {
                Toast.makeText(getActivity(), s, Toast.LENGTH_LONG).show();
            }
            else if(activityFlag == 1) {
                adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, arrayList);
                lstView.setAdapter(adapter);
            }
            else if(activityFlag == 2) {
                try {
                    AddMarkers(position);
                } catch (Exception e) {
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
                }

            }
        }

        private String ViewHistoryService() {
            try{

                String link = domain + "ViewHistory.php";

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                //Convert JSON to HashMap
                JSONArray jarray = new JSONArray(result);
                arrayList = new ArrayList<String>();
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject item = jarray.getJSONObject(i);
                    HashMap<String, String> hashMap= new HashMap<>();
                    hashMap.put("Name", item.getString("Name"));
                    hashMap.put("CNum",  item.getString("CNum"));
                    hashMap.put("FName", item.getString("FName"));
                    hashMap.put("LName", item.getString("LName"));
                    hashMap.put("SchedID", item.getString("SchedID"));
                    hashMap.put("MotorLat", item.getString("MotorLat"));
                    hashMap.put("MotorLng", item.getString("MotorLng"));
                    hashMap.put("SchedLat", item.getString("SchedLat"));
                    hashMap.put("SchedLng", item.getString("SchedLng"));
                    data.add(hashMap);
                    arrayList.add(item.getString("Date") + " " + item.getString("Time"));
                }

                hasError = false;
                return result;
            } catch(Exception e){
                hasError = true;
                return "Exception: " + e.getMessage();
            }
        }

        private String ViewBreadCrumbsService(Object... objects) {
            try{
                position = (int) objects[1];
                String link = domain + "ViewBreadCrumbs.php?SchedID=" + objects[0];

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                //Convert JSON to HashMap
                JSONArray jarray = new JSONArray(result);
                arrayList = new ArrayList<String>();
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject item = jarray.getJSONObject(i);
                    HashMap<String, String> hashMap= new HashMap<>();
                    hashMap.put("SchedID", item.getString("SchedID"));
                    hashMap.put("Lat",  item.getString("Lat"));
                    hashMap.put("Lng", item.getString("Lng"));
                    data.add(hashMap);
                }

                hasError = false;
                return result;
            } catch(Exception e){
                hasError = true;
                return "Exception: " + e.getMessage();
            }

        }
    }
}
