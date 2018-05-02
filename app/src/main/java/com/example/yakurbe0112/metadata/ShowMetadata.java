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
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import static com.example.yakurbe0112.metadata.R.*;

public class ShowMetadata extends AppCompatActivity {
    Barcode barcode;
    int RECURSION_DEPTH=2;
    String dblookup="https://barcodesdatabase.org/wp-content/themes/bigdb/lib/barcodelookup/lib/scraper/barcodeLookupService.php?source=";

    String wikipediaAPI="https://en.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=true&format=json&formatversion=2&titles=";

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
        String[] bonus=new String[1];       //additional array for bonus information found in query
        final String goal;                  //type of data to be found
        String data;                        //output
        String source;                      //source of data
        local[] newContexts;                //list of contexts to try next

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

//    public static String capEachWord(String source){
//        String result = "";
//        String[] splitString = source.split(" ");
//        for(String target : splitString){
//            result += Character.toUpperCase(target.charAt(0))
//                    + target.substring(1) + " ";
//        }
//        return result.trim();
//    }

    private String parseData(String data, URL url, local context){
        //takes JSON string and source arrays and context of search to figure out what the relevant information is
        //then adds relevant metacontexts to
        JSONObject obj;
        try {
            obj=new JSONObject(data);
        } catch (JSONException e) {//not actually json
            e.printStackTrace();
            return null;
        }

        try {
            if(obj.getString("source").matches("lookupbyisbn|isbndb")){//its a book
                //String auth="Author: ".concat(obj.getString("description"));
                URL[] url1=UrlCases(wikipediaAPI,obj.getString("description").toLowerCase().split(",|;|:|<|>|(|)")[0]);
                        //new URL(wikipediaAPI.concat(obj.getString("description").toLowerCase()));
                URL[] url2=UrlCases(wikipediaAPI,obj.getString("name").toLowerCase());

                LinearLayout UUID1=new LinearLayout(getApplicationContext());
                local auth=new local(url1,context.depth+1,UUID1,"extract");
                LinearLayout UUID2=new LinearLayout(getApplicationContext());
                local desc=new local(url2,context.depth+1,UUID2,"extract");



                context.newContexts= new local[]{desc,auth};
                context.bonus= new String[]{
                        "Author: ".concat(obj.getString("description"))
                };
            }


            switch (url.getHost()){
                case "barcodesdatabase.org":
                    String out=obj.getString(context.goal);
                    context.data=out;
                    return out;

                default: break;
            }
        } catch (Exception e) {
        return null;
        }
        return null;
    }

    private URL[] UrlCases(String dest,String query) throws MalformedURLException {
        String[] split=query.split(" ");
        int size=(int)Math.pow(2,split.length);
        String[] strings=new String[size];
        int i,j=0;
        for(i=0;i<size;++i) strings[i] = "";
        i=0;
        for(String substring:split){
            for(j=0;j<size;++j){
                String array=strings[j];
                if((j&(int)Math.pow(2,i))==0){
                    strings[j]=array.concat(substring.substring(0,1).toUpperCase()+substring.substring(1).concat(" "));
                }else{
                    strings[j]=array.concat(substring.concat(" "));
                }
            }
            ++i;
        }
        j=0;
        URL[]urls=new URL[size];
        for(String url:strings){
            urls[j++]=new URL(dest.concat(url));
        }
        return urls;
    }

    private class Networkcalls extends AsyncTask<local,Integer,local>{
        @Override
        //handles network calls given a local context
        protected local doInBackground(local... context){
            URL[] urls=context[0].urls;
            HttpsURLConnection urlConnection= null;
            for (URL url:urls) {
                try {
                    urlConnection = (HttpsURLConnection) url.openConnection();
                } catch (Exception e) {
                    e.printStackTrace();
                }finally {
                    assert urlConnection != null;
                    urlConnection.disconnect();
                }
                if(parseData(readHtmlStream(urlConnection),url,context[0])!=null){
                    break;
                }
            }

            return context[0];
        }

        protected void onProgressUpdate(Integer...progress){
            //spinner.setIndeterminate(true);
        }

        @Override
        //adds results to page and spawns recursive network calls
        protected void onPostExecute(local context){
            String data=context.data;

            LinearLayout UIID=context.UIID;
            TextView textView=new TextView(getApplicationContext());
            textView.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT));
            textView.setText(data);
            if(data==null){
                textView.setText("Data not found");
                UIID.addView(textView);
                UIID.invalidate();
                return;}
            UIID.addView(textView);
            UIID.invalidate();

            if(context.depth>RECURSION_DEPTH){return;}


            int i=0;
            for(local newContext:context.newContexts){
                LinearLayout newUUID= newContext.UIID;
                UIID.addView(newUUID);

//                if(newContext.bonus[i]!=null){
//                    textView=new TextView(getApplicationContext());
//                    textView.setLayoutParams(
//                            new ViewGroup.LayoutParams(
//                                    LinearLayout.LayoutParams.MATCH_PARENT,
//                                    LinearLayout.LayoutParams.WRAP_CONTENT));
//                    textView.setText(newContext.bonus[i]);
//                    newUUID.addView(textView);
//                    newUUID.invalidate();
//                }

                new Networkcalls().execute(newContext);
                ++i;
            }
        }
    }
}