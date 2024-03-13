package hu.szte.gergusz.musicshare.model;

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

    int id;
    String path;
    String uploader;
    int length;
    String albumArtPath;
    String album;
    String genre;

}
