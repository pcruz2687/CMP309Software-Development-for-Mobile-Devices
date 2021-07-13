package pcruz.dev.personalshopper.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Map;

import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.ActiveCustomerRequest;
import pcruz.dev.personalshopper.models.CustomerRequest;
import pcruz.dev.personalshopper.models.FragmentCallback;

public class ShoppingActivity extends AppCompatActivity implements FragmentCallback
{
    private static final String TAG = "ShoppingActivity";

    // Variables
    private ListenerRegistration customerRequestsEventListener;
    private FirebaseFirestore database;
    private ArrayList<CustomerRequest> customerRequestList = new ArrayList<>();

    // Used to send the shopping requests to the other active request fragment
    ActiveCustomerRequest activeShoppingRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_shopping_requests, R.id.navigation_active_request, R.id.navigation_history)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        database = FirebaseFirestore.getInstance();

        Bundle extras = getIntent().getExtras();
        String requestActiveMessage = "";

        // Show toast message that there is an active request
        if(extras != null)
        {
            requestActiveMessage = extras.getString("requestActive");
            Toast.makeText(this, ""+requestActiveMessage, Toast.LENGTH_LONG).show();
        }

        // Request permission to get device location
        //getAllCustomerRequests();
        setCurrentActivity();
    }

    @Override
    public void setCurrentActivity() {
        // Listens to the active Customer Request
        // Updates the returned CustomerRequest object called from the ActiveRequestFragment
        DocumentReference currentActivityRef = database
                .collection("users")
                .document(FirebaseAuth.getInstance().getUid())
                .collection("shopper data")
                .document("current activity");

        currentActivityRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    Log.d(TAG, "Current data: " + documentSnapshot.getData());
                    if (documentSnapshot.exists()) {
                        Map<String, Object> map = documentSnapshot.getData();
                        if (map.size() == 0) {
                            Log.d(TAG, "Document is empty!");
//                                currentActivity = false;
                            activeShoppingRequest = null;
                        } else {
                            Log.d(TAG, "Document is not empty!");
                            activeShoppingRequest = documentSnapshot.toObject(ActiveCustomerRequest.class);
                            Log.d(TAG, "onComplete: " + activeShoppingRequest.toString());
                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    @Override
    public ActiveCustomerRequest getCurrentActivity() {
        return activeShoppingRequest;
    }
}


