package pcruz.dev.personalshopper.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.OnCompleteListener;
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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.adapters.ActiveCustomerRequestAdapter;
import pcruz.dev.personalshopper.models.Account;
import pcruz.dev.personalshopper.models.ActiveCustomerRequest;
import pcruz.dev.personalshopper.models.ClusterMarker;
import pcruz.dev.personalshopper.models.FragmentCallback;
import pcruz.dev.personalshopper.models.CustomerRequest;
import pcruz.dev.personalshopper.util.ClusterManagerRenderer;

import static pcruz.dev.personalshopper.Constants.MAPVIEW_BUNDLE_KEY;

public class ActiveRequestFragment extends Fragment implements OnMapReadyCallback
{
    private static final String TAG = "ActiveRequestFragment";
    RecyclerView recyclerView;
    LinearLayoutManager manager;
    ActiveCustomerRequestAdapter activeRequestAdapter;
    View root;
    Context context;

    FirebaseUser user;
    FirebaseFirestore database;
    ArrayList<ActiveCustomerRequest> mCustomerRequest = new ArrayList<>();

    Boolean currentActivity;

    // Variables
    private MapView mapView;
    private GoogleMap googleMap;
    private LatLngBounds singleMarkerMapBoundary;
    private LatLngBounds.Builder multipleMarkersMapBoundary;
    private ClusterManager clusterManager;
    private ClusterManagerRenderer clusterManagerRenderer;
    private ArrayList<ClusterMarker> clusterMarkers = new ArrayList<>();
    private ListenerRegistration createActiveRequestListener;
    private ListenerRegistration updateActiveRequestListener;
    private ListenerRegistration personalShopperLocationListener;
    private Handler handler = new Handler();
    private Runnable runnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    FragmentCallback fragmentCallback;
//    Account account;

    GeoPoint shopperGeoPoint;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        account = ((AccountClient)getActivity().getApplicationContext()).getAccount();
        if(fragmentCallback.getCurrentActivity() != null)
        {
            mCustomerRequest.add(0, fragmentCallback.getCurrentActivity());
        }
        currentActivity = false;
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_active_request, container, false);
        mapView = root.findViewById(R.id.active_request_map);
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseFirestore.getInstance();
//        Log.d(TAG, "Account: " + account.toString());

        buildRecyclerView();
        initGoogleMap(savedInstanceState);
        createActiveCustomerRequest();

        return root;
    }

    // A thread for checking the Personal Shopper's current location every 3 seconds
    // to update the location marker on the map
    private void startPersonalShopperLocationRunnable()
    {
        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                getPersonalShopperLocation();
                handler.postDelayed(runnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }

    private void stopPersonalShopperLocationRunnable()
    {

        handler.removeCallbacks(runnable);
    }

    // Updates the marker position/location on the map
    private void getPersonalShopperLocation()
    {
        try
        {
            for (final ClusterMarker clusterMarker : clusterMarkers)
            {
                // Checks the database for the Personal Shopper's location
                // Updates the Personal Shopper's marker on the map
                if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Customer") && mCustomerRequest.size() > 0)
                {
                    DocumentReference docRef = database.collection("users").document(mCustomerRequest.get(0).getShopperUserID());
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful())
                            {
                                final GeoPoint personalShopperLocation =  task.getResult().getGeoPoint("geoPoint");
                                final LatLng newLocation = new LatLng(personalShopperLocation.getLatitude(), personalShopperLocation.getLongitude());

                                clusterMarker.setPosition(newLocation);
                                clusterManagerRenderer.updateMarker(clusterMarker);
                                setCameraView();
                            } else {
                                Log.d(TAG, "Failed with: ", task.getException());
                            }
                        }
                    });
                }
                else
                {
                    // Update the Personal Shopper's marker on the map if the signed in account is a Personal Shopper
                    // Also used by the Customer if there are no active requests
                    LatLng newLocation = new LatLng((
                            (AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLatitude(),
                            ((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLongitude());

                    clusterMarker.setPosition(newLocation);
                    clusterManagerRenderer.updateMarker(clusterMarker);
                    setCameraView();
                }
            }
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
        }
    }

    // Add the markers to the map
    private void addMapMarkers()
    {

        if(googleMap != null)
        {
            Log.d(TAG, "addMapMarkers: " + mCustomerRequest.size());
            if(clusterManager == null){
                clusterManager = new ClusterManager<ClusterMarker>(getActivity().getApplicationContext(), googleMap);
            }
            if(clusterManagerRenderer == null){
                clusterManagerRenderer = new ClusterManagerRenderer(
                        getActivity(),
                        googleMap,
                        clusterManager
                );
                clusterManager.setRenderer(clusterManagerRenderer);
            }

            for(ActiveCustomerRequest customerRequest: mCustomerRequest)
            {
                try{

                    int personalShopperAvatar = R.drawable.shopper_icon;
                    int customerAvatar = R.drawable.customer_icon;
                    String personalShopperSnippet = "Personal Shopper";
                    String customerSnippet = "Customer";
//                    ClusterMarker shopperClusterMarker = new ClusterMarker();

                    ClusterMarker customerClusterMarker = new ClusterMarker(new LatLng(customerRequest.getCustomerGeoPoint().getLatitude(), customerRequest.getCustomerGeoPoint().getLongitude()),
                            customerRequest.getCustomerEmail(),
                            personalShopperSnippet,
                            customerAvatar,
                            customerRequest);

                    ClusterMarker shopperClusterMarker = new ClusterMarker(
                            new LatLng(((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLatitude(), ((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLongitude()),
                            customerRequest.getShopperEmail(),
                            customerSnippet,
                            personalShopperAvatar,
                            customerRequest);

                    clusterManager.addItem(customerClusterMarker);
                    clusterManager.addItem(shopperClusterMarker);

                    // Add the Shopper marker to the list for easy access
                    clusterMarkers.add(shopperClusterMarker);

                }catch (NullPointerException e){
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage());
                    Log.d(TAG, "addMapMarkers: " + mCustomerRequest.size());
                }
            }
            clusterManager.cluster();

            setCameraView();
        }
    }

    private void setCameraView()
    {
        double bottomBoundary = 0;
        double leftBoundary = 0;
        double topBoundary = 0;
        double rightBoundary = 0;

        // Use the account's geo point in the database if there is no active request
        if(mCustomerRequest.size() == 0)
        {
            bottomBoundary = ((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLatitude() - .1;
            leftBoundary = ((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLongitude() - .1;
            topBoundary = ((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLatitude() + .1;
            rightBoundary = ((AccountClient)getActivity().getApplicationContext()).getAccount().getGeoPoint().getLongitude() + .1;

            singleMarkerMapBoundary = new LatLngBounds(
                    new LatLng(bottomBoundary, leftBoundary),
                    new LatLng(topBoundary, rightBoundary)
            );

            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(singleMarkerMapBoundary, 0));
        }
        else
        {
            // Set map boundaries using the Personal Shopper and Customer markers if an active request exists
            multipleMarkersMapBoundary = new LatLngBounds.Builder();
            for(ClusterMarker clusterMarker : clusterMarkers)
            {
                multipleMarkersMapBoundary.include(clusterMarker.getPosition());
            }

            multipleMarkersMapBoundary.include(new LatLng(mCustomerRequest.get(0).getCustomerGeoPoint().getLatitude(), mCustomerRequest.get(0).getCustomerGeoPoint().getLongitude()));

            LatLngBounds bounds = multipleMarkersMapBoundary.build();
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 450, 450, 10));
        }
    }

    private void initGoogleMap(Bundle savedInstanceState)
    {
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mapView.onCreate(mapViewBundle);
        mapView.getMapAsync(this);
    }

    public void buildRecyclerView()
    {
        recyclerView = root.findViewById(R.id.recyler_view_active_customer_request);
        manager = new LinearLayoutManager(getActivity());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        activeRequestAdapter = new ActiveCustomerRequestAdapter(getContext(), mCustomerRequest,ActiveRequestFragment.this, database);
        recyclerView.setAdapter(activeRequestAdapter);
        Log.d(TAG, "buildRecyclerView BUILT: ");
    }


    public void createActiveCustomerRequest()
    {
        CollectionReference createActiveRequestRef = database.collection("users");

        createActiveRequestListener = createActiveRequestRef
                .whereEqualTo("email", user.getEmail())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
                                    {
                                        // Used for when there is no active Customer Request in the database
                                        if(mCustomerRequest.size() == 0)
                                        {
                                            DocumentReference docRef = database.collection("users").document(user.getUid()).collection("shopper data").document("current activity");
                                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot document = task.getResult();
                                                        if (document.exists()) {
                                                            Map<String, Object> map = document.getData();
                                                            if (map.size() == 0) {
                                                                Log.d(TAG, "Document is empty!");
                                                                currentActivity = false;
                                                            }
                                                            else
                                                            {
                                                                mCustomerRequest.add(0, new ActiveCustomerRequest(
                                                                        document.getString("documentID"),
                                                                        document.getString("customerUserID"),
                                                                        document.getString("customerEmail"),
                                                                        user.getUid(), user.getEmail(),
                                                                        (GeoPoint) document.get("customerGeoPoint"),
                                                                        (GeoPoint) document.get("shopperGeoPoint"),
                                                                        (List<String>) document.get("shoppingList"),
                                                                        document.getString("role"), true));

                                                                activeRequestAdapter.notifyItemInserted(0);
                                                                updateActiveCustomerRequest();
                                                                if(googleMap != null)
                                                                {
                                                                    googleMap.clear();
                                                                    addMapMarkers();
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                        else
                                        {
                                            // Used for when there's an active Customer Request in the database on initial log in
                                            Log.d(TAG, "onEvent: "+mCustomerRequest.get(0).toString());
                                            activeRequestAdapter.notifyDataSetChanged();
                                            updateActiveCustomerRequest();
                                        }
                                    }
                                    else if (((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Customer"))
                                    {
                                        if(mCustomerRequest.size() == 0)
                                        {
                                            DocumentReference docRef = database.collection("users").document(user.getUid()).collection("shopper data").document("current activity");
                                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    if (task.isSuccessful()) {
                                                        DocumentSnapshot document = task.getResult();
                                                        if (document.exists()) {
                                                            Map<String, Object> map = document.getData();
                                                            if (map.size() == 0) {
                                                                Log.d(TAG, "Document is empty!");
                                                                currentActivity = false;
                                                            }
                                                            else
                                                            {
                                                                // Only show the request if active == true
                                                                // this means a shopper has picked up the customer request
                                                                if(document.getBoolean("active"))
                                                                {
                                                                    Log.d(TAG, "Adding shopper request from: " + document.getString("shopperEmail"));
                                                                    mCustomerRequest.add(0, new ActiveCustomerRequest(
                                                                            document.getString("documentID"),
                                                                            user.getUid(),
                                                                            user.getEmail(),
                                                                            document.getString("shopperUserID"),
                                                                            document.getString("shopperEmail"),
                                                                            (GeoPoint) document.get("customerGeoPoint"),
                                                                            (GeoPoint) document.get("shopperGeoPoint"),
                                                                            (List<String>) document.get("shoppingList"),
                                                                            document.getString("role"), true));
                                                                    activeRequestAdapter.notifyItemInserted(0);
                                                                    listenToPersonalShopperLocation();
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            });
                                        }
                                        else
                                        {
                                            // Used for when there's an active request in the database on initial log in
                                            activeRequestAdapter.notifyDataSetChanged();
                                            updateActiveCustomerRequest();
                                        }

                                    }

                                    break;
                                case MODIFIED:
                                    activeRequestAdapter.notifyDataSetChanged();
                                    break;
                                case REMOVED:
                                    if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
                                    {
                                        currentActivity = false;
                                    }
                                    mCustomerRequest.remove(0);
                                    activeRequestAdapter.notifyDataSetChanged();
                                    break;
                            }
                        }

                    }
                });
    }

    public void updateActiveCustomerRequest()
    {
        CollectionReference updateActiveRequestRef = database
                .collection("users")
                .document(user.getUid())
                .collection("shopper data");

        updateActiveRequestListener = updateActiveRequestRef
                .whereEqualTo("active", true)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "listen:error", e);
                            return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            switch (dc.getType()) {
                                case ADDED:
                                    break;
                                case MODIFIED:
                                    break;
                                case REMOVED:
                                    if(mCustomerRequest.size() > 0)
                                    {
                                        mCustomerRequest.clear();
                                    }
//                            mCustomerRequest.clear();
                                    if(((AccountClient)getActivity().getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
                                    {
                                        currentActivity = false;
                                    }
                                    activeRequestAdapter.notifyDataSetChanged();
                                    break;
                            }
                        }
                    }
                });
    }

    // Only used if the account type is Customer
    private void listenToPersonalShopperLocation()
    {
        //Listen to Shopper's Geo Location
        final DocumentReference docRef = database.collection("users").document(user.getUid());
        personalShopperLocationListener = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    shopperGeoPoint = snapshot.getGeoPoint("shopperGeoPoint");
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
    }


//    public Account getAccount()
//    {
//        return account;
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        /// Only start the thread for the map markers if there is an active request
        // Otherwise it will crash
        if(mCustomerRequest.size() > 0)
        {
            startPersonalShopperLocationRunnable();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    public void onAttach(Context context)
    {
        fragmentCallback = (FragmentCallback) context;
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onMapReady(GoogleMap map)
    {
        googleMap = map;
        Log.d(TAG, "onMapReady: MAP IS READY");
        addMapMarkers();
    }


    @Override
    public void onStop() {
        super.onStop();
        mapView.onStop();
        try
        {
            // Stop the database snapshot listeners and thread when the fragment is not in display
            if(createActiveRequestListener != null)
            {
                createActiveRequestListener.remove();
            }

            if (updateActiveRequestListener != null)
            {
                updateActiveRequestListener.remove();
            }

            if (personalShopperLocationListener != null)
            {
                personalShopperLocationListener.remove();
            }

            stopPersonalShopperLocationRunnable();
        }catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public void onPause() {
        mapView.onPause();
        super.onPause();
        try
        {
            // Stop the database snapshot listeners and thread when the fragment is not in display
            if(createActiveRequestListener != null)
            {
                createActiveRequestListener.remove();
            }

            if (updateActiveRequestListener != null)
            {
                updateActiveRequestListener.remove();
            }

            if (personalShopperLocationListener != null)
            {
                personalShopperLocationListener.remove();
            }

            stopPersonalShopperLocationRunnable();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        stopPersonalShopperLocationRunnable();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}