package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.LeaveRecord
import com.example.data.model.UserRole
import com.example.ui.SantriViewModel
import com.example.ui.util.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IzinSantriScreen(
    viewModel: SantriViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.studentsList.collectAsState()
    val leavesList by viewModel.leavesList.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val isAsramaOrSuper = currentUser?.role == UserRole.ASRAMA || currentUser?.role == UserRole.SUPER_ADMIN

    var selectedTab by remember { mutableIntStateOf(if (isAsramaOrSuper) 0 else 2) } // 0: Form, 1: Check-in, 2: Daftar Izin

    // Form state
    var selectedStudentId by remember { mutableStateOf("") }
    var tanggalMulai by remember { mutableStateOf("") }
    var tanggalKembali by remember { mutableStateOf("") }
    var alasan by remember { mutableStateOf("") }
    var generatedIdIzin by remember { mutableStateOf<String?>(null) }
    var generatedNamaSantri by remember { mutableStateOf<String?>(null) }

    // Check-in state
    var checkinIdInput by remember { mutableStateOf("") }

    // Search query for daftar izin
    var searchLeaveQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.fetchLeaves()
    }

    val expiredCount = remember(leavesList) {
        val now = Date()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        leavesList.count { l ->
            val statusStr = l.status ?: ""
            val tglKembali = l.tanggalKembali ?: ""
            if (!statusStr.lowercase().contains("check")) {
                try {
                    val kembaliDate = sdf.parse(tglKembali)
                    kembaliDate != null && now.after(kembaliDate)
                } catch (_: Exception) {
                    false
                }
            } else false
        }
    }

    val filteredLeaves = remember(leavesList, searchLeaveQuery) {
        if (searchLeaveQuery.isBlank()) leavesList
        else {
            val q = searchLeaveQuery.trim().lowercase()
            leavesList.filter { l ->
                (l.namaSantri ?: "").lowercase().contains(q) ||
                (l.idIzin ?: "").lowercase().contains(q) ||
                (l.kelas ?: "").lowercase().contains(q) ||
                (l.idSantri ?: "").lowercase().contains(q)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        // Notification Banner for Overdue Leaves
        if (expiredCount > 0) {
            Surface(
                color = Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "⚠️ Perhatian! Ada $expiredCount santri yang melewati batas waktu kembali izin asrama!",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        // Sub Tabs (Always 3 Tabs for UI stability)
        ScrollableTabRow(
            selectedTabIndex = selectedTab.coerceIn(0, 2),
            containerColor = Color.White,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Form Izin", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Check-in", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Daftar Izin (${leavesList.size})", fontWeight = FontWeight.Bold) }
            )
        }

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (selectedTab) {
                0 -> {
                    // 1. Form Izin
                    if (!isAsramaOrSuper) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Akses Terbatas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Form Pengajuan Izin khusus untuk role Asrama & Super Admin. Anda dapat melihat Daftar Izin di tab 'Daftar Izin'.", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(
                                            text = "Form Pengajuan Izin Santri",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF0F172A)
                                        )

                                        // Class Filter for Students
                                        val availableClasses = remember(students) {
                                            students.map { it.className.trim() }.filter { it.isNotBlank() }.distinct().sorted()
                                        }
                                        var selectedClassFilter by remember { mutableStateOf("Semua Kelas") }
                                        var classExpanded by remember { mutableStateOf(false) }

                                        val filteredStudentsForForm = remember(students, selectedClassFilter) {
                                            if (selectedClassFilter == "Semua Kelas") students
                                            else students.filter { it.className.equals(selectedClassFilter, ignoreCase = true) }
                                        }

                                        ExposedDropdownMenuBox(
                                            expanded = classExpanded,
                                            onExpandedChange = { classExpanded = !classExpanded }
                                        ) {
                                            OutlinedTextField(
                                                value = "Filter Kelas: $selectedClassFilter",
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = classExpanded,
                                                onDismissRequest = { classExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Semua Kelas") },
                                                    onClick = {
                                                        selectedClassFilter = "Semua Kelas"
                                                        classExpanded = false
                                                    }
                                                )
                                                availableClasses.forEach { cls ->
                                                    DropdownMenuItem(
                                                        text = { Text(cls) },
                                                        onClick = {
                                                            selectedClassFilter = cls
                                                            classExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Student Selector Dropdown
                                        var expanded by remember { mutableStateOf(false) }
                                        val selectedStudent = students.find { it.id == selectedStudentId }

                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = !expanded }
                                        ) {
                                            OutlinedTextField(
                                                value = if (selectedStudent != null) "${selectedStudent.id} - ${selectedStudent.name} (${selectedStudent.className})" else "-- Pilih Santri --",
                                                onValueChange = {},
                                                readOnly = true,
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                if (filteredStudentsForForm.isEmpty()) {
                                                    DropdownMenuItem(
                                                        text = { Text("Data santri kosong") },
                                                        onClick = { expanded = false }
                                                    )
                                                } else {
                                                    filteredStudentsForForm.forEach { s ->
                                                        DropdownMenuItem(
                                                            text = { Text("${s.id} - ${s.name} (${s.className})") },
                                                            onClick = {
                                                                selectedStudentId = s.id
                                                                expanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        fun showDateTimePicker(onSelected: (String) -> Unit) {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                                TimePickerDialog(context, { _, hourOfDay, minute ->
                                                    val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d %02d:%02d", dayOfMonth, month + 1, year, hourOfDay, minute)
                                                    onSelected(formatted)
                                                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                        }

                                        OutlinedTextField(
                                            value = tanggalMulai,
                                            onValueChange = { tanggalMulai = it },
                                            label = { Text("Tanggal & Waktu Berangkat") },
                                            placeholder = { Text("Ketuk ikon kalender...") },
                                            readOnly = true,
                                            trailingIcon = {
                                                IconButton(onClick = { showDateTimePicker { tanggalMulai = it } }) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = tanggalKembali,
                                            onValueChange = { tanggalKembali = it },
                                            label = { Text("Tanggal & Waktu Kembali (Batas)") },
                                            placeholder = { Text("Ketuk ikon kalender...") },
                                            readOnly = true,
                                            trailingIcon = {
                                                IconButton(onClick = { showDateTimePicker { tanggalKembali = it } }) {
                                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pilih Tanggal")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        OutlinedTextField(
                                            value = alasan,
                                            onValueChange = { alasan = it },
                                            label = { Text("Alasan / Keperluan Izin") },
                                            placeholder = { Text("Keperluan keluarga / sakit...") },
                                            modifier = Modifier.fillMaxWidth(),
                                            minLines = 3,
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        Button(
                                            onClick = {
                                                val st = students.find { it.id == selectedStudentId }
                                                if (st == null || tanggalMulai.isBlank() || tanggalKembali.isBlank() || alasan.isBlank()) {
                                                    Toast.makeText(context, "Lengkapi semua data form izin!", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                val leave = LeaveRecord(
                                                    idIzin = "",
                                                    idSantri = st.id,
                                                    namaSantri = st.name,
                                                    kelas = st.className,
                                                    unit = st.unitPendidikan,
                                                    tanggalIzin = tanggalMulai,
                                                    tanggalKembali = tanggalKembali,
                                                    alasan = alasan,
                                                    status = "Aktif",
                                                    timestamp = ""
                                                )
                                                viewModel.createLeaveRecord(leave) { success, msg, newId ->
                                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                                    if (success) {
                                                        generatedIdIzin = newId
                                                        generatedNamaSantri = st.name
                                                        tanggalMulai = ""
                                                        tanggalKembali = ""
                                                        alasan = ""
                                                        selectedStudentId = ""
                                                    }
                                                }
                                            },
                                            enabled = !isSyncing,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Memproses...")
                                            } else {
                                                Text("Buat Surat Izin & Generate QR", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Result Card with QR Code
                            if (generatedIdIzin != null) {
                                item {
                                    Card(
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("✅ Izin Berhasil Dibuat!", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF047857))
                                            Text(
                                                text = "ID Izin: $generatedIdIzin",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF065F46)
                                            )
                                            Text(
                                                text = generatedNamaSantri ?: "",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFF1F2937)
                                            )

                                            // QR Code Render
                                            val qrBitmap = remember(generatedIdIzin) {
                                                QrCodeGenerator.generateQrBitmap(generatedIdIzin ?: "", 300)
                                            }
                                            if (qrBitmap != null) {
                                                Image(
                                                    bitmap = qrBitmap.asImageBitmap(),
                                                    contentDescription = "QR Code Izin",
                                                    modifier = Modifier.size(160.dp)
                                                )
                                            }
                                            Text("Scan QR ini saat santri kembali ke asrama untuk Check-in", style = MaterialTheme.typography.labelSmall, color = Color(0xFF047857))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // 2. Check-in
                    if (!isAsramaOrSuper) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Akses Terbatas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Fitur Check-in khusus untuk role Asrama & Super Admin.", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Check-in Kepulangan Santri", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text("Masukkan ID Izin atau scan QR code surat izin santri.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))

                                OutlinedTextField(
                                    value = checkinIdInput,
                                    onValueChange = { checkinIdInput = it },
                                    label = { Text("Masukkan ID Izin (cth: IZIN-1234)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        if (checkinIdInput.isBlank()) {
                                            Toast.makeText(context, "Masukkan ID Izin!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        viewModel.checkinLeaveRecord(checkinIdInput.trim()) { success, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            if (success) {
                                                checkinIdInput = ""
                                                viewModel.fetchLeaves()
                                            }
                                        }
                                    },
                                    enabled = !isSyncing,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(50.dp)
                                ) {
                                    Text("Proses Check-in", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // 3. Daftar Izin
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchLeaveQuery,
                                onValueChange = { searchLeaveQuery = it },
                                placeholder = { Text("Cari nama/ID izin santri...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            IconButton(onClick = { viewModel.fetchLeaves() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }

                        if (filteredLeaves.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Belum ada data izin santri.", color = Color(0xFF94A3B8))
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                                items(filteredLeaves) { l ->
                                    val statusStr = l.status ?: "Aktif"
                                    val isCheckedIn = statusStr.lowercase().contains("check")
                                    val now = Date()
                                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                    var isExpired = false
                                    val tglKembali = l.tanggalKembali ?: ""
                                    if (tglKembali.isNotBlank()) {
                                        try {
                                            val kembaliDate = sdf.parse(tglKembali)
                                            if (kembaliDate != null && now.after(kembaliDate) && !isCheckedIn) {
                                                isExpired = true
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    Card(
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = if (isExpired) Color(0xFFFEF2F2) else Color.White),
                                        border = BorderStroke(1.dp, if (isExpired) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = l.idIzin ?: "-", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = when {
                                                        isCheckedIn -> Color(0xFFD1FAE5)
                                                        isExpired -> Color(0xFFFEE2E2)
                                                        else -> Color(0xFFFEF3C7)
                                                    }
                                                ) {
                                                    Text(
                                                        text = when {
                                                            isCheckedIn -> "Sudah Check-in"
                                                            isExpired -> "⚠️ Terlambat / Expired"
                                                            else -> "Aktif (Belum Kembali)"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = when {
                                                            isCheckedIn -> Color(0xFF065F46)
                                                            isExpired -> Color(0xFF991B1B)
                                                            else -> Color(0xFF92400E)
                                                        },
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(text = "${l.namaSantri ?: "-"} (${l.idSantri ?: "-"})", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0F172A))
                                            Text(text = "Kelas: ${l.kelas ?: "-"} • Unit: ${l.unit ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                            Text(text = "Izin: ${l.tanggalIzin ?: "-"} s/d ${l.tanggalKembali ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                                            Text(text = "Alasan: ${l.alasan ?: "-"}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF334155))

                                            if (!isCheckedIn && isAsramaOrSuper) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.checkinLeaveRecord(l.idIzin) { success, msg ->
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            if (success) viewModel.fetchLeaves()
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                    modifier = Modifier.align(Alignment.End)
                                                ) {
                                                    Text("Check-in", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
