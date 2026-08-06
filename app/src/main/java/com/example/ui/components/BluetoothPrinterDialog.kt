package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.db.InvoiceEntity
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldYellow
import com.example.util.ReceiptPrintHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothPrinterDialog(
    invoice: InvoiceEntity? = null,
    businessName: String,
    upiId: String = "merchant@upi",
    isGstModeInitial: Boolean = true,
    onGstModeToggle: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pairedPrinters by remember { mutableStateOf<List<ReceiptPrintHelper.PairedPrinter>>(emptyList()) }
    var selectedPrinterAddress by remember { mutableStateOf<String?>(null) }
    var paperWidthMm by remember { mutableIntStateOf(58) } // 58 or 80
    var isGstMode by remember { mutableStateOf(isGstModeInitial) }
    var printStatus by remember { mutableStateOf<String?>(null) }
    var isPrinting by remember { mutableStateOf(false) }

    var hasBtPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBtPermission = granted
        if (granted) {
            pairedPrinters = ReceiptPrintHelper.getPairedPrinters(context)
        }
    }

    fun loadPrinters() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hasBtPermission) {
            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            pairedPrinters = ReceiptPrintHelper.getPairedPrinters(context)
            if (pairedPrinters.isNotEmpty() && selectedPrinterAddress == null) {
                selectedPrinterAddress = pairedPrinters.first().address
            }
        }
    }

    LaunchedEffect(Unit) {
        loadPrinters()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x2210B981), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = "Printer", tint = EmeraldGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Bluetooth Thermal Print", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("ESC/POS Auto-Format Receipt", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.testTag("bluetooth_printer_close_btn")) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. GST Mode & Paper Width Settings Row
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F1E295D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // GST Mode Toggle Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isGstMode) "🔴 GST Invoice Mode" else "🟢 Simple Estimate (Non-GST)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = if (isGstMode) "Prints GSTIN, HSN, CGST/SGST breakdown" else "Prints simple Cash Memo without tax lines",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = isGstMode,
                                onCheckedChange = {
                                    isGstMode = it
                                    onGstModeToggle?.invoke(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = EmeraldGreen,
                                    checkedTrackColor = Color(0x4410B981)
                                ),
                                modifier = Modifier.testTag("printer_dialog_gst_toggle")
                            )
                        }

                        // Paper Width Options
                        Text("Paper Width / Column Size", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = paperWidthMm == 58,
                                onClick = { paperWidthMm = 58 },
                                label = { Text("58mm (32 Cols Standard)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("printer_paper_58mm")
                            )
                            FilterChip(
                                selected = paperWidthMm == 80,
                                onClick = { paperWidthMm = 80 },
                                label = { Text("80mm (48 Cols Wide)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldGreen,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f).testTag("printer_paper_80mm")
                            )
                        }
                    }
                }

                // 2. Bluetooth Paired Devices List
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Paired Thermal Printer", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { loadPrinters() }, modifier = Modifier.size(24.dp).testTag("refresh_printers_btn")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EmeraldLight)
                    }
                }

                if (pairedPrinters.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x22EF4444)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x44EF4444), RoundedCornerShape(10.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("⚠️ No paired Bluetooth devices found!", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "1. Turn on your thermal printer.\n2. Open Android Bluetooth Settings & Pair device.\n3. Return here and tap refresh icon.",
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(130.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(pairedPrinters) { printer ->
                            val isSelected = printer.address == selectedPrinterAddress
                            Card(
                                onClick = { selectedPrinterAddress = printer.address },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0x3310B981) else Color(0x11FFFFFF)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isSelected) EmeraldGreen else Color(0x22FFFFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .testTag("printer_device_${printer.address}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                                            contentDescription = null,
                                            tint = if (isSelected) EmeraldLight else Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(printer.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(printer.address, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                        }
                                    }

                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Status Message Box
                printStatus?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x2210B981)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().border(1.dp, Color(0x4410B981), RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = msg,
                            color = EmeraldLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Test Print Button
                OutlinedButton(
                    onClick = {
                        val targetAddr = selectedPrinterAddress ?: pairedPrinters.firstOrNull()?.address
                        if (targetAddr == null) {
                            printStatus = "Please select a Bluetooth printer first"
                        } else {
                            isPrinting = true
                            scope.launch {
                                ReceiptPrintHelper.printTestReceipt(
                                    context = context,
                                    deviceAddress = targetAddr,
                                    businessName = businessName,
                                    paperWidthMm = paperWidthMm,
                                    onStatusUpdate = { status -> printStatus = status }
                                )
                                isPrinting = false
                            }
                        }
                    },
                    enabled = !isPrinting && (selectedPrinterAddress != null || pairedPrinters.isNotEmpty()),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("test_print_btn")
                ) {
                    Text("Test Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Print Invoice Button
                if (invoice != null) {
                    Button(
                        onClick = {
                            val targetAddr = selectedPrinterAddress ?: pairedPrinters.firstOrNull()?.address
                            if (targetAddr == null) {
                                printStatus = "Please select a Bluetooth printer"
                            } else {
                                isPrinting = true
                                scope.launch {
                                    ReceiptPrintHelper.printReceipt(
                                        context = context,
                                        deviceAddress = targetAddr,
                                        invoice = invoice,
                                        businessName = businessName,
                                        upiId = upiId,
                                        paperWidthMm = paperWidthMm,
                                        isGstMode = isGstMode,
                                        onStatusUpdate = { status -> printStatus = status }
                                    )
                                    isPrinting = false
                                }
                            }
                        },
                        enabled = !isPrinting && (selectedPrinterAddress != null || pairedPrinters.isNotEmpty()),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.3f).testTag("print_invoice_receipt_btn")
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Printing...", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print Thermal Bill", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0x3310B981), RoundedCornerShape(20.dp))
    )
}
