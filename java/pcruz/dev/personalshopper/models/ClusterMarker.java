package pcruz.dev.personalshopper.models;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class ClusterMarker implements ClusterItem
{
    private LatLng mPosition;
    private String mTitle;
    private String mSnippet;
    private int mIconPicture;
    private ActiveCustomerRequest mCustomerRequest;

    public ClusterMarker(LatLng position, String title, String snippet, int iconPicture, ActiveCustomerRequest customerRequest) {
        mPosition = position;
        mTitle = title;
        mSnippet = snippet;
        mIconPicture = iconPicture;
        mCustomerRequest = customerRequest;
    }

    public ClusterMarker() {

    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    public void setPosition(LatLng position) {
        mPosition = position;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        return mSnippet;
    }

    public int getIconPicture() {
        return mIconPicture;
    }
}
