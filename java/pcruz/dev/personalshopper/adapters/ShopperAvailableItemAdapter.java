package pcruz.dev.personalshopper.adapters;

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
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.ShopperAvailable;
import pcruz.dev.personalshopper.ui.SearchFragment;

public class ShopperAvailableItemAdapter extends RecyclerView.Adapter<ShopperAvailableItemAdapter.ViewHolder>
{
    private static final String TAG = "ShopperAvailableItemAda";
    private ArrayList<ShopperAvailable> mShopperAvailableList;
    private SearchFragment mSearchFragment;
    private FirebaseFirestore mDatabase;

    public ShopperAvailableItemAdapter(ArrayList<ShopperAvailable> shopperAvailableList, SearchFragment searchFragment, FirebaseFirestore database)
    {
        mShopperAvailableList = shopperAvailableList;
        mSearchFragment = searchFragment;
        mDatabase = database;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shopper_available_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final ShopperAvailable currentShopperAvailable = mShopperAvailableList.get(position);

        Location startPoint=new Location("Customer Location");
        startPoint.setLatitude(((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getGeoPoint().getLatitude());
        startPoint.setLongitude(((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getGeoPoint().getLongitude());

        Location endPoint=new Location("Personal Shopper Location");
        endPoint.setLatitude(currentShopperAvailable.getShopperGeoPoint().getLatitude());
        endPoint.setLongitude(currentShopperAvailable.getShopperGeoPoint().getLongitude());

        // Convert meter to miles up to 2 decimal points
        double meterToMiles = startPoint.distanceTo(endPoint) * 0.00062137;
        double milesTo2Decimal = Math.round(meterToMiles * 100) / 100.0;

        holder.mEmail.setText(currentShopperAvailable.getShopperEmail());
        holder.mDistance.setText(milesTo2Decimal + " miles");

        // Customer selects a Personal Shopper
        holder.mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    Query availableShoppingRequest = mDatabase.collection("customerRequests").whereEqualTo("customerUserID", FirebaseAuth.getInstance().getUid());
                    availableShoppingRequest.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if(task.getResult().size() > 0)
                                {
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        if(document.getBoolean("available"))
                                        {
                                            // Data to add to the Customer's Active Request
                                            Map<String, Object> customerActiveRequest = new HashMap<>();
                                            customerActiveRequest.put("documentID", document.getString("documentID"));
                                            customerActiveRequest.put("customerUserID", ((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getUserID());
                                            customerActiveRequest.put("customerEmail", ((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getEmail());
                                            customerActiveRequest.put("shopperUserID", currentShopperAvailable.getShopperUserID());
                                            customerActiveRequest.put("shopperEmail", currentShopperAvailable.getShopperEmail());
                                            customerActiveRequest.put("customerGeoPoint", document.getGeoPoint("customerGeoPoint"));
                                            customerActiveRequest.put("shopperGeoPoint", currentShopperAvailable.getShopperGeoPoint());
                                            customerActiveRequest.put("shoppingList", document.get( "shoppingList"));
                                            customerActiveRequest.put("active", true);
                                            customerActiveRequest.put("role", "Customer");

                                            // Data to add to the Personal Shopper's Active Request
                                            Map<String, Object> personalShopperActiveRequest = new HashMap<>();
                                            personalShopperActiveRequest.put("documentID", document.getString("documentID"));
                                            personalShopperActiveRequest.put("customerUserID", ((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getUserID());
                                            personalShopperActiveRequest.put("customerEmail", ((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getEmail());
                                            personalShopperActiveRequest.put("shopperUserID", currentShopperAvailable.getShopperUserID());
                                            personalShopperActiveRequest.put("shopperEmail", currentShopperAvailable.getShopperEmail());
                                            personalShopperActiveRequest.put("customerGeoPoint", document.getGeoPoint("customerGeoPoint"));
                                            personalShopperActiveRequest.put("shopperGeoPoint", currentShopperAvailable.getShopperGeoPoint());
                                            personalShopperActiveRequest.put("shoppingList", document.get( "shoppingList"));
                                            personalShopperActiveRequest.put("active", true);
                                            personalShopperActiveRequest.put("role", "Personal Shoppper");

                                            try {
                                                Log.d(TAG, "onComplete: " + customerActiveRequest.toString());
                                                WriteBatch batch = mDatabase.batch();

                                                // Make the Shopper Request unavailable
                                                DocumentReference shoppingRequestRef = mDatabase
                                                        .collection("customerRequests")
                                                        .document(document.getString("documentID"));
                                                batch.update(shoppingRequestRef, "available", false);

                                                // Disable Personal Shopper's availability
                                                DocumentReference personalShopperRef = mDatabase
                                                        .collection("users")
                                                        .document(currentShopperAvailable.getShopperUserID());
                                                batch.update(personalShopperRef, "shopperAvailability", false);

                                                // Remove user from available shoppers
                                                DocumentReference personalShopperAvailableRef = mDatabase
                                                        .collection("personalShoppersAvailable")
                                                        .document(currentShopperAvailable.getShopperUserID());
                                                batch.delete(personalShopperAvailableRef);

                                                // Customer's Current Activitys
                                                DocumentReference customerActiveRef = mDatabase
                                                        .collection("users")
                                                        .document(FirebaseAuth.getInstance().getUid())
                                                        .collection("shopper data")
                                                        .document("current activity");
                                                batch.set(customerActiveRef, customerActiveRequest);

                                                // Personal Shopper's Current Activity
                                                DocumentReference personalShopperActiveRef = mDatabase
                                                        .collection("users")
                                                        .document(currentShopperAvailable.getShopperUserID())
                                                        .collection("shopper data")
                                                        .document("current activity");
                                                batch.set(personalShopperActiveRef, personalShopperActiveRequest);

                                                batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        Log.d(TAG, "onComplete: batch: batch queries successful.");
                                                    }
                                                });

                                                mSearchFragment.updatePosition(holder.getAdapterPosition());
//                                              shoppingListItems.clear();
                                            }
                                            catch (NullPointerException e)
                                            {
                                                Log.w(TAG, "onComplete: " + e.getMessage());
                                            }
                                        }
                                        else
                                        {
                                            Toast.makeText(mSearchFragment.getActivity(), "Please check if you already have an Active Request or create a new one.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                                else
                                {
                                    Toast.makeText(mSearchFragment.getActivity(), "Create a Shopping Request before selecting a Personal Shopper.", Toast.LENGTH_SHORT).show();
                                }
                            }
                            else {
                                Log.d(TAG, "Error getting documents: ", task.getException());
                            }
                        }
                    });
                }
                catch (NullPointerException e)
                {
                    Log.w(TAG, e.getMessage());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mShopperAvailableList.size();
    }

    // Holds individual widgets in memory for each shopper request
    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView mEmail;
        TextView mDistance;
        RelativeLayout mRelativeLayout;
        Button mAcceptButton;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mEmail = itemView.findViewById(R.id.tv_shopper_available_email);
            mDistance = itemView.findViewById(R.id.tv_shopper_available_distance);
            mRelativeLayout = itemView.findViewById(R.id.relative_layout_shopper_available_item);
            mAcceptButton = itemView.findViewById(R.id.btn_accept_shopper_available);
        }
    }
}
