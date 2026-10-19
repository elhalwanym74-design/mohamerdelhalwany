package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncEntity

@Composable
fun CloudSchemaDialog(
    item: SyncEntity?,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Room Entity", "Firestore JSON", "Sync Protocol")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Data Model & Cloud Schema",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> RoomEntityTabContent()
                    1 -> FirestoreJsonTabContent(item)
                    2 -> SyncProtocolTabContent()
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("close_schema_dialog")
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RoomEntityTabContent() {
    Column {
        Text(
            text = "Room SQLite Entity Definition",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        CodeBlock(
            code = """
@Entity(tableName = "sync_records")
data class SyncEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String = "General",
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val isDeleted: Boolean = false,
    val remoteVersion: Long = 0L
)
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Key Design Highlights:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "• UUID string keys unify Room SQLite and Firestore document paths.\n• updatedAt (epoch ms) enables deterministic Last-Write-Wins conflict resolution.\n• isDeleted acts as a soft-deletion tombstone so remote clients remove the doc.\n• syncStatus tracks PENDING_UPLOAD, SYNCED, or PENDING_DELETE.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FirestoreJsonTabContent(item: SyncEntity?) {
    val sample = item ?: SyncEntity(
        id = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
        title = "Cloud Synchronization Document",
        description = "Mirrored in Firestore collection /sync_records/{id}",
        category = "Cloud",
        isCompleted = false,
        updatedAt = System.currentTimeMillis(),
        remoteVersion = 1L
    )

    Column {
        Text(
            text = "Firestore Document Payload (/sync_records/${sample.id.take(8)}...)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        val jsonString = """
{
  "id": "${sample.id}",
  "title": "${sample.title}",
  "description": "${sample.description}",
  "category": "${sample.category}",
  "isCompleted": ${sample.isCompleted},
  "updatedAt": ${sample.updatedAt},
  "isDeleted": ${sample.isDeleted},
  "remoteVersion": ${sample.remoteVersion}
}
        """.trimIndent()

        CodeBlock(code = jsonString)

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Mapped via toFirestoreMap() and SyncEntity.fromFirestore(docId, data).",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun SyncProtocolTabContent() {
    Column {
        Text(
            text = "Bidirectional Sync Architecture",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        ProtocolStep(
            step = "1. Local First",
            desc = "All UI interactions save immediately to SQLite via Room DAO. Zero latency; fully functional offline."
        )
        ProtocolStep(
            step = "2. Real-Time Listeners",
            desc = "Firestore addSnapshotListener reacts to incoming changes from other devices or the Firebase console."
        )
        ProtocolStep(
            step = "3. Conflict Resolution",
            desc = "Last-Write-Wins: If remote updatedAt > local updatedAt, Room updates with SYNCED status."
        )
        ProtocolStep(
            step = "4. Tombstone Deletions",
            desc = "Deleted items are marked with isDeleted=true until Firestore confirms removal, preventing ghost resurrections."
        )
    }
}

@Composable
private fun ProtocolStep(step: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CodeBlock(code: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
            .padding(10.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            color = Color(0xFFE2E8F0),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
