package hu.szte.gergusz.musicshare.model;

import android.net.Uri;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
@ToString
public class Music {

    @Exclude @Getter(onMethod = @__({@Exclude})) private String firebaseId;
    private String title;
    private String artist;
    private String album;
    @Exclude @Getter(onMethod = @__({@Exclude})) private byte[] albumArtBytes;
    @Exclude @Getter(onMethod = @__({@Exclude})) private Uri albumArtUri;
    private String genre;
    private int length;
    private String uploaderId;

}
