package pcruz.dev.personalshopper.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.GeoPoint;

public class Account implements Parcelable {
    private String mUserID;
    private String mEmail;
    private String mAccountType;
    private boolean mCustomerRequestPending;
    private boolean mShopperAvailability;
    private GeoPoint mGeoPoint;

    public Account()
    {

    }

    public Account(String userID, String email, String accountType, boolean customerRequestPending, boolean shopperAvailability)
    {
        mUserID = userID;
        mEmail = email;
        mAccountType = accountType;
        mCustomerRequestPending = customerRequestPending;
        mShopperAvailability = shopperAvailability;
    }

    protected Account(Parcel in) {
        mUserID = in.readString();
        mEmail = in.readString();
        mAccountType = in.readString();
        mCustomerRequestPending = in.readByte() != 0;
        mShopperAvailability = in.readByte() != 0;
    }

    public static final Creator<Account> CREATOR = new Creator<Account>() {
        @Override
        public Account createFromParcel(Parcel in) {
            return new Account(in);
        }

        @Override
        public Account[] newArray(int size) {
            return new Account[size];
        }
    };

    public String getUserID() {
        return mUserID;
    }

    public void setUserID(String userID) {
        mUserID = userID;
    }

    public String getEmail() {
        return mEmail;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public String getAccountType() {
        return mAccountType;
    }

    public void setAccountType(String accountType) {
        mAccountType = accountType;
    }

    public boolean getCustomerRequest() {
        return mCustomerRequestPending;
    }

    public void setCustomerRequest(boolean customerRequest) {
        this.mCustomerRequestPending = customerRequest;
    }

    public boolean getShopperAvailability() {
        return mShopperAvailability;
    }

    public void setShopperAvailability(boolean shopperAvailability) {
        this.mShopperAvailability = shopperAvailability;
    }

    public GeoPoint getGeoPoint() {
        return mGeoPoint;
    }

    public void setGeoPoint(GeoPoint geoPoint) {
        mGeoPoint = geoPoint;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(mUserID);
        parcel.writeString(mEmail);
        parcel.writeString(mAccountType);
        parcel.writeByte((byte) (mCustomerRequestPending ? 1 : 0));
        parcel.writeByte((byte) (mShopperAvailability ? 1 : 0));
    }

    @Override
    public String toString() {
        return "Account{" +
                "userID='" + mUserID + '\'' +
                ", email='" + mEmail + '\'' +
                ", accountType='" + mAccountType + '\'' +
                ", customerRequest=" + mCustomerRequestPending +
                ", shopperAvailability=" + mShopperAvailability +
                ", geoPoint=" + mGeoPoint +
                '}';
    }
}

