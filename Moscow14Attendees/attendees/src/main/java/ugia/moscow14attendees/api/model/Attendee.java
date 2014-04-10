package ugia.moscow14attendees.api.model;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * Created by joseluisugia on 10/04/14.
 */
public class Attendee {

    public String name;

    public String email;

    @SerializedName("created_at")
    public Date createdAt;

    @Override
    public String toString() {
        return name;
    }
}
