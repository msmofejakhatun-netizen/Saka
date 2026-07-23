package com.example.data.repository

import android.util.Log
import com.example.data.db.CategoryDao
import com.example.data.db.CategoryEntity
import com.example.data.db.InvoiceDao
import com.example.data.db.InvoiceEntity
import com.example.data.db.ProductDao
import com.example.data.db.ProductEntity
import com.example.data.db.UserDao
import com.example.data.db.UserEntity
import com.example.data.firebase.FirebaseManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BillingRepository(
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val invoiceDao: InvoiceDao,
    private val productDao: ProductDao,
    private val customerDao: com.example.data.db.CustomerDao,
    private val customerTransactionDao: com.example.data.db.CustomerTransactionDao
) {
    private val TAG = "BillingRepository"

    // --- Users (Auth & Profile) ---
    
    suspend fun getUserByMobile(mobile: String): UserEntity? = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val snapshot = FirebaseManager.firestore
                    ?.collection("users")
                    ?.whereEqualTo("mobileNumber", mobile)
                    ?.limit(1)
                    ?.get()
                    ?.await()
                
                val doc = snapshot?.documents?.firstOrNull()
                if (doc != null) {
                    return@withContext UserEntity(
                        id = doc.hashCode(),
                        fullName = doc.getString("fullName") ?: "",
                        businessName = doc.getString("businessName") ?: "",
                        mobileNumber = doc.getString("mobileNumber") ?: "",
                        passwordHash = "", // Auth credentials handled securely via Firebase Auth
                        category = doc.getString("category") ?: "Retail"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore getUserByMobile error: ${e.localizedMessage}")
            }
        }
        // Fallback to Room local database
        userDao.getUserByMobile(mobile)
    }

    suspend fun registerUserInFirebase(user: UserEntity, passwordRaw: String): String = withContext(Dispatchers.IO) {
        if (!FirebaseManager.isFirebaseAvailable) {
            throw IllegalStateException("Firebase service is unavailable.")
        }
        
        val auth = FirebaseManager.auth ?: throw IllegalStateException("Firebase Auth is null.")
        val firestore = FirebaseManager.firestore ?: throw IllegalStateException("Firestore is null.")
        
        // 1. Authenticate with normalized email matching user's phone or email
        val normalizedEmail = FirebaseManager.normalizeToEmail(user.mobileNumber)
        val authResult = auth.createUserWithEmailAndPassword(normalizedEmail, passwordRaw).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("Firebase UID generation failed.")

        // 2. Save professional business profile metadata to Firestore
        val userMap = hashMapOf(
            "uid" to uid,
            "fullName" to user.fullName,
            "businessName" to user.businessName,
            "mobileNumber" to user.mobileNumber,
            "category" to user.category,
            "createdAt" to System.currentTimeMillis()
        )
        
        firestore.collection("users").document(uid).set(userMap).await()
        uid
    }

    suspend fun loginUserInFirebase(mobileOrEmail: String, passwordRaw: String): UserEntity = withContext(Dispatchers.IO) {
        if (!FirebaseManager.isFirebaseAvailable) {
            throw IllegalStateException("Firebase service is unavailable.")
        }

        val auth = FirebaseManager.auth ?: throw IllegalStateException("Firebase Auth is null.")
        val firestore = FirebaseManager.firestore ?: throw IllegalStateException("Firestore is null.")
        
        val normalizedEmail = FirebaseManager.normalizeToEmail(mobileOrEmail)
        val authResult = auth.signInWithEmailAndPassword(normalizedEmail, passwordRaw).await()
        val uid = authResult.user?.uid ?: throw IllegalStateException("Verification failed.")

        val doc = firestore.collection("users").document(uid).get().await()
        if (doc.exists()) {
            UserEntity(
                id = doc.hashCode(),
                fullName = doc.getString("fullName") ?: "",
                businessName = doc.getString("businessName") ?: "",
                mobileNumber = doc.getString("mobileNumber") ?: "",
                passwordHash = "",
                category = doc.getString("category") ?: "Retail"
            )
        } else {
            throw IllegalStateException("Profile document not found in Firestore.")
        }
    }

    suspend fun insertUser(user: UserEntity): Long = withContext(Dispatchers.IO) {
        val existingByMobile = userDao.getUserByMobile(user.mobileNumber)
        val existingById = if (user.id != 0) userDao.getUserById(user.id) else null
        val resolvedId = existingById?.id ?: existingByMobile?.id ?: user.id
        val resolvedUser = user.copy(id = resolvedId)
        userDao.insertUser(resolvedUser)
    }

    suspend fun getUserByUid(uid: String): UserEntity? = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val doc = FirebaseManager.firestore
                    ?.collection("users")
                    ?.document(uid)
                    ?.get()
                    ?.await()
                if (doc != null && doc.exists()) {
                    return@withContext UserEntity(
                        id = uid.hashCode(),
                        fullName = doc.getString("fullName") ?: "",
                        businessName = doc.getString("businessName") ?: "",
                        mobileNumber = doc.getString("mobileNumber") ?: doc.getString("mobile") ?: "",
                        passwordHash = "",
                        category = doc.getString("category") ?: doc.getString("selectedCategory") ?: "Retail"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore getUserByUid error: ${e.localizedMessage}")
            }
        }
        null
    }

    suspend fun saveUserProfile(
        uid: String,
        fullName: String,
        businessName: String,
        mobileOrEmail: String,
        category: String,
        authProvider: String
    ) = withContext(Dispatchers.IO) {
        val authUser = FirebaseManager.auth?.currentUser
        val targetUid = authUser?.uid ?: uid
        val defaultHashId = targetUid.hashCode()

        val existingByMobile = userDao.getUserByMobile(mobileOrEmail)
        val existingById = userDao.getUserById(defaultHashId)
        val resolvedId = existingById?.id ?: existingByMobile?.id ?: defaultHashId

        val localUser = UserEntity(
            id = resolvedId,
            fullName = fullName,
            businessName = businessName,
            mobileNumber = mobileOrEmail,
            passwordHash = "",
            category = category
        )

        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val data = hashMapOf(
                    "uid" to targetUid,
                    "fullName" to fullName,
                    "businessName" to businessName,
                    "mobileNumber" to (if (mobileOrEmail.contains("@")) "" else mobileOrEmail),
                    "email" to (if (mobileOrEmail.contains("@")) mobileOrEmail else ""),
                    "category" to category,
                    "role" to "user",
                    "authProvider" to authProvider,
                    "createdAt" to System.currentTimeMillis()
                )
                try {
                    firestore.collection("users").document(targetUid).set(data).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore saveUserProfile exception: ${e.localizedMessage}")
                    // Save to local Room database before rethrowing so local copy is safe
                    userDao.insertUser(localUser)
                    throw e
                }
            }
        }
        
        // Save locally to SQLite Room Database for offline support
        userDao.insertUser(localUser)
    }

    // --- Dynamic Categories (Real-time Stream & Updates) ---

    val allCategories: Flow<List<CategoryEntity>> = callbackFlow {
        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                // Listen to Firestore 'categories' in real-time
                val listenerRegistration = firestore.collection("categories")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Firestore categories listener error: ${error.localizedMessage}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.documents.map { doc ->
                                CategoryEntity(
                                    id = doc.id.hashCode(),
                                    name = doc.getString("name") ?: "",
                                    description = doc.getString("description") ?: "",
                                    iconName = doc.getString("iconName") ?: "shopping_basket",
                                    isEnabled = doc.getBoolean("isEnabled") ?: true
                                )
                            }
                            trySend(list)
                        }
                    }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            // Local Database fallback
            categoryDao.getAllCategories().collect { list ->
                trySend(list)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertCategory(category: CategoryEntity): Long = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val docRef = firestore.collection("categories").document()
                    val data = hashMapOf(
                        "name" to category.name,
                        "description" to category.description,
                        "iconName" to category.iconName,
                        "isEnabled" to category.isEnabled
                    )
                    docRef.set(data).await()
                    return@withContext docRef.id.hashCode().toLong()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore insertCategory error: ${e.localizedMessage}")
            }
        }
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    // Since Room id hashes doc.id, let's find the category document by name to update it safely
                    val snapshot = firestore.collection("categories")
                        .whereEqualTo("name", category.name)
                        .limit(1)
                        .get()
                        .await()
                    
                    val doc = snapshot.documents.firstOrNull()
                    if (doc != null) {
                        val data = hashMapOf(
                            "name" to category.name,
                            "description" to category.description,
                            "iconName" to category.iconName,
                            "isEnabled" to category.isEnabled
                        )
                        doc.reference.set(data).await()
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore updateCategory error: ${e.localizedMessage}")
            }
        }
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val snapshot = firestore.collection("categories")
                        .whereEqualTo("name", category.name)
                        .limit(1)
                        .get()
                        .await()
                    
                    val doc = snapshot.documents.firstOrNull()
                    if (doc != null) {
                        doc.reference.delete().await()
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore deleteCategory error: ${e.localizedMessage}")
            }
        }
        categoryDao.deleteCategory(category)
    }

    suspend fun prepopulateCategoriesIfEmpty() = withContext(Dispatchers.IO) {
        // Prepopulate local Room Categories
        if (categoryDao.getCategoryCount() == 0) {
            val defaults = listOf(
                CategoryEntity(name = "Kirana / Grocery", description = "Daily staples, loose items, pulses, rice, edible oils, spices", iconName = "shopping_basket", isEnabled = true),
                CategoryEntity(name = "Garments", description = "Clothing, activewear, and fashion accessories", iconName = "checkroom", isEnabled = true),
                CategoryEntity(name = "Electronics", description = "Smartphones, home appliances, laptops, and gadgets", iconName = "devices", isEnabled = true),
                CategoryEntity(name = "Pharmacy", description = "Medicines, healthcare devices, and wellness products", iconName = "local_pharmacy", isEnabled = true)
            )
            for (category in defaults) {
                categoryDao.insertCategory(category)
            }
        }

        // Prepopulate local Room Sample Kirana Loose Products if empty
        if (productDao.getProductCount() == 0) {
            val sampleProducts = listOf(
                ProductEntity(name = "Sugar (Loose)", salePrice = 40.0, purchasePrice = 34.0, stockQuantity = 50.0, unit = "Kg", category = "Kirana / Grocery"),
                ProductEntity(name = "Edible Sunflower Oil", salePrice = 150.0, purchasePrice = 130.0, stockQuantity = 30.0, unit = "Ltr", category = "Kirana / Grocery"),
                ProductEntity(name = "Basmati Rice", salePrice = 110.0, purchasePrice = 90.0, stockQuantity = 100.0, unit = "Kg", category = "Kirana / Grocery"),
                ProductEntity(name = "Toor Dal", salePrice = 160.0, purchasePrice = 135.0, stockQuantity = 40.0, unit = "Kg", category = "Kirana / Grocery"),
                ProductEntity(name = "Wheat Flour (Atta)", salePrice = 45.0, purchasePrice = 38.0, stockQuantity = 80.0, unit = "Kg", category = "Kirana / Grocery")
            )
            for (p in sampleProducts) {
                productDao.insertProduct(p)
            }
        }

        // Prepopulate local Room Sample Udhar Customers if empty
        if (customerDao.getCustomerCount() == 0) {
            val sampleCustomers = listOf(
                com.example.data.db.CustomerEntity(name = "Ramesh Kumar", mobileNumber = "9876543210", totalPendingBalance = 450.0, lastTransactionTimestamp = System.currentTimeMillis() - 86400000L),
                com.example.data.db.CustomerEntity(name = "Priya Sharma", mobileNumber = "9123456789", totalPendingBalance = 1250.0, lastTransactionTimestamp = System.currentTimeMillis() - 43200000L)
            )
            for (c in sampleCustomers) {
                customerDao.insertCustomer(c)
                customerTransactionDao.insertTransaction(
                    com.example.data.db.CustomerTransactionEntity(
                        customerMobile = c.mobileNumber,
                        customerName = c.name,
                        type = "DEBIT",
                        amount = c.totalPendingBalance,
                        paymentMode = "Credit",
                        note = "Monthly Udhar Ration",
                        timestamp = c.lastTransactionTimestamp
                    )
                )
            }
        }

        // Prepopulate Firestore if available
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val count = firestore.collection("categories").get().await().size()
                    if (count == 0) {
                        val defaults = listOf(
                            hashMapOf("name" to "Kirana / Grocery", "description" to "Daily staples, loose items, pulses, rice, edible oils, spices", "iconName" to "shopping_basket", "isEnabled" to true),
                            hashMapOf("name" to "Garments", "description" to "Clothing, activewear, and fashion accessories", "iconName" to "checkroom", "isEnabled" to true),
                            hashMapOf("name" to "Electronics", "description" to "Smartphones, home appliances, laptops, and gadgets", "iconName" to "devices", "isEnabled" to true),
                            hashMapOf("name" to "Pharmacy", "description" to "Medicines, healthcare devices, and wellness products", "iconName" to "local_pharmacy", "isEnabled" to true)
                        )
                        for (data in defaults) {
                            firestore.collection("categories").add(data).await()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore prepopulate error: ${e.localizedMessage}")
            }
        }
    }

    // --- Invoices ---

    val allInvoices: Flow<List<InvoiceEntity>> = callbackFlow {
        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val listenerRegistration = firestore.collection("invoices")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Firestore invoices listener error: ${error.localizedMessage}")
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val list = snapshot.documents.map { doc ->
                                InvoiceEntity(
                                    id = doc.id.hashCode(),
                                    firestoreId = doc.id,
                                    customerName = doc.getString("customerName") ?: "",
                                    customerMobile = doc.getString("customerMobile") ?: "",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    itemsCount = doc.getLong("itemsCount")?.toInt() ?: 1,
                                    subtotal = doc.getDouble("subtotal") ?: 0.0,
                                    discountAmount = doc.getDouble("discountAmount") ?: 0.0,
                                    taxAmount = doc.getDouble("taxAmount") ?: 0.0,
                                    paymentMode = doc.getString("paymentMode") ?: "Cash",
                                    itemsSummary = doc.getString("itemsSummary") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                    status = doc.getString("status") ?: "Paid"
                                )
                            }
                            trySend(list)
                        }
                    }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            invoiceDao.getAllInvoices().collect { list ->
                trySend(list)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    val totalSales: Flow<Double?> = callbackFlow {
        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val listenerRegistration = firestore.collection("invoices")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null) {
                            val total = snapshot.documents.sumOf { doc ->
                                if (doc.getString("status") == "Paid") {
                                    doc.getDouble("amount") ?: 0.0
                                } else 0.0
                            }
                            trySend(total)
                        }
                    }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            invoiceDao.getTotalSales().collect { sales ->
                trySend(sales)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    val invoicesCount: Flow<Int> = callbackFlow {
        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val listenerRegistration = firestore.collection("invoices")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) return@addSnapshotListener
                        if (snapshot != null) {
                            trySend(snapshot.size())
                        }
                    }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            invoiceDao.getInvoicesCount().collect { count ->
                trySend(count)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun insertInvoice(invoice: InvoiceEntity): Long = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable) {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val docRef = firestore.collection("invoices").document()
                    val data = hashMapOf(
                        "customerName" to invoice.customerName,
                        "customerMobile" to invoice.customerMobile,
                        "amount" to invoice.amount,
                        "itemsCount" to invoice.itemsCount,
                        "subtotal" to invoice.subtotal,
                        "discountAmount" to invoice.discountAmount,
                        "taxAmount" to invoice.taxAmount,
                        "paymentMode" to invoice.paymentMode,
                        "itemsSummary" to invoice.itemsSummary,
                        "timestamp" to invoice.timestamp,
                        "status" to invoice.status
                    )
                    docRef.set(data).await()
                    return@withContext docRef.id.hashCode().toLong()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore insertInvoice error: ${e.localizedMessage}")
            }
        }
        invoiceDao.insertInvoice(invoice)
    }

    suspend fun saveInvoiceAndDeductStock(
        userUid: String,
        invoice: InvoiceEntity,
        purchasedProducts: List<Pair<ProductEntity, Double>>
    ) = withContext(Dispatchers.IO) {
        var generatedFirestoreId = invoice.firestoreId

        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                try {
                    // Save to user subcollection if userUid is non-blank, and save to root collection
                    val rootDocRef = firestore.collection("invoices").document()
                    generatedFirestoreId = rootDocRef.id

                    val invoiceData = hashMapOf(
                        "customerName" to invoice.customerName,
                        "customerMobile" to invoice.customerMobile,
                        "amount" to invoice.amount,
                        "itemsCount" to invoice.itemsCount,
                        "subtotal" to invoice.subtotal,
                        "discountAmount" to invoice.discountAmount,
                        "taxAmount" to invoice.taxAmount,
                        "paymentMode" to invoice.paymentMode,
                        "itemsSummary" to invoice.itemsSummary,
                        "timestamp" to invoice.timestamp,
                        "status" to invoice.status
                    )

                    rootDocRef.set(invoiceData).await()

                    if (userUid.isNotBlank()) {
                        firestore.collection("users").document(userUid)
                            .collection("invoices").document(generatedFirestoreId)
                            .set(invoiceData).await()
                    }

                    // Auto-Deduct Stock for each purchased item
                    for ((prod, purchasedQty) in purchasedProducts) {
                        val newStock = (prod.stockQuantity - purchasedQty).coerceAtLeast(0.0)
                        val prodData = hashMapOf<String, Any>(
                            "stockQuantity" to newStock,
                            "updatedAt" to System.currentTimeMillis()
                        )

                        if (prod.firestoreId.isNotBlank()) {
                            if (userUid.isNotBlank()) {
                                firestore.collection("users").document(userUid)
                                    .collection("products").document(prod.firestoreId)
                                    .update(prodData)
                            }
                            firestore.collection("products").document(prod.firestoreId)
                                .update(prodData)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "saveInvoiceAndDeductStock firestore error: ${e.localizedMessage}")
                }
            }
        }

        // Deduct stock in local Room database
        for ((prod, purchasedQty) in purchasedProducts) {
            val newStock = (prod.stockQuantity - purchasedQty).coerceAtLeast(0.0)
            val updatedProd = prod.copy(stockQuantity = newStock, updatedAt = System.currentTimeMillis())
            productDao.insertProduct(updatedProd)
        }

        // Insert invoice into local Room database
        val localInvoice = invoice.copy(firestoreId = generatedFirestoreId)
        invoiceDao.insertInvoice(localInvoice)
    }

    // --- Product & Inventory Management ---

    fun getProductsStream(userUid: String): Flow<List<ProductEntity>> = callbackFlow {
        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val collectionRef = if (userUid.isNotBlank()) {
                    firestore.collection("users").document(userUid).collection("products")
                } else {
                    firestore.collection("products")
                }

                val listenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Products Firestore snapshot listener error: ${error.localizedMessage}")
                        // Fall back to Room SQLite
                        CoroutineScope(Dispatchers.IO).launch {
                            productDao.getAllProducts().collect { list ->
                                trySend(list)
                            }
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val productList = snapshot.documents.map { doc ->
                            ProductEntity(
                                id = doc.id.hashCode(),
                                firestoreId = doc.id,
                                name = doc.getString("name") ?: "",
                                salePrice = doc.getDouble("salePrice") ?: 0.0,
                                purchasePrice = doc.getDouble("purchasePrice") ?: 0.0,
                                stockQuantity = doc.getDouble("stockQuantity") ?: 0.0,
                                unit = doc.getString("unit") ?: "Pcs",
                                category = doc.getString("category") ?: "General",
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        }
                        trySend(productList)
                    }
                }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            productDao.getAllProducts().collect { list ->
                trySend(list)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun saveProduct(userUid: String, product: ProductEntity) = withContext(Dispatchers.IO) {
        var generatedFirestoreId = product.firestoreId

        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                try {
                    val collectionRef = if (userUid.isNotBlank()) {
                        firestore.collection("users").document(userUid).collection("products")
                    } else {
                        firestore.collection("products")
                    }

                    val docRef = if (generatedFirestoreId.isNotBlank()) {
                        collectionRef.document(generatedFirestoreId)
                    } else {
                        collectionRef.document()
                    }
                    generatedFirestoreId = docRef.id

                    val data = hashMapOf(
                        "name" to product.name,
                        "salePrice" to product.salePrice,
                        "purchasePrice" to product.purchasePrice,
                        "stockQuantity" to product.stockQuantity,
                        "unit" to product.unit,
                        "category" to product.category,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    docRef.set(data).await()
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore saveProduct exception: ${e.localizedMessage}")
                }
            }
        }

        // Always save locally to Room SQLite so local offline fallback is fully available
        val productToSave = product.copy(
            firestoreId = generatedFirestoreId,
            updatedAt = System.currentTimeMillis()
        )
        productDao.insertProduct(productToSave)
    }

    suspend fun deleteProduct(userUid: String, product: ProductEntity) = withContext(Dispatchers.IO) {
        if (FirebaseManager.isFirebaseAvailable && product.firestoreId.isNotBlank()) {
            try {
                val firestore = FirebaseManager.firestore
                if (firestore != null) {
                    val docRef = if (userUid.isNotBlank()) {
                        firestore.collection("users").document(userUid).collection("products").document(product.firestoreId)
                    } else {
                        firestore.collection("products").document(product.firestoreId)
                    }
                    docRef.delete().await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore deleteProduct error: ${e.localizedMessage}")
            }
        }
        if (product.id != 0) {
            productDao.deleteProductById(product.id)
        }
    }

    // --- Udhar Khata (Customer Credit Ledger) ---

    fun getCustomersStream(userUid: String): Flow<List<com.example.data.db.CustomerEntity>> = callbackFlow {
        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val collectionRef = if (userUid.isNotBlank()) {
                    firestore.collection("users").document(userUid).collection("customers")
                } else {
                    firestore.collection("customers")
                }

                val listenerRegistration = collectionRef.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Customers Firestore snapshot listener error: ${error.localizedMessage}")
                        CoroutineScope(Dispatchers.IO).launch {
                            customerDao.getAllCustomers().collect { list ->
                                trySend(list)
                            }
                        }
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val customerList = snapshot.documents.map { doc ->
                            com.example.data.db.CustomerEntity(
                                id = doc.id.hashCode(),
                                firestoreId = doc.id,
                                name = doc.getString("name") ?: "",
                                mobileNumber = doc.getString("mobileNumber") ?: "",
                                totalPendingBalance = doc.getDouble("totalPendingBalance") ?: 0.0,
                                lastTransactionTimestamp = doc.getLong("lastTransactionTimestamp") ?: System.currentTimeMillis()
                            )
                        }
                        trySend(customerList)
                    }
                }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            customerDao.getAllCustomers().collect { list ->
                trySend(list)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    fun getCustomerTransactionsStream(userUid: String, customerMobile: String): Flow<List<com.example.data.db.CustomerTransactionEntity>> = callbackFlow {
        if (customerMobile.isBlank()) {
            trySend(emptyList())
            awaitClose()
            return@callbackFlow
        }

        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                val docId = customerMobile.replace("+", "").replace(" ", "")
                val collectionRef = if (userUid.isNotBlank()) {
                    firestore.collection("users").document(userUid)
                        .collection("customers").document(docId)
                        .collection("transactions")
                } else {
                    firestore.collection("customers").document(docId).collection("transactions")
                }

                val listenerRegistration = collectionRef
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "Customer transactions listener error: ${error.localizedMessage}")
                            CoroutineScope(Dispatchers.IO).launch {
                                customerTransactionDao.getTransactionsForCustomer(customerMobile).collect { list ->
                                    trySend(list)
                                }
                            }
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            val txList = snapshot.documents.map { doc ->
                                com.example.data.db.CustomerTransactionEntity(
                                    id = doc.id.hashCode(),
                                    firestoreId = doc.id,
                                    customerMobile = doc.getString("customerMobile") ?: customerMobile,
                                    customerName = doc.getString("customerName") ?: "",
                                    type = doc.getString("type") ?: "DEBIT",
                                    amount = doc.getDouble("amount") ?: 0.0,
                                    paymentMode = doc.getString("paymentMode") ?: "Cash",
                                    note = doc.getString("note") ?: "",
                                    invoiceId = doc.getString("invoiceId") ?: "",
                                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                )
                            }
                            trySend(txList)
                        }
                    }
                awaitClose { listenerRegistration.remove() }
            } else {
                close()
            }
        } else {
            customerTransactionDao.getTransactionsForCustomer(customerMobile).collect { list ->
                trySend(list)
            }
            awaitClose()
        }
    }.flowOn(Dispatchers.IO)

    suspend fun recordUdharOrJamaTransaction(
        userUid: String,
        customerName: String,
        customerMobile: String,
        type: String, // "DEBIT" (Udhar) or "CREDIT" (Jama)
        amount: Double,
        paymentMode: String = "Cash",
        note: String = "",
        invoiceId: String = ""
    ) = withContext(Dispatchers.IO) {
        if (customerMobile.isBlank() || amount <= 0.0) return@withContext

        val now = System.currentTimeMillis()
        val docId = customerMobile.replace("+", "").replace(" ", "")

        val existingLocal = customerDao.getCustomerByMobile(customerMobile)
        val currentBalance = existingLocal?.totalPendingBalance ?: 0.0

        val newBalance = if (type == "DEBIT") {
            currentBalance + amount
        } else {
            (currentBalance - amount).coerceAtLeast(0.0)
        }

        val updatedCustomer = com.example.data.db.CustomerEntity(
            id = existingLocal?.id ?: 0,
            firestoreId = docId,
            name = if (customerName.isNotBlank()) customerName else (existingLocal?.name ?: "Customer"),
            mobileNumber = customerMobile,
            totalPendingBalance = newBalance,
            lastTransactionTimestamp = now
        )

        val txEntity = com.example.data.db.CustomerTransactionEntity(
            customerMobile = customerMobile,
            customerName = updatedCustomer.name,
            type = type,
            amount = amount,
            paymentMode = paymentMode,
            note = note,
            invoiceId = invoiceId,
            timestamp = now
        )

        if (FirebaseManager.isFirebaseAvailable) {
            val firestore = FirebaseManager.firestore
            if (firestore != null) {
                try {
                    val customerData = hashMapOf(
                        "name" to updatedCustomer.name,
                        "mobileNumber" to updatedCustomer.mobileNumber,
                        "totalPendingBalance" to updatedCustomer.totalPendingBalance,
                        "lastTransactionTimestamp" to now
                    )

                    val txData = hashMapOf(
                        "customerMobile" to customerMobile,
                        "customerName" to updatedCustomer.name,
                        "type" to type,
                        "amount" to amount,
                        "paymentMode" to paymentMode,
                        "note" to note,
                        "invoiceId" to invoiceId,
                        "timestamp" to now
                    )

                    firestore.collection("customers").document(docId).set(customerData).await()
                    firestore.collection("customers").document(docId).collection("transactions").add(txData).await()

                    if (userUid.isNotBlank()) {
                        firestore.collection("users").document(userUid).collection("customers").document(docId).set(customerData).await()
                        firestore.collection("users").document(userUid).collection("customers").document(docId).collection("transactions").add(txData).await()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "recordUdharOrJamaTransaction Firestore error: ${e.localizedMessage}")
                }
            }
        }

        customerDao.insertCustomer(updatedCustomer)
        customerTransactionDao.insertTransaction(txEntity)
    }
}
