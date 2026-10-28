// MediCare AI - Interactive 3D Biotech Visualizer (Three.js)
// Features: DNA Double Helix, Bio-Vital Pulse Core, and Molecular Cluster modes
// with pointer parallax, inertia drag-rotation, responsive canvas, and telemetry HUD.

export function initHealthcareVisual() {
    const container = document.getElementById('threejs-container');
    const canvas = document.getElementById('health-canvas');
    const fallback = document.getElementById('threejs-fallback');
    if (!canvas || !container) return;

    function showFallback() {
        if (canvas) canvas.style.display = 'none';
        if (fallback) fallback.style.display = 'flex';
    }

    function hasWebGL() {
        try {
            const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
            return !!(window.WebGLRenderingContext && gl);
        } catch (e) {
            return false;
        }
    }

    if (!hasWebGL()) {
        showFallback();
        return;
    }

    // Dynamic module import with CDN fallback
    import('https://unpkg.com/three@0.160.0/build/three.module.js')
        .then((THREE) => {
            try {
                setupBiotechEngine(THREE, container, canvas, fallback);
            } catch (err) {
                console.warn('Three.js setup error:', err);
                showFallback();
            }
        })
        .catch((err) => {
            console.debug('Three.js CDN unavailable, rendering fallback:', err.message);
            showFallback();
        });
}

function setupBiotechEngine(THREE, container, canvas, fallback) {
    let width = canvas.clientWidth || container.clientWidth || 360;
    let height = canvas.clientHeight || container.clientHeight || 220;

    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(45, width / height, 0.1, 1000);
    camera.position.set(0, 0, 5.2);

    const renderer = new THREE.WebGLRenderer({
        canvas: canvas,
        antialias: true,
        alpha: true,
        powerPreference: 'high-performance'
    });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    // Master groups
    const worldGroup = new THREE.Group();
    scene.add(worldGroup);

    const dnaGroup = new THREE.Group();
    const coreGroup = new THREE.Group();
    const moleculeGroup = new THREE.Group();
    const particleGroup = new THREE.Group();

    worldGroup.add(dnaGroup);
    worldGroup.add(coreGroup);
    worldGroup.add(moleculeGroup);
    worldGroup.add(particleGroup);

    // Initial mode: DNA
    let currentMode = 'dna';
    coreGroup.visible = false;
    moleculeGroup.visible = false;

    // -------------------------------------------------------------
    // 1. LIGHTING (Optimized for pristine light background)
    // -------------------------------------------------------------
    const ambientLight = new THREE.AmbientLight(0xffffff, 1.15);
    scene.add(ambientLight);

    const keyLight = new THREE.DirectionalLight(0x0284c7, 2.2); // Sapphire blue key
    keyLight.position.set(5, 6, 6);
    scene.add(keyLight);

    const fillLight = new THREE.DirectionalLight(0x7c3aed, 1.4); // Royal violet fill
    fillLight.position.set(-6, -4, 4);
    scene.add(fillLight);

    const pointLight = new THREE.PointLight(0x0d9488, 1.8, 12); // Teal center pulse
    pointLight.position.set(0, 0, 1.5);
    scene.add(pointLight);

    // -------------------------------------------------------------
    // 2. DNA DOUBLE HELIX GENERATOR (Vibrant light-mode contrast)
    // -------------------------------------------------------------
    const baseColors = [0x0284c7, 0x059669, 0x7c3aed, 0xd97706]; // Sapphire, Emerald, Violet, Amber
    const numPairs = 24;
    const helixRadius = 1.15;
    const helixHeight = 3.6;
    const strandGeom = new THREE.SphereGeometry(0.085, 16, 16);
    const rungGeom = new THREE.CylinderGeometry(0.024, 0.024, 1, 8);

    const strandAMaterial = new THREE.MeshStandardMaterial({
        color: 0x1d4ed8,
        metalness: 0.25,
        roughness: 0.2,
        emissive: 0x1e40af,
        emissiveIntensity: 0.25
    });

    const strandBMaterial = new THREE.MeshStandardMaterial({
        color: 0x0f766e,
        metalness: 0.25,
        roughness: 0.2,
        emissive: 0x115e59,
        emissiveIntensity: 0.25
    });

    for (let i = 0; i < numPairs; i++) {
        const t = (i / (numPairs - 1)) * Math.PI * 4;
        const y = ((i / (numPairs - 1)) - 0.5) * helixHeight;
        const x1 = Math.cos(t) * helixRadius;
        const z1 = Math.sin(t) * helixRadius;
        const x2 = Math.cos(t + Math.PI) * helixRadius;
        const z2 = Math.sin(t + Math.PI) * helixRadius;

        // Base A node
        const sphereA = new THREE.Mesh(strandGeom, strandAMaterial);
        sphereA.position.set(x1, y, z1);
        dnaGroup.add(sphereA);

        // Base B node
        const sphereB = new THREE.Mesh(strandGeom, strandBMaterial);
        sphereB.position.set(x2, y, z2);
        dnaGroup.add(sphereB);

        // Connecting hydrogen bond rung
        const rungMat = new THREE.MeshStandardMaterial({
            color: baseColors[i % baseColors.length],
            emissive: baseColors[i % baseColors.length],
            emissiveIntensity: 0.45,
            roughness: 0.2
        });
        const rung = new THREE.Mesh(rungGeom, rungMat);
        const p1 = new THREE.Vector3(x1, y, z1);
        const p2 = new THREE.Vector3(x2, y, z2);
        const mid = new THREE.Vector3().addVectors(p1, p2).multiplyScalar(0.5);
        rung.position.copy(mid);
        rung.scale.set(1, p1.distanceTo(p2), 1);
        rung.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), new THREE.Vector3().subVectors(p2, p1).normalize());
        dnaGroup.add(rung);
    }

    // -------------------------------------------------------------
    // 3. BIO-VITAL PULSE CORE GENERATOR
    // -------------------------------------------------------------
    const nucleusGeom = new THREE.SphereGeometry(0.75, 32, 32);
    const nucleusMat = new THREE.MeshStandardMaterial({
        color: 0x06b6d4,
        emissive: 0x0284c7,
        emissiveIntensity: 0.6,
        roughness: 0.15,
        metalness: 0.4,
        wireframe: false
    });
    const nucleus = new THREE.Mesh(nucleusGeom, nucleusMat);
    coreGroup.add(nucleus);

    const shieldGeom = new THREE.IcosahedronGeometry(1.25, 2);
    const shieldMat = new THREE.MeshStandardMaterial({
        color: 0x38bdf8,
        wireframe: true,
        transparent: true,
        opacity: 0.45
    });
    const shield = new THREE.Mesh(shieldGeom, shieldMat);
    coreGroup.add(shield);

    // Orbiting vitality rings
    const ringGeom = new THREE.TorusGeometry(1.6, 0.025, 16, 100);
    const ringMat1 = new THREE.MeshStandardMaterial({ color: 0x10b981, emissive: 0x059669, emissiveIntensity: 0.6 });
    const ringMat2 = new THREE.MeshStandardMaterial({ color: 0x818cf8, emissive: 0x6366f1, emissiveIntensity: 0.6 });
    const ringMat3 = new THREE.MeshStandardMaterial({ color: 0xf59e0b, emissive: 0xd97706, emissiveIntensity: 0.6 });

    const ring1 = new THREE.Mesh(ringGeom, ringMat1);
    ring1.rotation.x = Math.PI / 3;
    coreGroup.add(ring1);

    const ring2 = new THREE.Mesh(ringGeom, ringMat2);
    ring2.rotation.y = Math.PI / 4;
    ring2.rotation.x = -Math.PI / 6;
    coreGroup.add(ring2);

    const ring3 = new THREE.Mesh(ringGeom, ringMat3);
    ring3.rotation.z = Math.PI / 2.5;
    coreGroup.add(ring3);

    // -------------------------------------------------------------
    // 4. PHARMACOLOGICAL MOLECULAR CLUSTER GENERATOR
    // -------------------------------------------------------------
    const atomGeom = new THREE.SphereGeometry(0.24, 24, 24);
    const bondGeom = new THREE.CylinderGeometry(0.04, 0.04, 1, 12);

    const atomTypes = [
        { name: 'Carbon', color: 0x334155, emissive: 0x1e293b, scale: 1.0 },
        { name: 'Oxygen', color: 0xef4444, emissive: 0xdc2626, scale: 0.9 },
        { name: 'Nitrogen', color: 0x2563eb, emissive: 0x1d4ed8, scale: 0.95 },
        { name: 'Hydrogen', color: 0xe0f2fe, emissive: 0xbae6fd, scale: 0.65 }
    ];

    // Molecular nodes in 3D lattice
    const atomPositions = [
        { pos: [-0.9, 0.6, 0], type: 0 },
        { pos: [0.0, 1.2, 0.2], type: 0 },
        { pos: [0.9, 0.6, -0.1], type: 2 },
        { pos: [0.9, -0.6, 0.1], type: 0 },
        { pos: [0.0, -1.2, -0.2], type: 0 },
        { pos: [-0.9, -0.6, 0], type: 2 },
        // Side chains
        { pos: [-1.7, 1.1, 0.3], type: 1 },
        { pos: [1.7, 1.1, -0.3], type: 1 },
        { pos: [1.7, -1.1, 0.2], type: 3 },
        { pos: [-1.7, -1.1, -0.2], type: 3 },
        { pos: [0, 0, 0.7], type: 1 }
    ];

    const atomMeshes = [];
    atomPositions.forEach((at) => {
        const info = atomTypes[at.type];
        const mat = new THREE.MeshStandardMaterial({
            color: info.color,
            emissive: info.emissive,
            emissiveIntensity: 0.4,
            roughness: 0.2,
            metalness: 0.3
        });
        const mesh = new THREE.Mesh(atomGeom, mat);
        mesh.position.set(at.pos[0], at.pos[1], at.pos[2]);
        mesh.scale.setScalar(info.scale);
        moleculeGroup.add(mesh);
        atomMeshes.push(mesh);
    });

    const bonds = [
        [0, 1], [1, 2], [2, 3], [3, 4], [4, 5], [5, 0],
        [0, 6], [2, 7], [3, 8], [5, 9], [1, 10], [4, 10]
    ];

    bonds.forEach(([i, j]) => {
        const p1 = atomMeshes[i].position;
        const p2 = atomMeshes[j].position;
        const mat = new THREE.MeshStandardMaterial({ color: 0x94a3b8, roughness: 0.4, metalness: 0.2 });
        const bond = new THREE.Mesh(bondGeom, mat);
        const mid = new THREE.Vector3().addVectors(p1, p2).multiplyScalar(0.5);
        bond.position.copy(mid);
        bond.scale.set(1, p1.distanceTo(p2), 1);
        bond.quaternion.setFromUnitVectors(new THREE.Vector3(0, 1, 0), new THREE.Vector3().subVectors(p2, p1).normalize());
        moleculeGroup.add(bond);
    });

    // -------------------------------------------------------------
    // 5. AMBIENT BIO-PHOTON PARTICLE FIELD
    // -------------------------------------------------------------
    const particleCount = 140;
    const particleGeom = new THREE.BufferGeometry();
    const particlePositions = new Float32Array(particleCount * 3);

    for (let i = 0; i < particleCount * 3; i += 3) {
        particlePositions[i] = (Math.random() - 0.5) * 8;
        particlePositions[i + 1] = (Math.random() - 0.5) * 6;
        particlePositions[i + 2] = (Math.random() - 0.5) * 6;
    }
    particleGeom.setAttribute('position', new THREE.BufferAttribute(particlePositions, 3));

    const particleMat = new THREE.PointsMaterial({
        color: 0x0284c7,
        size: 0.06,
        transparent: true,
        opacity: 0.55
    });
    const particles = new THREE.Points(particleGeom, particleMat);
    particleGroup.add(particles);

    // -------------------------------------------------------------
    // 6. INTERACTION & CONTROLS (PARALLAX + DRAG)
    // -------------------------------------------------------------
    let isDragging = false;
    let previousPointerX = 0;
    let previousPointerY = 0;
    let targetRotationX = 0;
    let targetRotationY = 0;
    let currentRotationX = 0;
    let currentRotationY = 0;
    let isPaused = false;
    let speedFactor = 1.0;

    container.addEventListener('pointerdown', (e) => {
        isDragging = true;
        previousPointerX = e.clientX;
        previousPointerY = e.clientY;
        container.style.cursor = 'grabbing';
    });

    window.addEventListener('pointermove', (e) => {
        if (isDragging) {
            const deltaX = e.clientX - previousPointerX;
            const deltaY = e.clientY - previousPointerY;
            targetRotationY += deltaX * 0.008;
            targetRotationX += deltaY * 0.008;
            previousPointerX = e.clientX;
            previousPointerY = e.clientY;
        } else {
            // Subtle mouse parallax tilt when hovering
            const rect = container.getBoundingClientRect();
            if (e.clientX >= rect.left && e.clientX <= rect.right &&
                e.clientY >= rect.top && e.clientY <= rect.bottom) {
                const normX = ((e.clientX - rect.left) / rect.width) * 2 - 1;
                const normY = -(((e.clientY - rect.top) / rect.height) * 2 - 1);
                targetRotationY = normX * 0.35;
                targetRotationX = normY * 0.25;
            }
        }
    });

    window.addEventListener('pointerup', () => {
        isDragging = false;
        container.style.cursor = 'grab';
    });

    // Zoom on wheel within safe boundaries
    container.addEventListener('wheel', (e) => {
        e.preventDefault();
        camera.position.z = Math.min(Math.max(camera.position.z + e.deltaY * 0.003, 3.2), 7.5);
    }, { passive: false });

    // -------------------------------------------------------------
    // 7. HUD CONTROLS BINDING
    // -------------------------------------------------------------
    function switchMode(newMode) {
        currentMode = newMode;
        dnaGroup.visible = (newMode === 'dna');
        coreGroup.visible = (newMode === 'core');
        moleculeGroup.visible = (newMode === 'molecule');

        document.querySelectorAll('.three-mode-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.mode === newMode);
        });

        const statusPill = document.getElementById('three-status-text');
        if (statusPill) {
            if (newMode === 'dna') statusPill.textContent = '🧬 Double Helix Genomic Sequence';
            else if (newMode === 'core') statusPill.textContent = '💓 Bio-Vitality Heartbeat Core';
            else if (newMode === 'molecule') statusPill.textContent = '🔬 Pharmacological Lattice';
        }
    }

    // Attach listener to buttons
    document.querySelectorAll('.three-mode-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            switchMode(btn.dataset.mode);
        });
    });

    const pauseBtn = document.getElementById('three-pause-btn');
    if (pauseBtn) {
        pauseBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            isPaused = !isPaused;
            pauseBtn.textContent = isPaused ? '▶ Play' : '⏸ Pause';
        });
    }

    const speedBtn = document.getElementById('three-speed-btn');
    if (speedBtn) {
        speedBtn.addEventListener('click', (e) => {
            e.stopPropagation();
            if (speedFactor === 1.0) speedFactor = 1.75;
            else if (speedFactor === 1.75) speedFactor = 2.5;
            else speedFactor = 1.0;
            speedBtn.textContent = `⚡ ${speedFactor}x`;
        });
    }

    // -------------------------------------------------------------
    // 8. ANIMATION LOOP
    // -------------------------------------------------------------
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    let clock = new THREE.Clock();

    function animate() {
        requestAnimationFrame(animate);

        const delta = clock.getDelta();
        const elapsedTime = clock.getElapsedTime();

        // Damped interpolation for rotation
        currentRotationX += (targetRotationX - currentRotationX) * 0.06;
        currentRotationY += (targetRotationY - currentRotationY) * 0.06;

        worldGroup.rotation.x = currentRotationX;
        worldGroup.rotation.y = currentRotationY;

        if (!isPaused && !prefersReducedMotion.matches) {
            const rotStep = delta * speedFactor;

            // Rotate particle cloud gently
            particleGroup.rotation.y += rotStep * 0.05;

            if (currentMode === 'dna') {
                dnaGroup.rotation.y += rotStep * 0.65;
                dnaGroup.rotation.z = Math.sin(elapsedTime * 0.8) * 0.08;
            } else if (currentMode === 'core') {
                shield.rotation.x += rotStep * 0.4;
                shield.rotation.y += rotStep * 0.6;
                ring1.rotation.z += rotStep * 0.8;
                ring2.rotation.x -= rotStep * 0.7;
                ring3.rotation.y += rotStep * 0.9;

                // Heartbeat simulation
                const beat = Math.sin(elapsedTime * 4.5 * speedFactor);
                const scale = 1.0 + Math.max(0, beat) * 0.12;
                nucleus.scale.set(scale, scale, scale);
                pointLight.intensity = 1.8 + Math.max(0, beat) * 1.5;
            } else if (currentMode === 'molecule') {
                moleculeGroup.rotation.y += rotStep * 0.5;
                moleculeGroup.rotation.x = Math.sin(elapsedTime * 0.6) * 0.15;
            }
        }

        renderer.render(scene, camera);
    }

    animate();

    // -------------------------------------------------------------
    // 9. RESIZE HANDLING
    // -------------------------------------------------------------
    function handleResize() {
        width = canvas.clientWidth || container.clientWidth || 360;
        height = canvas.clientHeight || container.clientHeight || 220;
        camera.aspect = width / height;
        camera.updateProjectionMatrix();
        renderer.setSize(width, height, false);
    }

    window.addEventListener('resize', handleResize);
    // Trigger initial sizing
    setTimeout(handleResize, 50);
}

// Auto-run on DOM load
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initHealthcareVisual);
} else {
    initHealthcareVisual();
}
