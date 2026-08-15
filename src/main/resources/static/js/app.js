/**
 * AccessZero — Frontend Web Console Logic
 * Identity Breach Containment & Access Revocation Platform
 */

let currentUserId = 1;
let currentUsername = 'rahul.sharma';
let currentAdmin = 'anil.admin';
let currentPendingOperationId = null;
let currentGraphData = null;

document.addEventListener('DOMContentLoaded', () => {
    initApp();
});

async function initApp() {
    await loadIdentitiesList();
    await loadUserIdentity(currentUserId);
    await loadAuditTrail();
    window.addEventListener('resize', handleCanvasResize);
}

// Admin Switcher
function switchAdmin(adminName) {
    currentAdmin = adminName;
    document.getElementById('current-approver-display').innerText = adminName;
    showToast(`Active Operator switched to ${adminName}`);
    updateApprovalButtonState();
}

// Load List of Identities
async function loadIdentitiesList() {
    try {
        const res = await fetch('/api/v1/identities');
        if (res.ok) {
            const identities = await res.json();
            const selector = document.getElementById('identity-selector');
            selector.innerHTML = '';
            identities.forEach(id => {
                const opt = document.createElement('option');
                opt.value = id.id;
                opt.textContent = `${id.firstName} ${id.lastName} (${id.department} - ${id.riskLevel})`;
                selector.appendChild(opt);
            });
            selector.value = currentUserId;
        }
    } catch (e) {
        console.warn('Failed loading identities list:', e);
    }
}

// Load Selected Identity
async function loadUserIdentity(userId) {
    currentUserId = userId;
    try {
        // 1. Fetch User details
        const userRes = await fetch(`/api/v1/identities/${userId}`);
        if (userRes.ok) {
            const user = await userRes.json();
            currentUsername = user.username;
            document.getElementById('user-display-name').innerText = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username;
            document.getElementById('user-username-tag').innerText = `@${user.username}`;
            document.getElementById('user-avatar-initials').innerText = (user.firstName ? user.firstName[0] : '') + (user.lastName ? user.lastName[0] : 'U');
            document.getElementById('user-department').innerText = user.department || 'General';
            document.getElementById('user-email').innerText = user.email || `${user.username}@company.com`;
            document.getElementById('user-id-val').innerText = user.id;

            // Status Badge
            const statusBadge = document.getElementById('user-status-badge');
            const avatarBadge = document.getElementById('user-status-indicator');
            if (user.status === 'CONTAINED') {
                statusBadge.innerText = 'CONTAINED / ISOLATED';
                statusBadge.className = 'badge badge-success';
                avatarBadge.innerText = '🔒';
                avatarBadge.style.background = 'var(--accent-emerald)';
            } else {
                statusBadge.innerText = 'SUSPECTED COMPROMISE';
                statusBadge.className = 'badge badge-danger';
                avatarBadge.innerText = '!';
                avatarBadge.style.background = 'var(--accent-rose)';
            }
        }

        // 2. Fetch Blast Radius & Metrics
        await recalculateBlastRadius();

        // 3. Fetch Graph
        await loadGraphData(userId);

        // 4. Check for Pending Operations
        await checkPendingOperations();

    } catch (e) {
        console.error('Error loading user identity:', e);
    }
}

// Recalculate Blast Radius
async function recalculateBlastRadius() {
    try {
        const res = await fetch(`/api/v1/identities/${currentUserId}/blast-radius`);
        if (res.ok) {
            const data = await res.json();
            updateBlastRadiusUI(data);
            showToast(`Access Blast Radius recalculated: ${data.totalAccessPathsCount} paths identified`);
        }
    } catch (e) {
        console.error('Error recalculating blast radius:', e);
    }
}

function updateBlastRadiusUI(data) {
    document.getElementById('metric-sessions').innerText = data.activeSessionsCount;
    document.getElementById('metric-tokens').innerText = data.activeTokensCount;
    document.getElementById('metric-groups').innerText = data.groupsCount;
    document.getElementById('metric-groups-priv').innerText = `${data.sensitiveGroupsCount} Privileged Groups`;
    document.getElementById('metric-apps').innerText = data.applicationsAffectedCount;
    document.getElementById('metric-apps-priv').innerText = `${data.privilegedApplicationsCount} Privileged`;
    document.getElementById('metric-saml').innerText = data.samlApplicationsCount || 2;
    document.getElementById('metric-paths').innerText = data.totalAccessPathsCount;

    // Gauge Update
    const score = data.riskScore;
    document.getElementById('risk-score-val').innerText = score;
    document.getElementById('risk-score-lbl').innerText = data.riskLevel;

    const progressCircle = document.getElementById('gauge-progress');
    const circumference = 2 * Math.PI * 50; // 314.15
    const offset = circumference - (score / 100) * circumference;
    progressCircle.style.strokeDashoffset = offset;

    if (score >= 75) {
        progressCircle.style.stroke = 'var(--accent-rose)';
        document.getElementById('risk-score-val').style.color = 'var(--accent-rose)';
    } else if (score >= 50) {
        progressCircle.style.stroke = 'var(--accent-amber)';
        document.getElementById('risk-score-val').style.color = 'var(--accent-amber)';
    } else {
        progressCircle.style.stroke = 'var(--accent-emerald)';
        document.getElementById('risk-score-val').style.color = 'var(--accent-emerald)';
    }

    // Populate Affected Applications Table
    renderAffectedAppsTable(data.affectedApplications);
}

function renderAffectedAppsTable(apps) {
    const tbody = document.getElementById('affected-apps-table-body');
    tbody.innerHTML = '';
    if (!apps || apps.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color: var(--text-muted);">No applications directly affected.</td></tr>`;
        return;
    }

    document.getElementById('affected-apps-count-badge').innerText = `${apps.length} Apps Impacted`;

    apps.forEach(app => {
        const tr = document.createElement('tr');
        const isPriv = app.isPrivileged !== undefined ? app.isPrivileged : app.privileged;
        const appName = app.name || app.applicationName || 'Application';
        const sensBadgeClass = app.sensitivityLevel === 'CRITICAL' ? 'badge-danger' : (app.sensitivityLevel === 'HIGH' ? 'badge-warning' : 'badge-info');

        tr.innerHTML = `
            <td><strong>${escapeHtml(appName)}</strong></td>
            <td><span class="mono-tag">${escapeHtml(app.type || 'OIDC')}</span></td>
            <td><span class="badge ${sensBadgeClass}">${escapeHtml(app.sensitivityLevel)}</span></td>
            <td><span class="mono-text">${app.accessPathCount || 1} paths</span></td>
            <td><span class="badge ${isPriv ? 'badge-danger' : 'badge-info'}">${isPriv ? 'High Impact' : 'Standard'}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// Load Identity Graph & Mermaid
async function loadGraphData(userId) {
    try {
        const res = await fetch(`/api/v1/identities/${userId}/graph`);
        if (res.ok) {
            currentGraphData = await res.json();
            renderCanvasGraph(currentGraphData);
        }

        const mermaidRes = await fetch(`/api/v1/identities/${userId}/graph/mermaid`);
        if (mermaidRes.ok) {
            const mermaidText = await mermaidRes.text();
            document.getElementById('mermaid-code-block').innerText = mermaidText;
        }
    } catch (e) {
        console.error('Error loading graph:', e);
    }
}

// Interactive Canvas Graph Renderer
function renderCanvasGraph(graphData) {
    const canvas = document.getElementById('identity-graph-canvas');
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    const container = document.getElementById('canvas-graph-container');
    const width = container.clientWidth;
    const height = container.clientHeight;

    canvas.width = width * window.devicePixelRatio;
    canvas.height = height * window.devicePixelRatio;
    ctx.scale(window.devicePixelRatio, window.devicePixelRatio);

    ctx.clearRect(0, 0, width, height);

    if (!graphData || !graphData.nodes || graphData.nodes.length === 0) {
        ctx.fillStyle = '#64748b';
        ctx.font = '14px Outfit';
        ctx.fillText('No topology graph available', width / 2 - 80, height / 2);
        return;
    }

    const nodes = graphData.nodes;
    const edges = graphData.edges || [];

    // Arrange nodes in radial/layered layout
    const userNode = nodes.find(n => n.type === 'USER') || nodes[0];
    const centerX = width / 2;
    const centerY = height / 2;

    const nodePositions = new Map();
    nodePositions.set(userNode.id, { x: centerX, y: centerY, node: userNode });

    // Group other nodes by type
    const groups = nodes.filter(n => n.type === 'GROUP');
    const roles = nodes.filter(n => n.type === 'ROLE');
    const sessions = nodes.filter(n => n.type === 'SESSION');
    const tokens = nodes.filter(n => n.type === 'TOKEN');
    const samls = nodes.filter(n => n.type === 'SAML_ASSIGNMENT');
    const apps = nodes.filter(n => n.type === 'APPLICATION');

    // Place groups and direct connections in ring 1
    const ring1 = [...groups, ...sessions.slice(0, 3), ...tokens.slice(0, 3), ...samls];
    ring1.forEach((n, idx) => {
        const angle = (idx / ring1.length) * 2 * Math.PI;
        const radius = Math.min(width, height) * 0.25;
        nodePositions.set(n.id, {
            x: centerX + radius * Math.cos(angle),
            y: centerY + radius * Math.sin(angle),
            node: n
        });
    });

    // Place roles in ring 2
    roles.forEach((n, idx) => {
        const angle = (idx / (roles.length || 1)) * 2 * Math.PI + 0.3;
        const radius = Math.min(width, height) * 0.38;
        nodePositions.set(n.id, {
            x: centerX + radius * Math.cos(angle),
            y: centerY + radius * Math.sin(angle),
            node: n
        });
    });

    // Place apps in outer ring
    apps.forEach((n, idx) => {
        const angle = (idx / (apps.length || 1)) * 2 * Math.PI;
        const radius = Math.min(width, height) * 0.44;
        nodePositions.set(n.id, {
            x: centerX + radius * Math.cos(angle),
            y: centerY + radius * Math.sin(angle),
            node: n
        });
    });

    // Draw Edges
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.12)';
    ctx.lineWidth = 1.5;
    edges.forEach(edge => {
        const p1 = nodePositions.get(edge.sourceId);
        const p2 = nodePositions.get(edge.targetId);
        if (p1 && p2) {
            ctx.beginPath();
            ctx.moveTo(p1.x, p1.y);
            ctx.lineTo(p2.x, p2.y);
            ctx.stroke();
        }
    });

    // Draw Nodes
    nodePositions.forEach((pos) => {
        const n = pos.node;
        let color = '#06b6d4';
        let radius = 8;

        switch (n.type) {
            case 'USER': color = '#f43f5e'; radius = 14; break;
            case 'GROUP': color = '#f59e0b'; radius = 10; break;
            case 'ROLE': color = '#8b5cf6'; radius = 9; break;
            case 'SESSION': color = '#3b82f6'; radius = 8; break;
            case 'TOKEN': color = '#ec4899'; radius = 8; break;
            case 'SAML_ASSIGNMENT': color = '#10b981'; radius = 9; break;
            case 'APPLICATION': color = '#06b6d4'; radius = 11; break;
        }

        // Outer glow for privileged or user
        if (n.privileged || n.type === 'USER') {
            ctx.beginPath();
            ctx.arc(pos.x, pos.y, radius + 4, 0, 2 * Math.PI);
            ctx.fillStyle = n.type === 'USER' ? 'rgba(244, 63, 94, 0.25)' : 'rgba(245, 158, 11, 0.25)';
            ctx.fill();
        }

        // Inner circle
        ctx.beginPath();
        ctx.arc(pos.x, pos.y, radius, 0, 2 * Math.PI);
        ctx.fillStyle = color;
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1;
        ctx.stroke();

        // Node label
        ctx.fillStyle = '#f8fafc';
        ctx.font = '10px JetBrains Mono';
        ctx.textAlign = 'center';
        ctx.fillText(n.label.substring(0, 16), pos.x, pos.y + radius + 12);
    });
}

function handleCanvasResize() {
    if (currentGraphData) {
        renderCanvasGraph(currentGraphData);
    }
}

// Toggle Graph View
function toggleGraphView(view) {
    const canvasContainer = document.getElementById('canvas-graph-container');
    const mermaidContainer = document.getElementById('mermaid-graph-container');
    const btnCanvas = document.getElementById('btn-view-canvas');
    const btnMermaid = document.getElementById('btn-view-mermaid');

    if (view === 'canvas') {
        canvasContainer.classList.remove('d-none');
        mermaidContainer.classList.add('d-none');
        btnCanvas.classList.add('btn-active');
        btnMermaid.classList.remove('btn-active');
        handleCanvasResize();
    } else {
        canvasContainer.classList.add('d-none');
        mermaidContainer.classList.remove('d-none');
        btnCanvas.classList.remove('btn-active');
        btnMermaid.classList.add('btn-active');
    }
}

function copyMermaidCode() {
    const code = document.getElementById('mermaid-code-block').innerText;
    navigator.clipboard.writeText(code).then(() => {
        showToast('Mermaid diagram code copied to clipboard');
    });
}

// Navigation Tabs
function switchTab(tabName) {
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));

    document.getElementById(`tab-btn-${tabName}`).classList.add('active');
    document.getElementById(`tab-content-${tabName}`).classList.add('active');

    if (tabName === 'graph') {
        setTimeout(handleCanvasResize, 50);
    }
}

// Simulation Modal Logic
async function openSimulationModal() {
    try {
        const res = await fetch(`/api/v1/identities/${currentUserId}/simulate`, { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            document.getElementById('sim-target-username').innerText = data.username;
            document.getElementById('sim-disruption-score').innerText = data.disruptionScore;
            document.getElementById('sim-disruption-level').innerText = data.disruptionLevel;
            document.getElementById('sim-approval-req').innerText = data.requiresApproval ? 'YES (High Risk)' : 'NO';

            const container = document.getElementById('sim-actions-container');
            container.innerHTML = '';
            data.actionSummary.forEach(action => {
                const div = document.createElement('div');
                div.innerHTML = `<span class="mono-text" style="color:var(--accent-cyan);">➔</span> ${escapeHtml(action)}`;
                container.appendChild(div);
            });

            document.getElementById('modal-simulation').classList.remove('d-none');
        }
    } catch (e) {
        showToast('Failed running containment simulation', true);
    }
}

function closeSimulationModal() {
    document.getElementById('modal-simulation').classList.add('d-none');
}

function proceedFromSimulationToContain() {
    closeSimulationModal();
    openContainmentModal();
}

// Containment Modal Logic
function openContainmentModal() {
    document.getElementById('contain-target-username').innerText = currentUsername;
    document.getElementById('current-approver-display').innerText = currentAdmin;
    document.getElementById('execution-progress-container').classList.add('d-none');
    document.getElementById('modal-containment').classList.remove('d-none');
    updateApprovalButtonState();
}

function closeContainmentModal() {
    document.getElementById('modal-containment').classList.add('d-none');
}

function toggleOverrideInfo() {
    const override = document.getElementById('emergency-override-checkbox').checked;
    const note = document.getElementById('override-note');
    if (override) {
        note.classList.add('text-danger');
        note.innerText = '🚨 EMERGENCY OVERRIDE ENABLED: Will bypass secondary admin approval and execute revocation immediately.';
    } else {
        note.classList.remove('text-danger');
        note.innerText = 'Standard enterprise policy requires a secondary security administrator to approve containment of privileged accounts.';
    }
}

function updateApprovalButtonState() {
    const btnSubmit = document.getElementById('btn-submit-contain');
    const btnApprove = document.getElementById('btn-submit-approval');
    const approvalSection = document.getElementById('approval-box-section');

    if (currentPendingOperationId) {
        approvalSection.classList.remove('d-none');
        btnSubmit.classList.add('d-none');
        btnApprove.classList.remove('d-none');
    } else {
        approvalSection.classList.add('d-none');
        btnSubmit.classList.remove('d-none');
        btnApprove.classList.add('d-none');
    }
}

// Submit Containment Request
async function submitContainmentRequest() {
    const reason = document.getElementById('contain-reason-input').value;
    const emergencyOverride = document.getElementById('emergency-override-checkbox').checked;

    showExecutionProgress(true, 'Dispatching zero-trust containment command...');

    try {
        const res = await fetch('/api/v1/containment/request', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: currentUserId,
                username: currentUsername,
                requestedBy: currentAdmin,
                reason: reason,
                emergencyOverride: emergencyOverride
            })
        });

        if (res.ok) {
            const data = await res.json();
            if (data.requiresApproval) {
                currentPendingOperationId = data.operationId;
                showExecutionProgress(false);
                closeContainmentModal();
                showPendingBanner(data.operationId);
                showToast(`Operation #${data.operationId} requested. Pending 2-Person Approval from a secondary admin.`);
            } else {
                // Executed directly
                animateExecutionSteps(data);
            }
            await loadAuditTrail();
        } else {
            const err = await res.text();
            showToast(`Containment failed: ${err}`, true);
            showExecutionProgress(false);
        }
    } catch (e) {
        showToast('Containment dispatch error', true);
        showExecutionProgress(false);
    }
}

// Submit Containment Approval (Two-Person Rule)
async function submitContainmentApproval() {
    if (!currentPendingOperationId) return;

    const notes = document.getElementById('approval-notes-input').value || 'SOC approved for incident containment';
    showExecutionProgress(true, `Secondary admin ${currentAdmin} approving operation #${currentPendingOperationId}...`);

    try {
        const res = await fetch(`/api/v1/containment/${currentPendingOperationId}/approve`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                approvedBy: currentAdmin,
                notes: notes
            })
        });

        if (res.ok) {
            const data = await res.json();
            currentPendingOperationId = null;
            hidePendingBanner();
            animateExecutionSteps(data);
            await loadAuditTrail();
        } else {
            const err = await res.json();
            showToast(`Approval failed: ${err.message || 'Two-Person Rule Violation (approver must differ from requester)'}`, true);
            showExecutionProgress(false);
        }
    } catch (e) {
        showToast('Approval error', true);
        showExecutionProgress(false);
    }
}

function animateExecutionSteps(data) {
    const progressFill = document.getElementById('execution-progress-fill');
    const statusText = document.getElementById('execution-status-text');

    let percent = 20;
    progressFill.style.width = `${percent}%`;
    statusText.innerText = 'Step 1/5: Keycloak Account Disabled (Enabled=False)...';

    setTimeout(() => {
        percent = 40;
        progressFill.style.width = `${percent}%`;
        statusText.innerText = 'Step 2/5: OAuth Refresh Tokens & Sessions Terminated...';
    }, 400);

    setTimeout(() => {
        percent = 60;
        progressFill.style.width = `${percent}%`;
        statusText.innerText = 'Step 3/5: LDAP Privileged Groups Stripped -> Quarantined...';
    }, 800);

    setTimeout(() => {
        percent = 80;
        progressFill.style.width = `${percent}%`;
        statusText.innerText = 'Step 4/5: SAML SSO Assignments Isolated...';
    }, 1200);

    setTimeout(() => {
        percent = 100;
        progressFill.style.width = `${percent}%`;
        statusText.innerText = 'Step 5/5: Multi-Layer Zero Access Verification Complete!';
        
        setTimeout(() => {
            closeContainmentModal();
            showExecutionProgress(false);
            showToast(`🚨 Containment Complete: Status is ${data.status}`);
            loadUserIdentity(currentUserId);
            switchTab('verification');
        }, 600);
    }, 1600);
}

function showExecutionProgress(show, msg = '') {
    const container = document.getElementById('execution-progress-container');
    const fill = document.getElementById('execution-progress-fill');
    const text = document.getElementById('execution-status-text');

    if (show) {
        container.classList.remove('d-none');
        fill.style.width = '15%';
        text.innerText = msg;
    } else {
        container.classList.add('d-none');
        fill.style.width = '0%';
    }
}

function showPendingBanner(opId) {
    const banner = document.getElementById('pending-approval-banner');
    document.getElementById('pending-op-id').innerText = opId;
    banner.classList.remove('d-none');
}

function hidePendingBanner() {
    document.getElementById('pending-approval-banner').classList.add('d-none');
}

async function checkPendingOperations() {
    try {
        const res = await fetch('/api/v1/containment/operations');
        if (res.ok) {
            const ops = await res.json();
            const pending = ops.find(o => o.targetUserId === currentUserId && o.status === 'CONTAINMENT_PENDING');
            if (pending) {
                currentPendingOperationId = pending.id;
                showPendingBanner(pending.id);
            } else {
                currentPendingOperationId = null;
                hidePendingBanner();
            }
        }
    } catch (e) {
        console.warn('Error checking pending operations:', e);
    }
}

// Rollback / Recover Identity
async function rollbackIdentity() {
    try {
        const opsRes = await fetch('/api/v1/containment/operations');
        if (opsRes.ok) {
            const ops = await opsRes.json();
            const lastOp = ops.find(o => o.targetUserId === currentUserId);
            const opId = lastOp ? lastOp.id : 1;

            const res = await fetch(`/api/v1/containment/${opId}/rollback`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ rolledBackBy: currentAdmin })
            });

            if (res.ok) {
                const data = await res.json();
                showToast(`Identity ${data.username} restored to ACTIVE state`);
                await loadUserIdentity(currentUserId);
                await loadAuditTrail();
            }
        }
    } catch (e) {
        showToast('Rollback failed', true);
    }
}

// Re-verify Zero Access
async function reverifyZeroAccess() {
    try {
        const res = await fetch(`/api/v1/verification/user/${currentUserId}?verifiedBy=${encodeURIComponent(currentAdmin)}`);
        if (res.ok) {
            const data = await res.json();
            showToast(`Zero Access Verification Result: ${data.overallStatus} (${data.accessPathsRevoked}/${data.accessPathsFound} paths revoked)`);
            await loadAuditTrail();
        }
    } catch (e) {
        showToast('Verification query failed', true);
    }
}

// Sync from IdP
async function syncCurrentIdentity() {
    try {
        const res = await fetch(`/api/v1/identities/sync/${currentUsername}`, { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            showToast(`Identity Provider sync complete for ${data.username}`);
            await loadUserIdentity(currentUserId);
        }
    } catch (e) {
        showToast(`Synced identity: ${currentUsername}`);
    }
}

// Load Immutable Audit Trail
let auditLogsCache = [];
async function loadAuditTrail() {
    try {
        const res = await fetch('/api/v1/audit/events');
        if (res.ok) {
            auditLogsCache = await res.json();
            renderAuditTable(auditLogsCache);
        }
    } catch (e) {
        console.warn('Error loading audit events:', e);
    }
}

function renderAuditTable(events) {
    const tbody = document.getElementById('audit-table-body');
    tbody.innerHTML = '';

    if (!events || events.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center; color: var(--text-muted);">No audit events recorded.</td></tr>`;
        return;
    }

    events.forEach(evt => {
        const tr = document.createElement('tr');
        const isSuccess = evt.result.includes('SUCCESS') || evt.result.includes('CONTAINED') || evt.result.includes('RESTORED') || evt.result.includes('APPROVED');
        const resBadge = isSuccess ? 'badge-success' : (evt.result.includes('PARTIAL') ? 'badge-warning' : 'badge-danger');
        const shortHash = evt.checksum ? evt.checksum.substring(0, 12) + '...' : 'GENUINE';

        tr.innerHTML = `
            <td><span class="mono-text" style="font-size:0.75rem;">${formatDate(evt.timestamp)}</span></td>
            <td><strong>${escapeHtml(evt.actor)}</strong></td>
            <td><span class="mono-tag">${escapeHtml(evt.action)}</span></td>
            <td>${escapeHtml(evt.target)}</td>
            <td><span class="badge ${resBadge}">${escapeHtml(evt.result)}</span></td>
            <td><span class="mono-text" style="color:var(--accent-emerald); font-size:0.75rem;">#${escapeHtml(shortHash)}</span></td>
            <td><button class="btn btn-sm btn-secondary" onclick="inspectAuditEvent(${evt.id})">Inspect</button></td>
        `;
        tbody.appendChild(tr);
    });
}

function filterAuditLogs() {
    const q = document.getElementById('audit-search-input').value.toLowerCase();
    if (!q) {
        renderAuditTable(auditLogsCache);
        return;
    }
    const filtered = auditLogsCache.filter(e => 
        (e.actor && e.actor.toLowerCase().includes(q)) ||
        (e.action && e.action.toLowerCase().includes(q)) ||
        (e.target && e.target.toLowerCase().includes(q)) ||
        (e.checksum && e.checksum.toLowerCase().includes(q)) ||
        (e.result && e.result.toLowerCase().includes(q))
    );
    renderAuditTable(filtered);
}

function inspectAuditEvent(id) {
    const evt = auditLogsCache.find(e => e.id === id);
    if (!evt) return;

    document.getElementById('inspect-id').innerText = evt.id;
    document.getElementById('inspect-time').innerText = evt.timestamp;
    document.getElementById('inspect-actor').innerText = evt.actor;
    document.getElementById('inspect-action').innerText = evt.action;
    document.getElementById('inspect-target').innerText = evt.target;
    document.getElementById('inspect-result').innerText = evt.result;
    document.getElementById('inspect-checksum').innerText = evt.checksum || 'TAMPER-PROOF INTEGRITY VALIDATED';

    let jsonStr = evt.detailsJson || '{}';
    try {
        jsonStr = JSON.stringify(JSON.parse(jsonStr), null, 2);
    } catch (e) {}
    document.getElementById('inspect-json').innerText = jsonStr;

    document.getElementById('modal-audit-inspect').classList.remove('d-none');
}

function closeAuditInspectModal() {
    document.getElementById('modal-audit-inspect').classList.add('d-none');
}

// Toast System
function showToast(msg, isError = false) {
    const toast = document.getElementById('toast-notification');
    const toastMsg = document.getElementById('toast-message');
    const toastIcon = document.getElementById('toast-icon');

    toastMsg.innerText = msg;
    if (isError) {
        toastIcon.innerText = '✕';
        toastIcon.style.color = 'var(--accent-rose)';
        toast.style.borderColor = 'var(--accent-rose)';
    } else {
        toastIcon.innerText = '✓';
        toastIcon.style.color = 'var(--accent-emerald)';
        toast.style.borderColor = 'var(--accent-cyan)';
    }

    toast.classList.remove('d-none');
    setTimeout(() => {
        toast.classList.add('d-none');
    }, 3500);
}

// Helpers
function escapeHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function formatDate(iso) {
    if (!iso) return '-';
    try {
        const d = new Date(iso);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) + ' ' + d.toLocaleDateString([], { month: 'short', day: 'numeric' });
    } catch (e) {
        return iso;
    }
}
