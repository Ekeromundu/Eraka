package na.eraka.SmartRow;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import na.eraka.R;
import com.stonefacesoft.pictogramslibrary.utils.GlideAttatcher;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SmartRowAdapter extends RecyclerView.Adapter<SmartRowAdapter.ViewHolder> {

    private final List<SmartRowItem> items = new ArrayList<>();
    private SmartRowView.OnItemClickListener clickListener;

    public void setItems(List<SmartRowItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    /** Eraka: a copy of the items currently shown (used by the usage log). */
    public List<SmartRowItem> getItems() {
        return new ArrayList<>(items);
    }

    public void setOnItemClickListener(SmartRowView.OnItemClickListener listener) {
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.smart_row_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SmartRowItem item = items.get(position);
        holder.label.setText(item.label);

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });

        Context context = holder.itemView.getContext();
        GlideAttatcher attatcher = new GlideAttatcher(context);
        String imageUrl = item.imageUrl;

        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        if (imageUrl.startsWith("https://")) {
            attatcher.UseCornerRadius(true).loadDrawable(Uri.parse(imageUrl), holder.image);
            return;
        }

        File picto = new File(imageUrl);
        if (picto.exists()) {
            attatcher.UseCornerRadius(true).loadDrawable(picto, holder.image);
            return;
        }

        // Eraka 22 Oct 2026: bundled picture in the skin tone chosen in the child profile
        android.graphics.drawable.Drawable d = na.eraka.utils.ErakaSkinTone.resource(context, imageUrl);
        if (d != null) {
            attatcher.UseCornerRadius(true).loadDrawable(d, holder.image);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView label;
        ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            label = itemView.findViewById(R.id.smartRowItemLabel);
            image = itemView.findViewById(R.id.smartRowItemImage);
        }
    }
}