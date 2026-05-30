package com.bookstore;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Constants
    private static final int BROWSE = 0, CART = 1, CHECKOUT = 2, PAYMENT = 3, RECEIPT = 4, HISTORY = 5;

    // UI Components
    private FrameLayout screenContainer;
    private LinearLayout browseScreen, cartScreen, checkoutScreen, paymentScreen, receiptScreen, historyScreen;
    private RecyclerView recyclerViewBooks, recyclerViewCart, recyclerViewOrders;
    private TextView tvTotal, tvReceiptContent, tvCartEmpty, tvHistoryEmpty, tvPaymentStatus;
    private EditText etName, etEmail, etCard, etExpiry, etCVV;
    private Button btnCheckout, btnPayNow, btnBackToBrowse, btnViewHistory, btnBrowse, btnCart, btnHistory;
    private ProgressBar progressBar;
    private SearchView searchView;

    // Data
    private SQLiteDatabase db;
    private DatabaseHelper dbHelper;
    private List<Book> books = new ArrayList<>();
    private List<CartItem> cart = new ArrayList<>();
    private List<Order> orders = new ArrayList<>();
    private int currentScreen = BROWSE;
    private double totalPrice = 0;
    private int lastOrderId = -1;

    // Adapters
    private BooksAdapter booksAdapter;
    private CartAdapter cartAdapter;
    private OrdersAdapter ordersAdapter;

    // Handler for UI updates
    private Handler uiHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeDatabase();
        setupListeners();
        loadBooks();
        showScreen(BROWSE);
    }

    private void initializeUI() {
        // Screens
        screenContainer = findViewById(R.id.screenContainer);
        browseScreen = findViewById(R.id.browseScreen);
        cartScreen = findViewById(R.id.cartScreen);
        checkoutScreen = findViewById(R.id.checkoutScreen);
        paymentScreen = findViewById(R.id.paymentScreen);
        receiptScreen = findViewById(R.id.receiptScreen);
        historyScreen = findViewById(R.id.historyScreen);

        // RecyclerViews
        recyclerViewBooks = findViewById(R.id.recyclerViewBooks);
        recyclerViewCart = findViewById(R.id.recyclerViewCart);
        recyclerViewOrders = findViewById(R.id.recyclerViewOrders);

        // TextViews
        tvTotal = findViewById(R.id.tvTotal);
        tvReceiptContent = findViewById(R.id.tvReceiptContent);
        tvCartEmpty = findViewById(R.id.tvCartEmpty);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);

        // EditTexts
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etCard = findViewById(R.id.etCard);
        etExpiry = findViewById(R.id.etExpiry);
        etCVV = findViewById(R.id.etCVV);

        // Buttons
        btnCheckout = findViewById(R.id.btnCheckout);
        btnPayNow = findViewById(R.id.btnPayNow);
        btnBackToBrowse = findViewById(R.id.btnBackToBrowse);
        btnViewHistory = findViewById(R.id.btnViewHistory);
        btnBrowse = findViewById(R.id.btnBrowse);
        btnCart = findViewById(R.id.btnCart);
        btnHistory = findViewById(R.id.btnHistory);

        // Other
        progressBar = findViewById(R.id.progressBar);
        searchView = findViewById(R.id.searchView);

        // Setup RecyclerViews
        recyclerViewBooks.setLayoutManager(new GridLayoutManager(this, 2));
        booksAdapter = new BooksAdapter(books);
        recyclerViewBooks.setAdapter(booksAdapter);

        recyclerViewCart.setLayoutManager(new LinearLayoutManager(this));
        cartAdapter = new CartAdapter(cart);
        recyclerViewCart.setAdapter(cartAdapter);

        recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        ordersAdapter = new OrdersAdapter(orders);
        recyclerViewOrders.setAdapter(ordersAdapter);
    }

    private void setupListeners() {
        btnBrowse.setOnClickListener(v -> showScreen(BROWSE));
        btnCart.setOnClickListener(v -> {
            loadCart();
            showScreen(CART);
        });
        btnHistory.setOnClickListener(v -> {
            loadOrders();
            showScreen(HISTORY);
        });

        btnCheckout.setOnClickListener(v -> showScreen(CHECKOUT));
        btnPayNow.setOnClickListener(v -> validateAndProcessPayment());
        btnBackToBrowse.setOnClickListener(v -> showScreen(BROWSE));
        btnViewHistory.setOnClickListener(v -> {
            loadOrders();
            showScreen(HISTORY);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                searchBooks(newText);
                return false;
            }
        });
    }

    // ==================== DATABASE METHODS ====================

    private void initializeDatabase() {
        dbHelper = new DatabaseHelper(this);
        db = dbHelper.getWritableDatabase();
        dbHelper.insertSampleBooks(db);
    }

    private void loadBooks() {
        new Thread(() -> {
            books.clear();
            Cursor cursor = db.query("books", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                String author = cursor.getString(cursor.getColumnIndexOrThrow("author"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                books.add(new Book(id, title, author, price));
            }
            cursor.close();
            uiHandler.post(() -> booksAdapter.notifyDataSetChanged());
        }).start();
    }

    private void loadCart() {
        new Thread(() -> {
            cart.clear();
            Cursor cursor = db.query("cart_items", null, null, null, null, null, null);
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                int bookId = cursor.getInt(cursor.getColumnIndexOrThrow("book_id"));
                String bookTitle = cursor.getString(cursor.getColumnIndexOrThrow("book_title"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                cart.add(new CartItem(id, bookId, bookTitle, price, quantity));
            }
            cursor.close();
            uiHandler.post(() -> {
                cartAdapter.notifyDataSetChanged();
                updateCartUI();
            });
        }).start();
    }

    private void loadOrders() {
        new Thread(() -> {
            orders.clear();
            Cursor cursor = db.query("orders", null, null, null, null, null, "timestamp DESC");
            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String customerName = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
                double totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow("total_amount"));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"));
                String transactionId = cursor.getString(cursor.getColumnIndexOrThrow("transaction_id"));
                orders.add(new Order(id, customerName, totalAmount, timestamp, transactionId));
            }
            cursor.close();
            uiHandler.post(() -> {
                ordersAdapter.notifyDataSetChanged();
                if (orders.isEmpty()) {
                    tvHistoryEmpty.setVisibility(View.VISIBLE);
                    recyclerViewOrders.setVisibility(View.GONE);
                } else {
                    tvHistoryEmpty.setVisibility(View.GONE);
                    recyclerViewOrders.setVisibility(View.VISIBLE);
                }
            });
        }).start();
    }

    private void searchBooks(String query) {
        new Thread(() -> {
            List<Book> filteredBooks = new ArrayList<>();
            if (query.isEmpty()) {
                filteredBooks.addAll(books);
            } else {
                String lowerQuery = query.toLowerCase();
                for (Book book : books) {
                    if (book.title.toLowerCase().contains(lowerQuery) || 
                        book.author.toLowerCase().contains(lowerQuery)) {
                        filteredBooks.add(book);
                    }
                }
            }
            uiHandler.post(() -> booksAdapter.updateBooks(filteredBooks));
        }).start();
    }

    private void addToCart(Book book, int quantity) {
        new Thread(() -> {
            // Check if book already in cart
            Cursor cursor = db.query("cart_items", null, "book_id=?", new String[]{String.valueOf(book.id)}, null, null, null);
            if (cursor.moveToFirst()) {
                int currentQty = cursor.getInt(cursor.getColumnIndexOrThrow("quantity"));
                ContentValues values = new ContentValues();
                values.put("quantity", currentQty + quantity);
                db.update("cart_items", values, "book_id=?", new String[]{String.valueOf(book.id)});
            } else {
                ContentValues values = new ContentValues();
                values.put("book_id", book.id);
                values.put("book_title", book.title);
                values.put("price", book.price);
                values.put("quantity", quantity);
                db.insert("cart_items", null, values);
            }
            cursor.close();
            uiHandler.post(() -> Toast.makeText(MainActivity.this, "Added to cart", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void removeFromCart(int cartItemId) {
        new Thread(() -> {
            db.delete("cart_items", "id=?", new String[]{String.valueOf(cartItemId)});
            uiHandler.post(() -> {
                loadCart();
                Toast.makeText(MainActivity.this, "Removed from cart", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void updateCartItemQty(int cartItemId, int newQty) {
        new Thread(() -> {
            if (newQty <= 0) {
                removeFromCart(cartItemId);
            } else {
                ContentValues values = new ContentValues();
                values.put("quantity", newQty);
                db.update("cart_items", values, "id=?", new String[]{String.valueOf(cartItemId)});
                uiHandler.post(this::loadCart);
            }
        }).start();
    }

    private void clearCart() {
        new Thread(() -> {
            db.delete("cart_items", null, null);
            uiHandler.post(this::loadCart);
        }).start();
    }

    private void updateCartUI() {
        totalPrice = 0;
        for (CartItem item : cart) {
            totalPrice += item.price * item.quantity;
        }
        tvTotal.setText(String.format("Total: $%.2f", totalPrice));

        if (cart.isEmpty()) {
            tvCartEmpty.setVisibility(View.VISIBLE);
            recyclerViewCart.setVisibility(View.GONE);
        } else {
            tvCartEmpty.setVisibility(View.GONE);
            recyclerViewCart.setVisibility(View.VISIBLE);
        }
    }

    private void validateAndProcessPayment() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String card = etCard.getText().toString().trim();
        String expiry = etExpiry.getText().toString().trim();
        String cvv = etCVV.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || card.isEmpty() || expiry.isEmpty() || cvv.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (card.length() < 13) {
            Toast.makeText(this, "Invalid card number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cart.isEmpty()) {
            Toast.makeText(this, "Cart is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        processPayment(name, email);
    }

    private void processPayment(String name, String email) {
        showScreen(PAYMENT);
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Simulate API call
                String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                
                ContentValues values = new ContentValues();
                values.put("customer_name", name);
                values.put("total_amount", totalPrice);
                values.put("transaction_id", transactionId);
                values.put("timestamp", System.currentTimeMillis());
                lastOrderId = (int) db.insert("orders", null, values);

                clearCart();
                uiHandler.post(() -> {
                    displayReceipt(name, email, transactionId);
                    showScreen(RECEIPT);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                uiHandler.post(() -> {
                    Toast.makeText(MainActivity.this, "Payment failed", Toast.LENGTH_SHORT).show();
                    showScreen(CHECKOUT);
                });
            }
        }).start();
    }

    private void displayReceipt(String name, String email, String transactionId) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String date = sdf.format(new Date());

        StringBuilder receipt = new StringBuilder();
        receipt.append("========== RECEIPT ==========\n\n");
        receipt.append("Order ID: ").append(lastOrderId).append("\n");
        receipt.append("Date: ").append(date).append("\n\n");
        receipt.append("Customer: ").append(name).append("\n");
        receipt.append("Email: ").append(email).append("\n\n");
        receipt.append("------- Items -------\n");
        
        for (CartItem item : cart) {
            receipt.append(item.bookTitle).append(" x").append(item.quantity)
                    .append(" = $").append(String.format("%.2f", item.price * item.quantity)).append("\n");
        }
        
        receipt.append("\nTotal: $").append(String.format("%.2f", totalPrice)).append("\n");
        receipt.append("Transaction ID: ").append(transactionId).append("\n");
        receipt.append("Status: SUCCESS\n");
        receipt.append("============================\n");
        receipt.append("Thank you for shopping!");

        tvReceiptContent.setText(receipt.toString());
    }

    private void showScreen(int screen) {
        browseScreen.setVisibility(View.GONE);
        cartScreen.setVisibility(View.GONE);
        checkoutScreen.setVisibility(View.GONE);
        paymentScreen.setVisibility(View.GONE);
        receiptScreen.setVisibility(View.GONE);
        historyScreen.setVisibility(View.GONE);

        btnBrowse.setTextColor(getColor(R.color.gray));
        btnCart.setTextColor(getColor(R.color.gray));
        btnHistory.setTextColor(getColor(R.color.gray));

        currentScreen = screen;
        switch (screen) {
            case BROWSE:
                browseScreen.setVisibility(View.VISIBLE);
                btnBrowse.setTextColor(getColor(R.color.purple_500));
                break;
            case CART:
                cartScreen.setVisibility(View.VISIBLE);
                btnCart.setTextColor(getColor(R.color.purple_500));
                break;
            case CHECKOUT:
                checkoutScreen.setVisibility(View.VISIBLE);
                break;
            case PAYMENT:
                paymentScreen.setVisibility(View.VISIBLE);
                break;
            case RECEIPT:
                receiptScreen.setVisibility(View.VISIBLE);
                break;
            case HISTORY:
                historyScreen.setVisibility(View.VISIBLE);
                btnHistory.setTextColor(getColor(R.color.purple_500));
                break;
        }
    }

    // ==================== INNER CLASSES ====================

    static class Book {
        int id;
        String title;
        String author;
        double price;

        Book(int id, String title, String author, double price) {
            this.id = id;
            this.title = title;
            this.author = author;
            this.price = price;
        }
    }

    static class CartItem {
        int id;
        int bookId;
        String bookTitle;
        double price;
        int quantity;

        CartItem(int id, int bookId, String bookTitle, double price, int quantity) {
            this.id = id;
            this.bookId = bookId;
            this.bookTitle = bookTitle;
            this.price = price;
            this.quantity = quantity;
        }
    }

    static class Order {
        int id;
        String customerName;
        double totalAmount;
        long timestamp;
        String transactionId;

        Order(int id, String customerName, double totalAmount, long timestamp, String transactionId) {
            this.id = id;
            this.customerName = customerName;
            this.totalAmount = totalAmount;
            this.timestamp = timestamp;
            this.transactionId = transactionId;
        }
    }

    static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DB_NAME = "bookstore.db";
        private static final int DB_VERSION = 1;

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Books table
            db.execSQL("CREATE TABLE books (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "title TEXT, " +
                    "author TEXT, " +
                    "price REAL)");

            // Cart items table
            db.execSQL("CREATE TABLE cart_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "book_id INTEGER, " +
                    "book_title TEXT, " +
                    "price REAL, " +
                    "quantity INTEGER)");

            // Orders table
            db.execSQL("CREATE TABLE orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "customer_name TEXT, " +
                    "total_amount REAL, " +
                    "timestamp LONG, " +
                    "transaction_id TEXT)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }

        void insertSampleBooks(SQLiteDatabase db) {
            Cursor cursor = db.query("books", null, null, null, null, null, null);
            if (cursor.getCount() == 0) {
                String[][] books = {
                    {"The Great Gatsby", "F. Scott Fitzgerald", "9.99"},
                    {"To Kill a Mockingbird", "Harper Lee", "8.99"},
                    {"1984", "George Orwell", "10.99"},
                    {"Pride and Prejudice", "Jane Austen", "7.99"},
                    {"Clean Code", "Robert C. Martin", "12.99"}
                };

                for (String[] book : books) {
                    ContentValues values = new ContentValues();
                    values.put("title", book[0]);
                    values.put("author", book[1]);
                    values.put("price", Double.parseDouble(book[2]));
                    db.insert("books", null, values);
                }
            }
            cursor.close();
        }
    }

    class BooksAdapter extends RecyclerView.Adapter<BooksAdapter.ViewHolder> {
        List<Book> bookList;

        BooksAdapter(List<Book> bookList) {
            this.bookList = bookList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_book, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Book book = bookList.get(position);
            holder.title.setText(book.title);
            holder.author.setText("by " + book.author);
            holder.price.setText(String.format("$%.2f", book.price));
            holder.addBtn.setOnClickListener(v -> {
                try {
                    int qty = Integer.parseInt(holder.qty.getText().toString());
                    if (qty > 0) {
                        addToCart(book, qty);
                        holder.qty.setText("1");
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(MainActivity.this, "Invalid quantity", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return bookList.size();
        }

        void updateBooks(List<Book> newList) {
            bookList = newList;
            notifyDataSetChanged();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, author, price;
            EditText qty;
            Button addBtn;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.bookTitle);
                author = itemView.findViewById(R.id.bookAuthor);
                price = itemView.findViewById(R.id.bookPrice);
                qty = itemView.findViewById(R.id.bookQty);
                addBtn = itemView.findViewById(R.id.addBtn);
            }
        }
    }

    class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {
        List<CartItem> cartList;

        CartAdapter(List<CartItem> cartList) {
            this.cartList = cartList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_cart, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CartItem item = cartList.get(position);
            holder.title.setText(item.bookTitle);
            holder.price.setText(String.format("$%.2f", item.price));
            holder.qty.setText(String.valueOf(item.quantity));

            holder.increaseBtn.setOnClickListener(v -> updateCartItemQty(item.id, item.quantity + 1));
            holder.decreaseBtn.setOnClickListener(v -> updateCartItemQty(item.id, item.quantity - 1));
            holder.removeBtn.setOnClickListener(v -> removeFromCart(item.id));
        }

        @Override
        public int getItemCount() {
            return cartList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title, price, qty;
            Button increaseBtn, decreaseBtn, removeBtn;

            ViewHolder(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.cartItemTitle);
                price = itemView.findViewById(R.id.cartItemPrice);
                qty = itemView.findViewById(R.id.cartItemQty);
                increaseBtn = itemView.findViewById(R.id.increaseBtn);
                decreaseBtn = itemView.findViewById(R.id.decreaseBtn);
                removeBtn = itemView.findViewById(R.id.removeBtn);
            }
        }
    }

    class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.ViewHolder> {
        List<Order> orderList;

        OrdersAdapter(List<Order> orderList) {
            this.orderList = orderList;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_order, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Order order = orderList.get(position);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.orderId.setText("Order #" + order.id);
            holder.date.setText(sdf.format(new Date(order.timestamp)));
            holder.amount.setText(String.format("$%.2f", order.totalAmount));
            holder.txnId.setText("TXN: " + order.transactionId);
        }

        @Override
        public int getItemCount() {
            return orderList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView orderId, date, amount, txnId;

            ViewHolder(View itemView) {
                super(itemView);
                orderId = itemView.findViewById(R.id.orderId);
                date = itemView.findViewById(R.id.orderDate);
                amount = itemView.findViewById(R.id.orderAmount);
                txnId = itemView.findViewById(R.id.orderTxnId);
            }
        }
    }
}
