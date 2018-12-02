package design.scheduler;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GetPreferences();
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        getSupportActionBar().setTitle("Map Locations");
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Map()).commit();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            getSupportActionBar().setTitle("Settings");
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Settings()).commit();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        try {
            switch (item.getItemId()) {
                case R.id.nav_map:
                    getSupportActionBar().setTitle("Map Locations");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Map()).commit();
                    break;
                case R.id.nav_book:
                    getSupportActionBar().setTitle("Book");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Book()).commit();
                    break;
                case R.id.nav_schedule:
                    getSupportActionBar().setTitle("Schedule");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Schedule()).commit();
                    break;
                case R.id.nav_history:
                    getSupportActionBar().setTitle("History");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new History()).commit();
                    break;
                case R.id.nav_clients:
                    getSupportActionBar().setTitle("Clients");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Clients()).commit();
                    break;
                case R.id.nav_motorcycles:
                    getSupportActionBar().setTitle("Motorcycles");
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new Motorcycles()).commit();
                    break;

            }

            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            drawer.closeDrawer(GravityCompat.START);
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(),Toast.LENGTH_LONG).show();
        }
        return true;
    }

    public void GetPreferences() {
        try {
            SharedPreferences sp = this.getSharedPreferences("Settings", MODE_PRIVATE);
            String ipAddress = sp.getString("ipAddress", "192.168.1.10");
            double lat = Double.parseDouble(sp.getString("lat", "14.5905251"));
            double lng = Double.parseDouble(sp.getString("lng", "120.9781245"));
            LatLng location = new LatLng(lat, lng);
            double radius = Double.parseDouble(sp.getString("radius", "25"));

            ((MyApplication) this.getApplication()).SetRemoteLocation(location);
            ((MyApplication) this.getApplication()).SetWebSerAddress(ipAddress);
            ((MyApplication) this.getApplication()).SetRadius(radius);
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
