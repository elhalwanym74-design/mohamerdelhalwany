package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SyncEntity

@Composable
fun SchemaTab(
    sampleItem: SyncEntity?,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("نموذج Room", "مستند Firestore", "بروتوكول المزامنة")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
    ) {
        item(key = "tab_row") {
            TabRow(selectedTabIndex = selectedSubTab) {
                subTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTab == index,
                        onClick = { selectedSubTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }

        item(key = "content_card") {
            when (selectedSubTab) {
                0 -> RoomSchemaCard()
                1 -> FirestoreJsonCard(sampleItem)
                2 -> ProtocolCard()
            }
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RoomSchemaCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "تعريف نموذج الكيان في قاعدة بيانات Room SQLite",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            CodeBlock(
                code = """
@Entity(tableName = "sync_records")
data class SyncEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String = "عام",
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    val isDeleted: Boolean = false,
    val remoteVersion: Long = 0L
)
                """.trimIndent()
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "القرارات المعمارية لتصميم النموذج:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "• معرفات UUID الموحدة: تتيح إنشاء السجلات محلياً حتى في حال انقطاع الإنترنت دون أي تصادم في المعرفات مع السحابة.\n• طابع الوقت updatedAt: يمكن خوارزمية (Last-Write-Wins) من حل أي تعارض بين النسخة المحلية والسحابية.\n• الحذف المؤقت isDeleted (Tombstone): يضمن انتشار حذف السجل إلى الأجهزة الأخرى قبل حذفه نهائياً من قاعدة البيانات المحلية.\n• آلة الحالة syncStatus: تتبع الحالات (PENDING_UPLOAD, SYNCED, PENDING_DELETE).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FirestoreJsonCard(sampleItem: SyncEntity?) {
    val sample = sampleItem ?: SyncEntity(
        id = "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        title = "مستند المزامنة السحابية",
        description = "مستند يتم تخزينه في مجموعة /sync_records/{id}",
        category = "سحابي",
        isCompleted = false,
        updatedAt = System.currentTimeMillis()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "هيكل مستند Firestore (/sync_records/${sample.id.take(8)}...)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            val json = """
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

            CodeBlock(code = json)

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "قواعد حماية Firestore (Security Rules المقترحة):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            CodeBlock(
                code = """
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /sync_records/{docId} {
      allow read, write: if request.auth != null;
    }
  }
}
                """.trimIndent()
            )
        }
    }
}

@Composable
private fun ProtocolCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "مراحل بروتوكول المزامنة الثنائية (Offline-First)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            StepItem(
                step = "1. الأولوية للحفظ المحلي (Local First)",
                detail = "كافة عمليات الإضافة والتعديل والحذف تُحفظ فوراً في قاعدة بيانات Room SQLite المحلية مع تعيين حالة PENDING_UPLOAD بدون أي انتظار للشبكة."
            )
            StepItem(
                step = "2. الدفع السحابي في الخلفية (Cloud Push)",
                detail = "عند توفر اتصال بالإنترنت، يقوم محرك المزامنة برفع السجلات المعلقة عبر SetOptions.merge()، وعند النجاح تتحول الحالة إلى SYNCED."
            )
            StepItem(
                step = "3. استقبال التغييرات لحظياً (Snapshot Listener)",
                detail = "يستمع التطبيق لتغييرات مجموعة Firestore لحظياً ويحدث قاعدة البيانات المحلية تلقائياً عند قيام مستخدم أو جهاز آخر بإجراء تعديل."
            )
            StepItem(
                step = "4. فض النزاعات الحتمي (Last-Write-Wins)",
                detail = "إذا تم تعديل نفس السجل على أكثر من جهاز في نفس الوقت، فإن التعديل صاحب طابع الوقت الأحدث updatedAt هو الذي يسود دائماً."
            )
        }
    }
}

@Composable
private fun StepItem(step: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = detail,
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
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            color = Color(0xFFE2E8F0),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
