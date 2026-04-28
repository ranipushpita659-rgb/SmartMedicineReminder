package com.example.smartmedicinereminder.ui.medicine;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.Medicine;

import java.io.File;
import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private List<Medicine> medicineList;
    private OnMedicineClickListener listener;

    public interface OnMedicineClickListener {
        void onDeleteClick(Medicine medicine);
    }

    public MedicineAdapter(List<Medicine> medicineList, OnMedicineClickListener listener) {
        this.medicineList = medicineList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        Medicine medicine = medicineList.get(position);
        holder.tvName.setText(medicine.getName());
        holder.tvQuantity.setText(medicine.getQuantity());
        holder.tvTime.setText(medicine.getTimeSlot());

        if (medicine.getImageUrl() != null && !medicine.getImageUrl().isEmpty()) {
            File imgFile = new File(medicine.getImageUrl());
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                holder.ivImage.setImageBitmap(myBitmap);
            } else {
                holder.ivImage.setImageResource(R.mipmap.ic_launcher);
            }
        } else {
            holder.ivImage.setImageResource(R.mipmap.ic_launcher);
        }

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(medicine));
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    static class MedicineViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvQuantity, tvTime;
        ImageButton btnDelete;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivMedImage);
            tvName = itemView.findViewById(R.id.tvMedName);
            tvQuantity = itemView.findViewById(R.id.tvMedQuantity);
            tvTime = itemView.findViewById(R.id.tvMedTime);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}