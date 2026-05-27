package com.example.smartmedicinereminder.ui.medicine;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartmedicinereminder.R;
import com.example.smartmedicinereminder.models.Medicine;
import com.example.smartmedicinereminder.utils.AlarmHelper;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private final List<Medicine> medicineList;
    private final OnMedicineClickListener listener;
    private boolean stockTrackerMode = false;
    private String userRole = "user";

    public interface OnMedicineClickListener {
        void onDeleteClick(Medicine medicine);
        void onItemClick(Medicine medicine);
    }

    public MedicineAdapter(List<Medicine> medicineList, OnMedicineClickListener listener) {
        this.medicineList = medicineList;
        this.listener = listener;
    }

    public void setStockTrackerMode(boolean enabled) {
        this.stockTrackerMode = enabled;
        notifyDataSetChanged();
    }

    public void setUserRole(String role) {
        this.userRole = role;
        notifyDataSetChanged();
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
        if (medicine == null) return;

        holder.tvMedName.setText(medicine.getMedicineName());
        
        String timeDetails = (medicine.getPeriod() != null ? medicine.getPeriod().toUpperCase() : "") + 
                           (medicine.getMedicineTime() != null ? " · " + medicine.getMedicineTime() : "");
        holder.tvMedTimeHeader.setText(timeDetails);
        
        String quantity = medicine.getDosage() != null ? medicine.getDosage() : "1";
        holder.tvMedQuantityLabel.setText("Quantity: " + quantity);

        // Stock tracking display
        if (stockTrackerMode) {
            holder.tvStockCount.setVisibility(View.VISIBLE);
            holder.tvStockCount.setText("In Stock: " + medicine.getStockQuantity());
            if (medicine.getStockQuantity() < 5) {
                holder.tvStockCount.append(" (NEED RESTOCK)");
                holder.tvStockCount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.error_red));
            } else {
                holder.tvStockCount.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_grey));
            }
            holder.llActionArea.setVisibility(View.GONE);
            
            // Allow Caregiver to update stock in tracker mode
            holder.itemView.setOnClickListener(v -> showUpdateStockDialog(holder.itemView.getContext(), medicine));
        } else {
            holder.tvStockCount.setVisibility(View.GONE);
            holder.llActionArea.setVisibility(View.VISIBLE);
            
            // Disable checkbox for Caregiver in Dashboard to prevent accidental clicks
            holder.cbDone.setEnabled(userRole.equals("user"));
            holder.itemView.setOnClickListener(null);
        }

        holder.cbDone.setChecked(medicine.isTakenStatus());
        holder.itemView.setAlpha(medicine.isTakenStatus() ? 0.6f : 1.0f);

        holder.cbDone.setOnClickListener(v -> {
            boolean isChecked = holder.cbDone.isChecked();
            
            // Stock logic: only update if status actually changes
            if (isChecked && !medicine.isTakenStatus()) {
                if (medicine.getStockQuantity() > 0) {
                    medicine.setStockQuantity(medicine.getStockQuantity() - 1);
                }
            } else if (!isChecked && medicine.isTakenStatus()) {
                medicine.setStockQuantity(medicine.getStockQuantity() + 1);
            }
            
            medicine.setTakenStatus(isChecked);

            if (medicine.getId() != null) {
                FirebaseDatabase.getInstance().getReference("medicines")
                        .child(medicine.getId())
                        .setValue(medicine);
                
                if (isChecked && medicine.getPeriod() != null) {
                    AlarmHelper.checkIfSlotCleared(holder.itemView.getContext(), medicine.getPeriod());
                }
            }
            if (listener != null) listener.onItemClick(medicine);
            notifyItemChanged(position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDeleteClick(medicine);
            return true;
        });

        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(medicine);
                }
            });
        }
    }

    private void showUpdateStockDialog(Context context, Medicine medicine) {
        EditText etNewStock = new EditText(context);
        etNewStock.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etNewStock.setHint("Enter remaining stock");
        etNewStock.setText(String.valueOf(medicine.getStockQuantity()));

        new AlertDialog.Builder(context)
                .setTitle("Update Medicine Stock")
                .setMessage("Update available count for " + medicine.getMedicineName())
                .setView(etNewStock)
                .setPositiveButton("Update", (dialog, which) -> {
                    String val = etNewStock.getText().toString();
                    if (!val.isEmpty()) {
                        medicine.setStockQuantity(Integer.parseInt(val));
                        FirebaseDatabase.getInstance().getReference("medicines")
                                .child(medicine.getId())
                                .child("stockQuantity")
                                .setValue(medicine.getStockQuantity());
                        notifyDataSetChanged();
                        Toast.makeText(context, "Stock updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return medicineList != null ? medicineList.size() : 0;
    }

    public static class MedicineViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedTimeHeader, tvMedName, tvMedQuantityLabel, tvStockCount;
        CheckBox cbDone;
        ImageButton btnDelete;
        View llActionArea;

        public MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedTimeHeader = itemView.findViewById(R.id.tvMedTimeHeader);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvMedQuantityLabel = itemView.findViewById(R.id.tvMedQuantityLabel);
            tvStockCount = itemView.findViewById(R.id.tvStockCount);
            cbDone = itemView.findViewById(R.id.cbDone);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            llActionArea = itemView.findViewById(R.id.llActionArea);
        }
    }
}
