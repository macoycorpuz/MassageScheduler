package design.scheduler;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
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
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class Book extends Fragment {

    //region Attributes
    // Constants
    int PLACE_PICKER_REQUEST = 1;

    // Resources
    View view;
    Button btnBook;
    ImageButton btnAddClient, btnAddMotor;
    EditText txtDate, txtDestination, txtTime, txtClient, txtMotor;
    TextView txtContact, txtCNum, txtDisplay;
    DatePickerDialog dPicker;
    TimePickerDialog tPicker;

    // Variables
    String result;
    String fName, lName, cNum; //Client
    String name, num; //Motor
    LatLng location;

    // Output
    String address, date, time, clientID, motorID, lat, lng, statusFlag;

    //Task
    AsyncTask myTask;
    //endregion

    //region View
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.book, container, false);
        btnBook = (Button) view.findViewById(R.id.btnBook);

        btnAddClient = (ImageButton) view.findViewById(R.id.btnAddClient);
        btnAddMotor = (ImageButton) view.findViewById(R.id.btnAddMotor);

        txtDate = (EditText) view.findViewById(R.id.txtDate);
        txtDestination = (EditText) view.findViewById(R.id.txtDestination);
        txtTime = (EditText) view.findViewById(R.id.txtTime);
        txtClient = (EditText) view.findViewById(R.id.txtSchedClient);
        txtMotor = (EditText) view.findViewById(R.id.txtSchedMotor);

        txtContact = (TextView) view.findViewById(R.id.txtContact);
        txtCNum = (TextView) view.findViewById(R.id.txtCNum);

        txtDisplay = view.findViewById(R.id.txtDisplay);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        btnBook.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                BookSchedule();
            }
        });
        btnAddClient.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AddClient();
            }
        });
        btnAddMotor.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                AddMotor();
            }
        });

        txtDestination.setInputType(InputType.TYPE_NULL);
        txtDestination.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectDestination();
            }
        });

        txtDate.setInputType(InputType.TYPE_NULL);
        txtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectDate();
            }
        });

        txtTime.setInputType(InputType.TYPE_NULL);
        txtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectTime();
            }
        });

        txtClient.setInputType(InputType.TYPE_NULL);
        txtClient.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectClient();
            }
        });

        txtMotor.setInputType(InputType.TYPE_NULL);
        txtMotor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectMotor();
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(myTask != null) myTask.cancel(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        txtDestination.setEnabled(true);
    }

    //endregion

    //region Map
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == getActivity().RESULT_OK) {
                Place place = PlacePicker.getPlace(data, getActivity());
                address = place.getAddress().toString();
                location = place.getLatLng();
                lat = Double.toString(location.latitude);
                lng = Double.toString(location.longitude);
                txtDestination.setText(address);
                txtDestination.setEnabled(true);
                //Toast.makeText(getActivity(), location.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }
    //endregion

    //region Methods
    public void BookSchedule() {
        try {
            if(TextUtils.isEmpty(address) || TextUtils.isEmpty(lat) || TextUtils.isEmpty(lng)) throw new Exception("Incorrect Destination");
            if(TextUtils.isEmpty(date)) throw new Exception("Incorrect Date");
            if(TextUtils.isEmpty(time)) throw new Exception("Incorrect Time");
            if(TextUtils.isEmpty(clientID)) throw new Exception("Incorrect Client");
            if(TextUtils.isEmpty(motorID)) throw new Exception("Incorrect Motorcycle");

            statusFlag = "1";
            myTask = new BookTask(getActivity(), 3, txtDisplay).execute(clientID, motorID, date, time, address, lat, lng, statusFlag);
        } catch (Exception ex) {
            txtDisplay.setText(ex.getMessage());
        }

    }

    public void AddClient() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Add Client");

        LayoutInflater li = LayoutInflater.from(getActivity());
        View addDialog = li.inflate(R.layout.addclient_dialog, null);

        final EditText txtAddFName = (EditText) addDialog.findViewById(R.id.txtAddFName);
        final EditText txtAddLName = (EditText) addDialog.findViewById(R.id.txtAddLName);
        final EditText txtAddCNum = (EditText) addDialog.findViewById(R.id.txtAddCNum);
        alertDialog.setView(addDialog);

        alertDialog.setPositiveButton("Add",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        fName = txtAddFName.getText().toString();
                        lName = txtAddLName.getText().toString();
                        cNum = txtAddCNum.getText().toString();
                        myTask = new BookTask(getActivity(), 1, txtDisplay).execute(fName, lName, cNum);
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    public void AddMotor() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Add Motorcycle");

        LayoutInflater li = LayoutInflater.from(getActivity());
        View addDialog = li.inflate(R.layout.addmotor_dialog, null);


        final EditText txtAddName = (EditText) addDialog.findViewById(R.id.txtAddName);
        final EditText txtAddNum = (EditText) addDialog.findViewById(R.id.txtAddNum);
        alertDialog.setView(addDialog);

        alertDialog.setPositiveButton("Add",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        location = ((MyApplication) getActivity().getApplication()).GetRemoteLocation();
                        name = txtAddName.getText().toString();
                        num = txtAddNum.getText().toString();
                        lat = Double.toString(location.latitude);
                        lng = Double.toString(location.longitude);
                        myTask = new BookTask(getActivity(), 2, txtDisplay).execute(name, num, lat, lng);
                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.show();
    }

    public void SelectDestination() {
        txtDestination.setEnabled(false);
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            Intent intent = builder.build(getActivity());

            startActivityForResult(intent, PLACE_PICKER_REQUEST);
        }
        catch (GooglePlayServicesRepairableException e){
            e.printStackTrace();
        }
        catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    public void SelectDate() {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        // date picker dialog
        dPicker = new DatePickerDialog(getActivity(),
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker vw, int year, int monthOfYear, int dayOfMonth) {
                        date = dayOfMonth + "/" + (monthOfYear + 1) + "/" + year;
                        txtDate.setText(date);
                    }
                }, year, month, day);
        dPicker.setTitle("Select Date");
        dPicker.show();
    }

    public void SelectTime() {
        final Calendar cldr = Calendar.getInstance();
        int hour = cldr.get(Calendar.HOUR_OF_DAY);
        int minute = cldr.get(Calendar.MINUTE);

        tPicker = new TimePickerDialog(getActivity(), android.R.style.Theme_Holo_Dialog,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        time = hourOfDay + ":" + minute;
                        txtTime.setText(time);
                    }
                }, hour, minute, false);//Yes 24 hour time
        tPicker.setTitle("Select Time");
        tPicker.show();
    }

    public void SelectClient() {

        //Create View
        LayoutInflater li = LayoutInflater.from(getActivity());
        View lstDialog = li.inflate(R.layout.list_dialog, null);

        //Create List
        final ListView lstView = (ListView) lstDialog.findViewById(R.id.lstView);
        final ArrayList<HashMap<String, String>> data = new ArrayList<>();
        myTask = new BookTask(getActivity(), 4, lstView, data).execute();

        //Create Dialog
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Choose Client");
        alertDialog.setView(lstDialog);
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();

        //Click Listener
        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clientID = data.get(position).get("ClientID");
                String name = data.get(position).get("FName") + " " + data.get(position).get("LName");
                String cnum = data.get(position).get("CNum");
                txtClient.setText(name);
                txtCNum.setText(cnum);

                Toast.makeText(getActivity(), clientID, Toast.LENGTH_SHORT).show();
                alert.dismiss();

            }
        });
    }

    public void SelectMotor() {
        //Create View
        LayoutInflater li = LayoutInflater.from(getActivity());
        View lstDialog = li.inflate(R.layout.list_dialog, null);

        //Create List
        final ListView lstView = (ListView) lstDialog.findViewById(R.id.lstView);
        final ArrayList<HashMap<String, String>> data = new ArrayList<>();
        myTask = new BookTask(getActivity(), 5, lstView, data).execute();

        //Create Dialog
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle("Choose Motorcycle");
        alertDialog.setView(lstDialog);
        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();

        //Click Listener
        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                motorID = data.get(position).get("MotorID");
                String name = data.get(position).get("Name");
                String num = data.get(position).get("Num");
                String lat = data.get(position).get("Lat");
                String lng = data.get(position).get("Lng");
                txtMotor.setText(name);

                Toast.makeText(getActivity(), motorID, Toast.LENGTH_SHORT).show();
                alert.dismiss();

            }
        });
    }
    //endregion

    //region Book Task Class
    private class BookTask extends AsyncTask<String, Void, String> {

        //region Attributes
        String ipAddress;
        String domain;

        Integer activityFlag;
        Context context;

        //View
        TextView txtDisplay;
        ListView lstView;
        ArrayList<HashMap<String, String>> data;

        //endregion

        //region Constructor
        public BookTask(Context context, Integer activityFlag, Object... objects) {
            this.context = context;
            this.activityFlag = activityFlag;
            if(activityFlag < 4) this.txtDisplay = (TextView) objects[0];
            else if(activityFlag > 3) {
                this.lstView = (ListView) objects[0];
                this.data = (ArrayList<HashMap<String, String>>) objects[1];
            }

            ipAddress = ((MyApplication) getActivity().getApplication()).GetWebSerAddress();
            domain = "http://" + ipAddress + "/schedulerService/";
        }
        //endregion

        //region Task
        @Override
        protected String doInBackground(String... objects) {

            String result = "";

            switch(activityFlag) {
                case 1:
                    result = AddClientService(objects);
                    break;
                case 2:
                    result = AddMotorService(objects);
                    break;
                case 3:
                    result = AddScheduleService(objects);
                    break;
                case 4:
                    result = ViewClientsService();
                    break;
                case 5:
                    result = ViewMotorcyclesService();
                    break;
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if(activityFlag < 4) txtDisplay.setText(result);
            else if(activityFlag == 4) {
                String[] itemStr = new String[]{"FName", "LName", "CNum"};
                int[] itemID = new int[]{R.id.txtClientFName, R.id.txtClientLName, R.id.txtClientCNum};
                ListAdapter adapter = new SimpleAdapter(context, data, R.layout.list_item_client, itemStr, itemID);
                lstView.setAdapter(adapter);
            }
            else if(activityFlag == 5) {
                String[] itemStr = new String[]{"Name", "Num"};
                int[] itemID = new int[]{R.id.txtMotorName, R.id.txtMotorNum};
                ListAdapter adapter = new SimpleAdapter(context, data, R.layout.list_item_motor, itemStr, itemID);
                lstView.setAdapter(adapter);
            }

        }
        //endregion

        //region Methods
        public String AddClientService(String... objects) {

            try {
                //Initialize
                String FName = objects[0];
                String LNam = objects[1];
                String CNum = objects[2];
                String link = domain + "AddClient.php?FName=" + FName + "&LName=" + LNam + "&CNum=" + CNum;

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                return result;
            } catch(Exception e) {
                return "Exception: " + e.getMessage();
            }
        }

        public String AddMotorService(String... objects) {

            try{
                //Initialize
                String Name = objects[0];
                String Num = objects[1];
                String Lat = objects[2];
                String Lng = objects[3];
                String link = domain + "AddMotorcycle.php?Name=" + Name + "&Num=" + Num + "&Lat=" + Lat + "&Lng=" + Lng;

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                return result;
            } catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }

        public String AddScheduleService(String... objects) {

            try{
                //Initialize
                String ClientID = objects[0];
                String MotorID = objects[1];
                String Date = objects[2];
                String Time = objects[3];
                String Address = objects[4];
                String Lat = objects[5];
                String Lng = objects[6];
                String StatusFlag = objects[7];
                String link = domain + "AddSchedule.php";
                String data  = URLEncoder.encode("ClientID", "UTF-8") + "=" +
                        URLEncoder.encode(ClientID, "UTF-8");
                data += "&" + URLEncoder.encode("MotorID", "UTF-8") + "=" +
                        URLEncoder.encode(MotorID, "UTF-8");
                data += "&" + URLEncoder.encode("Date", "UTF-8") + "=" +
                        URLEncoder.encode(Date, "UTF-8");
                data += "&" + URLEncoder.encode("Time", "UTF-8") + "=" +
                        URLEncoder.encode(Time, "UTF-8");
                data += "&" + URLEncoder.encode("Address", "UTF-8") + "=" +
                        URLEncoder.encode(Address, "UTF-8");
                data += "&" + URLEncoder.encode("Lat", "UTF-8") + "=" +
                        URLEncoder.encode(Lat, "UTF-8");
                data += "&" + URLEncoder.encode("Lng", "UTF-8") + "=" +
                        URLEncoder.encode(Lng, "UTF-8");
                data += "&" + URLEncoder.encode("StatusFlag", "UTF-8") + "=" +
                        URLEncoder.encode(StatusFlag, "UTF-8");


                //Access Web Service
                URL url = new URL(link);
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.flush();

                BufferedReader reader = new BufferedReader(new
                        InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line = null;

                // Read Server Response
                while((line = reader.readLine()) != null) {
                    sb.append(line);
                    break;
                }

                return sb.toString();
            } catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }

        public String ViewClientsService() {

            try{

                String link = domain + "ViewInactiveClients.php";

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
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject item = jarray.getJSONObject(i);
                    HashMap<String, String> hashMap= new HashMap<>();
                    hashMap.put("ClientID", item.getString("ClientID"));
                    hashMap.put("FName",  item.getString("FName"));
                    hashMap.put("LName", item.getString("LName"));
                    hashMap.put("CNum", item.getString("CNum"));
                    data.add(hashMap);
                }

                return result;
            } catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }

        public String ViewMotorcyclesService() {

            try{

                String link = domain + "ViewInactiveMotorcycles.php";

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
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject item = jarray.getJSONObject(i);
                    HashMap<String, String> hashMap= new HashMap<>();
                    hashMap.put("MotorID", item.getString("MotorID"));
                    hashMap.put("Name",  item.getString("Name"));
                    hashMap.put("Num", item.getString("Num"));
                    hashMap.put("Lat", item.getString("Lat"));
                    hashMap.put("Lng", item.getString("Lng"));
                    data.add(hashMap);
                }

                return result;
            } catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }
        //endregion
    }
    //endregion

}
