package com.inlight;



import java.util.ArrayList;
        import java.util.Arrays;
        import android.app.Activity;
        import android.content.Intent;
        import android.os.Bundle;
        import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
        import android.widget.AdapterView.OnItemClickListener;
        import android.widget.GridView;
//This application uses some deprecated methods.


public class MainActivity extends Activity {
    protected static final String EXTRA_RES_ID = "POS";
    private ArrayList<Integer> mThumbIdsFabrics = new ArrayList<Integer>(

            Arrays.asList(R.drawable.sun));
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        GridView gridview = (GridView) findViewById(R.id.gridview);
// Create a new ImageAdapter and set it as the Adapter for this GridView
        gridview.setAdapter(new ImageAdapter(this, mThumbIdsFabrics));

// Set an setOnItemClickListener on the GridView
        gridview.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
//Create an Intent to start the ImageViewActivity
                Intent intent = new Intent(MainActivity.this,
                        RenderActivity.class);
// Add the ID of the thumbnail to display as an Intent Extra
                intent.putExtra(EXTRA_RES_ID, (int) id);
// Start the ImageViewActivity
                startActivity(intent);
            }
        });
    }
}