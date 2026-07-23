package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ProductEntity
import com.example.ui.theme.*
import com.example.util.KiranaUnitUtils
import com.example.util.LooseInputType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LooseQuantityDialog(
    product: ProductEntity,
    initialQuantity: Double = 1.0,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Double) -> Unit
) {
    val isVolume = KiranaUnitUtils.isVolumeUnit(product.unit)
    val isLoose = KiranaUnitUtils.isLooseUnit(product.unit)

    val primaryUnitLabel = KiranaUnitUtils.getPrimaryUnitLabel(product.unit)
    val secondaryUnitLabel = KiranaUnitUtils.getSecondaryUnitLabel(product.unit)

    var inputType by remember { mutableStateOf(LooseInputType.DUAL) }

    // Dual fields state
    val (initPrimary, initSecondary) = remember(initialQuantity) {
        KiranaUnitUtils.extractDualValues(initialQuantity, product.unit)
    }
    var primaryInput by remember { mutableStateOf(if (initPrimary > 0) initPrimary.toString() else "0") }
    var secondaryInput by remember { mutableStateOf(if (initSecondary > 0) initSecondary.toString() else "0") }

    // Decimal input state
    var decimalInput by remember {
        mutableStateOf(if (initialQuantity > 0) String.format(Locale.US, "%.3f", initialQuantity).trimEnd('0').trimEnd('.') else "1.0")
    }

    // Calculated total quantity
    val currentQuantity by remember {
        derivedStateOf {
            if (inputType == LooseInputType.DUAL) {
                val p = primaryInput.toIntOrNull() ?: 0
                val s = secondaryInput.toIntOrNull() ?: 0
                KiranaUnitUtils.computeQuantityFromInput(product.unit, LooseInputType.DUAL, 0.0, p, s)
            } else {
                val d = decimalInput.toDoubleOrNull() ?: 0.0
                KiranaUnitUtils.computeQuantityFromInput(product.unit, LooseInputType.DECIMAL, d, 0, 0)
            }
        }
    }

    val currentTotalAmount by remember(currentQuantity, product.salePrice) {
        derivedStateOf {
            KiranaUnitUtils.calculateExactPrice(product.salePrice, currentQuantity)
        }
    }

    val isExceedingStock = currentQuantity > product.stockQuantity

    // Preset options
    val presets = remember(isVolume) {
        if (isVolume) {
            listOf(
                Triple("250 ml", 0, 250),
                Triple("500 ml", 0, 500),
                Triple("750 ml", 0, 750),
                Triple("1 Ltr", 1, 0),
                Triple("1.5 Ltr", 1, 500),
                Triple("2 Ltr", 2, 0),
                Triple("5 Ltr", 5, 0)
            )
        } else {
            listOf(
                Triple("250 gm", 0, 250),
                Triple("500 gm", 0, 500),
                Triple("750 gm", 0, 750),
                Triple("1 kg", 1, 0),
                Triple("1.5 kg", 1, 500),
                Triple("2 kg", 2, 0),
                Triple("5 kg", 5, 0)
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Scale,
                    contentDescription = null,
                    tint = GoldYellow,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "Price: ₹${String.format(Locale.US, "%.2f", product.salePrice)} / ${product.unit}  •  Stock: ${KiranaUnitUtils.formatQuantityWithUnit(product.stockQuantity, product.unit)}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8))
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Input Mode Toggle (Dual Fields vs Decimal)
                if (isLoose) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = inputType == LooseInputType.DUAL,
                            onClick = {
                                inputType = LooseInputType.DUAL
                                // Sync dual inputs from decimal
                                val d = decimalInput.toDoubleOrNull() ?: 1.0
                                val (p, s) = KiranaUnitUtils.extractDualValues(d, product.unit)
                                primaryInput = p.toString()
                                secondaryInput = s.toString()
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldGreen,
                                activeContentColor = Color.White,
                                inactiveContainerColor = Color(0x22FFFFFF),
                                inactiveContentColor = Color(0xFF94A3B8)
                            )
                        ) {
                            Text("Dual ($primaryUnitLabel / $secondaryUnitLabel)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        SegmentedButton(
                            selected = inputType == LooseInputType.DECIMAL,
                            onClick = {
                                inputType = LooseInputType.DECIMAL
                                // Sync decimal input from dual
                                val p = primaryInput.toIntOrNull() ?: 0
                                val s = secondaryInput.toIntOrNull() ?: 0
                                val computed = KiranaUnitUtils.computeQuantityFromInput(product.unit, LooseInputType.DUAL, 0.0, p, s)
                                decimalInput = String.format(Locale.US, "%.3f", computed).trimEnd('0').trimEnd('.')
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldGreen,
                                activeContentColor = Color.White,
                                inactiveContainerColor = Color(0x22FFFFFF),
                                inactiveContentColor = Color(0xFF94A3B8)
                            )
                        ) {
                            Text("Decimal Input", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Input Controls
                if (inputType == LooseInputType.DUAL && isLoose) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = primaryInput,
                            onValueChange = { primaryInput = it.filter { char -> char.isDigit() } },
                            label = { Text(primaryUnitLabel, color = Color(0xFF94A3B8), fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("loose_qty_primary_input")
                        )

                        OutlinedTextField(
                            value = secondaryInput,
                            onValueChange = { secondaryInput = it.filter { char -> char.isDigit() } },
                            label = { Text(secondaryUnitLabel, color = Color(0xFF94A3B8), fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("loose_qty_secondary_input")
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = decimalInput,
                        onValueChange = { decimalInput = it },
                        label = { Text("Quantity in ${product.unit} (e.g. 1.25)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loose_qty_decimal_input")
                    )
                }

                // Quick Presets Row
                if (isLoose) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Quick Quantity Presets:", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presets.take(4).forEach { (label, pVal, sVal) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x2210B981))
                                        .clickable {
                                            if (inputType == LooseInputType.DUAL) {
                                                primaryInput = pVal.toString()
                                                secondaryInput = sVal.toString()
                                            } else {
                                                val comp = pVal + (sVal / 1000.0)
                                                decimalInput = String.format(Locale.US, "%.3f", comp).trimEnd('0').trimEnd('.')
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(label, color = EmeraldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Automatic Price Calculation Display Engine
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x221E295D)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isExceedingStock) AccentPink else EmeraldGreen, RoundedCornerShape(12.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Total Loose Quantity:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                KiranaUnitUtils.formatQuantityWithUnit(currentQuantity, product.unit),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Exact Price Formula:", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", product.salePrice)} × ${String.format(Locale.US, "%.3f", currentQuantity).trimEnd('0').trimEnd('.')}",
                                color = GoldYellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Calculated Total Price:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(
                                "₹${String.format(Locale.US, "%.2f", currentTotalAmount)}",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                        }

                        if (isExceedingStock) {
                            Text(
                                text = "⚠ Exceeds available stock (${KiranaUnitUtils.formatQuantityWithUnit(product.stockQuantity, product.unit)})",
                                color = AccentPink,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentQuantity > 0 && !isExceedingStock) {
                        onConfirm(currentQuantity)
                    }
                },
                enabled = currentQuantity > 0 && !isExceedingStock,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                modifier = Modifier.testTag("loose_qty_confirm_button")
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add to Bill", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("loose_qty_cancel_button")
            ) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.border(1.dp, Color(0x3310B981), RoundedCornerShape(20.dp))
    )
}
