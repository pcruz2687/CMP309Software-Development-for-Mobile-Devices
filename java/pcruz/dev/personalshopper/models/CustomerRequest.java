package pcruz.dev.personalshopper.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.GeoPoint;

import java.util.List;

public class CustomerRequest
{
    // Variables
    private String mDocumentID;
    private String mCustomerUserID;
    private String mCustomerEmail;
    private GeoPoint mCustomerGeoPoint;
    private List<String> mShoppingList;

    // Default Constructor
    public CustomerRequest()
    {

    }

    // For creating a Customer Request objects to be displayed to the Personal Shopper
    public CustomerRequest(String documentID, String customerUserID, String customerEmail, GeoPoint customerGeoPoint, List<String> shoppingList)
    {
        mDocumentID = documentID;
        mCustomerUserID = customerUserID;
        mCustomerEmail = customerEmail;
        mCustomerGeoPoint = customerGeoPoint;
        mShoppingList = shoppingList;
    }

    public String getDocumentID() {
        return mDocumentID;
    }

    public String getCustomerUserID() {
        return mCustomerUserID;
    }

    public String getCustomerEmail() {
        return mCustomerEmail;
    }

    public GeoPoint getCustomerGeoPoint() {
        return mCustomerGeoPoint;
    }

    public List<String> getShoppingList() {
        return mShoppingList;
    }
}
