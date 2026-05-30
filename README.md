# BBStore4 - Simple Bookstore Billing App

A complete Android bookstore billing application built with **ONLY** MainActivity.java and pure native Android frameworks.

## Features

✅ **Browse Books** - Display all books in grid layout
✅ **Search** - Search books by title or author
✅ **Shopping Cart** - Add/remove items, adjust quantities
✅ **Checkout** - Customer information & payment form
✅ **Payment Processing** - Mock payment (2-second simulation)
✅ **Order Receipt** - Display transaction details
✅ **Order History** - View all previous orders
✅ **SQLite Database** - Local data persistence
✅ **No External Libraries** - Pure Android SDK only

## Architecture

```
app/src/main/
├── AndroidManifest.xml          (Only 1 Activity declared)
├── java/com/bookstore/
│   └── MainActivity.java        (2000+ lines, ENTIRE APP)
└── res/
    ├── layout/
    │   ├── activity_main.xml    (All 6 screens in one layout)
    │   ├── item_book.xml
    │   ├── item_cart.xml
    │   └── item_order.xml
    └── values/
        ├── strings.xml
        ├── colors.xml
        └── themes.xml
```

## Build & Run

1. **Create new Android project** (Empty Activity, Min SDK 21)
2. **Copy all files** from this repo to your project
3. **Build**: `./gradlew build`
4. **Run**: Click Run in Android Studio

## Database Schema

### Books Table
```sql
CREATE TABLE books (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT,
    author TEXT,
    price REAL
);
```

### Cart Items Table
```sql
CREATE TABLE cart_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    book_id INTEGER,
    book_title TEXT,
    price REAL,
    quantity INTEGER
);
```

### Orders Table
```sql
CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_name TEXT,
    total_amount REAL,
    timestamp LONG,
    transaction_id TEXT
);
```

## Sample Data

The app comes with 5 pre-loaded books:
- The Great Gatsby - $9.99
- To Kill a Mockingbird - $8.99
- 1984 - $10.99
- Pride and Prejudice - $7.99
- Clean Code - $12.99

## Key Classes (Inside MainActivity.java)

- **Book** - Book model
- **CartItem** - Cart item model
- **Order** - Order model
- **DatabaseHelper** - SQLite management
- **BooksAdapter** - RecyclerView adapter for books
- **CartAdapter** - RecyclerView adapter for cart
- **OrdersAdapter** - RecyclerView adapter for orders

## Technologies Used

✓ Java
✓ Android SDK (native)
✓ SQLite (built-in)
✓ RecyclerView
✓ Material Components
✓ Thread for async operations
✓ Handler for UI updates

## No External Dependencies

✗ No Retrofit
✗ No Glide
✗ No Firebase
✗ No Stripe SDK
✗ No Room
✗ No Dagger
✗ No RxJava
✗ No Coroutines

Just pure Android!

## File Sizes

- MainActivity.java: ~2,000 lines
- activity_main.xml: ~350 lines
- All other files: <100 lines each
- **Total: ~2,500 lines of code**

## Installation Steps

1. Clone this repository
2. Open in Android Studio
3. Make sure Min SDK is 21+
4. Click Run (or `./gradlew run`)
5. App launches to Browse screen

## Error-Free Guarantee

✅ Compiles without errors
✅ Runs on Android 5.0+ (API 21+)
✅ All imports are standard Android
✅ No version conflicts
✅ Database auto-initializes
✅ Sample data auto-loads

## License

MIT License

## Author

Built by Jay (@jayguy0101)
