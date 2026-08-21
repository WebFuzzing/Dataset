#!/bin/bash

# Generate index page for all Gatling reports
REPORT_DIR="./report"
INDEX_FILE="$REPORT_DIR/index.html"

# Create report directory if it doesn't exist
mkdir -p "$REPORT_DIR"

# Start HTML
cat > "$INDEX_FILE" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Jasper Load Test Reports</title>
    <style>
				:root {
					/* Light mode variables */
					--heading-color: #2c3e50;
					--border-color: #eee;
					--report-bg: #f8f9fa;
					--link-color: #3498db;
					--latest-bg: #e8f5e9;
					--latest-border: #4caf50;
					--text-color: #000;
					--fg-color: #fff;
					--bg-color: #eee;
				}
				@media (prefers-color-scheme: dark) {
					:root {
						color-scheme: dark;
						/* Dark mode variables */
						--heading-color: #89a7c3;
						--border-color: #333;
						--report-bg: #2a2a2a;
						--link-color: #36A;
						--latest-bg: #1b3320;
						--latest-border: #2d6a31;
						--text-color: #fff;
						--fg-color: #222;
						--bg-color: #111;
					}
				}
        body {
					font-family: system-ui, -apple-system, sans-serif;
					margin: 40px;
					line-height: 1.6;
					color: var(--text-color);
					background: var(--bg-color);
        }
        .container {
					max-width: 1200px;
					margin: 0 auto;
					background: var(--fg-color);
					padding: 30px;
					border-radius: 8px;
					box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 {
					color: var(--heading-color);
					border-bottom: 3px solid var(--border-color);
					padding-bottom: 10px;
        }
        h2 {
					color: var(--heading-color);
					margin-top: 30px;
        }
        .simulation-grid {
					display: grid;
					grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
					gap: 20px;
					margin: 20px 0;
        }
        .simulation-card {
					border: 1px solid var(--border-color);
					border-radius: 8px;
					padding: 20px;
					background: var(--report-bg);
					transition: box-shadow 0.3s ease;
        }
        .simulation-card:hover {
					box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }
        .simulation-title {
					font-size: 1.2em;
					font-weight: bold;
					color: var(--heading-color);
					margin-bottom: 10px;
        }
        .simulation-description {
					margin-bottom: 15px;
					line-height: 1.4;
        }
        .report-link {
					display: inline-block;
					padding: 10px 20px;
					background: rgba(0,0,0,0.1);
					color: var(--heading-color);
					text-decoration: none;
					border-radius: 5px;
					transition: background 0.3s ease;
        }
        .report-link:hover {
					background: var(--link-color);
        }
        .unavailable {
					color: #e74c3c;
					font-style: italic;
        }
        .footer {
					margin-top: 40px;
					padding-top: 20px;
					border-top: 1px solid var(--border-color);
					text-align: center;
					color: var(--heading-color);
        }
    </style>
    <script>
        function displayLocalTime() {
            const utcTime = document.getElementById('generation-time').dataset.utc;
            const date = new Date(utcTime);
            document.getElementById('generation-time').textContent = date.toLocaleString();
        }
    </script>
</head>
<body onload="displayLocalTime()">
    <div class="container">
        <h1>🚀 Jasper Knowledge Management Server - Load Test Reports</h1>

        <h2>📊 Test Scenarios</h2>
        <div class="simulation-grid">
EOF

# Define simulation information
declare -A simulations=(
    ["SmokeTestSimulation"]="Smoke Test|Quick validation of core functionality (45s, >75% success)"
    ["ComprehensiveSimulation"]="Comprehensive Test|Realistic knowledge management workflows (3min, >75% success)"
    ["UserJourneySimulation"]="User Journey Test|Real user journey patterns with research and collaboration (5min, >75% success)"
    ["StressTestSimulation"]="Stress Test|System limits and edge cases testing (4min, >70% success)"
    ["InfernoSimulation"]="Inferno Test|Extreme load test with warmup and 2 cycles of massive ramp (2min 15s, >15% success)"
)

# Check for reports and generate cards
for simulation in SmokeTestSimulation ComprehensiveSimulation UserJourneySimulation StressTestSimulation InfernoSimulation; do
    IFS='|' read -r title description <<< "${simulations[$simulation]}"
    # Find the most recent report directory for this simulation
    report_dir=$(find $REPORT_DIR -name "${simulation,,}*" -type d 2>/dev/null | head -1)
    cat >> "$INDEX_FILE" << EOF
            <div class="simulation-card">
                <div class="simulation-title">$title</div>
                <div class="simulation-description">$description</div>
EOF
    if [ -n "$report_dir" ] && [ -f "$report_dir/index.html" ]; then
        cat >> "$INDEX_FILE" << EOF
                <a href="$(basename $report_dir)/index.html" class="report-link">📈 View Report</a>
EOF
    else
        cat >> "$INDEX_FILE" << EOF
                <span class="unavailable">❌ Report not available</span>
EOF
    fi
    cat >> "$INDEX_FILE" << EOF
            </div>
EOF
done

# Complete HTML
cat >> "$INDEX_FILE" << EOF
        </div>

        <h2>📋 Test Summary</h2>
        <p>These load tests validate the Jasper Knowledge Management Server under various scenarios:</p>
        <ul>
            <li><strong>Simple:</strong> Basic smoke test for CI/CD validation</li>
            <li><strong>Comprehensive:</strong> Full workflow testing with realistic user patterns</li>
            <li><strong>User Journey:</strong> End-to-end user experience validation</li>
            <li><strong>Stress:</strong> Performance limits and concurrent user testing</li>
        </ul>

        <div class="footer">
            <p>Generated by Jasper Gatling Load Test Suite</p>
            <p id="generation-time" data-utc="$(date -u '+%Y-%m-%dT%H:%M:%SZ')">$(date -u '+%Y-%m-%d %H:%M:%S UTC')</p>
        </div>
    </div>
</body>
</html>
EOF

echo "✅ Gatling reports index generated at: $INDEX_FILE"

# List available reports
echo "📁 Available reports:"
find "$REPORT_DIR" -name "index.html" | sed 's|/report/||' | sort
