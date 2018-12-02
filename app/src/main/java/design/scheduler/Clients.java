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

public class Clients extends Fragment {

    View view;
    ListView lstView;
    TextView txtClientName, txtCNum;
    ArrayList<HashMap<String, String>> data;
    AsyncTask myTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.clients, container, false);
        lstView = (ListView) view.findViewById(R.id.lstViewClients);

        return  view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ShowClients();
        lstView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                LayoutInflater li = LayoutInflater.from(getActivity());
                View viewSchedDialog = li.inflate(R.layout.view_clients_dialog, null);
                final AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                ClientDetails(position, viewSchedDialog);
                alertDialog.setTitle("Client Details");
                alertDialog.setView(viewSchedDialog);
                alertDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DeleteClient(position);
                        dialog.dismiss();
                    }
                });
                alertDialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
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

    public void ShowClients() {
        data = new ArrayList<>();
        myTask = new ClientTask(getActivity(), 1, lstView, data).execute();
    }

    public void DeleteClient(int position) {
        String s = data.get(position).get("ClientID");
        data = new ArrayList<>();
        myTask = new ClientTask(getActivity(), 2, lstView, data).execute(s);
    }

    public void ClientDetails(int position, View view) {
        try {
            txtClientName = (TextView) view.findViewById(R.id.txtClientName);
            txtCNum = (TextView) view.findViewById(R.id.txtContactNumber);

            String ClientName = data.get(position).get("FName") + " " + data.get(position).get("LName");
            String CNum = data.get(position).get("CNum");

            txtClientName.setText(ClientName);
            txtCNum.setText(CNum);
        } catch (Exception e) {
            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private class ClientTask extends AsyncTask<String, Void, String> {

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

        public ClientTask(Context context, Integer activityFlag, Object... objects) {
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
            if(activityFlag == 2) result = DeleteClient(objects[0]);
            String err = ViewClients();
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

        private String ViewClients() {
            try{

                String link = domain + "ViewClients.php";

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
                    hashMap.put("ClientID", item.getString("ClientID"));
                    hashMap.put("FName",  item.getString("FName"));
                    hashMap.put("LName", item.getString("LName"));
                    hashMap.put("CNum", item.getString("CNum"));
                    data.add(hashMap);

                    arrayList.add(item.getString("FName") + " " + item.getString("LName"));
                }

                hasError = false;
                return result;
            } catch(Exception e){
                hasError = true;
                return "Exception: " + e.getMessage();
            }
        }

        private String DeleteClient(String clientID) {
            try{

                String link = domain + "DeleteClient.php?ClientID=" + clientID;

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

