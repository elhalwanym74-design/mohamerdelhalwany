package com.example

import com.example.data.model.SyncEntity
import com.example.data.model.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun `test sync entity to firestore map and back`() {
    val entity = SyncEntity(
      id = "test-uuid-1234",
      title = "Test Firestore Sync",
      description = "Verifying Room to Firestore mapping",
      category = "Cloud",
      isCompleted = true,
      updatedAt = 1700000000000L,
      syncStatus = SyncStatus.PENDING_UPLOAD,
      isDeleted = false,
      remoteVersion = 2L
    )

    val map = entity.toFirestoreMap()
    assertEquals("test-uuid-1234", map["id"])
    assertEquals("Test Firestore Sync", map["title"])
    assertEquals("Cloud", map["category"])
    assertEquals(true, map["isCompleted"])
    assertEquals(false, map["isDeleted"])

    val restored = SyncEntity.fromFirestore("test-uuid-1234", map)
    assertEquals(entity.id, restored.id)
    assertEquals(entity.title, restored.title)
    assertEquals(entity.description, restored.description)
    assertEquals(entity.category, restored.category)
    assertTrue(restored.isCompleted)
    assertFalse(restored.isDeleted)
    assertEquals(SyncStatus.SYNCED, restored.syncStatus)
  }
}
