package ugia.moscow14attendees;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import ugia.moscow14attendees.api.model.Attendee;
import ugia.moscow14attendees.api.request.Request;


public class MainActivity extends Activity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

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

        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));
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
        gb.setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        mGson = gb.create();
    }

    private void setupViews() {
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getFragmentManager().findFragmentById(R.id.navigation_drawer);

        mAvatarImageView = (ImageView) findViewById(R.id.avatar_imageView);
        mAttendeeName = (TextView) findViewById(R.id.attendee_name);
        mAttendeeEmail = (TextView) findViewById(R.id.attendee_email);
    }

    private void fetchAttendees() {

        new Thread(new Request().forUri("/users").callbacksTo(new Request.RequestCallback() {

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

        Attendee attendee = mNavigationDrawerFragment.getAttendees().get(position);

        String avatarResource = "avatar" + attendee.name.charAt(0) % 10;
        mAvatarImageView.setImageResource(getResources().getIdentifier(avatarResource, "drawable",
                getBaseContext().getPackageName()));
        mAttendeeName.setText("Name: " + attendee.name);
        mAttendeeEmail.setText("Email: " + attendee.email);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.global, menu);
        restoreActionBar();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Toast.makeText(getBaseContext(), getString(R.string.action_settings), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
