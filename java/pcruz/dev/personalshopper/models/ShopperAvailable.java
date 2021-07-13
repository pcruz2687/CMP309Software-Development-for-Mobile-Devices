package pcruz.dev.personalshopper.models;

import com.google.firebase.firestore.GeoPoint;

public class ShopperAvailable
{
    String mUserID;
    String mEmail;
    GeoPoint mGeoPoint;

    public ShopperAvailable(String userID, String email, GeoPoint geoPoint) {
        mUserID = userID;
        mEmail = email;
        mGeoPoint = geoPoint;
    }

    public String getShopperUserID() {
        return mUserID;
    }

    public String getShopperEmail() {
        return mEmail;
    }

    public GeoPoint getShopperGeoPoint() {
        return mGeoPoint;
    }

    @Override
    public String toString() {
        return "ShopperAvailable{" +
                "userID='" + mUserID + '\'' +
                ", email='" + mEmail + '\'' +
                ", geoPoint=" + mGeoPoint +
                '}';
    }
}
