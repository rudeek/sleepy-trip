package com.example.sleepytrip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locations = new ArrayList<>();
    private OnLocationClickListener listener;

    // Режим удаления
    private boolean isDeleteMode = false;

    // Множество выбранных локаций
    private Set<Integer> selectedPositions = new HashSet<>();

    public interface OnLocationClickListener {
        void onSwitchChanged(Location location, boolean isChecked);
    }

    public interface OnSelectionChangeListener {
        void onSelectionChanged(boolean allSelected, boolean noneSelected);
    }

    private OnSelectionChangeListener selectionChangeListener;

    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
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

        // === РЕЖИМ УДАЛЕНИЯ ===
        if (isDeleteMode) {
            // Показываем checkbox, скрываем switch и иконку
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.switchLocation.setVisibility(View.GONE);
            holder.ivIcon.setVisibility(View.GONE);

            // Устанавливаем состояние checkbox
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedPositions.contains(position));

            // Обработчик checkbox
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPositions.add(position);
                } else {
                    selectedPositions.remove(position);
                }

                // Уведомляем фрагмент об изменении выбора
                if (selectionChangeListener != null) {
                    boolean allSelected = isAllSelected();
                    boolean noneSelected = selectedPositions.isEmpty();
                    selectionChangeListener.onSelectionChanged(allSelected, noneSelected);
                }
            });


            // Клик по карточке тоже переключает checkbox
            holder.itemView.setOnClickListener(v -> {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            });

        } else {
            // === ОБЫЧНЫЙ РЕЖИМ ===
            // Скрываем checkbox, показываем switch и иконку
            holder.checkBox.setVisibility(View.GONE);
            holder.switchLocation.setVisibility(View.VISIBLE);
            holder.ivIcon.setVisibility(View.VISIBLE);

            // Устанавливаем состояние switch
            holder.switchLocation.setOnCheckedChangeListener(null);
            holder.switchLocation.setChecked(location.isActive());

            // Обработчик switch
            holder.switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onSwitchChanged(location, isChecked);
                }
            });

            // Убираем обработчик клика
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return locations.size();
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    // Включить/выключить режим удаления
    public void setDeleteMode(boolean deleteMode) {
        this.isDeleteMode = deleteMode;
        if (!deleteMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    // Выбрать все локации
    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < locations.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
    }

    // Снять выбор со всех
    public void deselectAll() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    // Получить выбранные локации
    public List<Location> getSelectedLocations() {
        List<Location> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < locations.size()) {
                selected.add(locations.get(position));
            }
        }
        return selected;
    }

    // Проверить выбраны ли все локации
    public boolean isAllSelected() {
        return selectedPositions.size() == locations.size() && locations.size() > 0;
    }

    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvRadius;
        SwitchMaterial switchLocation;
        CheckBox checkBox;
        ImageView ivIcon;


        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_location_name);
            tvAddress = itemView.findViewById(R.id.tv_location_address);
            tvRadius = itemView.findViewById(R.id.tv_location_radius);
            switchLocation = itemView.findViewById(R.id.switch_location);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            ivIcon = itemView.findViewById(R.id.iv_location_icon);
        }
    }
}