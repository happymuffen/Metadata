package com.example.yakurbe0112.metadata;

import android.content.res.Resources;
import android.graphics.drawable.DrawableContainer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static com.example.yakurbe0112.metadata.R.*;

public class ShowMetadata extends AppCompatActivity {
    Barcode barcode;
    int RECURSION_DEPTH=2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedInstanceState=getIntent().getExtras();
        if(savedInstanceState != null){
            barcode= (Barcode) savedInstanceState.get("barcode");
        }
        setContentView(layout.activity_show_metadata);
        Toolbar toolbar =  findViewById(id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(barcode.displayValue);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        get1dData(barcode.displayValue);
    }
    String getBarcodeType(Barcode barcode){
        String value;
        switch (barcode.valueFormat){
            case Barcode.CALENDAR_EVENT: value="CalendarEvent";break;
            case Barcode.CONTACT_INFO: value="ContactInfo";break;
            case Barcode.DRIVER_LICENSE: value="DriverLicense";break;
            case Barcode.EMAIL: value="Email";break;
            case Barcode.GEO: value="Geo";break;
            case Barcode.PHONE: value="Phone";break;
            case Barcode.SMS: value="Sms";break;
            case Barcode.URL: value="Url";break;
            case Barcode.WIFI: value="Wifi";break;
            default: value="Raw";
        }

        if (value.equals("Raw")){
            boolean b=false;
            for (int i=0;i<barcode.rawValue.length();++i){
                b|=Character.isDigit(barcode.rawValue.charAt(i));
            }
            if (!b){
                value="1d";
            }
        }

        return value;
    }


    void get1dData(String Raw) {
        //0th layer lookup for unknown barcodes
        URL url0, url1, url2, url3, url4;
        url0=url1=url2=url3=url4=null;
        String dblookup="https://barcodesdatabase.org/wp-content/themes/bigdb/lib/barcodelookup/lib/scraper/barcodeLookupService.php?source=";
        try {
            //url0 = new URL("https://barcodesdatabase.org/barcode/".concat(Raw));
            url1 = new URL(dblookup.concat("isbndb&q=".concat(Raw)));
            url2 = new URL(dblookup.concat("lookupbyisbn&q=".concat(Raw)));
            url3 = new URL(dblookup.concat("amazon&q=".concat(Raw)));
            url4 = new URL(dblookup.concat("eandata&q=".concat(Raw)));

        } catch (Exception e) {
            e.printStackTrace();
        }
        URL urls[]=new URL[]{url1,url2,url3,url4};
        local basic= new local(urls);
        new Networkcalls().execute(basic);
    }


    String readHtmlStream(HttpsURLConnection connection) {
        //helper function to open connection and read response
        InputStream inputStream;
        String html="";
        try {
            inputStream = new BufferedInputStream(connection.getInputStream());
            InputStreamReader inputStreamReader=new InputStreamReader(inputStream);

            int data=inputStreamReader.read();
            while(data != -1) {
                char current = (char) data;
                data = inputStreamReader.read();
                String c=Character.toString(current);
                html=html.concat(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return html;
    }

    class local{
        //container for useful variables
        final URL[] urls;                   //the url list currently being searched
        int depth;                          //# of layers of recursive searching
        final LinearLayout UIID;            //location to add data to
        String type;                        //additional tag for categorising thing being searched
        final String goal;                  //type of data to be found
        String[] metavalues=new String[0];  //list of future goals
        URL[][] metalocations=new URL[0][]; //list of locations to look for future goals in

        local(URL[] urls){
            this.urls=urls;
            depth=0;
            UIID=findViewById(R.id.layout);
            goal="name";
        }
        local(URL[] urls,int depth, LinearLayout UIID, String goal){
            this.urls=urls;
            this.depth=depth;
            this.UIID=UIID;
            this.goal=goal;
        }
    }

    private static String parseData(String data, URL url, local context){
        //takes JSON string and source arrays and context of search to figure out what the relevant information is
        //then adds relevant metacontexts to context
        JSONObject obj;
        try {
            obj=new JSONObject(data);
        } catch (JSONException e) {//not actually json
            e.printStackTrace();
            return null;
        }
        switch (url.getHost()){
            case "barcodesdatabase.org":
                try {
                    String out=obj.getString(context.goal);
                    if(obj.getString("source").matches("lookupbyisbn|isbndb")){
                        context.type="book";
                    }
                    return out;
                } catch (JSONException e) {
                    return null;
                }
            default: break;
        }
        return null;
    }

    String collapseArray(String[] strings){
        //helper function for simplifying multiple strings with similar information into one
        for(String string:strings){
            if (string!=null){
                return string;
            }
        }
        return null;
    }

    class postVar{
        local context;
        String data;
        postVar(local context, String data){
            this.context=context;
            this.data=data;
        }
    }

    private class Networkcalls extends AsyncTask<local,Integer,postVar>{
        @Override
        //handles network calls given a local context
        protected postVar doInBackground(local... context){
            URL[] urls=context[0].urls;
            HttpsURLConnection urlConnection= null;
            String[] finished= new String[urls.length];
            int i=0;
            for (URL url:urls) {
                try {
                    urlConnection = (HttpsURLConnection) url.openConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    assert urlConnection != null;
                    urlConnection.disconnect();
                }
                finished[i]=parseData(readHtmlStream(urlConnection),url,context[0]);
                ++i;
            }

            return new postVar(context[0],collapseArray(finished));
        }

        protected void onProgressUpdate(Integer...progress){
            //spinner.setIndeterminate(true);
        }

        @Override
        //adds results to page and spawns recursive network calls
        protected void onPostExecute(postVar results){
            local context=results.context;
            String data=results.data;
            LinearLayout UIID=context.UIID;
            TextView textView=new TextView(getApplicationContext());
            textView.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setText(data);
            UIID.addView(textView);
            UIID.invalidate();

            if(context.depth>RECURSION_DEPTH){return;}
            LinearLayout newUUID=new LinearLayout(getApplicationContext());
            UIID.addView(newUUID);
            int i=0;
            for(String metavalue:context.metavalues){
                local newContext= new local(
                        context.metalocations[i],context.depth+1,newUUID,metavalue);
                new Networkcalls().execute(newContext);
                ++i;
            }
        }
    }
}