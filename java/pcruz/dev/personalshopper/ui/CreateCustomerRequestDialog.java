package pcruz.dev.personalshopper.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pcruz.dev.personalshopper.AccountClient;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.Account;
import pcruz.dev.personalshopper.models.CheckInternetConnection;

public class CreateCustomerRequestDialog extends AppCompatDialogFragment {
    private static final String TAG = "CustomerListDialog";

    // Variables
    private FirebaseFirestore database;
    private List<String> customerListItems = new ArrayList<>();
    private EditText item_1;
    private EditText item_2;
    private EditText item_3;
    GeoPoint emptyGeoPoint;
    String blankShopperID;
    String blankShopperEmail;
    CheckInternetConnection checkInternetConnection;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_create_customer_request, null);
        database = FirebaseFirestore.getInstance();
        final Account account = ((AccountClient)getActivity().getApplicationContext()).getAccount();
        emptyGeoPoint = new GeoPoint(0.0, 0.0);
        blankShopperID = "";
        blankShopperEmail = "";

        item_1 = view.findViewById(R.id.edit_item_1);
        item_2 = view.findViewById(R.id.edit_item_2);
        item_3 = view.findViewById(R.id.edit_item_3);

        builder.setView(view)
                .setTitle("Create Customer Request")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // Checks if the device is connected to the internet when the "Create" button is clicked
                        checkInternetConnection = new CheckInternetConnection(getActivity());
                        if(checkInternetConnection.isConnected())
                        {
                            addItemToList(item_1);
                            addItemToList(item_2);
                            addItemToList(item_3);

                            if(customerListItems.size() > 0)
                            {
                                // Add the customer request to the database collection for pending Customer Requests
                                CollectionReference customerRequestsRef = database.collection("customerRequests");
                                String documentID = customerRequestsRef.document().getId();

                                Map<String, Object> customerRequest = new HashMap<>();
                                customerRequest.put("available", true);
                                customerRequest.put("documentID", documentID);
                                customerRequest.put("customerUserID", account.getUserID());
                                customerRequest.put("customerEmail", account.getEmail());
                                customerRequest.put("customerGeoPoint", account.getGeoPoint());;
                                customerRequest.put("shoppingList", customerListItems);

                                customerRequestsRef
                                        .document(documentID)
                                        .set(customerRequest)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "DocumentSnapshot written");

                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error adding document", e);
                                            }
                                        });


                                // Change customerRequest to true
                                // meaning there is an active customer request in this account
                                database.collection("users")
                                        .document(account.getUserID())
                                        .update("customerRequest", true);

                                ((AccountClient)getActivity().getApplicationContext()).getAccount().setCustomerRequest(true);
                            }
                            else
                            {
                                Toast.makeText(getActivity(), "Customer list must contain at least 1 item.", Toast.LENGTH_SHORT).show();
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

    // Add the items to the list variable
    private void addItemToList(EditText editText)
    {
        if(editText.getText().toString().trim().length() > 0)
        {
            customerListItems.add(editText.getText().toString());
        }
    }
}
