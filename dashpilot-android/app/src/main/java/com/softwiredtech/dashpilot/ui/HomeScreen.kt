package com.softwiredtech.dashpilot.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softwiredtech.dashpilot.BuildConfig
import com.softwiredtech.dashpilot.ble.TeslaStatus
import com.softwiredtech.dashpilot.datamodel.dash.CarState
import com.softwiredtech.dashpilot.datamodel.dash.DashState
import com.softwiredtech.dashpilot.datasource.ConnectionStatus
import com.softwiredtech.dashpilot.datasource.DashKitBleManager
import com.softwiredtech.dashpilot.datasource.DataSourceType
import com.softwiredtech.dashpilot.ui.controls.ControlAction
import com.softwiredtech.dashpilot.ui.controls.controlById
import com.softwiredtech.dashpilot.ui.tesla.TeslaTile
import com.softwiredtech.dashpilot.ui.theme.AccentColor
import com.softwiredtech.dashpilot.ui.theme.DarkColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import com.softwiredtech.dashpilot.R

@Composable
fun HomeScreen(
    connectionStatus: ConnectionStatus,
    activeSourceType: String?,
    bleManager: DashKitBleManager?,
    dashState: StateFlow<DashState>?,
    pinnedControlIds: List<String>,
    teslaStatus: StateFlow<TeslaStatus>?,
    teslaResetPending: StateFlow<Boolean>?,
    onEnrollTesla: () -> Unit,
    onConnect: (serverAddress: String, dataSourceType: String) -> Unit,
    onDisconnect: () -> Unit,
    onNext: () -> Unit,
    onBattery: () -> Unit,
    onClimate: () -> Unit,
    onDrive: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var selectedDataSource by rememberSaveable { mutableStateOf(DataSourceType.DASHKIT) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkColors.Background)
            .systemBarsPadding()
    ) {
        if (selectedDataSource == DataSourceType.DASHKIT) {
            ConnectedHomeContent(
                dashState = dashState,
                bleManager = bleManager,
                connectionStatus = connectionStatus,
                activeSourceType = activeSourceType,
                pinnedControlIds = pinnedControlIds,
                teslaStatus = teslaStatus,
                teslaResetPending = teslaResetPending,
                onEnrollTesla = onEnrollTesla,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onSelectDataSource = { selectedDataSource = it },
                onBattery = onBattery,
                onClimate = onClimate,
                onDrive = onDrive,
                onSettingsClick = onSettingsClick
            )
        } else {
            SetupScreen(
                connectionStatus = connectionStatus,
                selectedDataSource = selectedDataSource,
                onSelectDataSource = { selectedDataSource = it },
                onConnect = onConnect,
                onDisconnect = onDisconnect,
                onNext = onNext,
                onSettingsClick = onSettingsClick
            )
        }
    }
}

private val ScreenPadding = 24.dp
private val GridGap = 16.dp

@Composable
private fun ConnectedHomeContent(
    dashState: StateFlow<DashState>?,
    bleManager: DashKitBleManager?,
    connectionStatus: ConnectionStatus,
    activeSourceType: String?,
    pinnedControlIds: List<String>,
    teslaStatus: StateFlow<TeslaStatus>?,
    teslaResetPending: StateFlow<Boolean>?,
    onEnrollTesla: () -> Unit,
    onConnect: (serverAddress: String, dataSourceType: String) -> Unit,
    onDisconnect: () -> Unit,
    onSelectDataSource: (String) -> Unit,
    onBattery: () -> Unit,
    onClimate: () -> Unit,
    onDrive: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val idleDash = remember { MutableStateFlow(DashState()) }
    val state by (dashState ?: idleDash).collectAsState()
    val car = state.carState
    val useImperial = state.displaySettings.useImperial
    val idleTesla = remember { MutableStateFlow(TeslaStatus.Idle) }
    val tesla by (teslaStatus ?: idleTesla).collectAsState()
    val idleReset = remember { MutableStateFlow(false) }
    val resetPending by (teslaResetPending ?: idleReset).collectAsState()
    val pinned = pinnedControlIds.mapNotNull(::controlById)
    val driveEnabled = connectionStatus == ConnectionStatus.Connected

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val verticalPadding = if (landscape) 12.dp else ScreenPadding
        val contentMinHeight = maxHeight - verticalPadding * 2

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (landscape) Modifier else Modifier.verticalScroll(rememberScrollState()))
                .padding(horizontal = ScreenPadding, vertical = verticalPadding)
        ) {
            if (landscape) {
                LandscapeContent(
                    minHeight = contentMinHeight,
                    car = car,
                    useImperial = useImperial,
                    bleManager = bleManager,
                    pinned = pinned,
                    tesla = tesla,
                    resetPending = resetPending,
                    onEnrollTesla = onEnrollTesla,
                    onBattery = onBattery,
                    onClimate = onClimate,
                    onDrive = onDrive,
                    driveEnabled = driveEnabled,
                    connectionStatus = connectionStatus,
                    dataSource = {
                        DebugDataSourceMenu(connectionStatus, activeSourceType, onSelectDataSource, onConnect, onDisconnect)
                    }
                )
            } else {
                PortraitContent(
                    car = car,
                    useImperial = useImperial,
                    bleManager = bleManager,
                    pinned = pinned,
                    tesla = tesla,
                    resetPending = resetPending,
                    connectionStatus = connectionStatus,
                    activeSourceType = activeSourceType,
                    onEnrollTesla = onEnrollTesla,
                    onConnect = onConnect,
                    onDisconnect = onDisconnect,
                    onSelectDataSource = onSelectDataSource,
                    onBattery = onBattery,
                    onClimate = onClimate,
                    onDrive = onDrive,
                    driveEnabled = driveEnabled
                )
            }
        }
    }
}

@Composable
private fun PortraitContent(
    car: CarState,
    useImperial: Boolean,
    bleManager: DashKitBleManager?,
    pinned: List<ControlAction>,
    tesla: TeslaStatus,
    resetPending: Boolean,
    connectionStatus: ConnectionStatus,
    activeSourceType: String?,
    onEnrollTesla: () -> Unit,
    onConnect: (serverAddress: String, dataSourceType: String) -> Unit,
    onDisconnect: () -> Unit,
    onSelectDataSource: (String) -> Unit,
    onBattery: () -> Unit,
    onClimate: () -> Unit,
    onDrive: () -> Unit,
    driveEnabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WidgetGrid(car = car, useImperial = useImperial, fillHeight = false, onBattery = onBattery, onClimate = onClimate)

        Spacer(modifier = Modifier.height(32.dp))

        ConnectionPanel(pinned, bleManager, tesla, resetPending, onEnrollTesla,
            connectionStatus, onDrive, driveEnabled, Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            DebugDataSourceMenu(connectionStatus, activeSourceType, onSelectDataSource, onConnect, onDisconnect)
        }
    }
}

@Composable
private fun LandscapeContent(
    minHeight: Dp,
    car: CarState,
    useImperial: Boolean,
    bleManager: DashKitBleManager?,
    pinned: List<ControlAction>,
    tesla: TeslaStatus,
    resetPending: Boolean,
    onEnrollTesla: () -> Unit,
    onBattery: () -> Unit,
    onClimate: () -> Unit,
    onDrive: () -> Unit,
    driveEnabled: Boolean,
    connectionStatus: ConnectionStatus,
    dataSource: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(minHeight.coerceAtLeast(0.dp)),
        horizontalArrangement = Arrangement.spacedBy(ScreenPadding)
    ) {
        WidgetGrid(
            car = car,
            useImperial = useImperial,
            fillHeight = true,
            onBattery = onBattery,
            onClimate = onClimate,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        )
        ConnectionPanel(pinned, bleManager, tesla, resetPending, onEnrollTesla,
            connectionStatus, onDrive, driveEnabled,
            Modifier.weight(0.6f).fillMaxHeight(), bounded = true, dataSource = dataSource)
    }
}

@Composable
private fun WidgetGrid(
    car: CarState,
    useImperial: Boolean,
    fillHeight: Boolean,
    onBattery: () -> Unit,
    onClimate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(GridGap)
    ) {
        val rowModifier = if (fillHeight) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth()
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(GridGap)
        ) {
            InfoWidget(
                icon = Icons.Rounded.BatteryChargingFull,
                label = stringResource(R.string.home_widget_battery),
                value = socText(car),
                fillHeight = fillHeight,
                modifier = Modifier.weight(1f),
                onClick = onBattery
            )
            InfoWidget(
                icon = Icons.Rounded.DeviceThermostat,
                label = stringResource(R.string.home_widget_battery_temp),
                value = batteryTempText(car),
                fillHeight = fillHeight,
                modifier = Modifier.weight(1f),
                onClick = onBattery
            )
        }
        Row(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(GridGap)
        ) {
            InfoWidget(
                icon = Icons.Rounded.AcUnit,
                label = stringResource(R.string.home_widget_ac_temp),
                value = tempText(car.acTemp),
                fillHeight = fillHeight,
                modifier = Modifier.weight(1f),
                onClick = onClimate
            )
            InfoWidget(
                icon = Icons.Rounded.Speed,
                label = stringResource(R.string.home_widget_odometer),
                value = odometerText(car.odometer, useImperial),
                fillHeight = fillHeight,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ConnectionPanel(
    pinned: List<ControlAction>, bleManager: DashKitBleManager?, tesla: TeslaStatus,
    resetPending: Boolean, onEnrollTesla: () -> Unit, connectionStatus: ConnectionStatus,
    onDrive: () -> Unit, driveEnabled: Boolean, modifier: Modifier, bounded: Boolean = false,
    dataSource: (@Composable () -> Unit)? = null
) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(DarkColors.Surface).padding(16.dp)) {
        if (BuildConfig.DEBUG) {
            TeslaTile(status = tesla, resetPending = resetPending, onEnroll = onEnrollTesla, embedded = true)
        } else {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(if (driveEnabled) AccentColor else DarkColors.Disabled, RoundedCornerShape(4.dp)))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.tesla_label), color = Color.White, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text(stringResource(when (connectionStatus) {
                    ConnectionStatus.Connected -> R.string.settings_firmware_info_connected
                    ConnectionStatus.Connecting -> R.string.settings_firmware_connecting
                    else -> R.string.settings_firmware_info_disconnected
                }),
                    color = DarkColors.TextMuted, fontSize = 13.sp)
            }
        }
        if (pinned.isNotEmpty() || dataSource != null) {
            HorizontalDivider(color = DarkColors.Border)
            Row(Modifier.fillMaxWidth().padding(vertical = if (dataSource == null) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically) {
                if (pinned.isNotEmpty()) {
                    Text(stringResource(R.string.home_pinned), color = Color.White, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                } else Spacer(Modifier.weight(1f))
                dataSource?.invoke()
            }
        }
        if (pinned.isNotEmpty()) {
            PinnedGrid(pinned, bleManager,
                if (bounded) Modifier.weight(1f) else Modifier.heightIn(max = 208.dp))
            Spacer(Modifier.height(12.dp))
        } else if (bounded) {
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.height(12.dp))
        }
        DriveButton(onDrive, driveEnabled)
    }
}

@Composable
private fun PinnedGrid(pinned: List<ControlAction>, bleManager: DashKitBleManager?, modifier: Modifier) {
    val rows = pinned.chunked(2)
    val scroll = rememberLazyListState()
    Box(modifier.fillMaxWidth()) {
        LazyColumn(state = scroll, modifier = Modifier.fillMaxWidth().heightIn(max = 208.dp).padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { action -> PinnedTile(action, bleManager, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Canvas(Modifier.matchParentSize()) {
            val viewport = scroll.layoutInfo.viewportEndOffset - scroll.layoutInfo.viewportStartOffset
            val total = (rows.size * 72.dp.toPx() - 8.dp.toPx()).coerceAtLeast(1f)
            if (viewport > 0 && total > viewport) {
                val trackHeight = viewport.toFloat()
                val thumb = trackHeight * viewport / total
                val offset = scroll.firstVisibleItemIndex * 72.dp.toPx() + scroll.firstVisibleItemScrollOffset
                val width = 3.dp.toPx()
                drawRoundRect(DarkColors.Border, topLeft = Offset(size.width - width, 0f), size = Size(width, trackHeight), cornerRadius = CornerRadius(width))
                drawRoundRect(DarkColors.TextMuted, topLeft = Offset(size.width - width, (trackHeight - thumb) * (offset / (total - viewport)).coerceIn(0f, 1f)),
                    size = Size(width, thumb), cornerRadius = CornerRadius(width))
            }
        }
    }
}

@Composable
private fun PinnedTile(action: ControlAction, bleManager: DashKitBleManager?, modifier: Modifier) {
    val enabled = bleManager != null
    val active = action.active()
    Row(modifier.height(64.dp).clip(RoundedCornerShape(12.dp))
        .background(if (active) AccentColor.copy(alpha = 0.25f) else DarkColors.SurfaceSelected)
        .clickable(enabled = enabled) { action.perform(bleManager) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconChip(action.icon, if (enabled) AccentColor else DarkColors.TextMuted, AccentColor.copy(alpha = 0.12f))
        Text(stringResource(action.labelRes()), color = if (enabled) Color.White else DarkColors.ContentDisabled,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.PushPin, contentDescription = stringResource(R.string.control_pinned_cd),
            tint = DarkColors.TextMuted, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun DriveButton(onDrive: () -> Unit, enabled: Boolean) {
    ActionButton(
        label = stringResource(R.string.drive_start),
        icon = Icons.Rounded.DirectionsCar,
        accent = true,
        onClick = onDrive,
        enabled = enabled
    )
}

@Composable
private fun InfoWidget(
    icon: ImageVector,
    label: String,
    value: String,
    fillHeight: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier.aspectRatio(1.4f))
            .background(DarkColors.Surface, RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(if (fillHeight) 12.dp else 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        IconChip(icon = icon, tint = AccentColor, background = AccentColor.copy(alpha = 0.14f))
        Column {
            Text(
                text = value,
                color = Color.White,
                fontSize = if (fillHeight) 22.sp else 26.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = label,
                color = DarkColors.TextMuted,
                fontSize = if (fillHeight) 12.sp else 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun IconChip(icon: ImageVector, tint: Color, background: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(background, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    accent: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val containerColor = if (accent) AccentColor else DarkColors.Surface
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun socText(car: CarState): String {
    val fullPack = car.fullPackEnergy
    val remaining = car.nominalEnergyRemaining
    val buffer = car.energyBuffer
    if (fullPack <= 0f || remaining <= 0f || (fullPack - buffer) <= 0f) return "—"
    val soc = ((remaining - buffer) / (fullPack - buffer) * 100f).roundToInt()
    return "$soc%"
}

private fun tempText(temp: Float): String {
    if (temp == 0f) return "—"
    return "${temp.roundToInt()}°"
}

private fun batteryTempText(car: CarState): String {
    val tMin = car.packTMin
    val tMax = car.packTMax
    if (tMin == 0f && tMax == 0f) return "—"
    val avg = (tMin + tMax) / 2f
    return "${avg.roundToInt()}°"
}

private fun odometerText(odometer: Float, useImperial: Boolean): String {
    if (odometer <= 0f) return "—"
    val unit = if (useImperial) "mi" else "km"
    return "${odometer.roundToInt()} $unit"
}
