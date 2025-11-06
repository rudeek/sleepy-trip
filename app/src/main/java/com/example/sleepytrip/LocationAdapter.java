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

    //список локаций для отображения
    private List<Location> locations = new ArrayList<>();
    //слушатель кликов и переключений
    private OnLocationClickListener listener;

    //режим удаления
    private boolean isDeleteMode = false;

    //множество выбранных позиций для удаления
    private Set<Integer> selectedPositions = new HashSet<>();

    //интерфейс для обработки переключения switch
    public interface OnLocationClickListener {
        void onSwitchChanged(Location location, boolean isChecked);
    }

    //интерфейс для отслеживания изменения выбора
    public interface OnSelectionChangeListener {
        void onSelectionChanged(boolean allSelected, boolean noneSelected);
    }

    //слушатель изменений выбора
    private OnSelectionChangeListener selectionChangeListener;

    //установка слушателя выбора
    public void setOnSelectionChangeListener(OnSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    //конструктор адаптера с обработчиком переключения
    public LocationAdapter(OnLocationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //создаёт элемент списка из макета item_location
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        //получаем текущую локацию
        Location location = locations.get(position);

        holder.tvName.setText(location.getName());
        holder.tvAddress.setText(location.getAddress());

        //форматируем радиус с учётом языка
        String radiusFormatted = SettingsFragment.formatDistance(
                holder.itemView.getContext(),
                location.getRadius()
        );
        holder.tvRadius.setText(
                holder.itemView.getContext().getString(
                        R.string.location_radius_label,
                        radiusFormatted
                )
        );

        //если включён режим удаления
        if (isDeleteMode) {
            //показываем чекбокс и скрываем switch и иконку
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.switchLocation.setVisibility(View.GONE);
            holder.ivIcon.setVisibility(View.GONE);

            //устанавливаем состояние чекбокса
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedPositions.contains(position));

            //обработчик для чекбокса
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedPositions.add(position);
                } else {
                    selectedPositions.remove(position);
                }

                //уведомляем слушателя о смене выбора
                if (selectionChangeListener != null) {
                    boolean allSelected = isAllSelected();
                    boolean noneSelected = selectedPositions.isEmpty();
                    selectionChangeListener.onSelectionChanged(allSelected, noneSelected);
                }
            });

            //клик по элементу переключает чекбокс
            holder.itemView.setOnClickListener(v -> {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            });

        } else {
            //если обычный режим
            //скрываем чекбокс и показываем switch с иконкой
            holder.checkBox.setVisibility(View.GONE);
            holder.switchLocation.setVisibility(View.VISIBLE);
            holder.ivIcon.setVisibility(View.VISIBLE);

            //устанавливаем состояние switch
            holder.switchLocation.setOnCheckedChangeListener(null);
            holder.switchLocation.setChecked(location.isActive());

            //обработчик переключения состояния локации
            holder.switchLocation.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (listener != null) {
                    listener.onSwitchChanged(location, isChecked);
                }
            });

            //убираем клик по элементу
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        //возвращает количество элементов списка
        return locations.size();
    }

    //устанавливает новые данные и обновляет список
    public void setLocations(List<Location> locations) {
        this.locations = locations;
        notifyDataSetChanged();
    }

    //включает или выключает режим удаления
    public void setDeleteMode(boolean deleteMode) {
        this.isDeleteMode = deleteMode;
        if (!deleteMode) {
            selectedPositions.clear();
        }
        notifyDataSetChanged();
    }

    //выделяет все элементы списка
    public void selectAll() {
        selectedPositions.clear();
        for (int i = 0; i < locations.size(); i++) {
            selectedPositions.add(i);
        }
        notifyDataSetChanged();
    }

    //снимает выделение со всех элементов
    public void deselectAll() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    //возвращает список выбранных локаций
    public List<Location> getSelectedLocations() {
        List<Location> selected = new ArrayList<>();
        for (int position : selectedPositions) {
            if (position < locations.size()) {
                selected.add(locations.get(position));
            }
        }
        return selected;
    }

    //проверяет, выбраны ли все локации
    public boolean isAllSelected() {
        return selectedPositions.size() == locations.size() && locations.size() > 0;
    }

    //внутренний класс для хранения ссылок на элементы интерфейса
    static class LocationViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvAddress;
        TextView tvRadius;
        SwitchMaterial switchLocation;
        CheckBox checkBox;
        ImageView ivIcon;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            //инициализация элементов макета
            tvName = itemView.findViewById(R.id.tv_location_name);
            tvAddress = itemView.findViewById(R.id.tv_location_address);
            tvRadius = itemView.findViewById(R.id.tv_location_radius);
            switchLocation = itemView.findViewById(R.id.switch_location);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            ivIcon = itemView.findViewById(R.id.iv_location_icon);
        }
    }
}
