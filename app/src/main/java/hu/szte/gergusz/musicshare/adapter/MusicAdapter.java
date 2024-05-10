package hu.szte.gergusz.musicshare.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.color.utilities.CorePalette;

import java.lang.reflect.Array;
import java.util.ArrayList;

import hu.szte.gergusz.musicshare.R;
import hu.szte.gergusz.musicshare.activity.MusicShareActivity;
import hu.szte.gergusz.musicshare.model.Music;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> implements Filterable {

    private ArrayList<Music> musicList;
    private ArrayList<Music> filteredMusicList;
    private Context context;
    private int lastPosition = -1;

    public MusicAdapter(Context context, ArrayList<Music> musicList) {
        this.musicList = musicList;
        this.filteredMusicList = musicList;
        this.context = context;
    }

    @NonNull
    @Override
    public MusicAdapter.MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MusicViewHolder(LayoutInflater.from(context).inflate(R.layout.music_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Music currentItem = filteredMusicList.get(position);

        holder.bindTo(currentItem);

        if (holder.getAdapterPosition() > lastPosition) {
            holder.itemView.setAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in));
            lastPosition = holder.getAdapterPosition();
        }
    }

    @Override
    public int getItemCount() {
        return filteredMusicList.size();
    }

    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String searchString = constraint.toString();
                if (searchString.isEmpty()) {
                    filteredMusicList = musicList;
                } else {
                    ArrayList<Music> temp = new ArrayList<>();
                    for (Music music : musicList) {
                        if (music.getTitle().toLowerCase().contains(searchString.toLowerCase()) || music.getArtist().toLowerCase().contains(searchString.toLowerCase())) {
                            temp.add(music);
                        }
                    }
                    filteredMusicList = temp;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = filteredMusicList;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredMusicList = (ArrayList<Music>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {

        private final ImageView albumArt;
        private final TextView songTitle;
        private final TextView songArtist;
        private final TextView songDuration;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);

            albumArt = itemView.findViewById(R.id.albumArtImage);
            songTitle = itemView.findViewById(R.id.songTitle);
            songArtist = itemView.findViewById(R.id.songArtist);
            songDuration = itemView.findViewById(R.id.songDuration);

        }

        @SuppressLint("DefaultLocale")
        void bindTo(@NonNull Music music) {
            songTitle.setText(music.getTitle());
            songArtist.setText(music.getArtist());
            songDuration.setText(String.format("%02d:%02d", music.getLength() / 60000, (music.getLength() % 60000) / 1000));
            if (music.getAlbumArtUri() != null) {
                Glide.with(itemView.getContext()).load(music.getAlbumArtUri()).into(albumArt);
            } else {
                albumArt.setImageResource(R.drawable.baseline_album_64);
            }
            itemView.setOnClickListener(view -> {
                ((MusicShareActivity) view.getContext()).showMusicInfo(music);
            });
        }
    }
}
