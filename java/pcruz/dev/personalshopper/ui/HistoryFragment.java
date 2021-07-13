package pcruz.dev.personalshopper.ui;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import pcruz.dev.personalshopper.adapters.HistoryAdapter;
import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.ActiveCustomerRequest;
import pcruz.dev.personalshopper.models.FragmentCallback;

public class HistoryFragment extends Fragment
{
    private static final String TAG = "HistoryFragment";

    // Variables
    FirebaseUser user;
    FirebaseFirestore database;
    FragmentCallback fragmentCallback;
    ListenerRegistration historyListener;
    private ArrayList<ActiveCustomerRequest> customerRequestListHistory;
    int position;

    // UI
    View root;
    Context context;

    // Recycler View
    RecyclerView recyclerView;
    LinearLayoutManager manager;
    HistoryAdapter historyAdapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_history, container, false);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        user = FirebaseAuth.getInstance().getCurrentUser();
        database = FirebaseFirestore.getInstance();

        customerRequestListHistory = new ArrayList<>();
        buildRecyclerView();
        createCustomerRequestList();
    }

    public void buildRecyclerView()
    {
        recyclerView = root.findViewById(R.id.recyler_view_history);
        manager = new LinearLayoutManager(getActivity());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(manager);
        historyAdapter = new HistoryAdapter(getContext(), customerRequestListHistory, HistoryFragment.this, database);
        recyclerView.setAdapter(historyAdapter);
    }

    public void createCustomerRequestList()
    {
        position = 0;
        Query query = database.collection("users").document(user.getUid()).collection("history");
        historyListener = ((Query) query).addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                            //customerRequestListHistory.add(new CustomerRequest(dc.getDocument().getId(), dc.getDocument().getString("userID"), dc.getDocument().getString("email"), (GeoPoint) dc.getDocument().get("customerGeoPoint"), (List<String>) dc.getDocument().get("shoppingList")));
                            customerRequestListHistory.add(new ActiveCustomerRequest(dc.getDocument().getString("documentID"), dc.getDocument().getString("customerUserID"), dc.getDocument().getString("customerEmail"), dc.getDocument().getString("shopperUserID"), dc.getDocument().getString("shopperEmail"), (GeoPoint) dc.getDocument().get("customerGeoPoint"), (GeoPoint) dc.getDocument().get("shopperGeoPoint"), (List<String>) dc.getDocument().get("shoppingList"), dc.getDocument().getString("role"), true));
                            historyAdapter.notifyDataSetChanged();
                            break;
                        case MODIFIED:
                            historyAdapter.notifyItemChanged(position);
                            break;
                        case REMOVED:
                            customerRequestListHistory.remove(position);
                            historyAdapter.notifyItemRemoved(position);
                            break;
                    }
                }

            }
        });
    }

    @Override
    public void onAttach(Context context)
    {
        fragmentCallback = (FragmentCallback) context;
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onStop()
    {
        super.onStop();
        try
        {
            // Stop the database snapshot listener is not in display
            historyListener.remove();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            historyListener.remove();
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