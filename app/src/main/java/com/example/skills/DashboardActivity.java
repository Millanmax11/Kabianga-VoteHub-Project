/* THIS IS AN ACTIVITY TO HANDLE NAV HEADER AND MENU BAR */
package com.example.skills;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Ensure this is the correct layout file

        Toolbar toolbar = findViewById(R.id.toolbar); // Ensure this matches the ID in the XML
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // Load the default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.home);
        }
        // Programmatically add version item
        Menu menu = navigationView.getMenu();
        MenuItem versionItem = menu.findItem(R.id.version);
        if (versionItem == null) {
            versionItem = menu.add(Menu.NONE, R.id.version, Menu.NONE, "App Version 1.0.0");
            versionItem.setEnabled(false);
            menu.setGroupVisible(Menu.NONE, true); // Ensure the group is visible
        }

        // Handle back press with OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getSupportFragmentManager().popBackStack();
                    } else {
                        finish();
                    }
                }
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.START); // Close the drawer before showing the toast
        if (!isConnected()) {
            Toast.makeText(DashboardActivity.this, "Please connect to the internet", Toast.LENGTH_LONG).show();
            return false;
        }
        switch (item.getItemId()) {
            case R.id.home:
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new HomeFragment()).commit();
                break;
            case R.id.account:
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new AccountFragment()).commit();
                break;
            case R.id.about:
                getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new AboutFragment()).commit();
                break;
            case R.id.logout:
                mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();//sign out the user
                Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(DashboardActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.version:
                break;
            default:
                return false;
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }



}

