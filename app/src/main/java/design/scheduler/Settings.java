package design.scheduler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class Settings extends Fragment {

    //region Attributes
    View view;
    Button btnSaveSettings;
    EditText txtWebServiceAddress, txtRemoteLocation, txtLocationRadius;
    String ipAddress;
    LatLng location;
    double radius = 0;
    //endregion

    //region View
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.settings, container, false);
        btnSaveSettings = (Button) view.findViewById(R.id.btnSaveSettings);

        txtRemoteLocation = (EditText) view.findViewById(R.id.txtRemoteLocation);
        txtWebServiceAddress = (EditText) view.findViewById(R.id.txtWebServiceAddress);
        txtLocationRadius = (EditText) view.findViewById(R.id.txtLocationRadius);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnSaveSettings.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                SaveSettings();
            }
        });
        txtRemoteLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoteLocation();
            }
        });
        ShowSettings();
    }

    //endregion

    //region Methods
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (resultCode == getActivity().RESULT_OK) {
                Place place = PlacePicker.getPlace(data, getActivity());
                location = place.getLatLng();
                txtRemoteLocation.setText(place.getAddress().toString());
            }
        }
    }

    private void ShowSettings(){
        try {
            ipAddress = ((MyApplication) getActivity().getApplication()).GetWebSerAddress();
            location = ((MyApplication) getActivity().getApplication()).GetRemoteLocation();
            radius = ((MyApplication) getActivity().getApplication()).GetRadius();

            Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1);
            String address = addressList.get(0).getAddressLine(0);

            txtWebServiceAddress.setText(ipAddress);
            txtRemoteLocation.setText(address);
            txtLocationRadius.setText(Double.toString(radius));
        }catch (Exception ex) {
                Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void SaveSettings() {
        try {
            if(txtRemoteLocation.getText().toString().equals("") || txtWebServiceAddress.getText().toString().equals("")) {
                throw new Exception("Invalid Input");
            }
            new SettingsTask(getActivity()).execute(txtWebServiceAddress.getText().toString());
        } catch (Exception ex) {
            Toast.makeText(getActivity(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void RemoteLocation() {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            Intent intent = builder.build(getActivity());

            startActivityForResult(intent, 1);
        }
        catch (GooglePlayServicesRepairableException e){
            e.printStackTrace();
        }
        catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }
    //endregion

    private class SettingsTask extends AsyncTask<String, Void, String> {

        //region Attributes
        Context context;
        String ipAddress;
        String domain;
        Boolean hasError = false;
        //endregion

        private SettingsTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... objects) {

            try{
                ipAddress = objects[0];
                domain = "http://" + ipAddress + "/schedulerService/";
                String link = domain + "dbConnect.php?connect=1";

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                hasError = false;
                if(!result.contains("1")) throw new Exception(result);
                return result;
            } catch(Exception e){
                hasError = true;
                return new String("Exception: " + e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                if(hasError) throw new Exception(s);
                String ipAddress = txtWebServiceAddress.getText().toString();
                ((MyApplication) getActivity().getApplication()).SetRemoteLocation(location);
                ((MyApplication) getActivity().getApplication()).SetWebSerAddress(ipAddress);

                SharedPreferences sp = context.getSharedPreferences("Settings", context.MODE_PRIVATE);
                SharedPreferences.Editor Ed = sp.edit();
                Ed.putString("ipAddress",ipAddress);
                Ed.putString("lat",Double.toString(location.latitude));
                Ed.putString("lng",Double.toString(location.longitude));
                Ed.commit();
                Toast.makeText(context, "Settings have been saved", Toast.LENGTH_LONG).show();
            } catch (Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
