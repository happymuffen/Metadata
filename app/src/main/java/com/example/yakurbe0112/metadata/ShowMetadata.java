package com.example.yakurbe0112.metadata;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.View;

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

public class ShowMetadata extends AppCompatActivity {
    Barcode barcode;
    private Handler connector= new Handler(Looper.getMainLooper());
    private Handler looper=new Handler(Looper.getMainLooper());
    private String html=new String();
    private ArrayList<String> keywords=new ArrayList<String>() {
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedInstanceState=getIntent().getExtras();
        if(savedInstanceState != null){
            barcode= (Barcode) savedInstanceState.get("barcode");
        }
        setContentView(R.layout.activity_show_metadata);
        Toolbar toolbar =  findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(barcode.displayValue);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);


        get1dData("9780547928227");
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
        return;
    }


    String readHtmlStream(HttpsURLConnection connection) {
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
        final URL[] urls;
        int depth;
        final String UIID;
        String type;
        String goal="name";

        local(URL[] urls){
            this.urls=urls;
            depth=0;
            UIID="ScrollLayout";
        }
        local(URL[] urls,int depth, String UIID){
            this.urls=urls;
            this.depth=depth;
            this.UIID=UIID;
        }
    }

    private static String parseData(String data, URL url, local context){
        //takes JSON string and source arrays and context of search to figure out what the relevant information is
        int i=0;
        JSONObject obj = null;
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
                    urlConnection.disconnect();
                }
                finished[i]=parseData(readHtmlStream(urlConnection),url,context[0]);
                ++i;
            }
                postVar next= new postVar(context[0],collapseArray(finished));

            return next;
        }

        @Override

        protected void onPostExecute(postVar results){

        }
    }

    void findBook(){

    }
    void findLocation(){

    }
}