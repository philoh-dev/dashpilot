import Foundation

// Standalone model regression test; compile alongside the dashboard models and
// import bridge/bridge_ios.h. No iOS simulator or Rive runtime required.
@main
enum RiveThemeTests {
    static func darkMode(_ values: [(String, DashValue)]) -> Bool {
        guard let entry = values.first(where: { $0.0 == "darkMode" }),
              case .bool(let value) = entry.1 else {
            fatalError("Missing Boolean darkMode property")
        }
        return value
    }

    static func main() {
        for stored in [false, true] {
            var state = DashState()
            state.displaySettings.darkMode = stored
            state.carState.egoSpeed = 72
            state.carState.accSetSpeed = 80
            assert(darkMode(state.dashValues()) == stored)
            // Reuse the same telemetry while the system changes appearance.
            for system in [false, true, false] {
                let values = state.dashValues(systemDarkMode: system)
                assert(darkMode(values) == system)
                let baseline = state.dashValues().filter { $0.0 != "darkMode" }
                let actual = values.filter { $0.0 != "darkMode" }
                assert(baseline.count == actual.count)
                for ((name, expected), (actualName, value)) in zip(baseline, actual) {
                    assert(name == actualName)
                    switch (expected, value) {
                    case (.bool(let a), .bool(let b)): assert(a == b)
                    case (.number(let a), .number(let b)): assert(a == b)
                    case (.string(let a), .string(let b)): assert(a == b)
                    default: fatalError("Property type changed: \(name)")
                    }
                }
            }
        }
        print("Rive system-theme mapping tests passed")
    }
}
