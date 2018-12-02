package design.scheduler;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class Schedule extends Fragment {

    View view;
    ListView lstView;
    TextView txtStatus, txtSchedID, txtMotor, txtClient, txtDate, txtTime, txtAddress;
    ArrayList<HashMap<String, String>> data;
    AsyncTask myTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.schedule, container, false);
        lstView = (ListView) view.findViewById(R.id.lstViewSchedule);

        return  view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        data = new ArrayList<>();
        myTask = new ScheduleTask(getActivity(), 1, lstView, data).execute();

        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                LayoutInflater li = LayoutInflater.from(getActivity());
                View viewSchedDialog = li.inflate(R.layout.view_schedule_dialog, null);
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                ScheduleDetails(position, viewSchedDialog);
                alertDialog.setTitle("Schedule Details");
                alertDialog.setView(viewSchedDialog);
                alertDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeleteSchedule(position);
                        dialog.dismiss();
                    }
                });
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
        if(myTask!= null) myTask.cancel(true);
    }

    public void DeleteSchedule(int position) {
        String s = data.get(position).get("SchedID");
        data = new ArrayList<>();
        myTask = new ScheduleTask(getActivity(), 2, lstView, data).execute(s);
    }

    public void ScheduleDetails(int position, View view) {
        try {
            txtStatus = (TextView) view.findViewById(R.id.txtSchedStatus);
            txtSchedID = (TextView) view.findViewById(R.id.txtSchedID);
            txtMotor = (TextView) view.findViewById(R.id.txtSchedMotor);
            txtClient = (TextView) view.findViewById(R.id.txtSchedClient);
            txtDate = (TextView) view.findViewById(R.id.txtSchedDate);
            txtTime = (TextView) view.findViewById(R.id.txtSchedTime);
            txtAddress = (TextView) view.findViewById(R.id.txtSchedAddress);

            String Status;
            String SchedID = data.get(position).get("SchedID");
            String ClientName = data.get(position).get("FName") + " " + data.get(position).get("LName");
            String MotorName = data.get(position).get("Name");
            String Date = data.get(position).get("Date");
            String Time = data.get(position).get("Time");
            String Address = data.get(position).get("Address");
            if (data.get(position).get("StatusFlag").equals("0")) Status = "Idle";
            else Status = "On the way";

            txtStatus.setText(Status);
            txtSchedID.setText(SchedID);
            txtMotor.setText(MotorName);
            txtClient.setText(ClientName);
            txtDate.setText(Date);
            txtTime.setText(Time);
            txtAddress.setText(Address);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class ScheduleTask extends AsyncTask<String, Void, String> {

        //region Attributes
        Context context;
        String ipAddress;
        String domain;
        Integer activityFlag;

        ListView lstView;
        ArrayAdapter<String> adapter;
        ArrayList<String> arrayList;
        ArrayList<HashMap<String, String>> data;
        Boolean hasError = false;
        //endregion

        public ScheduleTask(Context context, Integer activityFlag, Object... objects) {
            this.context = context;
            this.activityFlag = activityFlag;
            this.lstView = (ListView) objects[0];
            this.data = (ArrayList<HashMap<String, String>>) objects[1];
            this.ipAddress = ((MyApplication) getActivity().getApplication()).GetWebSerAddress();
            this.domain = "http://" + ipAddress + "/schedulerService/";
        }

        @Override
        protected String doInBackground(String... objects) {
            String result = "";
            if(activityFlag == 2) result = DeleteSchedule(objects[0]);
            String err = ViewSchedules();
            if(hasError) return err;
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            try {
                if(hasError) throw new Exception(s);
                if(activityFlag == 2) Toast.makeText(context, s, Toast.LENGTH_LONG).show();
                adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, arrayList);
                lstView.setAdapter(adapter);
            } catch (Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_LONG).show();
            }

        }

        private String ViewSchedules() {
            try{

                String link = domain + "ViewSchedules.php";

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();
                if(result.equals("")) throw new Exception("Book a schedule.");
                //Convert JSON to HashMap
                JSONArray jarray = new JSONArray(result);
                arrayList = new ArrayList<String>();
                for (int i = 0; i < jarray.length(); i++) {
                    JSONObject item = jarray.getJSONObject(i);
                    HashMap<String, String> hashMap= new HashMap<>();
                    hashMap.put("SchedID", item.getString("SchedID"));
                    hashMap.put("Address",  item.getString("Address"));
                    hashMap.put("Date", item.getString("Date"));
                    hashMap.put("Time", item.getString("Time"));
                    hashMap.put("FName", item.getString("FName"));
                    hashMap.put("LName", item.getString("LName"));
                    hashMap.put("Name", item.getString("Name"));
                    hashMap.put("StatusFlag", item.getString("StatusFlag"));
                    data.add(hashMap);

                    arrayList.add(item.getString("Name"));
                }

                hasError = false;
                return result;
            } catch(Exception e){
                hasError = true;
                return "Exception: " + e.getMessage();
            }
        }

        private String DeleteSchedule(String schedID) {
            try{

                String link = domain + "DeleteSchedule.php?SchedID=" + schedID;

                //Access Web Service
                String line;
                String result = "";
                URL url = new URL(link);
                InputStream is = url.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();

                hasError = false;
                return result;
            } catch(Exception e){
                hasError = true;
                return "Exception: " + e.getMessage();
            }
        }

    }
}
