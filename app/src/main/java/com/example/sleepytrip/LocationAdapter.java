package com.example.sleepytrip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    // Список локаций
    private List<Location> locations = new ArrayList<>();

    // Интерфейс для обработки кликов
    private OnLocationClickListener listener;

    // Интерфейс для кликов
    public interface OnLocationClickListener {
        void onDeleteClick(Location location);
    }

    // Конструктор
    public LocationAdapter(OnLocationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Создаём view из layout
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        // Получаем локацию по позиции
        Location location = locations.get(position);

        // Заполняем данные
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

        // Обработчик кнопки удаления
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(location);
            }
        });
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    // Обновить список локаций
    public void setLocations(List<Location> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    // ViewHolder для элемента списка
    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvRadius;
        ImageView btnDelete;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_location_name);
            tvAddress = itemView.findViewById(R.id.tv_location_address);
            tvRadius = itemView.findViewById(R.id.tv_location_radius);
            btnDelete = itemView.findViewById(R.id.btn_delete_location);
        }
    }
}