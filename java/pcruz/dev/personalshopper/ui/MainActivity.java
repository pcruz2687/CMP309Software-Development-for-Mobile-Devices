package pcruz.dev.personalshopper.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;
import com.google.type.TimeOfDayOrBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.Account;
import pcruz.dev.personalshopper.models.CheckInternetConnection;
import pcruz.dev.personalshopper.services.LocationService;

import static pcruz.dev.personalshopper.Constants.REQUEST_CODE_LOCATION_PERMISSION;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    private static final String TAG = "MainActivity";

    private static final int RC_SIGN_IN = 123;

    // Variables
    private Account account;
    private boolean locationPermissionGranted = false;
    // Used to send the shopping requests to the other active request fragment
    boolean activeShoppingRequest;
    ListenerRegistration activeRequestListener;
    // Cloud Firestore
    FirebaseFirestore database;
    FirebaseUser user;
    DocumentReference signedInUserRef;
    DocumentSnapshot documentSnapshot;
    CheckInternetConnection checkInternetConnection;

    // View
    ConstraintLayout mainActivity;
    TextView email;
    TextView accountType;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        database = FirebaseFirestore.getInstance();
        mainActivity = findViewById(R.id.main_activity);
        Button signOut = (Button)findViewById(R.id.btn_sign_out);
        Button changeAccountType = (Button)findViewById(R.id.btn_change_account_type);
        Button startShopping = (Button)findViewById(R.id.btn_start_shopping);
        accountType = (TextView)findViewById(R.id.tv_account_type);
        email = (TextView)findViewById(R.id.tv_email);

        // Initialise object for checking internet connection
        checkInternetConnection = new CheckInternetConnection(this);

//        // Request for location services
//        getLocationPermission();
        if(FirebaseAuth.getInstance().getCurrentUser() == null)
        {
            createSignInIntent();
        }
        else
        {
            // TODO: 06/05/2020 Prompt that the user must allow Location Services to use the app
            // Add new user to the database
            storeUserInDatabase();

            // Request for location services
            getLocationPermission();
        }

        changeAccountType.setOnClickListener(this);
        startShopping.setOnClickListener(this);
        signOut.setOnClickListener(this);

        Log.d(TAG, "onCreate: connection:" + checkInternetConnection.isConnected());
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.btn_change_account_type:
                if(checkInternetConnection.isConnected())
                {
                    checkForActiveRequest();
                    changeAccountType();
                }
                else
                {
                    Toast.makeText(this, "Internet Connection is Required", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_start_shopping:
                if(locationPermissionGranted)
                {
                    checkForActiveRequest();
                    startShopping();
                }
                else
                {
                    if(!locationPermissionGranted)
                    {
                        Toast.makeText(this, "You must allow Location Services to use this app.", Toast.LENGTH_LONG).show();
                    }
                    getLocationPermission();
                }
                break;
            case R.id.btn_sign_out:
                signOut();
                break;
        }
    }

    // Ask for user permission to get the device location
    private void getLocationPermission()
    {
        if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            locationPermissionGranted = true;
            // Start listening to account changes
            listenForAccountUpdates();

            // Start service to get user current location
            startLocationService();
        }
        else
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }
    }

    // Check the permission request result, if permission given get current location, else display Toast
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        if(requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0)
        {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                locationPermissionGranted = true;
                // Start listening to account changes
                listenForAccountUpdates();

                // Start service to get user current location
                startLocationService();
            }
            else
            {
                Toast.makeText(this, "You must allow Location Services to use this app.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changeAccountType()
    {
        signedInUserRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task)
            {
                if(task.isSuccessful())
                {
                    documentSnapshot = task.getResult();
                    String currentAccountType = documentSnapshot.get("accountType").toString();
                    boolean shopperAvailability = (boolean)documentSnapshot.get("shopperAvailability");
                    boolean customerRequest = (boolean)documentSnapshot.get("customerRequest");

                    // Check for active requests before changing account type
                    if (currentAccountType.equals("Customer"))
                    {
                        if(customerRequest)
                        {
                            Toast.makeText(MainActivity.this, "Delete your existing request before changing to a Personal Shopper Account.", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            // Change Account Type to Shopper
                            signedInUserRef.update("accountType", "Personal Shopper");
                            accountType.setText("Personal Shopper");
                            // Updates the account type in the object used by the fragments
                            account.setAccountType("Personal Shopper");
                        }
                    }
                    else if (currentAccountType.equals("Personal Shopper"))
                    {
                        if(shopperAvailability || isRequestActive())
                        {
                            Toast.makeText(MainActivity.this, "Disable your availability or Finish your active request.", Toast.LENGTH_LONG).show();
                        }
                        else
                        {
                            // Change Account Type to Customer
                            signedInUserRef.update("accountType", "Customer");
                            accountType.setText("Customer");
                            // Updates the account type in the object used by the fragments
                            account.setAccountType("Customer");
                        }

                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Finish your active Customer Request before changing to a Customer Account.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    // Go to Shopping Activity
    private void startShopping()
    {
        //Account account = new Account(user.getUid(), user.getEmail(), accountType.getText(), )
        Intent intent = new Intent(MainActivity.this, ShoppingActivity.class);
        if(isRequestActive())
        {
            intent.putExtra("requestActive", "Don't forget, you have an active request! Check the Track tab.");
        }
        startActivity(intent);
    }

    public void createSignInIntent()
    {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        startActivityForResult(AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(true)
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_create_intent]

    }

    // [START auth_fui_result]
    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN)
        {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            if (resultCode == RESULT_OK)
            {
                user = FirebaseAuth.getInstance().getCurrentUser();
                signedInUserRef = database.collection("users").document(FirebaseAuth.getInstance().getUid());

                // Request for location services
                getLocationPermission();

                // Add new user to the database
                storeUserInDatabase();

                // Start listening to account changes
                listenForAccountUpdates();

                // Start service to get user current location
                startLocationService();
                // ...
            }
            else
            {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                //createSignInIntent();
            }
        }
    }
    // [END auth_fui_result]

    public void signOut()
    {
        // [START auth_fui_signout]
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>()
                {
                    public void onComplete(@NonNull Task<Void> task)
                    {
//                        email.setText("");
                        createSignInIntent();
                    }
                });
        // [END auth_fui_signout]
    }

    // Add new user to the database
    private void storeUserInDatabase()
    {
        signedInUserRef = database.collection("users").document(FirebaseAuth.getInstance().getCurrentUser().getUid());
        // Create a new user document using the user ID as the document name
        signedInUserRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task)
            {
                if(task.isSuccessful())
                {
                    documentSnapshot = task.getResult();

                    // Adds the user to the database if it's the first time logging in
                    if(documentSnapshot != null && documentSnapshot.exists())
                    {
//                        Toast.makeText(MainActivity.this,"Data Exists",Toast.LENGTH_LONG).show();
                        account = new Account(FirebaseAuth.getInstance().getCurrentUser().getUid(), FirebaseAuth.getInstance().getCurrentUser().getEmail(), documentSnapshot.getString("accountType"), documentSnapshot.getBoolean("customerRequest"), documentSnapshot.getBoolean("shopperAvailability"));
                        ((AccountClient)getApplicationContext()).setAccount(account);
                        accountType.setText(account.getAccountType());
                        email.setText(account.getEmail());
                        mainActivity.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        account = new Account(FirebaseAuth.getInstance().getCurrentUser().getUid(), FirebaseAuth.getInstance().getCurrentUser().getEmail(), "Customer", false, false);
                        ((AccountClient)getApplicationContext()).setAccount(account);
                        signedInUserRef
                                .set(account)
                                .addOnSuccessListener(new OnSuccessListener<Void>()
                                {
                                    @Override
                                    public void onSuccess(Void aVoid)
                                    {
                                        Toast.makeText(MainActivity.this, "Registered", Toast.LENGTH_SHORT).show();
                                        Log.d(TAG, "DocumentSnapshot successfully written!");
                                        accountType.setText(account.getAccountType());
                                        email.setText(account.getEmail());
                                        Log.d(TAG, "Newly Registered Account: " + account.toString());
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener()
                                {
                                    @Override
                                    public void onFailure(@NonNull Exception e)
                                    {
                                        Log.w(TAG, "Error writing document", e);
                                    }
                                });

                        // Get a new write batch
                        WriteBatch batch = database.batch();

                        // Create Current Activity document
                        DocumentReference currentActivity = signedInUserRef.collection("shopper data").document("current activity");
                        batch.set(currentActivity, new HashMap<String, Object>());

                        // Create Active Shopper Request document
                        DocumentReference activeRequest = signedInUserRef.collection("shopper data").document("active request");
                        batch.set(activeRequest, new HashMap<String, Object>());

                        // Commit Batch
                        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.d(TAG, "onComplete: successful batch commit");
                            }
                        });

                        mainActivity.setVisibility(View.VISIBLE);
                    }
                }
            }
        });
    }

    // Listen to changes in the currently signed in user
    private void listenForAccountUpdates()
    {
        signedInUserRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    account = snapshot.toObject(Account.class);
                    ((AccountClient)getApplicationContext()).setAccount(account);
                    Log.d(TAG, "Account Update: " + ((AccountClient)getApplicationContext()).getAccount().toString());
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    // Start location service to get the device location
    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(MainActivity.this, LocationService.class);
//        this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                MainActivity.this.startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }
    }

    // Check if the service is already running
    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("pcruz.dev.personalshopper.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");

                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }

    public void checkForActiveRequest() {
            // Listens to the active Customer Request
            // Updates the returned CustomerRequest object called from the ActiveRequestFragment
            DocumentReference currentActivityRef = database
                    .collection("users")
                    .document(FirebaseAuth.getInstance().getUid())
                    .collection("shopper data")
                    .document("current activity");

            activeRequestListener = currentActivityRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
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
                                activeShoppingRequest = false;
                            } else {
                                Log.d(TAG, "Document is not empty!");
                                activeShoppingRequest = true;
                            }
                        }
                    } else {
                        Log.d(TAG, "Current data: null");
                    }
                }
            });
    }

    public boolean isRequestActive()
    {
        return activeShoppingRequest;
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkInternetConnection = new CheckInternetConnection(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try
        {
            // Remove the active request listener
            if(activeRequestListener != null)
            {
                activeRequestListener.remove();
                Log.d(TAG, "onPause: REMOVED");
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try
        {
            // Remove the active request listener
            if(activeRequestListener != null)
            {
                activeRequestListener.remove();
                Log.d(TAG, "onStop: REMOVED");
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        signOut();
    }
}
