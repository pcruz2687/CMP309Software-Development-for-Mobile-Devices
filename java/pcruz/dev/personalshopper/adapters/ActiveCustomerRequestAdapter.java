package pcruz.dev.personalshopper.adapters;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.ActiveCustomerRequest;
import pcruz.dev.personalshopper.models.CheckInternetConnection;
import pcruz.dev.personalshopper.models.CustomerRequest;
import pcruz.dev.personalshopper.ui.ActiveRequestFragment;

public class ActiveCustomerRequestAdapter extends RecyclerView.Adapter<ActiveCustomerRequestAdapter.ViewHolder>
{
    private static final String TAG = "ActiveCustomerRequestAdapter";
    private ArrayList<ActiveCustomerRequest> shoppingRequest;
    private Context context;
    private ActiveRequestFragment activeRequestFragment;
    private FirebaseFirestore database;
    private DocumentReference personalShopperRef;
    private GeoPoint personalShopperGeoPoint;
    private ListenerRegistration personalShopperListener;
    private CheckInternetConnection checkInternetConnection;

    public ActiveCustomerRequestAdapter(Context context, ArrayList<ActiveCustomerRequest> shoppingRequest, ActiveRequestFragment activeRequestFragment, FirebaseFirestore database)
    {
        this.context = context;
        this.shoppingRequest = shoppingRequest;
        this.activeRequestFragment = activeRequestFragment;
        this.database = database;
        Log.d(TAG, "ActiveCustomerRequestAdapter: BUILT");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.customer_active_request_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ActiveCustomerRequestAdapter.ViewHolder holder, int position)
    {
        final String TAG = "ActiveCustomerRequestAdapter";
        final ActiveCustomerRequest currentShoppingRequest = shoppingRequest.get(position);

        try
        {
            // Update distance from the Customer using the device location
            if(((AccountClient)context.getApplicationContext()).getAccount().getAccountType().equals("Personal Shopper"))
            {
                if(personalShopperListener != null)
                {
                    personalShopperListener.remove();
                }
                holder.mEmail.setText(currentShoppingRequest.getCustomerEmail());
                holder.mFinishButton.setVisibility(View.VISIBLE);
                Location startPoint=new Location("Personal Shopper Location");
                startPoint.setLatitude(((AccountClient)context.getApplicationContext()).getAccount().getGeoPoint().getLatitude());
                startPoint.setLongitude(((AccountClient)context.getApplicationContext()).getAccount().getGeoPoint().getLongitude());

                Location endPoint=new Location("Customer Location");
                endPoint.setLatitude(currentShoppingRequest.getCustomerGeoPoint().getLatitude());
                endPoint.setLongitude(currentShoppingRequest.getCustomerGeoPoint().getLongitude());

                double meterToMiles = startPoint.distanceTo(endPoint) * 0.00062137;
                double milesTo2Decimal = Math.round(meterToMiles * 100) / 100.0;
                holder.mDistance.setText(milesTo2Decimal + " miles");
            }
            // Update distance from the Personal Shopper using the Personal Shopper's Location on Firebase
            else if(((AccountClient)context.getApplicationContext()).getAccount().getAccountType().equals("Customer"))
            {
                holder.mEmail.setText(currentShoppingRequest.getShopperEmail());
                holder.mFinishButton.setVisibility(View.INVISIBLE);;

                personalShopperRef = database.collection("users").document(currentShoppingRequest.getShopperUserID());
                personalShopperListener = personalShopperRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot snapshot,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (snapshot != null && snapshot.exists()) {
                            Log.d(TAG, "onEvent: " + snapshot.getGeoPoint("geoPoint"));
                            //Log.d(TAG, "onBindViewHolder: shopper geopoint" + personalShopperGeoPoint);

                            Location startPoint=new Location("Personal Shopper Location");
                            startPoint.setLatitude(snapshot.getGeoPoint("geoPoint").getLatitude());
                            startPoint.setLongitude(snapshot.getGeoPoint("geoPoint").getLongitude());

                            Location endPoint=new Location("Customer Location");
                            endPoint.setLatitude(currentShoppingRequest.getCustomerGeoPoint().getLatitude());
                            endPoint.setLongitude(currentShoppingRequest.getCustomerGeoPoint().getLongitude());

                            double meterToMiles = startPoint.distanceTo(endPoint) * 0.00062137;
                            double milesTo2Decimal = Math.round(meterToMiles * 100) / 100.0;
                            holder.mDistance.setText(milesTo2Decimal + " miles");

                        } else {
                            Log.d(TAG, "Current data: null");
                        }
                    }
                });
            }
            // Display the role of the logged in user for the active request
            // Customer or Personal Shopper
            holder.mRole.setText(currentShoppingRequest.getRole());
        }
        catch (NullPointerException e)
        {
            Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
        }

        // A loop to display the Customer Request shopping list items in separate lines
        if(holder.mShoppingList.getText() == "")
        {
            for(int i = 0; i < shoppingRequest.get(0).getShoppingList().size(); i++)
            {
                holder.mShoppingList.append("• " + shoppingRequest.get(0).getShoppingList().get(i) + "\n");
            }
        }

        holder.mEmail.setVisibility(View.VISIBLE);
        holder.mShoppingListHeader.setVisibility(View.VISIBLE);
        holder.mShoppingList.setVisibility(View.VISIBLE);


            holder.mFinishButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkInternetConnection = new CheckInternetConnection(context);
                    if(checkInternetConnection.isConnected())
                    {
                        String distanceString = holder.mDistance.getText().toString().substring(0, holder.mDistance.getText().toString().length() - 6);
                        double distance = Double.parseDouble(distanceString);

                        // A Personal Shopper must be less than 0.1 miles from the Customer to Finish the request
                        if (distance > 0.1)
                        {
                            Toast.makeText(context, "You must be less than 0.2 miles from the delivery point to finish a shopping request.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Create the data to be added to Shopper's History
                        Map<String, Object> personalShopperData = new HashMap<>();
                        personalShopperData.put("documentID", currentShoppingRequest.getDocumentID());
                        personalShopperData.put("customerUserID", currentShoppingRequest.getCustomerUserID());
                        personalShopperData.put("customerEmail", currentShoppingRequest.getCustomerEmail());
                        personalShopperData.put("shopperUserID", currentShoppingRequest.getShopperUserID());
                        personalShopperData.put("shopperEmail", currentShoppingRequest.getShopperEmail());
                        personalShopperData.put("customerGeoPoint", currentShoppingRequest.getCustomerGeoPoint());
                        personalShopperData.put("shopperGeoPoint", currentShoppingRequest.getShopperGeoPoint());
                        personalShopperData.put("shoppingList", currentShoppingRequest.getShoppingList());
                        personalShopperData.put("role", "Personal Shopper");

                        Map<String, Object> customerData = new HashMap<>();
                        customerData.put("documentID", currentShoppingRequest.getDocumentID());
                        customerData.put("customerUserID", currentShoppingRequest.getCustomerUserID());
                        customerData.put("customerEmail", currentShoppingRequest.getCustomerEmail());
                        customerData.put("shopperUserID", currentShoppingRequest.getShopperUserID());
                        customerData.put("shopperEmail", currentShoppingRequest.getShopperEmail());
                        customerData.put("customerGeoPoint", currentShoppingRequest.getCustomerGeoPoint());
                        customerData.put("shopperGeoPoint", currentShoppingRequest.getShopperGeoPoint());
                        customerData.put("shoppingList", currentShoppingRequest.getShoppingList());
                        customerData.put("role", "Customer");

                        // Add the data to the Shopper's history collection
                        database.collection("users")
                                .document(currentShoppingRequest.getShopperUserID())
                                .collection("history")
                                .document(currentShoppingRequest.getDocumentID())
                                .set(personalShopperData)
                                .addOnCompleteListener(new OnCompleteListener<Void>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        Log.d(TAG, "DocumentSnapshot written with ID: " + currentShoppingRequest.getCustomerEmail());

                                        // Create the data to be removed from the Shopper's Current Activity
                                        final Map<String,Object> updates = new HashMap<>();
                                        updates.put("documentID", FieldValue.delete());
                                        updates.put("active", FieldValue.delete());
                                        updates.put("customerUserID", FieldValue.delete());
                                        updates.put("customerEmail", FieldValue.delete());
                                        updates.put("shopperUserID", FieldValue.delete());
                                        updates.put("shopperEmail", FieldValue.delete());
                                        updates.put("customerGeoPoint", FieldValue.delete());
                                        updates.put("shopperGeoPoint", FieldValue.delete());
                                        updates.put("shoppingList", FieldValue.delete());
                                        updates.put("role", FieldValue.delete());

                                        // Remove the selected Customer Request from the Shopper's Current Activity
                                        DocumentReference currentActivityRef = database.collection("users")
                                                .document(((AccountClient)context.getApplicationContext()).getAccount().getUserID())
                                                .collection("shopper data")
                                                .document("current activity");

                                        currentActivityRef.update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "onSuccess: Successfully moved to history: " + currentShoppingRequest.getDocumentID());
                                            }
                                        });
                                    }
                                });

                        // Add the data to the Customer's history collection
                        database.collection("users")
                                .document(currentShoppingRequest.getCustomerUserID())
                                .collection("history")
                                .document(currentShoppingRequest.getDocumentID())
                                .set(customerData)
                                .addOnCompleteListener(new OnCompleteListener<Void>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        Log.d(TAG, "DocumentSnapshot written with ID: " + currentShoppingRequest.getCustomerEmail());

                                        // Create the data to be removed from the Customer's Current Activity
                                        Map<String,Object> updates = new HashMap<>();
                                        updates.put("documentID", FieldValue.delete());
                                        updates.put("active", FieldValue.delete());
                                        updates.put("customerUserID", FieldValue.delete());
                                        updates.put("customerEmail", FieldValue.delete());
                                        updates.put("shopperUserID", FieldValue.delete());
                                        updates.put("shopperEmail", FieldValue.delete());
                                        updates.put("customerGeoPoint", FieldValue.delete());
                                        updates.put("shopperGeoPoint", FieldValue.delete());
                                        updates.put("shoppingList", FieldValue.delete());
                                        updates.put("role", FieldValue.delete());

                                        // Remove the selected Customer Request from the Shopper's Current Activity
                                        DocumentReference currentActivityRef = database.collection("users")
                                                .document(currentShoppingRequest.getCustomerUserID())
                                                .collection("shopper data")
                                                .document("current activity");

                                        currentActivityRef.update(updates).addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "onSuccess: Successfully moved to history: " + currentShoppingRequest.getDocumentID());
                                            }
                                        });
                                    }
                                });

                        // Update the Customer's customerRequest boolean value to false
                        // so the Customer can create a new request
                        database.collection("users")
                                .document(currentShoppingRequest.getCustomerUserID())
                                .update("customerRequest", false);
                    }
                    else
                    {
                        Toast.makeText(context, "Internet Connection is Required", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    }

    @Override
    public int getItemCount()
    {
        return shoppingRequest.size();
    }

    // Holds individual widgets in memory for each active request
    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView mRole;
        TextView mEmail;
        TextView mShoppingListHeader;
        TextView mShoppingList;
        TextView mDistance;
        RelativeLayout mParentLayout;
        Button mFinishButton;

        public ViewHolder(@NonNull View itemView)
        {
            super(itemView);
            mRole = itemView.findViewById(R.id.tv_active_request_role);
            mEmail = itemView.findViewById(R.id.tv_active_request_customer_email);
            mShoppingListHeader = itemView.findViewById(R.id.tv_active_request_shopping_list_header);
            mShoppingList = itemView.findViewById(R.id.tv_active_request_shopping_list);
            mDistance = itemView.findViewById(R.id.tv_active_request_distance);
            mParentLayout = itemView.findViewById(R.id.relative_layout_active_request_item);
            mFinishButton = itemView.findViewById(R.id.btn_active_request_finish);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if(personalShopperListener != null)
        {
            personalShopperListener.remove();
        }
    }
}
