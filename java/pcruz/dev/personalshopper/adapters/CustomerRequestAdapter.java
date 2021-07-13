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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.ActiveCustomerRequest;
import pcruz.dev.personalshopper.models.CustomerRequest;
import pcruz.dev.personalshopper.ui.SearchFragment;

public class CustomerRequestAdapter extends RecyclerView.Adapter<CustomerRequestAdapter.ViewHolder> {
    private static final String TAG = "CustomerRequestAdapter";

    private ArrayList<CustomerRequest> mShoppingRequestList;
    private Context mContext;
    private SearchFragment mSearchFragment;
    private FirebaseFirestore mDatabase;
    //List<String> shoppingListItems;

    public CustomerRequestAdapter(Context context, ArrayList<CustomerRequest> shoppingRequestList, SearchFragment searchFragment, FirebaseFirestore database)
    {
        mShoppingRequestList = shoppingRequestList;
        mContext = context;
        mSearchFragment = searchFragment;
        mDatabase = database;
    }

    // Responsible for inflating the view
    // Recycling the view holders
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.customer_request_item, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    // Gets called every time a new item is added to the list of shopper request
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        final CustomerRequest currentShoppingRequest = mShoppingRequestList.get(position);
        final float scale = holder.mParentLayout.getResources().getDisplayMetrics().density;

        Location startPoint = new Location("Personal Shopper Location");
        startPoint.setLatitude(((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getGeoPoint().getLatitude());
        startPoint.setLongitude(((AccountClient)mSearchFragment.getActivity().getApplicationContext()).getAccount().getGeoPoint().getLongitude());

        Location endPoint = new Location("Customer Location");
        endPoint.setLatitude(currentShoppingRequest.getCustomerGeoPoint().getLatitude());
        endPoint.setLongitude(currentShoppingRequest.getCustomerGeoPoint().getLongitude());

        // Convert meter to miles up to 2 decimal points
        double meterToMiles = startPoint.distanceTo(endPoint) * 0.00062137;
        double milesTo2Decimal = Math.round(meterToMiles * 100) / 100.0;

        holder.mEmail.setText(currentShoppingRequest.getCustomerEmail());
        holder.mDistance.setText(milesTo2Decimal + " miles");
        holder.mCollapseIcon.setText("▼");
        holder.mShoppingList.setText("");

        // Set the default size of each card view to 80dp
        holder.mParentLayout.getLayoutParams().height = (int) (80 * scale);
        holder.mAcceptButton.setVisibility(View.INVISIBLE);

        holder.mParentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // Clicking the card view at 80dp enlarges it to 200dp to display the Customer Shopping List
                if(holder.mParentLayout.getLayoutParams().height == (int) (80 * scale))
                {
                    holder.mParentLayout.getLayoutParams().height = (int) (200 * scale);
                    for(int i = 0; i < currentShoppingRequest.getShoppingList().size(); i++)
                    {
                        holder.mShoppingList.append("• " + currentShoppingRequest.getShoppingList().get(i) + "\n");
                    }
                    holder.mShoppingListHeader.setVisibility(View.VISIBLE);
                    holder.mShoppingList.setVisibility(View.VISIBLE);
                    holder.mAcceptButton.setVisibility(View.VISIBLE);
                    holder.mCollapseIcon.setText("▲");
                }
                // Clicking the card view at 200dp puts it back to default 80dp to hide the Customer Shopping List
                else
                {
                    holder.mParentLayout.getLayoutParams().height = (int) (80 * scale);
                    holder.mShoppingList.setText("");
                    holder.mCollapseIcon.setText("▼");
                    holder.mShoppingListHeader.setVisibility(View.INVISIBLE);
                    holder.mShoppingList.setVisibility(View.INVISIBLE);
                    holder.mAcceptButton.setVisibility(View.INVISIBLE);
                }
                // Updates the layout
                holder.mParentLayout.requestLayout();
            }
        });

        // Personal Shopper selects a Customer Request
        holder.mAcceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
//                Log.d(TAG, "CURRENT ID: " + mSearchFragment.getAccount().getUserID());

                DocumentReference userCurrentActivityDoc = mDatabase.collection("users")
                        .document(FirebaseAuth.getInstance().getUid())
                        .collection("shopper data")
                        .document("current activity");
                userCurrentActivityDoc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                Map<String, Object> map = document.getData();
                                if (map.size() == 0) {
                                    Log.d(TAG, "Button TEST: " + currentShoppingRequest.getDocumentID());

                                    WriteBatch batch = mDatabase.batch();

                                    // Remove user from available shoppers
                                    DocumentReference availableShopperRef = mDatabase
                                            .collection("personalShoppersAvailable")
                                            .document(((AccountClient)mContext.getApplicationContext()).getAccount().getUserID());
                                    batch.delete(availableShopperRef);

                                    // Make the Shopper Request unavailable
                                    DocumentReference shoppingRequestRef = mDatabase
                                            .collection("customerRequests")
                                            .document(currentShoppingRequest.getDocumentID());
                                    batch.update(shoppingRequestRef, "available", false);

                                    // Change Shopper Availability to False
                                    DocumentReference userRef = mDatabase
                                            .collection("users")
                                            .document(((AccountClient)mContext.getApplicationContext()).getAccount().getUserID());
                                    batch.update(userRef,"shopperAvailability", false);

                                    // Add the selected Shopper Request/Customer to the Active Document
                                    ActiveCustomerRequest shoppingListShopper = new ActiveCustomerRequest(
                                            currentShoppingRequest.getDocumentID(),
                                            currentShoppingRequest.getCustomerUserID(),
                                            currentShoppingRequest.getCustomerEmail(),
                                            ((AccountClient)mContext.getApplicationContext()).getAccount().getUserID(),
                                            ((AccountClient)mContext.getApplicationContext()).getAccount().getEmail(),
                                            currentShoppingRequest.getCustomerGeoPoint(),
                                            ((AccountClient)mContext.getApplicationContext()).getAccount().getGeoPoint(),
                                            currentShoppingRequest.getShoppingList(),
                                            "Personal Shopper", true);

                                    ActiveCustomerRequest shoppingListCustomer = new ActiveCustomerRequest(
                                            currentShoppingRequest.getDocumentID(),
                                            currentShoppingRequest.getCustomerUserID(),
                                            currentShoppingRequest.getCustomerEmail(),
                                            ((AccountClient)mContext.getApplicationContext()).getAccount().getUserID(),
                                            ((AccountClient)mContext.getApplicationContext()).getAccount().getEmail(),
                                            currentShoppingRequest.getCustomerGeoPoint(),
                                            ((AccountClient)mContext.getApplicationContext()).getAccount().getGeoPoint(),
                                            currentShoppingRequest.getShoppingList(),
                                            "Customer", true);

                                    // Add the accepted Customer Request to the Shopper's Current Activity Document
                                    DocumentReference personalShopperCurrentActivityRef = mDatabase.collection("users")
                                            .document(FirebaseAuth.getInstance().getUid())
                                            .collection("shopper data")
                                            .document("current activity");
                                    batch.set(personalShopperCurrentActivityRef, shoppingListShopper);

                                    // Add the accepted Shopper Available to the Customer's Active Request Document
                                    DocumentReference customerActiveRequestRef = mDatabase
                                            .collection("users")
                                            .document(currentShoppingRequest.getCustomerUserID())
                                            .collection("shopper data")
                                            .document("current activity");
                                    batch.set(customerActiveRequestRef, shoppingListCustomer);

                                    batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Log.d(TAG, "onComplete: batch: batch queries successful.");
                                        }
                                    });

                                    TextView txt = mSearchFragment.getActivity().findViewById(R.id.btn_shopper_update_availability);
                                    txt.setText(R.string.btn_shopper_unavailable);
                                    mSearchFragment.updatePosition(holder.getAdapterPosition());
//                                  shoppingListItems.clear();

                                    Log.d(TAG, "POSITION: " + holder.getAdapterPosition());
                                } else {
                                    Log.d(TAG, "Document is not empty!");
                                }
                            }
                        }
                    }
                });
            }
        });
    }

    // Tells the adapter how many list items are in the list
    @Override
    public int getItemCount() {
        return mShoppingRequestList.size();
    }

    // Holds individual widgets in memory for each customer request
    public class ViewHolder extends RecyclerView.ViewHolder
    {
        TextView mEmail;
        TextView mShoppingListHeader;
        TextView mShoppingList;
        TextView mDistance;
        TextView mCollapseIcon;
        Button mAcceptButton;
        RelativeLayout mParentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mEmail = itemView.findViewById(R.id.tv_customer_request_email);
            mShoppingListHeader = itemView.findViewById(R.id.tv_customer_shopping_list_header);
            mShoppingList = itemView.findViewById(R.id.tv_customer_request_shopping_list);
            mDistance = itemView.findViewById(R.id.tv_customer_request_distance);
            mCollapseIcon = itemView.findViewById(R.id.tv_customer_request_collapse_icon);
            mParentLayout = itemView.findViewById(R.id.relative_layout_customer_request_item);
            mAcceptButton = itemView.findViewById(R.id.btn_accept_customer_request);
        }
    }
}
