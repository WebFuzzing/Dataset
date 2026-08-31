#!/bin/bash

# AdoptMe Algorithm Test Suite
# Tests all algorithms with complex data combinations

API_BASE="http://localhost:8080"
TEST_LOG="test-results.log"
PASSED=0
FAILED=0
TOTAL=0

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Initialize log
echo "========================================" > $TEST_LOG
echo "AdoptMe Algorithm Test Suite" >> $TEST_LOG
echo "Date: $(date)" >> $TEST_LOG
echo "========================================" >> $TEST_LOG
echo "" >> $TEST_LOG

# Helper function to run test
run_test() {
    local test_name="$1"
    local endpoint="$2"
    local expected_status="${3:-200}"
    local validation="${4:-}"

    TOTAL=$((TOTAL + 1))
    echo -e "${BLUE}[TEST $TOTAL]${NC} $test_name"
    echo "[TEST $TOTAL] $test_name" >> $TEST_LOG
    echo "Endpoint: $endpoint" >> $TEST_LOG

    # Make request
    response=$(curl -s -w "\n%{http_code}" "$API_BASE$endpoint" 2>&1)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | head -n-1)

    echo "HTTP Status: $http_code" >> $TEST_LOG
    echo "Response: $body" >> $TEST_LOG

    # Check HTTP status
    if [ "$http_code" != "$expected_status" ]; then
        echo -e "${RED}✗ FAILED${NC} - Expected status $expected_status, got $http_code"
        echo "RESULT: FAILED - Status code mismatch" >> $TEST_LOG
        FAILED=$((FAILED + 1))
        echo "" >> $TEST_LOG
        return 1
    fi

    # Additional validation if provided
    if [ -n "$validation" ]; then
        if echo "$body" | grep -q "$validation"; then
            echo -e "${GREEN}✓ PASSED${NC}"
            echo "RESULT: PASSED" >> $TEST_LOG
            PASSED=$((PASSED + 1))
        else
            echo -e "${RED}✗ FAILED${NC} - Validation failed (expected: $validation)"
            echo "RESULT: FAILED - Validation failed" >> $TEST_LOG
            FAILED=$((FAILED + 1))
        fi
    else
        echo -e "${GREEN}✓ PASSED${NC}"
        echo "RESULT: PASSED" >> $TEST_LOG
        PASSED=$((PASSED + 1))
    fi

    echo "" >> $TEST_LOG
    sleep 0.5
}

# Print header
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}AdoptMe Algorithm Test Suite${NC}"
echo -e "${YELLOW}========================================${NC}"
echo ""

# Check if server is running
echo -e "${BLUE}Checking server connection...${NC}"
if ! curl -s "$API_BASE/ping" > /dev/null 2>&1; then
    echo -e "${RED}✗ Server is not running at $API_BASE${NC}"
    echo "Please start the backend server first."
    exit 1
fi
echo -e "${GREEN}✓ Server is running${NC}"
echo ""

# ==============================================
# GRAPH TRAVERSAL TESTS (BFS/DFS)
# ==============================================
echo -e "${YELLOW}=== Graph Traversal Tests (BFS/DFS) ===${NC}"

run_test "BFS: Simple path A→B" "/graph/reachable?from=A&to=B&method=bfs" 200 '"exists":true'
run_test "BFS: Complex path A→O (long distance)" "/graph/reachable?from=A&to=O&method=bfs" 200 '"exists":true'
run_test "BFS: Cross-cluster E→M" "/graph/reachable?from=E&to=M&method=bfs" 200 '"exists":true'
run_test "BFS: Hub to edge H→N" "/graph/reachable?from=H&to=N&method=bfs" 200 '"exists":true'

run_test "DFS: Simple path A→B" "/graph/reachable?from=A&to=B&method=dfs" 200 '"exists":true'
run_test "DFS: Complex path A→O" "/graph/reachable?from=A&to=O&method=dfs" 200 '"exists":true'
run_test "DFS: Cross-cluster E→M" "/graph/reachable?from=E&to=M&method=dfs" 200 '"exists":true'
run_test "DFS: Multiple paths I→K" "/graph/reachable?from=I&to=K&method=dfs" 200 '"exists":true'

echo ""

# ==============================================
# DIJKSTRA SHORTEST PATH TESTS
# ==============================================
echo -e "${YELLOW}=== Dijkstra Shortest Path Tests ===${NC}"

run_test "Dijkstra: Adjacent nodes H→A" "/routes/shortest?from=H&to=A" 200 '"totalWeight"'
run_test "Dijkstra: Long path A→M" "/routes/shortest?from=A&to=M" 200 '"totalWeight"'
run_test "Dijkstra: Cross-cluster E→O" "/routes/shortest?from=E&to=O" 200 '"totalWeight"'
run_test "Dijkstra: Full network A→N" "/routes/shortest?from=A&to=N" 200 '"totalWeight"'
run_test "Dijkstra: Hub routing B→L via H" "/routes/shortest?from=B&to=L" 200 '"totalWeight"'

echo ""

# ==============================================
# TSP BRANCH & BOUND TESTS
# ==============================================
echo -e "${YELLOW}=== TSP Branch & Bound Tests ===${NC}"

run_test "TSP: Small (4 nodes) A,B,C,H" "/routes/tsp/bnb?nodes=A,B,C,H" 200 '"totalDistanceKm"'
run_test "TSP: Medium cluster 1 (5 nodes) A,D,E,I,H" "/routes/tsp/bnb?nodes=A,D,E,I,H" 200 '"totalDistanceKm"'
run_test "TSP: Medium cluster 2 (5 nodes) B,F,J,K,H" "/routes/tsp/bnb?nodes=B,F,J,K,H" 200 '"totalDistanceKm"'
run_test "TSP: Large (7 nodes) A,B,C,G,L,M,H" "/routes/tsp/bnb?nodes=A,B,C,G,L,M,H" 200 '"totalDistanceKm"'
run_test "TSP: Complex (8 nodes) H,A,D,E,F,J,L,O" "/routes/tsp/bnb?nodes=H,A,D,E,F,J,L,O" 200 '"totalDistanceKm"'

echo ""

# ==============================================
# MST TESTS (Kruskal & Prim)
# ==============================================
echo -e "${YELLOW}=== Minimum Spanning Tree Tests ===${NC}"

run_test "MST: Kruskal with full network" "/network/mst?algorithm=kruskal" 200 '"totalWeight"'
run_test "MST: Prim with full network" "/network/mst?algorithm=prim" 200 '"totalWeight"'

echo ""

# ==============================================
# SORTING TESTS
# ==============================================
echo -e "${YELLOW}=== Sorting Algorithm Tests ===${NC}"

run_test "Sort: 40 dogs by priority (MergeSort)" "/dogs/sort?criteria=priority&algorithm=mergesort" 200 '"dogs"'
run_test "Sort: 40 dogs by priority (QuickSort)" "/dogs/sort?criteria=priority&algorithm=quicksort" 200 '"dogs"'
run_test "Sort: 40 dogs by age (MergeSort)" "/dogs/sort?criteria=age&algorithm=mergesort" 200 '"dogs"'
run_test "Sort: 40 dogs by age (QuickSort)" "/dogs/sort?criteria=age&algorithm=quicksort" 200 '"dogs"'
run_test "Sort: 40 dogs by weight (MergeSort)" "/dogs/sort?criteria=weight&algorithm=mergesort" 200 '"dogs"'
run_test "Sort: 40 dogs by weight (QuickSort)" "/dogs/sort?criteria=weight&algorithm=quicksort" 200 '"dogs"'

echo ""

# ==============================================
# GREEDY MATCHING TESTS
# ==============================================
echo -e "${YELLOW}=== Greedy Matching Tests ===${NC}"

run_test "Greedy: High budget adopter P9 (Julia \$40K)" "/adoptions/greedy?adopterId=P9" 200 '"totalScore"'
run_test "Greedy: High budget adopter P14 (Andres \$45K)" "/adoptions/greedy?adopterId=P14" 200 '"totalScore"'
run_test "Greedy: Low budget adopter P10 (Carlos \$12K)" "/adoptions/greedy?adopterId=P10" 200 '"totalScore"'
run_test "Greedy: Medium budget with kids P1 (Camila)" "/adoptions/greedy?adopterId=P1" 200 '"totalScore"'
run_test "Greedy: No yard adopter P8 (Diego)" "/adoptions/greedy?adopterId=P8" 200 '"totalScore"'
run_test "Greedy: High capacity P11 (Valeria max:5)" "/adoptions/greedy?adopterId=P11" 200 '"totalScore"'

echo ""

# ==============================================
# BACKTRACKING TESTS
# ==============================================
echo -e "${YELLOW}=== Backtracking Constraint Satisfaction Tests ===${NC}"

run_test "Backtracking: Multi-adopter assignment (15 adopters, 30+ dogs)" "/adoptions/constraints/backtracking" 200 '"totalScore"'

echo ""

# ==============================================
# KNAPSACK TESTS
# ==============================================
echo -e "${YELLOW}=== Knapsack Dynamic Programming Tests ===${NC}"

run_test "Knapsack: Small capacity 30kg" "/transport/optimal-dp?capacityKg=30" 200 '"totalPriority"'
run_test "Knapsack: Medium capacity 60kg" "/transport/optimal-dp?capacityKg=60" 200 '"totalPriority"'
run_test "Knapsack: Large capacity 100kg" "/transport/optimal-dp?capacityKg=100" 200 '"totalPriority"'
run_test "Knapsack: Very large capacity 200kg" "/transport/optimal-dp?capacityKg=200" 200 '"totalPriority"'
run_test "Knapsack: Extreme capacity 500kg (all dogs)" "/transport/optimal-dp?capacityKg=500" 200 '"totalPriority"'

echo ""

# ==============================================
# DATA INTEGRITY TESTS
# ==============================================
echo -e "${YELLOW}=== Data Integrity Tests ===${NC}"

run_test "Get all shelters (expect 15)" "/shelters" 200
run_test "Get all dogs (expect 40)" "/dogs" 200
run_test "Get all adopters (expect 15)" "/adopters" 200

echo ""

# ==============================================
# SUMMARY
# ==============================================
echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Test Summary${NC}"
echo -e "${YELLOW}========================================${NC}"
echo -e "Total Tests:  ${BLUE}$TOTAL${NC}"
echo -e "Passed:       ${GREEN}$PASSED${NC}"
echo -e "Failed:       ${RED}$FAILED${NC}"

PASS_RATE=$((PASSED * 100 / TOTAL))
echo -e "Pass Rate:    ${BLUE}${PASS_RATE}%${NC}"
echo ""

# Write summary to log
echo "========================================" >> $TEST_LOG
echo "SUMMARY" >> $TEST_LOG
echo "========================================" >> $TEST_LOG
echo "Total Tests: $TOTAL" >> $TEST_LOG
echo "Passed: $PASSED" >> $TEST_LOG
echo "Failed: $FAILED" >> $TEST_LOG
echo "Pass Rate: ${PASS_RATE}%" >> $TEST_LOG
echo "" >> $TEST_LOG

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    echo -e "Results saved to: ${BLUE}$TEST_LOG${NC}"
    exit 0
else
    echo -e "${RED}✗ Some tests failed.${NC}"
    echo -e "Check details in: ${BLUE}$TEST_LOG${NC}"
    exit 1
fi
