package pcruz.dev.personalshopper.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.adapters.CustomerRequestAdapter;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.adapters.ShopperAvailableItemAdapter;
import pcruz.dev.personalshopper.models.Account;
import pcruz.dev.personalshopper.models.CheckInternetConnection;
import pcruz.dev.personalshopper.models.CustomerRequest;
import pcruz.dev.personalshopper.models.FragmentCallback;
import pcruz.dev.personalshopper.models.ShopperAvailable;

import static com.firebase.ui.auth.AuthUI.getApplicationContext;

public class SearchFragment extends Fragment implements View.OnClickListener
{
    private static final String TAG = "SearchFragment";

    // Variables
//    Account account;
    Context context;
    FirebaseUser user;
    FirebaseFirestore database;
    RecyclerView recyclerView;
    LinearLayoutManager manager;
    CustomerRequestAdapter customerRequestAdapter;
    ShopperAvailableItemAdapter shopperAvailableAdapter;

    CheckInternetConnection checkInternetConnection;

    private FragmentCallback fragmentCallback;
    int position;
    private ArrayList<CustomerRequest> mCustomerRequestList;
    ShoppingActivity shoppingActivity;
    private ArrayList<ShopperAvailable> mShopperAvailableList;

    ListenerRegistration shopperAvailableListListener;
    ListenerRegistration customerRequestListListener;
    ListenerRegistration checkPendingRequestListener;

    // View
    View root;
    Button search;
    Button updateAvailability;




    @SuppressLint("RestrictedApi")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        root = inflater.inflate(R.layout.fragment_customer_requests, container, false);
//        account = ((AccountClient)getApplicationContext()).getAccount();

        if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
        {
            root.findViewById(R.id.shopper_list).setVisibility(View.INVISIBLE);
            root.findViewById(R.id.customer_list).setVisibility(View.VISIBLE);
        }
        else if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals(("Customer")))
        {
            root.findViewById(R.id.customer_list).setVisibility(View.INVISIBLE);
            root.findViewById(R.id.shopper_list).setVisibility(View.VISIBLE);
        }
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        //locationBroadcastReceiver = new LocationBroadcastReceiver();
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseFirestore.getInstance();
        shoppingActivity = (ShoppingActivity) getActivity();

        if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
        {
            search = root.findViewById(R.id.btn_customer_search);
            search.setText(R.string.btn_shopper_search);
            updateAvailability = root.findViewById(R.id.btn_shopper_update_availability);

            updateShopperButtonText();

            mCustomerRequestList = new ArrayList<>();
            buildRecyclerView();
            getCurrentLocation();
            createCustomerRequestList();
        }
        else if (((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Customer"))
        {
            search = root.findViewById(R.id.btn_shopper_search);
            search.setText(getString(R.string.btn_customer_search));
            updateAvailability = root.findViewById(R.id.btn_customer_update_availability);

            updateAvailability.setText(R.string.btn_customer_create_request);

            mShopperAvailableList = new ArrayList<>();
            buildRecyclerView();
            getCurrentLocation();
            createPersonalShopperAvailableList();
            updateCustomerButtonText();
        }

        // Set Click Listeners
        search.setOnClickListener(this);
        updateAvailability.setOnClickListener(this);

    }

    // Build Recycler View
    public void buildRecyclerView()
    {
        if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
        {
            recyclerView = root.findViewById(R.id.recyler_view_customer_list);
            customerRequestAdapter = new CustomerRequestAdapter(getContext(), mCustomerRequestList, SearchFragment.this, database);
            recyclerView.setAdapter(customerRequestAdapter);
        }
        else if (((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Customer"))
        {
            recyclerView = root.findViewById(R.id.recyler_view_shopper_list);
            shopperAvailableAdapter = new ShopperAvailableItemAdapter(mShopperAvailableList, SearchFragment.this, database);
            recyclerView.setAdapter(shopperAvailableAdapter);
        }

        manager = new LinearLayoutManager(getActivity());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);

    }

    // onClick switch
    @Override
    public void onClick(View v)
    {
        checkInternetConnection = new CheckInternetConnection(getActivity());
        if(checkInternetConnection.isConnected())
        {
            switch (v.getId()) {
                case R.id.btn_customer_search:
                    if (mCustomerRequestList.size() > 0)
                    {
                        mCustomerRequestList.clear();
                        customerRequestAdapter.notifyDataSetChanged();
                        customerRequestListListener.remove();
                        getCurrentLocation();
                        createCustomerRequestList();
                    }
                    else
                    {
                        Toast.makeText(context, "No Customer Requests available.", Toast.LENGTH_SHORT).show();
                    }

                    break;
                case R.id.btn_shopper_update_availability:
                    updateAvailability();
                    break;
                case R.id.btn_customer_update_availability:
                    // Display different dialog depending on
                    // if there is an active request or not

                    if(!((AccountClient)getActivity().getApplicationContext()).getAccount().getCustomerRequest())
                    {
                        createCustomerRequestDialog();
                    }
                    else
                    {
                        displayPendingCustomerRequestDialog();
                    }
                    break;
                case R.id.btn_shopper_search:
                    if (mShopperAvailableList.size() > 0)
                    {
                        mShopperAvailableList.clear();
                        shopperAvailableAdapter.notifyDataSetChanged();
                        shopperAvailableListListener.remove();
                        getCurrentLocation();
                        createPersonalShopperAvailableList();
                    }
                    else
                    {
                        Toast.makeText(context, "No Personal Shoppers available", Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
        else
        {
            Toast.makeText(context, "Internet Connection is Required", Toast.LENGTH_SHORT).show();
        }
    }

    // Get location once for Shopper Available / Customer Request Search
    private void getCurrentLocation()
    {
        final LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(300);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.getFusedLocationProviderClient(getActivity()).requestLocationUpdates(locationRequest,new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult)
            {
                super.onLocationResult(locationResult);
                LocationServices.getFusedLocationProviderClient(getActivity()).removeLocationUpdates(this);
                if(locationResult != null && locationResult.getLocations().size() > 0)
                {
                    int latestLocationIndex = locationResult.getLocations().size() - 1;

                    ((AccountClient)getActivity().getApplicationContext()).getAccount().setGeoPoint(new GeoPoint(locationResult.getLocations().get(latestLocationIndex).getLatitude(), locationResult.getLocations().get(latestLocationIndex).getLongitude()));

                    // Update the current location in the database
                    updateCurrentLocation(((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint());
                }
            }
        }, Looper.getMainLooper());
    }

    // Create Customer Requests List
    public void createCustomerRequestList()
    {
        CollectionReference customerRequestsRef = database.collection("customerRequests");
        customerRequestListListener = customerRequestsRef
                .whereEqualTo("available", true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        for (DocumentChange dc : snapshots.getDocumentChanges())
                        {
                            switch (dc.getType()) {
                                case ADDED:
                                    mCustomerRequestList.add(getPosition(), new CustomerRequest(dc.getDocument().getId(), dc.getDocument().getString("customerUserID"), dc.getDocument().getString("customerEmail"), (GeoPoint) dc.getDocument().get("customerGeoPoint"), (List<String>) dc.getDocument().get("shoppingList")));
                                    customerRequestAdapter.notifyItemInserted(getPosition());
                                    break;
                                case MODIFIED:
                                    mCustomerRequestList.remove(getPosition());
                                    customerRequestAdapter.notifyItemChanged(getPosition());
                                    break;
                                case REMOVED:
                                    mCustomerRequestList.remove(getPosition());
                                    customerRequestAdapter.notifyItemRemoved(getPosition());
                                    break;
                            }
                        }

                    }
                });
    }

    // Create List of Available Shoppers
    public void createPersonalShopperAvailableList()
    {
        CollectionReference personalShopperAvailableRef = database.collection("personalShoppersAvailable");

        shopperAvailableListListener = personalShopperAvailableRef
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges())
                        {
                            switch (dc.getType()) {
                                case ADDED:
                                    mShopperAvailableList.add(getPosition(), new ShopperAvailable(dc.getDocument().getString("userID"), dc.getDocument().getString("email"), (GeoPoint) dc.getDocument().get("geoPoint")));
                                    shopperAvailableAdapter.notifyItemInserted(getPosition());
                                    break;
                                case MODIFIED:
//                            mShopperAvailableList.remove(getPosition());
                                    shopperAvailableAdapter.notifyItemChanged(getPosition());
                                    break;
                                case REMOVED:
                                    mShopperAvailableList.remove(getPosition());
                                    shopperAvailableAdapter.notifyItemRemoved(getPosition());
                                    break;
                            }
                        }

                    }
                });
    }

    // Update user location in the database
    public void updateCurrentLocation(final GeoPoint geoPoint)
    {
        database.collection("users")
                .document(user.getUid())
                .update("geoPoint", geoPoint)
                .addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void aVoid)
                    {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
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

        database.collection("shopperAvailable")
                .document(user.getUid())
                .update("geoPoint", geoPoint)
                .addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void aVoid)
                    {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
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
    }

    // Change position to remove, modify or update in the shopper request list
    // Uses the adapter position in the recycler view
    public void updatePosition(int itemPositionInAdapter)
    {
        position = itemPositionInAdapter;
    }

    public int getPosition()
    {
        return position;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case 1:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //startService();
                }
                else
                {
                    Log.d(TAG, "You must allow Location Services.");
                }
        }
    }

    // Change account availability
    // Needs changing
    public void updateAvailability()
    {
        DocumentReference userRef = database.collection("users").document(user.getUid());
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task)
            {
                if(task.isSuccessful())
                {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    String currentAccountType = documentSnapshot.get("accountType").toString();
                    boolean shopperAvailability = (boolean)documentSnapshot.get("shopperAvailability");
                    if (currentAccountType.equals("Personal Shopper"))
                    {
                        if(shopperAvailability)
                        {
                            database.collection("users")
                                    .document(user.getUid())
                                    .update("shopperAvailability", false);
                            updateAvailability.setText(R.string.btn_shopper_unavailable);

                            // Remove user from available shoppers
                            database.collection("personalShoppersAvailable")
                                    .document(user.getUid())
                                    .delete();
                        }
                        else
                        {
                            if(fragmentCallback.getCurrentActivity() == null)
                            {
                                database.collection("users")
                                        .document(user.getUid())
                                        .update("shopperAvailability", true);

                                // Add user to available shoppers
                                database.collection("personalShoppersAvailable")
                                        .document(user.getUid())
                                        .set(((AccountClient)getActivity().getApplicationContext()).getAccount());
                                updateAvailability.setText(R.string.btn_shopper_available);
                            }
                            else
                            {
                                Toast.makeText(context, "Please finish your current active customer request.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
            }
        });
    }

    // Changes button text depending on if there's a pending request or not
    private void updateCustomerButtonText()
    {
        CollectionReference currentActivityRef = database.collection("customerRequests");

        checkPendingRequestListener = currentActivityRef
                .whereEqualTo("customerUserID", user.getUid())
                .whereEqualTo("available", true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        if (value.size() > 0)
                        {
                            for (QueryDocumentSnapshot doc : value)
                            {
                                if (doc.getBoolean("available"))
                                {
                                    updateAvailability.setText(R.string.btn_customer_view_pending_request);
                                } else
                                {
                                    updateAvailability.setText(R.string.btn_customer_create_request);
                                }
                            }
                        }
                        else
                        {
                            updateAvailability.setText(R.string.btn_customer_create_request);
                        }
                    }
                });
    }

    // Changes button text depending on if there's a pending request or not
    private void updateShopperButtonText()
    {
        DocumentReference currentActivityRef = database.collection("users").document(user.getUid());
        currentActivityRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.getBoolean("shopperAvailability"))
                    {
                        updateAvailability.setText(R.string.btn_shopper_available);
                    } else
                    {
                        updateAvailability.setText(R.string.btn_shopper_unavailable);
                    }

                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }

    public void createCustomerRequestDialog()
    {
        CreateCustomerRequestDialog createCustomerRequestDialog = new CreateCustomerRequestDialog();
        createCustomerRequestDialog.show(getChildFragmentManager(), "Customer List Dialog");
    }

    private void displayPendingCustomerRequestDialog()
    {
        ActiveCustomerRequestDialog activeCustomerRequestDialog = new ActiveCustomerRequestDialog();
        activeCustomerRequestDialog.show(getChildFragmentManager(), "Pending Customer List Dialog");
    }
    @Override
    public void onResume() {
        super.onResume();
        try
        {
            // Check for internet connection to reload the recycler view
            checkInternetConnection = new CheckInternetConnection(getActivity());
        }
        catch (Exception e)
        {
            // already registered
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context)
    {
        fragmentCallback = (FragmentCallback) context;
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        // Stop the database snapshot listeners and thread when the fragment is not in display
        try
        {
            if(customerRequestListListener != null)
            {
                customerRequestListListener.remove();
            }
            else if (shopperAvailableListListener != null)
            {
                shopperAvailableListListener.remove();
            }
            else if (checkPendingRequestListener != null)
            {
                checkPendingRequestListener.remove();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        // Stop the database snapshot listeners and thread when the fragment is not in display
        try
        {
            if(customerRequestListListener != null)
            {
                customerRequestListListener.remove();
            }
            else if (shopperAvailableListListener != null)
            {
                shopperAvailableListListener.remove();
            }
            else if (checkPendingRequestListener != null)
            {
                checkPendingRequestListener.remove();
            }
        }
        catch (IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }
    @Override
    public void onDestroy ()
    {
        super.onDestroy();

    }
}