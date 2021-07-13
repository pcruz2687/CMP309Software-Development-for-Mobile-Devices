package pcruz.dev.personalshopper.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import pcruz.dev.personalshopper.R;
import pcruz.dev.personalshopper.models.ActiveCustomerRequest;
import pcruz.dev.personalshopper.models.CustomerRequest;
import pcruz.dev.personalshopper.ui.HistoryFragment;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder>
{
    private static final String TAG = "HistoryAdapter";
    private ArrayList<ActiveCustomerRequest> mShoppingRequestListHistory;
    private Context mContext;
    private HistoryFragment mHistoryFragment;
    private FirebaseFirestore mDatabase;
    List<String> shoppingList;

    public HistoryAdapter(Context context, ArrayList<ActiveCustomerRequest> shoppingRequestListHistory, HistoryFragment historyFragment, FirebaseFirestore database)
    {
        mShoppingRequestListHistory = shoppingRequestListHistory;
        mContext = context;
        mHistoryFragment = historyFragment;
        mDatabase = database;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_history, parent, false);
        ViewHolder holder = new ViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final ActiveCustomerRequest currentShoppingRequest = mShoppingRequestListHistory.get(position);
        final float scale = holder.mParentLayout.getResources().getDisplayMetrics().density;

        holder.mDocumentID.setText(currentShoppingRequest.getDocumentID());
        holder.mCustomerEmail.setText(currentShoppingRequest.getShopperEmail());
        holder.mPersonalShopperEmail.setText(currentShoppingRequest.getCustomerEmail());
        holder.mRole.setText(currentShoppingRequest.getRole());
        holder.mCollapseIcon.setText("▼");
        holder.mShoppingList.setText("");

        // Set the default size of each card view to 110dp
        holder.mParentLayout.getLayoutParams().height = (int) (110 * scale);
        holder.mShoppingListIcon.setVisibility(View.INVISIBLE);

        Log.d(TAG, "onClick: email " + currentShoppingRequest.getCustomerEmail());

        holder.mParentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // Clicking the card view at 110dp enlarges it to 200dp to display the Customer Shopping List
                if(holder.mParentLayout.getLayoutParams().height == (int) (110 * scale))
                {
                    holder.mParentLayout.getLayoutParams().height = (int) (200 * scale);
                    for(int i = 0; i < currentShoppingRequest.getShoppingList().size(); i++)
                    {
                        holder.mShoppingList.append("• " + currentShoppingRequest.getShoppingList().get(i) + "\n");
                    }
                    holder.mShoppingListIcon.setVisibility(View.VISIBLE);
                    holder.mShoppingListHeader.setVisibility(View.VISIBLE);
                    holder.mShoppingList.setVisibility(View.VISIBLE);
                    holder.mCollapseIcon.setText("▲");
                }
                // Clicking the card view at 200dp puts it back to default 110dp to hide the Customer Shopping List
                else
                {
                    holder.mParentLayout.getLayoutParams().height = (int) (110 * scale);
                    holder.mShoppingList.setText("");
                    holder.mCollapseIcon.setText("▼");
                    holder.mShoppingListIcon.setVisibility(View.INVISIBLE);
                    holder.mShoppingListHeader.setVisibility(View.INVISIBLE);
                    holder.mShoppingList.setVisibility(View.INVISIBLE);
                }
                // Updates the layout
                holder.mParentLayout.requestLayout();
            }
        });
    }

    @Override
    public int getItemCount() {
        return  mShoppingRequestListHistory.size();
    }

    // Holds individual widgets in memory for each finished request
    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView mDocumentID;
        TextView mCustomerEmail;
        TextView mPersonalShopperEmail;
        TextView mRole;
        TextView mCollapseIcon;
        TextView mShoppingListIcon;
        TextView mShoppingListHeader;
        TextView mShoppingList;
        RelativeLayout mParentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mDocumentID = itemView.findViewById(R.id.tv_document_id_3);
            mCustomerEmail = itemView.findViewById(R.id.tv_history_customer_email);
            mPersonalShopperEmail = itemView.findViewById(R.id.tv_history_shopper_email);
            mRole = itemView.findViewById(R.id.tv_history_role);
            mCollapseIcon = itemView.findViewById(R.id.tv_history_collapse_icon);
            mShoppingListIcon = itemView.findViewById(R.id.tv_history_shopping_list_header_icon);
            mShoppingListHeader = itemView.findViewById(R.id.tv_history_shopping_list_header);
            mShoppingList = itemView.findViewById(R.id.tv_history_shopping_list);
            mParentLayout = itemView.findViewById(R.id.relative_layout_history_item);
        }
    }
}
