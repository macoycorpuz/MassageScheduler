package design.scheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        //Get the SMS message
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs;
        //Retrieve the SMS message received.
        Object[] pdus = (Object[]) bundle.get("pdus");
        if(pdus != null) {
            msgs = new SmsMessage[pdus.length];
            for(int i = 0; i < msgs.length; i++) {
                msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                new UpdateTask(context).execute(msgs[i].getOriginatingAddress(), msgs[i].getMessageBody());
            }
        }
    }

    private class UpdateTask extends AsyncTask<String, Void, String> {

        //region Attributes
        Context context;
        String ipAddress;
        String domain;
        //endregion

        //region Constructor
        private UpdateTask(Context context) {
            this.context = context;
            this.ipAddress = ((MyApplication) context.getApplicationContext()).GetWebSerAddress();
            this.domain = "http://" + ipAddress + "/schedulerService/";
        }
        //endregion

        //region Task
        @Override
        protected String doInBackground(String... objects) {
            return UpdateMotorLocationService(objects);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(context, s, Toast.LENGTH_LONG).show();
        }
        //endregion

        //region Methods
        private String UpdateMotorLocationService(String... objects) {
            try {
                String Num = "0" + objects[0].substring(3);
                String[] Location = objects[1].split(" ");

                String link, line, result = "";
                URL url;
                InputStream is;
                BufferedReader br;

                //Update motor location
                link = domain + "UpdateMotorLocation.php?Num=" + Num + "&Lat=" + Location[0] + "&Lng=" + Location[1];
                url = new URL(link);
                is = url.openStream();
                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) { result += line; }
                is.close();
                return result;
            } catch(Exception e) {
                return "Exception: " + e.getMessage();
            }
        }
        //endregion
    }
}
