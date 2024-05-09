package hu.szte.gergusz.musicshare.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserData {
    private boolean listener;

    public boolean _isMusician() {
        return !listener;
    }
}
