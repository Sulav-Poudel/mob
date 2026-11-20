package com.example.data

import android.content.Context
import com.example.R
import com.example.model.ApplicationStatus
import com.example.model.CssProfileRequirement
import com.example.model.DEFAULT_STARTER_ESSAY_TYPES
import com.example.model.EnglishProficiency
import com.example.model.FinancialAidDocStatus
import com.example.model.Supplement
import com.example.model.SupplementStatus
import com.example.model.SupplementType
import com.example.model.University
import com.example.model.WordBucket
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class UniversityInput(
    val id: String? = null,
    val name: String,
    val applicationDeadlineMillis: Long?,
    val applicationStatus: ApplicationStatus,
    val targetSupplementCount: Int,
    val financialAidDeadlineMillis: Long?,
    val cssProfileRequirement: CssProfileRequirement,
    val financialAidDocStatus: FinancialAidDocStatus,
    val englishProficiency: EnglishProficiency,
    val collegeTraits: String,
    val autoCreateSupplementSlots: Boolean = false
)

data class SupplementInput(
    val id: String? = null,
    val universityId: String,
    val title: String,
    val status: SupplementStatus,
    val wordBucket: WordBucket,
    val essayType: String,
    val prompt: String,
    val brainstormNotes: String
)

class TrackerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun observeUniversities(userId: String): Flow<List<University>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/universities"
        emitAll(
            userDoc(userId).collection("universities")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(University::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<University> { it.applicationDeadlineMillis ?: Long.MAX_VALUE }
                                .thenBy { it.name.lowercase() }
                        )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSupplements(userId: String): Flow<List<Supplement>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/supplements"
        emitAll(
            userDoc(userId).collection("supplements")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Supplement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeSupplementTypes(userId: String): Flow<List<SupplementType>> = flow {
        require(userId.isNotBlank()) { "userId must not be blank" }
        val path = "users/$userId/supplementTypes"
        emitAll(
            userDoc(userId).collection("supplementTypes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(SupplementType::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name.lowercase() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun seedStarterSupplementTypesIfEmpty(): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplementTypes"
        return try {
            val uid = requireUserId()
            val colRef = userDoc(uid).collection("supplementTypes")
            path = colRef.path
            val existing = colRef.limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            val batch = db.batch()
            DEFAULT_STARTER_ESSAY_TYPES.forEachIndexed { index, typeName ->
                val slug = typeName.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
                val docRef = colRef.document("starter_${index}_$slug")
                batch.set(
                    docRef,
                    mapOf(
                        "userId" to uid,
                        "name" to typeName,
                        "normalizedName" to typeName.trim().lowercase(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun saveUniversity(input: UniversityInput, existingSupplementCount: Int = 0): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/universities"
        return try {
            val uid = requireUserId()
            val cleanName = input.name.trim().take(120)
            require(cleanName.isNotEmpty()) { "University name is required." }
            val cleanCount = input.targetSupplementCount.coerceIn(0, 50)
            val cleanTraits = input.collegeTraits.trim().take(2000)

            val colRef = userDoc(uid).collection("universities")
            val isCreate = input.id.isNullOrBlank()
            val docRef = if (isCreate) colRef.document() else colRef.document(input.id!!)
            docPath = docRef.path

            if (isCreate) {
                val createPayload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "name" to cleanName,
                    "applicationDeadlineMillis" to input.applicationDeadlineMillis?.takeIf { it >= 0L },
                    "applicationStatus" to input.applicationStatus.name,
                    "targetSupplementCount" to cleanCount.toLong(),
                    "financialAidDeadlineMillis" to input.financialAidDeadlineMillis?.takeIf { it >= 0L },
                    "cssProfileRequirement" to input.cssProfileRequirement.name,
                    "financialAidDocStatus" to input.financialAidDocStatus.name,
                    "englishProficiency" to input.englishProficiency.name,
                    "collegeTraits" to cleanTraits,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>

                if (input.autoCreateSupplementSlots && cleanCount > 0) {
                    val batch = db.batch()
                    batch.set(docRef, createPayload)
                    val suppCol = userDoc(uid).collection("supplements")
                    for (idx in 1..cleanCount) {
                        val suppRef = suppCol.document()
                        batch.set(
                            suppRef,
                            mapOf(
                                "userId" to uid,
                                "universityId" to docRef.id,
                                "title" to "Supplement $idx",
                                "status" to SupplementStatus.NOT_STARTED.name,
                                "wordBucket" to WordBucket.W250.name,
                                "essayType" to "",
                                "prompt" to "",
                                "brainstormNotes" to "",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                } else {
                    docRef.set(createPayload).await()
                }
            } else {
                val updatePayload = mapOf<String, Any>(
                    "name" to cleanName,
                    "applicationDeadlineMillis" to (input.applicationDeadlineMillis?.takeIf { it >= 0L } ?: FieldValue.delete()),
                    "applicationStatus" to input.applicationStatus.name,
                    "targetSupplementCount" to cleanCount.toLong(),
                    "financialAidDeadlineMillis" to (input.financialAidDeadlineMillis?.takeIf { it >= 0L } ?: FieldValue.delete()),
                    "cssProfileRequirement" to input.cssProfileRequirement.name,
                    "financialAidDocStatus" to input.financialAidDocStatus.name,
                    "englishProficiency" to input.englishProficiency.name,
                    "collegeTraits" to cleanTraits,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()

                val missing = cleanCount - existingSupplementCount
                if (input.autoCreateSupplementSlots && missing > 0) {
                    val batch = db.batch()
                    val suppCol = userDoc(uid).collection("supplements")
                    for (idx in (existingSupplementCount + 1)..cleanCount) {
                        val suppRef = suppCol.document()
                        batch.set(
                            suppRef,
                            mapOf(
                                "userId" to uid,
                                "universityId" to docRef.id,
                                "title" to "Supplement $idx",
                                "status" to SupplementStatus.NOT_STARTED.name,
                                "wordBucket" to WordBucket.W250.name,
                                "essayType" to "",
                                "prompt" to "",
                                "brainstormNotes" to "",
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    batch.commit().await()
                }
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUniversityQuickStatus(
        universityId: String,
        applicationStatus: ApplicationStatus? = null,
        financialAidDocStatus: FinancialAidDocStatus? = null
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/universities/$universityId"
        return try {
            val uid = requireUserId()
            require(universityId.isNotBlank()) { "University ID cannot be blank" }
            val docRef = userDoc(uid).collection("universities").document(universityId)
            docPath = docRef.path
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            applicationStatus?.let { updates["applicationStatus"] = it.name }
            financialAidDocStatus?.let { updates["financialAidDocStatus"] = it.name }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteUniversity(universityId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/universities/$universityId"
        return try {
            val uid = requireUserId()
            require(universityId.isNotBlank()) { "University ID cannot be blank" }
            val uniRef = userDoc(uid).collection("universities").document(universityId)
            docPath = uniRef.path
            val linkedSupplements = userDoc(uid).collection("supplements")
                .whereEqualTo("universityId", universityId)
                .get()
                .await()

            val batch = db.batch()
            linkedSupplements.documents.forEach { batch.delete(it.reference) }
            batch.delete(uniRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun generateMissingSupplementSlots(
        universityId: String,
        currentCount: Int,
        targetCount: Int
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplements"
        return try {
            val uid = requireUserId()
            require(universityId.isNotBlank()) { "University ID cannot be blank" }
            val toCreate = (targetCount - currentCount).coerceIn(0, 50)
            if (toCreate == 0) return Result.success(Unit)
            val suppCol = userDoc(uid).collection("supplements")
            docPath = suppCol.path
            val batch = db.batch()
            for (idx in (currentCount + 1)..(currentCount + toCreate)) {
                val suppRef = suppCol.document()
                batch.set(
                    suppRef,
                    mapOf(
                        "userId" to uid,
                        "universityId" to universityId,
                        "title" to "Supplement $idx",
                        "status" to SupplementStatus.NOT_STARTED.name,
                        "wordBucket" to WordBucket.W250.name,
                        "essayType" to "",
                        "prompt" to "",
                        "brainstormNotes" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveSupplement(
        input: SupplementInput,
        existingTypes: List<SupplementType>
    ): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplements"
        return try {
            val uid = requireUserId()
            require(input.universityId.isNotBlank()) { "University ID is required." }

            val cleanTitle = input.title.trim().take(120)
            val cleanType = input.essayType.trim().take(80)
            val cleanPrompt = input.prompt.trim().take(3000)
            val cleanNotes = input.brainstormNotes.trim().take(10000)

            // If user typed a new custom essay type, persist it into shared catalog automatically
            if (cleanType.isNotEmpty()) {
                val normalized = cleanType.lowercase()
                val alreadyExists = existingTypes.any { it.normalizedName == normalized }
                if (!alreadyExists) {
                    val typeRef = userDoc(uid).collection("supplementTypes").document()
                    typeRef.set(
                        mapOf(
                            "userId" to uid,
                            "name" to cleanType,
                            "normalizedName" to normalized,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }

            val colRef = userDoc(uid).collection("supplements")
            val isCreate = input.id.isNullOrBlank()
            val docRef = if (isCreate) colRef.document() else colRef.document(input.id!!)
            docPath = docRef.path

            if (isCreate) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "universityId" to input.universityId,
                        "title" to cleanTitle,
                        "status" to input.status.name,
                        "wordBucket" to input.wordBucket.name,
                        "essayType" to cleanType,
                        "prompt" to cleanPrompt,
                        "brainstormNotes" to cleanNotes,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "title" to cleanTitle,
                        "status" to input.status.name,
                        "wordBucket" to input.wordBucket.name,
                        "essayType" to cleanType,
                        "prompt" to cleanPrompt,
                        "brainstormNotes" to cleanNotes,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun quickUpdateSupplementStatus(supplementId: String, status: SupplementStatus): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplements/$supplementId"
        return try {
            val uid = requireUserId()
            require(supplementId.isNotBlank()) { "Supplement ID cannot be blank" }
            val docRef = userDoc(uid).collection("supplements").document(supplementId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplement(supplementId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplements/$supplementId"
        return try {
            val uid = requireUserId()
            require(supplementId.isNotBlank()) { "Supplement ID cannot be blank" }
            val docRef = userDoc(uid).collection("supplements").document(supplementId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addSupplementType(name: String, existingTypes: List<SupplementType>): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplementTypes"
        return try {
            val uid = requireUserId()
            val clean = name.trim().take(80)
            require(clean.isNotEmpty()) { "Essay type name cannot be empty." }
            val normalized = clean.lowercase()
            if (existingTypes.any { it.normalizedName == normalized }) {
                return Result.success(Unit)
            }
            val docRef = userDoc(uid).collection("supplementTypes").document()
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "name" to clean,
                    "normalizedName" to normalized,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun renameSupplementType(typeId: String, newName: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplementTypes/$typeId"
        return try {
            val uid = requireUserId()
            require(typeId.isNotBlank()) { "Type ID cannot be blank" }
            val clean = newName.trim().take(80)
            require(clean.isNotEmpty()) { "Essay type name cannot be empty." }
            val docRef = userDoc(uid).collection("supplementTypes").document(typeId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "name" to clean,
                    "normalizedName" to clean.lowercase(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSupplementType(typeId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/supplementTypes/$typeId"
        return try {
            val uid = requireUserId()
            require(typeId.isNotBlank()) { "Type ID cannot be blank" }
            val docRef = userDoc(uid).collection("supplementTypes").document(typeId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
