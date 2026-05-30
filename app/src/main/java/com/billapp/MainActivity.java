package com.billapp;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private LinearLayout itemsContainer;
    private Button btnAddItem, btnGenerateBill;
    private TextView totalPrice;
    private List<ItemRow> itemRows = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        itemsContainer = findViewById(R.id.itemsContainer);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnGenerateBill = findViewById(R.id.btnGenerateBill);
        totalPrice = findViewById(R.id.totalPrice);

        btnAddItem.setOnClickListener(v -> addItemRow());
        btnGenerateBill.setOnClickListener(v -> generateBill());

        // Add first item row by default
        addItemRow();
    }

    private void addItemRow() {
        View itemView = getLayoutInflater().inflate(R.layout.item_layout, itemsContainer, false);
        EditText itemName = itemView.findViewById(R.id.itemName);
        EditText itemPrice = itemView.findViewById(R.id.itemPrice);
        EditText itemQty = itemView.findViewById(R.id.itemQty);
        Button btnRemove = itemView.findViewById(R.id.btnRemove);

        ItemRow row = new ItemRow(itemView, itemName, itemPrice, itemQty);
        itemRows.add(row);

        btnRemove.setOnClickListener(v -> removeItemRow(row));

        itemName.addTextChangedListener(new SimpleTextWatcher(() -> updateTotal()));
        itemPrice.addTextChangedListener(new SimpleTextWatcher(() -> updateTotal()));
        itemQty.addTextChangedListener(new SimpleTextWatcher(() -> updateTotal()));

        itemsContainer.addView(itemView);
    }

    private void removeItemRow(ItemRow row) {
        itemsContainer.removeView(row.view);
        itemRows.remove(row);
        updateTotal();
    }

    private void updateTotal() {
        double total = 0;
        for (ItemRow row : itemRows) {
            String priceStr = row.price.getText().toString();
            String qtyStr = row.qty.getText().toString();

            if (!priceStr.isEmpty() && !qtyStr.isEmpty()) {
                try {
                    double price = Double.parseDouble(priceStr);
                    int qty = Integer.parseInt(qtyStr);
                    total += price * qty;
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        totalPrice.setText(String.format("$%.2f", total));
    }

    private void generateBill() {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light);
        dialog.setContentView(R.layout.bill_dialog);
        dialog.getWindow().setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        );

        LinearLayout billItemsContainer = dialog.findViewById(R.id.billItemsContainer);
        TextView subtotal = dialog.findViewById(R.id.subtotal);
        TextView tax = dialog.findViewById(R.id.tax);
        TextView billTotal = dialog.findViewById(R.id.billTotal);
        Button btnClose = dialog.findViewById(R.id.btnClose);

        double totalAmount = 0;
        StringBuilder billItems = new StringBuilder();

        for (ItemRow row : itemRows) {
            String name = row.name.getText().toString().trim();
            String priceStr = row.price.getText().toString();
            String qtyStr = row.qty.getText().toString();

            if (!name.isEmpty() && !priceStr.isEmpty() && !qtyStr.isEmpty()) {
                try {
                    double price = Double.parseDouble(priceStr);
                    int qty = Integer.parseInt(qtyStr);
                    double itemTotal = price * qty;
                    totalAmount += itemTotal;

                    // Create bill item view
                    LinearLayout itemBillView = new LinearLayout(this);
                    itemBillView.setOrientation(LinearLayout.HORIZONTAL);
                    itemBillView.setPadding(12, 8, 12, 8);

                    TextView nameView = new TextView(this);
                    nameView.setText(name + " x" + qty);
                    nameView.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    nameView.setTextSize(14);

                    TextView priceView = new TextView(this);
                    priceView.setText(String.format("$%.2f", itemTotal));
                    priceView.setTextSize(14);

                    itemBillView.addView(nameView);
                    itemBillView.addView(priceView);
                    billItemsContainer.addView(itemBillView);
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        double taxAmount = totalAmount * 0.10;
        double finalTotal = totalAmount + taxAmount;

        subtotal.setText(String.format("$%.2f", totalAmount));
        tax.setText(String.format("$%.2f", taxAmount));
        billTotal.setText(String.format("$%.2f", finalTotal));

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    static class ItemRow {
        View view;
        EditText name, price, qty;

        ItemRow(View view, EditText name, EditText price, EditText qty) {
            this.view = view;
            this.name = name;
            this.price = price;
            this.qty = qty;
        }
    }

    static class SimpleTextWatcher implements android.text.TextWatcher {
        private Runnable onChanged;

        SimpleTextWatcher(Runnable onChanged) {
            this.onChanged = onChanged;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            onChanged.run();
        }

        @Override
        public void afterTextChanged(android.text.Editable s) {}
    }
}
