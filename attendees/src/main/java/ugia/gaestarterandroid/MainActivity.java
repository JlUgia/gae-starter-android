package ugia.gaestarterandroid;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ugia.gaestarterandroid.api.model.Attendee;
import ugia.gaestarterandroid.api.request.Request;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {


    /**
     * Per the design guidelines, you should show the drawer on launch until the user manually
     * expands it. This shared preference tracks this.
     */
    private static final String PREF_USER_LEARNED_DRAWER = "navigation_drawer_learned";

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private View mFragmentContainerView;

    private boolean mUserLearnedDrawer;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private ImageView mAvatarImageView;
    private TextView mAttendeeName;
    private TextView mAttendeeEmail;

    private Gson mGson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupGson();
        setupViews();
        setupDrawer(savedInstanceState != null);

        mTitle = getTitle();
    }

    @Override
    protected void onStart() {
        super.onStart();

        fetchAttendees();
    }

    public void restoreActionBar() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    private void setupGson() {
        GsonBuilder gb = new GsonBuilder();
        gb.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        mGson = gb.create();
    }

    private void setupViews() {
        mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id
                .navigation_drawer);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mFragmentContainerView = findViewById(R.id.navigation_drawer);

        mAvatarImageView = (ImageView) findViewById(R.id.avatar_imageView);
        mAttendeeName = (TextView) findViewById(R.id.attendee_name);
        mAttendeeEmail = (TextView) findViewById(R.id.attendee_email);
    }

    private void setupDrawer(boolean isRecreatingView) {

        // Read in the flag indicating whether or not the user has demonstrated awareness of the
        // drawer. See PREF_USER_LEARNED_DRAWER for details.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        mUserLearnedDrawer = sp.getBoolean(PREF_USER_LEARNED_DRAWER, false);

        // If the user hasn't 'learned' about the drawer, open it to introduce them to the drawer,
        // per the navigation drawer design guidelines.
        if (!mUserLearnedDrawer && !isRecreatingView) {
            mDrawerLayout.openDrawer(mFragmentContainerView);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                             /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.drawable.ic_drawer,             /* nav drawer image to replace 'Up' caret */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                if (!mUserLearnedDrawer) {
                    // The user manually opened the drawer; store this flag to prevent auto-showing
                    // the navigation drawer automatically in the future.
                    mUserLearnedDrawer = true;
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                    sp.edit().putBoolean(PREF_USER_LEARNED_DRAWER, true).apply();
                }

                invalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    public boolean isDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(mFragmentContainerView);
    }

    private void fetchAttendees() {

        new Thread(new Request().forUri("/users").callsBackTo(new Request.RequestCallback() {

            @Override
            public void onSuccess(String responseBody) {

                Type collectionType = new TypeToken<List<Attendee>>() {
                }.getType();
                final List<Attendee> attendees = mGson.fromJson(responseBody, collectionType);

                // Sort by registration date
                Collections.sort(attendees, new Comparator<Attendee>() {
                    @Override
                    public int compare(Attendee lhs, Attendee rhs) {
                        return lhs.createdAt.compareTo(rhs.createdAt);
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNavigationDrawerFragment.updateAttendees(attendees);
                    }
                });
            }

            @Override
            public void onFailure(int statusCode, final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        })).start();
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {

        if (mNavigationDrawerFragment == null || mNavigationDrawerFragment.getAttendees() == null) {
            return;
        }

        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }

        Attendee attendee = mNavigationDrawerFragment.getAttendees().get(position);

        String avatarResource = "avatar" + attendee.name.charAt(0) % 10;
        mAvatarImageView.setImageResource(getResources().getIdentifier(avatarResource, "drawable",
                getBaseContext().getPackageName()));
        mAttendeeName.setText("Name: " + attendee.name);
        mAttendeeEmail.setText("Email: " + attendee.email);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Forward the new configuration the drawer toggle component.
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global, menu);
        restoreActionBar();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Toast.makeText(getBaseContext(), getString(R.string.action_settings), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
