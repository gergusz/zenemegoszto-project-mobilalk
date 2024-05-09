package hu.szte.gergusz.musicshare.model;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

import lombok.AccessLevel;
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

    private String title;
    private String artist;
    private String album;
    @Getter(AccessLevel.NONE) private Bitmap albumArt;
    private String genre;
    private int length;
    private String uploaderId;

    public byte[] _getAlbumArt() {
        Bitmap bitmap = this.albumArt;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        bitmap.recycle();
        return stream.toByteArray();
    }
}
