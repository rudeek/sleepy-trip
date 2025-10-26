package com.example.sleepytrip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locations = new ArrayList<>();
    private OnLocationClickListener listener;

    // Интерфейс для кликов
    public interface OnLocationClickListener {
        void onSwitchChanged(Location location, boolean isChecked);
    }

    public LocationAdapter(OnLocationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locations.get(position);

        holder.tvName.setText(location.getName());
        holder.tvAddress.setText(location.getAddress());

        // Форматируем радиус
        String radiusText;
        if (location.getRadius() >= 1000) {
            radiusText = String.format("📍 Радиус: %.1f км", location.getRadius() / 1000);
        } else {
            radiusText = String.format("📍 Радиус: %.0f м", location.getRadius());
        }
        holder.tvRadius.setText(radiusText);

        // Устанавливаем состояние switch без вызова listener
        holder.switchLocation.setOnCheckedChangeListener(null);
        holder.switchLocation.setChecked(location.isActive());

        // Обработчик изменения switch
        holder.switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onSwitchChanged(location, isChecked);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvRadius;
        SwitchMaterial switchLocation;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_location_name);
            tvAddress = itemView.findViewById(R.id.tv_location_address);
            tvRadius = itemView.findViewById(R.id.tv_location_radius);
            switchLocation = itemView.findViewById(R.id.switch_location);
        }
    }
}