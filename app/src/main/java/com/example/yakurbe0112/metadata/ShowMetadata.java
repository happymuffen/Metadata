package com.example.yakurbe0112.metadata;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.vision.barcode.Barcode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

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


    }

    void findProduct(){

    }
    void findBook(){

    }
    void findLocation(){

    }

    void connect(final URL url, final View view) {
        Runnable handleConnection=new Runnable() {
            @Override
            public void run() {
                {
                    BufferedReader reader=null;
                    StringBuffer buffer=new StringBuffer();
                    try {
                        URLConnection urlConnection = url.openConnection();
                        InputStream in = urlConnection.getInputStream();
                        InputStreamReader isr = new InputStreamReader(in);
                        reader=new BufferedReader(isr);
                        String line;
                        while((line=reader.readLine())!=null){
                            buffer.append(line);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    html=buffer.toString();
                    //todo add looper job
                }
            }
        };
        connector.post(handleConnection);
    }


}