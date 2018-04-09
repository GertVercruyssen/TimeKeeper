package tydysoft.timesaver;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import static java.lang.System.in;

public class MainActivity extends AppCompatActivity
{
    Chronometer mChronometer;
    long TimerZero;
    String currentcompany;
    String[] complist = new String[0];
    String[] viewlist = new String[0];
    TextView mtextview;
    ListView mlistview;
    HashMap<String,Long> mStartTimes;
    HashMap<String,Long> mAccumulatedTime;
    HashMap<String,Boolean> mIsRunning;
    private AdView mAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
        mStartTimes = new HashMap<String, Long>();
        mAccumulatedTime = new HashMap<String, Long>();
        mIsRunning = new HashMap<String, Boolean>();
        mtextview =  (TextView) findViewById(R.id.companyname);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                //Snackbar .make(view, "Add a company", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                AlertDialog.Builder alert = new AlertDialog.Builder(view.getContext());
                alert.setTitle("Title of company");
                alert.setMessage("New company name");
                final EditText input = new EditText(view.getContext());
                alert.setView(input);

                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        addcomp(value,0,0,false);
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            }
        });

        FloatingActionButton fab2 = (FloatingActionButton) findViewById(R.id.fab2);
        fab2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final Intent emailIntent = new Intent( android.content.Intent.ACTION_SEND);
                emailIntent.setType("plain/text");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { "" }); //fill in suggested email
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Export uren app");
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, SerializeForMail());
                startActivity(Intent.createChooser(emailIntent, "Send mail..."));
            }
        });

        Button button;
        button = (Button) findViewById(R.id.start);
        button.setOnClickListener(mStartListener);
        button = (Button) findViewById(R.id.stop);
        button.setOnClickListener(mStopListener);
        button = (Button) findViewById(R.id.delete);
        button.setOnClickListener(mDeleteListener);
        button = (Button) findViewById(R.id.reset);
        button.setOnClickListener(mResetListener);

        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        TimerZero = mChronometer.getBase();

        mlistview = (ListView) findViewById(R.id.list);
        ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.listview,viewlist);
        mlistview.setAdapter(adapter);
        mlistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Setactivecompany(position);
            }
        });

        LoadFile();
        if(complist.length>0)
            Setactivecompany(0);
        else
            currentcompany = "Please add a company";
        mtextview.setText(currentcompany);
        MobileAds.initialize(this, ""); //fill in your add key
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    protected void onStop()
    {
        super.onStop();
        SaveFile();
    }

    View.OnClickListener mStartListener = new View.OnClickListener() {
        public void onClick(View v) {
            Startclock();
        }
    };

    View.OnClickListener mStopListener = new View.OnClickListener() {
        public void onClick(View v) {
            StopClock();
        }
    };

    private void Startclock()
    {
        if(complist.length > 0)
        {
            //zeroclock Chronometer.setBase(SystemClock.elapsedRealtime()); ///// TimerZero
            AdjustClockTime();
            mChronometer.start();
            if(!mIsRunning.get(complist[0]))
            {
                mStartTimes.put(complist[0], System.currentTimeMillis());
                mIsRunning.put(complist[0], true);
            }
            PrepViewList();
            ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.listview,viewlist);
            mlistview.setAdapter(adapter);
        }
    }
    private void AdjustClockTime()
    {
        long zerotime = SystemClock.elapsedRealtime();
        long totalTime =  mAccumulatedTime.get(complist[0]);
        if (mIsRunning.get(complist[0]))
            totalTime = totalTime + System.currentTimeMillis() - mStartTimes.get(complist[0]);
        mChronometer.setBase(zerotime-totalTime);
    }
    private void StopClock()
    {
        if(complist.length > 0)
        {
            mChronometer.stop();
            if (mIsRunning.get(complist[0]))
            {
                long extratime = System.currentTimeMillis() - mStartTimes.get(complist[0]) + mAccumulatedTime.get(complist[0]);
                mAccumulatedTime.put(complist[0], extratime);
                mIsRunning.put(complist[0], false);
            }
            PrepViewList();
            ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.listview,viewlist);
            mlistview.setAdapter(adapter);
        }
    }

    View.OnClickListener mDeleteListener = new View.OnClickListener() {
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        }
    };

    View.OnClickListener mResetListener = new View.OnClickListener() {
        public void onClick(View v) {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setMessage("Are you sure?").setPositiveButton("Yes", dialogClickListener2)
                    .setNegativeButton("No", dialogClickListener2).show();
        }
    };

    DialogInterface.OnClickListener dialogClickListener2 = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    String currentcomp = complist[0];
                    deletecurrentcomp();
                    addcomp(currentcomp,0,0,false);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    deletecurrentcomp();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu)
//    {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item)
//    {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings)
//        {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }
    private void Setactivecompany(int position)
    {
        if(complist.length > 0)
        {
            String listItem =complist[position];
            String temp = complist[0];
            complist[position] = temp;
            complist[0] = listItem;
            currentcompany = listItem;
            PrepViewList();
            mtextview.setText(currentcompany);
            ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.listview,viewlist);
            mlistview.setAdapter(adapter);
            AdjustClockTime();

            if(mIsRunning.get(currentcompany))
                Startclock();
            else
                StopClock();
        }
        else
        {
            PrepViewList();
            ArrayAdapter adapter = new ArrayAdapter<String>(this,R.layout.listview,viewlist);
            mlistview.setAdapter(adapter);
            mtextview.setText("Please add a company");
            mChronometer.stop();
            mChronometer.setBase(SystemClock.elapsedRealtime());
        }
    }
    private void deletecurrentcomp()
    {
        if(complist.length > 0)
        {
            if (mIsRunning.containsKey(complist[0]))
            {
                mIsRunning.remove(complist[0]);
                mAccumulatedTime.remove(complist[0]);
                mStartTimes.remove(complist[0]);
            }
            String[] result;
            result = new String[complist.length - 1];
            for (int teller = 1; teller < complist.length; ++teller)
            {
                result[teller - 1] = complist[teller];
            }
            complist = result;
            Setactivecompany(0);
        }
        else
            mtextview.setText("Please add a company");
    }
    private void addcomp(String value, long startTime, long accumulatedTime, boolean isrunning)
    {
        String[] temparr = new String[complist.length+1];
        for(int teller = 0; teller < complist.length; ++teller)
        {
            temparr[teller] = complist[teller];
        }
        temparr[temparr.length-1] = value;
        complist = temparr;
        if(!mIsRunning.containsKey(value))
        {
            mIsRunning.put(value,isrunning);
            mAccumulatedTime.put(value,accumulatedTime);
            mStartTimes.put(value,startTime);
        }
        Setactivecompany(complist.length-1);
    }
    private void LoadFile()
    {
        String filename = "companylist.txt";
        String text= "";
        String singleLine = "";
        BufferedReader buf ;
        FileReader reader;
        File newfile = new File(getFilesDir(), filename);

        try
        {
            reader = new FileReader(newfile);
            buf = new BufferedReader(reader);
            while ((singleLine = buf.readLine()) != null)
            {
                text += singleLine;
                text += "\n";
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        Parsetocomplist(text);
    }

    private void Parsetocomplist(String string)
    {
        if(string != "")
        {
            String[] strings = string.split("\n");
            for (int teller = 0; teller < strings.length; teller += 4)
            {
                addcomp(strings[teller], Long.parseLong(strings[teller + 1]), Long.parseLong(strings[teller + 2]), Boolean.parseBoolean(strings[teller + 3]));
            }
        }
    }

    private void SaveFile()
    {
        String filename = "companylist.txt";
        String string = Serialize(complist);
        FileWriter writer;
        File newfile = new File(getFilesDir(), filename);
        try
        {
            writer = new FileWriter(newfile);
            writer.write(string);
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private String Serialize(String[] complist)
    {
        String text = "";

        for(int teller = 0; teller < complist.length; ++teller)
        {
            text += complist[teller];
            text += "\n";
            text += mStartTimes.get(complist[teller]);
            text += "\n";
            text += mAccumulatedTime.get(complist[teller]);
            text += "\n";
            text += mIsRunning.get(complist[teller]);
            text += "\n";
        }

        return text;
    }

    private String SerializeForMail()
    {
        String text = "";
        long elapsedTIme;
        text += "Times from TimeSaver app";
        text += "\n";

        for(int teller = 0; teller < complist.length; ++teller)
        {
            text += "Naam: "+complist[teller];
            text += ", Tijd: ";
            if(mIsRunning.get(complist[teller]))
                elapsedTIme =  System.currentTimeMillis() - mStartTimes.get(complist[teller]) + mAccumulatedTime.get(complist[teller]);
            else
                elapsedTIme = mAccumulatedTime.get(complist[teller]);
            text += ConvertLongToTimeString(elapsedTIme);
            text += "\n";
        }

        return text;
    }
    private String ConvertLongToTimeString(long l)
    {
        int seconds = 0;
        int minutes = 0;
        int hours = 0;
        hours = (int)l/3600000;
        l -= hours*3600000;
        minutes = (int)l/60000;
        l -= minutes*60000;
        seconds = (int)l/1000;
        return ""+hours+":"+minutes+":"+seconds;
    }

    private void PrepViewList()
    {
        viewlist = complist.clone();
        String tempstring = "";
        for(int teller = 0;teller < complist.length; ++teller)
        {
            tempstring = "\u231B - ";
            tempstring += complist[teller];
            if (mIsRunning.get(complist[teller]))
                viewlist[teller] = tempstring;
        }
    }
}
