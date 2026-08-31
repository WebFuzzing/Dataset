// API Configuration
const API_BASE_URL = 'http://localhost:8080';

// Global State
let appData = {
    refugios: [],
    perros: [],
    adoptantes: []
};

// ==================== Initialization ====================

document.addEventListener('DOMContentLoaded', () => {
    initializeTabs();
    checkConnection();
    loadDashboardData();
    populateSelectors();
});

// ==================== Tab Navigation ====================

function initializeTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');

    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            const tabId = button.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    // Hide all tab contents
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    // Remove active class from all buttons
    document.querySelectorAll('.tab-button').forEach(button => {
        button.classList.remove('active');
    });

    // Show selected tab content
    document.getElementById(tabId).classList.add('active');

    // Add active class to selected button
    document.querySelector(`[data-tab="${tabId}"]`).classList.add('active');
}

// ==================== Connection Check ====================

async function checkConnection() {
    const statusIndicator = document.querySelector('.status-indicator');
    const statusText = document.querySelector('.status-text');

    try {
        const response = await fetch(`${API_BASE_URL}/ping`, { timeout: 5000 });

        if (response.ok) {
            statusIndicator.classList.add('connected');
            statusText.textContent = 'Conectado';
        } else {
            throw new Error('Connection failed');
        }
    } catch (error) {
        statusIndicator.classList.add('error');
        statusText.textContent = 'Error de conexión';
        console.error('Connection error:', error);
    }
}

// ==================== Dashboard Data Loading ====================

async function loadDashboardData() {
    try {
        showLoading();

        // Load all data in parallel
        const [refugios, perros, adoptantes] = await Promise.all([
            fetchAPI('/shelters'),
            fetchAPI('/dogs'),
            fetchAPI('/adopters')
        ]);

        appData.refugios = refugios;
        appData.perros = perros;
        appData.adoptantes = adoptantes;

        // Update stats
        document.getElementById('statsRefugios').textContent = refugios.length;
        document.getElementById('statsPerros').textContent = perros.length;
        document.getElementById('statsAdoptantes').textContent = adoptantes.length;

        // Display data
        displayRefugios(refugios);
        displayPerros(perros);
        displayAdoptantes(adoptantes);

        hideLoading();
    } catch (error) {
        hideLoading();
        console.error('Error loading dashboard data:', error);
        showError('Error al cargar los datos del dashboard');
    }
}

function displayRefugios(refugios) {
    const container = document.getElementById('refugiosList');

    if (refugios.length === 0) {
        container.innerHTML = '<p>No hay refugios disponibles</p>';
        return;
    }

    container.innerHTML = refugios.map(refugio => `
        <div class="data-item">
            <strong>${refugio.name || refugio.id}</strong>
            <p>Capacidad: ${refugio.capacity || 'N/A'} perros</p>
        </div>
    `).join('');
}

function displayPerros(perros) {
    const container = document.getElementById('perrosList');

    if (perros.length === 0) {
        container.innerHTML = '<p>No hay perros disponibles</p>';
        return;
    }

    container.innerHTML = perros.map(perro => `
        <div class="data-item">
            <strong>${perro.name}</strong>
            <p>
                Tamaño: ${perro.size} |
                Peso: ${perro.weight}kg |
                Edad: ${perro.age} años |
                Prioridad: ${perro.priority || 'N/A'}
            </p>
            <p>Energía: ${perro.energy} | Amigable con niños: ${perro.goodWithKids ? 'Sí' : 'No'}</p>
        </div>
    `).join('');
}

function displayAdoptantes(adoptantes) {
    const container = document.getElementById('adoptantesList');

    if (adoptantes.length === 0) {
        container.innerHTML = '<p>No hay adoptantes registrados</p>';
        return;
    }

    container.innerHTML = adoptantes.map(adoptante => `
        <div class="data-item">
            <strong>${adoptante.name}</strong>
            <p>
                Presupuesto: $${adoptante.budget} |
                Max. perros: ${adoptante.maxDogs}
            </p>
            <p>
                Patio: ${adoptante.hasYard ? 'Sí' : 'No'} |
                Niños: ${adoptante.hasKids ? 'Sí' : 'No'}
            </p>
        </div>
    `).join('');
}

// ==================== Graph Algorithms ====================

async function executeBFS() {
    const from = document.getElementById('bfsFrom').value;
    const to = document.getElementById('bfsTo').value;
    const resultBox = document.getElementById('bfsResult');

    if (from === to) {
        showResult(resultBox, 'error', 'Error', 'El origen y destino deben ser diferentes');
        return;
    }

    try {
        showLoading();
        const result = await fetchAPI(`/graph/reachable?from=${from}&to=${to}&method=bfs`);
        hideLoading();

        displayPathResult(resultBox, result, 'BFS');
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

async function executeDFS() {
    const from = document.getElementById('bfsFrom').value;
    const to = document.getElementById('bfsTo').value;
    const resultBox = document.getElementById('bfsResult');

    if (from === to) {
        showResult(resultBox, 'error', 'Error', 'El origen y destino deben ser diferentes');
        return;
    }

    try {
        showLoading();
        const result = await fetchAPI(`/graph/reachable?from=${from}&to=${to}&method=dfs`);
        hideLoading();

        displayPathResult(resultBox, result, 'DFS');
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

async function executeDijkstra() {
    const from = document.getElementById('dijkstraFrom').value;
    const to = document.getElementById('dijkstraTo').value;
    const resultBox = document.getElementById('dijkstraResult');

    if (from === to) {
        showResult(resultBox, 'error', 'Error', 'El origen y destino deben ser diferentes');
        return;
    }

    try {
        showLoading();
        const result = await fetchAPI(`/routes/shortest?from=${from}&to=${to}`);
        hideLoading();

        let html = '<h4>✅ Camino Más Corto Encontrado (Dijkstra)</h4>';
        html += `<div class="result-item">
            <strong>Camino:</strong> ${result.path.join(' → ')}<br>
            <strong>Distancia Total:</strong> ${result.totalWeight} km<br>
            <strong>Número de Saltos:</strong> ${result.steps}
        </div>`;

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

async function executeTSP() {
    const checkboxes = document.querySelectorAll('input[name="tspNode"]:checked');
    const nodes = Array.from(checkboxes).map(cb => cb.value);
    const resultBox = document.getElementById('tspResult');

    if (nodes.length < 2) {
        showResult(resultBox, 'error', 'Error', 'Debes seleccionar al menos 2 refugios');
        return;
    }

    try {
        showLoading();
        const result = await fetchAPI(`/routes/tsp/bnb?nodes=${nodes.join(',')}`);
        hideLoading();

        let html = '<h4>✅ Ruta Óptima TSP Encontrada (Branch & Bound)</h4>';
        html += `<div class="result-item">
            <strong>Ruta Óptima:</strong> ${result.route ? result.route.join(' → ') : 'N/A'}<br>
            <strong>Distancia Total:</strong> ${result.totalDistanceKm || 'N/A'} km<br>
            <strong>Número de Refugios:</strong> ${result.route ? result.route.length : 0}<br>
            <strong>Nodos Explorados:</strong> ${result.nodesExplored || 'N/A'}
        </div>`;

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

function displayPathResult(resultBox, result, algorithm) {
    if (result.exists) {
        let html = `<h4>✅ Camino Encontrado (${algorithm})</h4>`;
        html += `<div class="result-item">
            <strong>Camino:</strong> ${result.path.join(' → ')}<br>
            <strong>Número de Saltos:</strong> ${result.steps}
        </div>`;
        showResult(resultBox, 'success', '', html);
    } else {
        const from = result.path && result.path.length > 0 ? result.path[0] : 'origen';
        const to = 'destino';
        showResult(resultBox, 'error', 'No Alcanzable', `No existe un camino accesible con ${algorithm}`);
    }
}

// ==================== Network Algorithms ====================

async function executeMST(algorithm) {
    const resultBox = document.getElementById('mstResult');

    try {
        showLoading();
        const result = await fetchAPI(`/network/mst?algorithm=${algorithm}`);
        hideLoading();

        const algorithmName = algorithm === 'kruskal' ? 'Kruskal' : 'Prim';

        let html = `<h4>✅ MST Calculado (${algorithmName})</h4>`;
        html += `<div class="result-item">
            <strong>Distancia Total Mínima:</strong> ${result.totalWeight} km<br>
            <strong>Número de Aristas:</strong> ${result.edges.length}
        </div>`;

        html += '<h4>Aristas del MST:</h4>';
        result.edges.forEach(edge => {
            html += `<div class="result-item">
                ${edge.from} ↔ ${edge.to}: <strong>${edge.weight} km</strong>
            </div>`;
        });

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

// ==================== Matching Algorithms ====================

async function executeGreedy() {
    const adopterId = document.getElementById('greedyAdopter').value;
    const resultBox = document.getElementById('greedyResult');

    try {
        showLoading();
        const result = await fetchAPI(`/adoptions/greedy?adopterId=${adopterId}`);
        hideLoading();

        let html = '<h4>✅ Selección Voraz Completada</h4>';
        html += `<div class="result-item">
            <strong>Adoptante:</strong> ${result.adopterName || adopterId}<br>
            <strong>Perros Seleccionados:</strong> ${result.assignedDogs ? result.assignedDogs.length : 0}<br>
            <strong>Score Total:</strong> ${result.totalScore ? result.totalScore.toFixed(2) : 'N/A'}<br>
            <strong>Costo Total:</strong> $${result.totalCost ? result.totalCost.toFixed(2) : 'N/A'}
        </div>`;

        if (result.assignedDogs && result.assignedDogs.length > 0) {
            html += '<h4>Perros Asignados:</h4>';
            result.assignedDogs.forEach(dog => {
                html += `<div class="result-item">
                    <strong>${dog.dogName || dog.dogId}</strong><br>
                    Costo: $${dog.cost ? dog.cost.toFixed(2) : 'N/A'}
                </div>`;
            });
        }

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

async function executeBacktracking() {
    const resultBox = document.getElementById('backtrackingResult');

    try {
        showLoading();
        const result = await fetchAPI('/adoptions/constraints/backtracking');
        hideLoading();

        let html = '<h4>✅ Asignación por Backtracking Completada</h4>';
        html += `<div class="result-item">
            <strong>Score Total:</strong> ${result.totalScore ? result.totalScore.toFixed(2) : 'N/A'}
        </div>`;

        if (result.assignments) {
            const assignmentsList = Object.values(result.assignments);

            if (assignmentsList.length > 0) {
                html += `<div class="result-item">
                    <strong>Total de Asignaciones:</strong> ${assignmentsList.length}
                </div>`;

                assignmentsList.forEach(assignment => {
                    html += `<div class="result-item">
                        <strong>Adoptante:</strong> ${assignment.adopterName}<br>
                        <strong>Perros Asignados:</strong> ${assignment.assignedDogs.map(d => d.dogName).join(', ')}<br>
                        <strong>Cantidad:</strong> ${assignment.assignedDogs.length} perro(s)
                    </div>`;
                });
            } else {
                html += '<p>No se encontraron asignaciones válidas</p>';
            }
        } else {
            html += '<p>No se encontraron asignaciones válidas</p>';
        }

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

// ==================== Sorting Algorithms ====================

async function executeSort() {
    const criteria = document.getElementById('sortCriteria').value;
    const algorithm = document.getElementById('sortAlgorithm').value;
    const resultBox = document.getElementById('sortResult');

    try {
        showLoading();
        const result = await fetchAPI(`/dogs/sort?criteria=${criteria}&algorithm=${algorithm}`);
        hideLoading();

        const algorithmName = algorithm === 'mergesort' ? 'MergeSort' : 'QuickSort';
        const dogs = result.dogs || [];

        let html = `<h4>✅ Perros Ordenados (${algorithmName})</h4>`;
        html += `<div class="result-item">
            <strong>Criterio:</strong> ${result.criteria || criteria}<br>
            <strong>Algoritmo:</strong> ${result.algorithm || algorithmName}<br>
            <strong>Total de Perros:</strong> ${dogs.length}
        </div>`;

        if (dogs.length > 0) {
            html += '<h4>Lista Ordenada:</h4>';
            dogs.forEach((perro, index) => {
                let value;
                if (criteria === 'priority') value = perro.priority;
                else if (criteria === 'age') value = perro.age;
                else if (criteria === 'weight') value = perro.weight;

                html += `<div class="result-item">
                    <strong>${index + 1}. ${perro.name}</strong><br>
                    ${criteria.charAt(0).toUpperCase() + criteria.slice(1)}: ${value} |
                    Tamaño: ${perro.size} |
                    Peso: ${perro.weight}kg
                </div>`;
            });
        } else {
            html += '<p>No hay perros para ordenar</p>';
        }

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

// ==================== Transport Optimization ====================

async function executeKnapsack() {
    const capacity = document.getElementById('vehicleCapacity').value;
    const resultBox = document.getElementById('knapsackResult');

    if (!capacity || capacity < 1) {
        showResult(resultBox, 'error', 'Error', 'Debes ingresar una capacidad válida');
        return;
    }

    try {
        showLoading();
        const result = await fetchAPI(`/transport/optimal-dp?capacityKg=${capacity}`);
        hideLoading();

        let html = '<h4>✅ Transporte Optimizado (Knapsack DP)</h4>';
        html += `<div class="result-item">
            <strong>Capacidad del Vehículo:</strong> ${result.vehicleCapacityKg || capacity} kg<br>
            <strong>Peso Total Usado:</strong> ${result.totalWeightKg || 0} kg<br>
            <strong>Prioridad Total Maximizada:</strong> ${result.totalPriority || 0}<br>
            <strong>Perros Transportados:</strong> ${result.selectedDogs ? result.selectedDogs.length : 0}
        </div>`;

        if (result.selectedDogs && result.selectedDogs.length > 0) {
            html += '<h4>Perros Seleccionados:</h4>';
            result.selectedDogs.forEach(dog => {
                html += `<div class="result-item">
                    <strong>${dog.name}</strong><br>
                    Peso: ${dog.weightKg}kg |
                    Prioridad: ${dog.priority} |
                    Relación P/W: ${(dog.priority / dog.weightKg).toFixed(2)}
                </div>`;
            });
        }

        showResult(resultBox, 'success', '', html);
    } catch (error) {
        hideLoading();
        showResult(resultBox, 'error', 'Error', error.message);
    }
}

// ==================== Utility Functions ====================

async function fetchAPI(endpoint) {
    const response = await fetch(`${API_BASE_URL}${endpoint}`);

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Error ${response.status}: ${errorText || response.statusText}`);
    }

    return await response.json();
}

function showLoading() {
    document.getElementById('loadingOverlay').classList.add('active');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.remove('active');
}

function showResult(element, type, title, content) {
    element.className = `result-box visible ${type}`;

    let html = '';
    if (title) {
        html += `<h4>${title}</h4>`;
    }

    if (typeof content === 'string') {
        html += content;
    } else {
        html += `<pre>${JSON.stringify(content, null, 2)}</pre>`;
    }

    element.innerHTML = html;
}

function showError(message) {
    alert(message);
}

// ==================== Dynamic Selector Population ====================

async function populateSelectors() {
    try {
        // Load shelters and adopters
        const [shelters, adopters] = await Promise.all([
            fetchAPI('/shelters'),
            fetchAPI('/adopters')
        ]);

        // Populate shelter selectors
        populateShelterSelectors(shelters);

        // Populate TSP checkboxes
        populateTSPCheckboxes(shelters);

        // Populate adopter selector
        populateAdopterSelector(adopters);
    } catch (error) {
        console.error('Error populating selectors:', error);
        // Keep static options as fallback
    }
}

function populateShelterSelectors(shelters) {
    if (!shelters || shelters.length === 0) return;

    const shelterIds = ['bfsFrom', 'bfsTo', 'dijkstraFrom', 'dijkstraTo'];

    shelterIds.forEach(selectId => {
        const select = document.getElementById(selectId);
        if (!select) return;

        const currentValue = select.value;
        select.innerHTML = '';

        shelters.forEach((shelter, index) => {
            const option = document.createElement('option');
            option.value = shelter.id;
            option.textContent = shelter.id === 'H'
                ? `Hub Central (${shelter.id})`
                : `${shelter.name || 'Refugio ' + shelter.id}`;

            // Set default selections
            if ((selectId.includes('To') && index === Math.min(shelters.length - 1, 12)) ||
                (selectId.includes('From') && index === 0)) {
                option.selected = true;
            }

            select.appendChild(option);
        });

        // Restore previous value if it exists
        if (currentValue && Array.from(select.options).some(opt => opt.value === currentValue)) {
            select.value = currentValue;
        }
    });
}

function populateTSPCheckboxes(shelters) {
    if (!shelters || shelters.length === 0) return;

    const container = document.querySelector('.checkbox-group');
    if (!container) return;

    container.innerHTML = '';

    shelters.forEach((shelter, index) => {
        const label = document.createElement('label');
        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.name = 'tspNode';
        checkbox.value = shelter.id;

        // Check first 4 by default (A, B, C, H typically)
        if (index < 4) {
            checkbox.checked = true;
        }

        const text = document.createTextNode(
            shelter.id === 'H'
                ? ` Hub Central (${shelter.id})`
                : ` ${shelter.name || 'Refugio ' + shelter.id}`
        );

        label.appendChild(checkbox);
        label.appendChild(text);
        container.appendChild(label);
    });
}

function populateAdopterSelector(adopters) {
    if (!adopters || adopters.length === 0) return;

    const select = document.getElementById('greedyAdopter');
    if (!select) return;

    const currentValue = select.value;
    select.innerHTML = '';

    adopters.forEach(adopter => {
        const option = document.createElement('option');
        option.value = adopter.id;

        const yard = adopter.hasYard ? '✓' : '✗';
        const kids = adopter.hasKids ? '✓' : '✗';
        const budget = adopter.budget ? `$${adopter.budget.toLocaleString('es-AR')}` : 'N/A';
        const maxDogs = adopter.maxDogs || 1;

        option.textContent = `${adopter.id} - ${adopter.name} (${budget}, Patio ${yard}, Niños ${kids}, Max: ${maxDogs})`;

        select.appendChild(option);
    });

    // Restore previous value if it exists
    if (currentValue && Array.from(select.options).some(opt => opt.value === currentValue)) {
        select.value = currentValue;
    }
}

// ==================== TSP Selection Helpers ====================

function selectAllTSP() {
    const checkboxes = document.querySelectorAll('input[name="tspNode"]');
    checkboxes.forEach(cb => cb.checked = true);
}

function clearAllTSP() {
    const checkboxes = document.querySelectorAll('input[name="tspNode"]');
    checkboxes.forEach(cb => cb.checked = false);
}

function selectDefaultTSP() {
    const checkboxes = document.querySelectorAll('input[name="tspNode"]');
    checkboxes.forEach((cb, index) => {
        cb.checked = index < 4; // Select first 4
    });
}

// ==================== Helper Functions ====================

function formatDistance(distance) {
    return `${distance.toFixed(2)} km`;
}

function formatCurrency(amount) {
    return `$${amount.toLocaleString('es-AR')}`;
}

// ==================== Graph Visualization ====================

let cytoscapeInstance = null;

async function showGraphVisualization() {
    const container = document.getElementById('graphContainer');
    const canvas = document.getElementById('graphCanvas');

    // Toggle visibility
    if (container.style.display === 'none') {
        container.style.display = 'block';

        // Fetch graph data
        try {
            const data = await fetchAPI('/network/graph');

            if (!data.nodes || !data.edges) {
                canvas.innerHTML = '<div style="padding: 20px; text-align: center; color: #e74c3c;"><h3>❌ Error</h3><p>No se pudieron cargar los datos del grafo</p></div>';
                return;
            }

            // Prepare nodes for Cytoscape
            const cytoscapeNodes = data.nodes.map(nodeId => ({
                data: {
                    id: nodeId,
                    label: nodeId
                }
            }));

            // Prepare edges for Cytoscape (avoid duplicates for undirected graph)
            const edgeSet = new Set();
            const cytoscapeEdges = [];

            data.edges.forEach(edge => {
                const edgeKey = [edge.from, edge.to].sort().join('-');
                if (!edgeSet.has(edgeKey)) {
                    edgeSet.add(edgeKey);
                    cytoscapeEdges.push({
                        data: {
                            source: edge.from,
                            target: edge.to,
                            weight: edge.weight,
                            label: `${edge.weight.toFixed(1)} km`
                        }
                    });
                }
            });

            // Destroy previous instance if exists
            if (cytoscapeInstance) {
                cytoscapeInstance.destroy();
            }

            // Initialize Cytoscape
            cytoscapeInstance = cytoscape({
                container: canvas,

                elements: {
                    nodes: cytoscapeNodes,
                    edges: cytoscapeEdges
                },

                style: [
                    {
                        selector: 'node',
                        style: {
                            'background-color': '#3498db',
                            'label': 'data(label)',
                            'color': '#2c3e50',
                            'text-valign': 'center',
                            'text-halign': 'center',
                            'font-size': '14px',
                            'font-weight': 'bold',
                            'width': 50,
                            'height': 50,
                            'border-width': 3,
                            'border-color': '#2980b9',
                            'text-outline-width': 2,
                            'text-outline-color': '#ffffff'
                        }
                    },
                    {
                        selector: 'edge',
                        style: {
                            'width': 3,
                            'line-color': '#95a5a6',
                            'target-arrow-color': '#95a5a6',
                            'curve-style': 'bezier',
                            'label': 'data(label)',
                            'font-size': '11px',
                            'color': '#34495e',
                            'text-background-color': '#ffffff',
                            'text-background-opacity': 0.8,
                            'text-background-padding': '3px',
                            'text-border-width': 1,
                            'text-border-color': '#bdc3c7',
                            'text-border-opacity': 0.5
                        }
                    },
                    {
                        selector: 'node:selected',
                        style: {
                            'background-color': '#e74c3c',
                            'border-color': '#c0392b'
                        }
                    },
                    {
                        selector: 'edge:selected',
                        style: {
                            'line-color': '#e74c3c',
                            'width': 5
                        }
                    }
                ],

                layout: {
                    name: 'cose',
                    idealEdgeLength: 100,
                    nodeOverlap: 20,
                    refresh: 20,
                    fit: true,
                    padding: 30,
                    randomize: false,
                    componentSpacing: 100,
                    nodeRepulsion: 400000,
                    edgeElasticity: 100,
                    nestingFactor: 5,
                    gravity: 80,
                    numIter: 1000,
                    initialTemp: 200,
                    coolingFactor: 0.95,
                    minTemp: 1.0
                },

                wheelSensitivity: 0.2
            });

            // Add interactivity
            cytoscapeInstance.on('tap', 'node', function(evt) {
                const node = evt.target;
                console.log('Clicked node:', node.id());
            });

            console.log(`Grafo renderizado: ${data.nodes.length} nodos, ${cytoscapeEdges.length} aristas`);

        } catch (error) {
            console.error('Error loading graph:', error);
            canvas.innerHTML = `<div style="padding: 20px; text-align: center; color: #e74c3c;">
                <h3>❌ Error al cargar el grafo</h3>
                <p>${error.message}</p>
                <p style="font-size: 0.9em; color: #7f8c8d;">Verifica que el servidor esté corriendo y reiniciado.</p>
            </div>`;
        }
    } else {
        container.style.display = 'none';
    }
}
