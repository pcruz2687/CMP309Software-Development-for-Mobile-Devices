package pcruz.dev.personalshopper.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.Account;
import pcruz.dev.personalshopper.models.CheckInternetConnection;

public class ActiveCustomerRequestDialog extends AppCompatDialogFragment {

    private static final String TAG = "RequestedShoppingListDi";

    // View
    LayoutInflater inflater;
    View view;
    private TextView shoppingList;

    // Variables
    AlertDialog.Builder builder;
    private FirebaseFirestore database;
    private List<String> shoppingListItems = new ArrayList<>();
    CheckInternetConnection checkInternetConnection;

    ListenerRegistration pendingShoppingRequestListener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        builder = new AlertDialog.Builder(getActivity());
        inflater = getActivity().getLayoutInflater();
        view = inflater.inflate(R.layout.dialog_active_customer_request, null);
        database = FirebaseFirestore.getInstance();
        createShoppingList();

        builder.setView(view)
                .setTitle("Shopping Request")
                .setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Checks if the device is connected to the internet before deleting
                        checkInternetConnection = new CheckInternetConnection(getActivity());
                        if(checkInternetConnection.isConnected())
                        {
                            if(shoppingListItems.size() > 0)
                            {
                                // Create the data to be removed from the Shopper's Current Activity
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

                                // Remove the selected Customer Request from the Shopper's Current Activity
                                Query currentActivityRef = database.collection("customerRequests").whereEqualTo("available", true)
                                        .whereEqualTo("customerUserID", ((AccountClient)getActivity().getApplicationContext()).getAccount().getUserID());

                                currentActivityRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                    @Override
                                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                        for(QueryDocumentSnapshot querySnapshot : queryDocumentSnapshots)
                                        {
                                            querySnapshot.getReference().delete();
                                        }
                                    }
                                });

                                // Change customerRequest to false
                                // meaning there is no pending shopping request anymore
                                database.collection("users")
                                        .document(((AccountClient)getActivity().getApplicationContext()).getAccount().getUserID())
                                        .update("customerRequest", false);

                                ((AccountClient)getActivity().getApplicationContext()).getAccount().setCustomerRequest(false);

                                Toast.makeText(getActivity(), "" + ((AccountClient)getActivity().getApplicationContext()).getAccount().getCustomerRequest(), Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(getActivity(), "Your request can no longer be deleted.", Toast.LENGTH_SHORT).show();
                            }
                        }
                        else
                        {
                            Toast.makeText(getActivity(), "Internet Connection is Required", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        return builder.create();
    }

    // Display the Customer Shopping List if a request is pending
    // Display check Track Tab if the request is active
    private void createShoppingList()
    {
        shoppingList = view.findViewById(R.id.tv_pending_customer_request);

        CollectionReference updateActiveRequestRef = database
                .collection("customerRequests");

        pendingShoppingRequestListener = updateActiveRequestRef
                .whereEqualTo("customerUserID", ((AccountClient)getActivity().getApplicationContext()).getAccount().getUserID())
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
                                Log.d(TAG, "onEvent: " + doc.get("shoppingList").toString());
                                shoppingListItems = (List<String>) doc.get("shoppingList");
                                for(int i = 0; i < shoppingListItems.size(); i++)
                                {
                                    shoppingList.append("• " + shoppingListItems.get(i) + "\n");
                                }
                            }
                        }
                        else
                        {
                            shoppingList.setText("A Personal Shopper has accepted your request. Check your Track tab for further details.");
                            shoppingListItems.clear();
                            pendingShoppingRequestListener.remove();
                        }
                    }
                });
    }
}
