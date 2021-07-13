package pcruz.dev.personalshopper.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class ActiveCustomerRequest implements Parcelable
{
    // Variables
    private String mDocumentID;
    private String mCustomerUserID;
    private String mCustomerEmail;
    private String mShopperUserID;
    private String mShopperEmail;
    private GeoPoint mCustomerGeoPoint;
    private GeoPoint mShopperGeoPoint;
    private List<String> mShoppingList;
    private boolean mActive;
    private String mRole;

    // Default Constructor
    public ActiveCustomerRequest()
    {

    }

    // Used for displaying an Active Request
    public ActiveCustomerRequest(String documentID, String customerUserID, String customerEmail, String shopperUserID, String shopperEmail, GeoPoint customerGeoPoint, GeoPoint shopperGeoPoint, List<String> shoppingList, String role, boolean active)
    {
        mDocumentID= documentID;
        mCustomerUserID = customerUserID;
        mCustomerEmail = customerEmail;
        mShopperUserID= shopperUserID;
        mShopperEmail = shopperEmail;
        mCustomerGeoPoint = customerGeoPoint;
        mShopperGeoPoint = shopperGeoPoint;
        mShoppingList = shoppingList;
        mRole = role;
        mActive = active;
    }

    protected ActiveCustomerRequest(Parcel in) {
        mDocumentID= in.readString();
        mCustomerUserID = in.readString();
        mCustomerEmail = in.readString();
        mShopperUserID = in.readString();
        mShopperEmail = in.readString();
        mShoppingList = in.createStringArrayList();
        mRole = in.readString();
        mActive = in.readByte() != 0;
    }

    public static final Creator<ActiveCustomerRequest> CREATOR = new Creator<ActiveCustomerRequest>() {
        @Override
        public ActiveCustomerRequest createFromParcel(Parcel in) {
            return new ActiveCustomerRequest(in);
        }

        @Override
        public ActiveCustomerRequest[] newArray(int size) {
            return new ActiveCustomerRequest[size];
        }
    };

    public String getDocumentID() {
        return mDocumentID;
    }

    public void setDocumentID(String documentID) {
        mDocumentID= documentID;
    }

    public String getCustomerUserID() {
        return mCustomerUserID;
    }

    public void setCustomerUserID(String customerUserID) {
        mCustomerUserID = customerUserID;
    }

    public String getCustomerEmail() {
        return mCustomerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.mCustomerEmail = customerEmail;
    }

    public String getShopperUserID() {
        return mShopperUserID;
    }

    public void setShopperUserID(String shopperUserID) {
        mShopperUserID = shopperUserID;
    }

    public String getShopperEmail() {
        return mShopperEmail;
    }

    public void setShopperEmail(String shopperEmail) {
        mShopperEmail = shopperEmail;
    }

    public GeoPoint getCustomerGeoPoint() {
        return mCustomerGeoPoint;
    }

    public void setCustomerGeoPoint(GeoPoint customerGeoPoint) {
        mCustomerGeoPoint = customerGeoPoint;
    }

    public GeoPoint getShopperGeoPoint() {
        return mShopperGeoPoint;
    }

    public void setShopperGeoPoint(GeoPoint shopperGeoPoint) {
        mShopperGeoPoint = shopperGeoPoint;
    }

    public List<String> getShoppingList() {
        return mShoppingList;
    }

    public void setShoppingList(List<String> shoppingList) {
        mShoppingList = shoppingList;
    }


    public String getRole() {
        return mRole;
    }

    public void setRole(String role) {
        mRole = role;
    }


    public boolean isActive() {
        return mActive;
    }

    public void setActive(boolean active) {
        mActive = active;
    }

    @Override
    public String toString() {
        return "CustomerRequest{" +
                "documentID='" + mDocumentID + '\'' +
                ", customerUserID='" + mCustomerUserID + '\'' +
                ", customerEmail='" + mCustomerEmail + '\'' +
                ", shopperUserID='" + mShopperUserID + '\'' +
                ", shopperEmail='" + mShopperEmail + '\'' +
                ", customerGeoPoint=" + mCustomerGeoPoint +
                ", shopperGeoPoint=" + mShopperGeoPoint +
                ", shoppingList=" + mShoppingList +
                ", role=" + mRole +
                ", active=" + mActive +
                '}';
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mDocumentID);
        dest.writeString(mCustomerUserID);
        dest.writeString(mCustomerEmail);
        dest.writeString(mShopperUserID);
        dest.writeString(mShopperEmail);
        dest.writeStringList(mShoppingList);
        dest.writeString(mRole);
        dest.writeByte((byte) (mActive ? 1 : 0));
    }
}
