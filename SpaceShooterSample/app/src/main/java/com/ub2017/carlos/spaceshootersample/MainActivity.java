package com.ub2017.carlos.spaceshootersample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);//Menu Resource, Menu

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d("optionselected","inside the function");
        switch (item.getItemId()){
            case R.id.stage1:
                Intent intent = new Intent(getApplicationContext(),TheGameLevel1.class);
                startActivity(intent);

                break;
            case R.id.stage2:
              /*  Log.d("exit","exiting the app");
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);*/
                intent = new Intent(getApplicationContext(),TheGameLevel2.class);
                startActivity(intent);

                break;

        }


        return  true;
    }
}
