import SwiftUI

struct HomeView: View {
    @Binding var navigationPath: NavigationPath
    @Environment(ConnectionViewModel.self) var connectionVM

    @AppStorage("pinned_control_id") var pinnedControlId: String = ""
    @AppStorage("pinned_control_ids") private var pinnedSelection = PinnedControls.legacySelection
    // Read the session's cached snapshot immediately when returning to Drive.
    // A new per-view stream starts asynchronously and can flash empty values.
    private var dash: DashState? {
        connectionVM.connectionStatus == .disconnected ? nil : connectionVM.latestDashState
    }

    var body: some View {
        ZStack {
            Color.dashBackground
                .ignoresSafeArea()

            GeometryReader { geo in
                let landscape = geo.size.width > geo.size.height
                if landscape {
                    landscapeContent(
                        width: max(0, geo.size.width - 2 * DashMetrics.screenPadding),
                        height: max(0, geo.size.height - 24)
                    )
                    .padding(.horizontal, DashMetrics.screenPadding)
                    .padding(.vertical, 12)
                } else {
                    ScrollView {
                        VStack(spacing: 0) {
                            portraitContent
                        }
                        .padding(DashMetrics.screenPadding)
                    }
                }
            }
        }
        .navigationBarHidden(true)
    }

    private var portraitContent: some View {
        VStack(spacing: 0) {
            widgetGrid(fillHeight: false)
            Spacer().frame(height: 32)
            connectionPanel(gridHeight: pinnedGridHeight, bounded: false)
            Spacer().frame(height: 12)
            DataSourceMenu(navigationPath: $navigationPath)
        }
    }

    private func landscapeContent(width: CGFloat, height: CGFloat) -> some View {
        let availableWidth = max(0, width - DashMetrics.screenPadding)
        // Status, section label, gaps, panel padding, and the fixed dashboard button.
        let gridHeight = min(pinnedGridHeight, max(0, height - 220))
        return HStack(alignment: .top, spacing: DashMetrics.screenPadding) {
            widgetGrid(fillHeight: true)
                .frame(width: availableWidth * 0.4, height: height)
            connectionPanel(gridHeight: gridHeight, bounded: true)
                .frame(width: availableWidth * 0.6, height: height)
        }
    }

    private func connectionPanel(gridHeight: CGFloat, bounded: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Circle().fill(connectionVM.connectionStatus == .connected ? Color.dashAccent : Color.dashTextMuted)
                    .frame(width: 8, height: 8)
                Text(verbatim: "Tesla").font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                Spacer(minLength: 8)
                Text(connectionLabel).font(.system(size: 13)).foregroundColor(.dashTextMuted)
                    .lineLimit(1).minimumScaleFactor(0.8)
            }
            .frame(minHeight: 28)
            if !pinnedControls.isEmpty || bounded {
                Divider().overlay(Color.white.opacity(0.08))
                HStack(spacing: 8) {
                    if !pinnedControls.isEmpty {
                        Text("Pinned commands").font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
                            .layoutPriority(1)
                    }
                    if bounded {
                        DataSourceMenu(navigationPath: $navigationPath, alignment: .trailing)
                    }
                }
            }
            if !pinnedControls.isEmpty {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                        ForEach(pinnedControls) { control in pinnedTile(control) }
                    }
                    .padding(.trailing, 6)
                }
                .frame(height: gridHeight)
                .scrollIndicators(.visible)
                .scrollBounceBehavior(.basedOnSize)
            }
            if bounded { Spacer(minLength: 0) }
            driveButton
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: bounded ? .infinity : nil, alignment: .topLeading)
        .background(Color.dashSurface)
        .clipShape(RoundedRectangle(cornerRadius: DashMetrics.corner))
    }

    private var connectionLabel: LocalizedStringKey {
        switch connectionVM.connectionStatus {
        case .connected: return "Connected"
        case .connecting: return "Connecting…"
        case .disconnected, .error: return "Disconnected"
        }
    }

    private var pinnedControls: [ControlAction] {
        PinnedControls.decode(pinnedSelection, legacy: pinnedControlId).compactMap { controlById($0) }
    }

    private var pinnedGridHeight: CGFloat {
        let rows = min(3, (pinnedControls.count + 1) / 2)
        return CGFloat(rows * 64 + max(0, rows - 1) * 8)
    }

    private func pinnedTile(_ control: ControlAction) -> some View {
        let enabled = connectionVM.bleManager != nil
        return Button { control.perform(connectionVM.bleManager) } label: {
            HStack(spacing: 8) {
                IconChip(systemName: control.icon, tint: enabled ? .dashAccent : .dashTextMuted,
                         background: Color.dashAccent.opacity(0.12))
                Text(control.label()).font(.system(size: 13, weight: .semibold))
                    .foregroundColor(enabled ? .white : .dashTextMuted)
                    .lineLimit(2).frame(maxWidth: .infinity, alignment: .leading)
                Image(systemName: "pin.fill").font(.system(size: 10)).foregroundColor(.dashTextMuted)
                    .accessibilityHidden(true)
            }
            .padding(10)
            .frame(maxWidth: .infinity)
            .frame(height: 64)
            .background(control.active() ? Color.dashAccent.opacity(0.25) : Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
    }

    private func widgetGrid(fillHeight: Bool) -> some View {
        VStack(spacing: DashMetrics.gridGap) {
            HStack(spacing: DashMetrics.gridGap) {
                Button {
                    navigationPath.append(AppRoute.battery)
                } label: {
                    InfoWidget(
                        icon: "battery.100.bolt",
                        label: "home_widget_battery",
                        value: socText,
                        fillHeight: fillHeight
                    )
                }
                .buttonStyle(.plain)
                Button {
                    navigationPath.append(AppRoute.battery)
                } label: {
                    InfoWidget(
                        icon: "thermometer.medium",
                        label: "Battery Temp",
                        value: batteryTempText,
                        fillHeight: fillHeight
                    )
                }
                .buttonStyle(.plain)
            }
            HStack(spacing: DashMetrics.gridGap) {
                Button {
                    navigationPath.append(AppRoute.climate)
                } label: {
                    InfoWidget(
                        icon: "snowflake",
                        label: "AC Setpoint",
                        value: acTempText,
                        fillHeight: fillHeight
                    )
                }
                .buttonStyle(.plain)
                InfoWidget(
                    icon: "gauge.with.dots.needle.bottom.50percent",
                    label: "Odometer",
                    value: odometerText,
                    fillHeight: fillHeight
                )
            }
        }
    }


    private var driveButton: some View {
        ActionButton(
            label: "Open dashboard",
            icon: "car.fill",
            accent: true,
            enabled: connectionVM.connectionStatus == .connected
        ) {
            let dashboard = selectedDashboard()
            navigationPath.append(
                AppRoute.dashboard(type: dashboard.type.rawValue, url: dashboard.url)
            )
        }
    }

    private var socText: String {
        guard let car = dash?.carState else { return "—" }
        let fullPack = car.fullPackEnergy
        let remaining = car.nominalEnergyRemaining
        let buffer = car.energyBuffer
        guard fullPack > 0, remaining > 0, fullPack - buffer > 0 else { return "—" }
        let soc = Int(((remaining - buffer) / (fullPack - buffer) * 100).rounded())
        return "\(soc)%"
    }

    private var acTempText: String {
        guard let car = dash?.carState, car.acTemp != 0 else { return "—" }
        return "\(Int(car.acTemp.rounded()))°"
    }

    private var batteryTempText: String {
        guard let car = dash?.carState else { return "—" }
        let tMin = car.packTMin
        let tMax = car.packTMax
        guard tMin != 0 || tMax != 0 else { return "—" }
        let avg = (tMin + tMax) / 2
        return "\(Int(avg.rounded()))°"
    }

    private var odometerText: String {
        guard let state = dash, state.carState.odometer > 0 else { return "—" }
        let unit = state.displaySettings.useImperial ? "mi" : "km"
        return "\(Int(state.carState.odometer.rounded())) \(unit)"
    }
}

// Separate view so 25 Hz dash updates don't rebuild the open Menu (taps get dropped).
private struct DataSourceMenu: View {
    @Binding var navigationPath: NavigationPath
    var alignment: Alignment = .center
    @Environment(ConnectionViewModel.self) var connectionVM

    var body: some View {
        VStack(spacing: 4) {
            Menu {
                if isIdle {
                    Button("comma") { navigationPath.append(AppRoute.setup) }
                    Button("DashKit") { connectionVM.connectDashKit() }
                    Button("Demo") { connectionVM.connectDemo() }
                } else {
                    Button("Disconnect") { connectionVM.disconnect() }
                }
            } label: {
                HStack(spacing: 4) {
                    Text(dataSourceLabel)
                        .foregroundColor(.dashTextMuted)
                        .font(.system(size: 13))
                    Image(systemName: "arrowtriangle.down.fill")
                        .font(.system(size: 9))
                        .foregroundColor(.dashTextMuted)
                }
                .padding(.horizontal, 12)
                .frame(height: 36)
            }
            if case .error(let message) = connectionVM.connectionStatus {
                Text(message)
                    .foregroundColor(.dashError)
                    .font(.system(size: 12))
                    .multilineTextAlignment(.center)
            }
        }
        .frame(maxWidth: .infinity, alignment: alignment)
    }

    private var isIdle: Bool {
        switch connectionVM.connectionStatus {
        case .disconnected, .error: return true
        case .connecting, .connected: return false
        }
    }

    private var dataSourceLabel: LocalizedStringKey {
        let name = connectionVM.activeSourceType?.displayName
            ?? String(localized: "Data source")
        switch connectionVM.connectionStatus {
        case .connected: return "\(name) connected"
        case .connecting: return "Connecting to \(name)…"
        case .disconnected, .error: return "Data source"
        }
    }
}

private struct InfoWidget: View {
    let icon: String
    let label: LocalizedStringKey
    let value: String
    var fillHeight: Bool = false

    var body: some View {
        if fillHeight {
            card
        } else {
            card.aspectRatio(1.4, contentMode: .fit)
        }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: 0) {
            IconChip(
                systemName: icon,
                tint: .dashAccent,
                background: Color.dashAccent.opacity(0.14)
            )
            Spacer(minLength: 0)
            VStack(alignment: .leading, spacing: 2) {
                Text(value)
                    .foregroundColor(.white)
                    .font(.system(size: fillHeight ? 22 : 26, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.6)
                Text(label)
                    .foregroundColor(.dashTextMuted)
                    .font(.system(size: fillHeight ? 12 : 13))
                    .lineLimit(2)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .padding(fillHeight ? 12 : 16)
        .background(Color.dashSurface)
        .clipShape(RoundedRectangle(cornerRadius: DashMetrics.corner))
    }
}

private struct ActionButton: View {
    let label: LocalizedStringKey
    let icon: String
    var accent: Bool = false
    var enabled: Bool = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                Text(label)
                    .font(.system(size: 16, weight: .semibold))
            }
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: DashMetrics.buttonHeight)
        }
        .background(accent ? Color.dashAccent : Color.dashSurface)
        .clipShape(RoundedRectangle(cornerRadius: DashMetrics.corner))
        .disabled(!enabled)
        .opacity(enabled ? 1.0 : 0.5)
    }
}
