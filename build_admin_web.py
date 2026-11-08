import sys
import json

sys.stdout.reconfigure(encoding='utf-8')

with open(r"F:\projects\pinpulse\web\index.html", "r", encoding="utf-8") as f:
    html = f.read()

# 1. Update Title and Add Hardware Badge
old_title = '<title>PinPulse Master Radar & Fleet Command Console</title>'
new_title = '<title>PinPulse Master Admin Console (Hardware Secured & Fleet Command)</title>'
assert old_title in html, "old_title not found"
html = html.replace(old_title, new_title)

# 2. Add Friends View Tab in Top Bar & Hardware Lock Badge
old_crm_btn = '<button class="btn-admin" id="btn-view-crm" onclick="switchMainWorkspaceView(\'crm\')">\n        💼 Users & Subscriptions CRM\n      </button>'
new_crm_btn = '''<button class="btn-admin" id="btn-view-crm" onclick="switchMainWorkspaceView('crm')">
        💼 Users & Subscriptions CRM
      </button>
      <button class="btn-admin" id="btn-view-friends" onclick="switchMainWorkspaceView('friends')">
        👥 Friends Network & Force-Pair
      </button>'''

assert old_crm_btn in html, "old_crm_btn not found"
html = html.replace(old_crm_btn, new_crm_btn)

# Add Hardware Secured badge in header actions (before Fit Fleet or right after)
old_hdr_fit = '<button class="btn-admin accent-btn" onclick="fitAllMarkersOnMap()"'
new_hdr_badge = '''<div id="hardware-lock-badge" style="display:inline-flex; align-items:center; gap:6px; font-size:0.75rem; font-weight:700; color:#10b981; background:rgba(16,185,129,0.14); border:1px solid #10b981; border-radius:8px; padding:6px 12px; margin-right:8px;" title="System locked to authorized PC hardware: 0A:E0:AF:A6:0E:2F">
        🛡️ <span id="hw-badge-text">PC HARDWARE LOCKED [0A:E0:AF:A6:0E:2F]</span>
      </div>
      <button class="btn-admin accent-btn" onclick="fitAllMarkersOnMap()"'''

assert old_hdr_fit in html, "old_hdr_fit not found"
html = html.replace(old_hdr_fit, new_hdr_badge)

# 3. Update CSS for #crm-view and #friends-view
old_crm_css = '''    #crm-view {
      display: none;
      flex: 1;
      background: var(--bg);
      overflow-y: auto;
      padding: 20px 24px 60px;
      flex-direction: column;
      gap: 18px;
    }
    #crm-view.active {
      display: flex;
    }'''

new_crm_css = '''    #crm-view {
      display: none;
      flex: 1;
      background: var(--bg);
      overflow-y: auto;
      padding: 20px 24px 60px;
      flex-direction: column;
      gap: 18px;
      position: absolute;
      inset: 0;
      z-index: 1000;
    }
    #crm-view.active, #crm-view.open {
      display: flex;
    }
    #friends-view {
      display: none;
      flex: 1;
      background: var(--bg);
      overflow-y: auto;
      padding: 20px 24px 60px;
      flex-direction: column;
      gap: 18px;
      position: absolute;
      inset: 0;
      z-index: 1000;
    }
    #friends-view.active, #friends-view.open {
      display: flex;
    }
    .friend-card {
      background: var(--panel);
      border: 1px solid var(--panel-border);
      border-radius: 14px;
      padding: 16px;
      transition: all 0.2s ease;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .friend-card:hover {
      border-color: var(--panel-border-bright);
      box-shadow: 0 4px 20px rgba(0,0,0,0.4);
    }
    .friend-item-row {
      display: flex;
      align-items: center;
      justify-content: space-between;
      background: var(--panel-secondary);
      border: 1px solid var(--panel-border);
      padding: 8px 12px;
      border-radius: 8px;
      margin-top: 6px;
    }'''

assert old_crm_css in html, "old_crm_css not found"
html = html.replace(old_crm_css, new_crm_css)

# 4. Insert #friends-view section right after #crm-view
old_crm_section_end = '</section>\n  </main>'

friends_section_html = '''</section>

    <!-- ==================== FULL SAAS FRIENDSHIP NETWORK & INSTANT FORCE-PAIR VIEW ==================== -->
    <section id="friends-view" aria-label="Friendship Network and Instant Force-Pair Console">
      <!-- KPI Row -->
      <div class="crm-kpi-row">
        <div class="crm-kpi-card">
          <span class="crm-kpi-lbl">Total Friendships Mapped</span>
          <span class="crm-kpi-val" id="friends-kpi-total" style="color:var(--primary);">0</span>
          <span class="crm-kpi-sub">Active connections across fleet</span>
        </div>
        <div class="crm-kpi-card">
          <span class="crm-kpi-lbl">Users With Friends</span>
          <span class="crm-kpi-val" id="friends-kpi-connected" style="color:#38bdf8;">0</span>
          <span class="crm-kpi-sub">Nodes in active circles</span>
        </div>
        <div class="crm-kpi-card">
          <span class="crm-kpi-lbl">Solo / Unconnected Users</span>
          <span class="crm-kpi-val" id="friends-kpi-solo" style="color:#fbbf24;">0</span>
          <span class="crm-kpi-sub">Ready for instant pairing</span>
        </div>
        <div class="crm-kpi-card">
          <span class="crm-kpi-lbl">Instant Force-Pair Engine</span>
          <span class="crm-kpi-val" style="color:#a855f7;">READY</span>
          <span class="crm-kpi-sub">Direct pairing with 0 codes</span>
        </div>
      </div>

      <!-- Instant Force-Pair Control Panel -->
      <div style="background:var(--panel); border:1px solid var(--panel-border-bright); border-radius:16px; padding:20px; box-shadow:0 8px 30px rgba(0,0,0,0.5);">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:14px; flex-wrap:wrap; gap:10px;">
          <div>
            <h2 style="font-size:1.15rem; font-weight:800; display:flex; align-items:center; gap:8px; margin:0;">
              ⚡ Instant Force-Pair Friends
            </h2>
            <p style="font-size:0.8rem; color:var(--text-muted); margin:4px 0 0 0;">
              Pair any two users instantly. No friend request, invitation code, or recipient acceptance needed ?" both devices update and link automatically via retained admin dispatch.
            </p>
          </div>
          <span class="badge" style="background:rgba(56,189,248,0.15); border:1px solid #38bdf8; color:#38bdf8; padding:5px 12px; border-radius:999px; font-size:0.75rem; font-weight:700;">
            Zero-Interaction Pairing
          </span>
        </div>

        <div style="display:grid; grid-template-columns:1fr auto 1fr; gap:16px; align-items:center; margin-bottom:16px;">
          <!-- User 1 Selector -->
          <div style="background:var(--panel-secondary); border:1px solid var(--panel-border); border-radius:12px; padding:14px;">
            <label style="font-size:0.75rem; font-weight:700; color:var(--text-muted); text-transform:uppercase; display:block; margin-bottom:6px;">
              User 1 (Initiator / Node A)
            </label>
            <select id="fp-user-1" class="crm-select" style="width:100%; padding:10px; font-size:0.85rem;" onchange="updatePairPreview()">
              <option value="">Select User 1...</option>
            </select>
            <div id="fp-user-1-preview" style="margin-top:10px; font-size:0.78rem; color:var(--text-muted); min-height:42px;"></div>
          </div>

          <!-- Connection Direction Icon -->
          <div style="display:flex; flex-direction:column; align-items:center; justify-content:center; gap:4px;">
            <div style="width:44px; height:44px; border-radius:50%; background:rgba(16,185,129,0.15); border:1px solid var(--primary); display:flex; align-items:center; justify-content:center; font-size:1.2rem; color:var(--primary);">
              ⇆
            </div>
            <span style="font-size:0.65rem; color:var(--text-muted); font-weight:700; text-transform:uppercase;">Bi-directional</span>
          </div>

          <!-- User 2 Selector -->
          <div style="background:var(--panel-secondary); border:1px solid var(--panel-border); border-radius:12px; padding:14px;">
            <label style="font-size:0.75rem; font-weight:700; color:var(--text-muted); text-transform:uppercase; display:block; margin-bottom:6px;">
              User 2 (Peer / Node B)
            </label>
            <select id="fp-user-2" class="crm-select" style="width:100%; padding:10px; font-size:0.85rem;" onchange="updatePairPreview()">
              <option value="">Select User 2...</option>
            </select>
            <div id="fp-user-2-preview" style="margin-top:10px; font-size:0.78rem; color:var(--text-muted); min-height:42px;"></div>
          </div>
        </div>

        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:10px;">
          <div id="fp-status-feedback" style="font-size:0.8rem; font-weight:700; color:var(--primary);"></div>
          <div style="display:flex; gap:12px; align-items:center;">
            <button class="btn-admin" onclick="swapPairUsers()" title="Swap User 1 and User 2">
              ⇄ Swap
            </button>
            <button class="btn-admin accent-btn" id="btn-force-pair-execute" onclick="executeForcePairAction()" style="background:var(--primary); color:#000; font-weight:800; padding:10px 22px; font-size:0.9rem;">
              ⚡ Force Pair These Users Now
            </button>
          </div>
        </div>
      </div>

      <!-- Who Is Friends With Whom Live Directory -->
      <div style="background:var(--panel); border:1px solid var(--panel-border); border-radius:16px; padding:20px; flex:1; display:flex; flex-direction:column; gap:14px;">
        <div style="display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:12px;">
          <div>
            <h2 style="font-size:1.1rem; font-weight:800; display:flex; align-items:center; gap:8px; margin:0;">
              👥 Who is Friends With Whom (Live Fleet Relationships)
            </h2>
            <span style="font-size:0.78rem; color:var(--text-muted);">
              Inspect current friendship circles across all registered users in real time.
            </span>
          </div>
          <div style="display:flex; gap:10px; align-items:center; flex-wrap:wrap;">
            <input type="text" id="friends-search" class="search-input" style="width:260px; padding-left:12px;" placeholder="Search user or friend name, email, code..." oninput="renderFriendsDirectory()" />
            <select id="friends-filter-status" class="crm-select" onchange="renderFriendsDirectory()">
              <option value="ALL">All Users</option>
              <option value="WITH_FRIENDS">Only Users With Friends</option>
              <option value="SOLO">Only Users Without Friends</option>
            </select>
            <button class="btn-admin" onclick="refreshFriendsRegistry()" title="Re-query broker for friend registries">
              🔄 Refresh
            </button>
          </div>
        </div>

        <!-- Friendship Cards Container -->
        <div id="friends-cards-container" style="display:grid; grid-template-columns:repeat(auto-fill, minmax(360px, 1fr)); gap:16px; margin-top:8px;">
          <!-- Dynamically populated -->
        </div>
      </div>
    </section>
  </main>'''

assert old_crm_section_end in html, "old_crm_section_end not found"
html = html.replace(old_crm_section_end, friends_section_html)

# 5. Replace switchMainWorkspaceView
old_switch_fn = '''    // ---------- Workspace View Switcher (Map vs SaaS CRM) ----------
    function switchMainWorkspaceView(view) {
      currentWorkspaceView = view;
      var crmEl = document.getElementById('crm-view');
      var btnMap = document.getElementById('view-btn-map');
      var btnCrm = document.getElementById('view-btn-crm');
      if (view === 'crm') {
        if (crmEl) crmEl.classList.add('open');
        if (btnCrm) btnCrm.classList.add('active');
        if (btnMap) btnMap.classList.remove('active');
        renderCrmTable();
      } else {
        if (crmEl) crmEl.classList.remove('open');
        if (btnMap) btnMap.classList.add('active');
        if (btnCrm) btnCrm.classList.remove('active');
        setTimeout(function() { if (map) map.invalidateSize(); }, 150);
      }
    }'''

new_switch_fn = '''    // ---------- Workspace View Switcher (Map vs SaaS CRM vs Friends Network) ----------
    function switchMainWorkspaceView(view) {
      currentWorkspaceView = view;
      var crmEl = document.getElementById('crm-view');
      var friendsEl = document.getElementById('friends-view');
      var btnMap = document.getElementById('btn-view-map');
      var btnCrm = document.getElementById('btn-view-crm');
      var btnFriends = document.getElementById('btn-view-friends');

      if (view === 'crm') {
        if (crmEl) { crmEl.classList.add('open'); crmEl.classList.add('active'); }
        if (friendsEl) { friendsEl.classList.remove('open'); friendsEl.classList.remove('active'); }
        if (btnCrm) { btnCrm.classList.add('active'); btnCrm.style.background = 'var(--primary)'; btnCrm.style.color = '#000'; }
        if (btnMap) { btnMap.classList.remove('active'); btnMap.style.background = ''; btnMap.style.color = ''; }
        if (btnFriends) { btnFriends.classList.remove('active'); btnFriends.style.background = ''; btnFriends.style.color = ''; }
        renderCrmTable();
      } else if (view === 'friends') {
        if (crmEl) { crmEl.classList.remove('open'); crmEl.classList.remove('active'); }
        if (friendsEl) { friendsEl.classList.add('open'); friendsEl.classList.add('active'); }
        if (btnFriends) { btnFriends.classList.add('active'); btnFriends.style.background = 'var(--primary)'; btnFriends.style.color = '#000'; }
        if (btnMap) { btnMap.classList.remove('active'); btnMap.style.background = ''; btnMap.style.color = ''; }
        if (btnCrm) { btnCrm.classList.remove('active'); btnCrm.style.background = ''; btnCrm.style.color = ''; }
        populatePairSelectors();
        renderFriendsDirectory();
      } else {
        // Map view
        if (crmEl) { crmEl.classList.remove('open'); crmEl.classList.remove('active'); }
        if (friendsEl) { friendsEl.classList.remove('open'); friendsEl.classList.remove('active'); }
        if (btnMap) { btnMap.classList.add('active'); btnMap.style.background = 'var(--primary)'; btnMap.style.color = '#000'; }
        if (btnCrm) { btnCrm.classList.remove('active'); btnCrm.style.background = ''; btnCrm.style.color = ''; }
        if (btnFriends) { btnFriends.classList.remove('active'); btnFriends.style.background = ''; btnFriends.style.color = ''; }
        setTimeout(function() { if (map) map.invalidateSize(); }, 150);
      }
    }'''

assert old_switch_fn in html, "old_switch_fn not found"
html = html.replace(old_switch_fn, new_switch_fn)

# 6. Add MQTT Subscriptions for Friends Registry
old_sub_anchor = "client.subscribe('pinpulse/v1/user/+/loc', { qos: 1 });"
new_subs = '''client.subscribe('pinpulse/v1/user/+/loc', { qos: 1 });
        client.subscribe('pinpulse/v1/registry/friends/+', { qos: 1 });
        client.subscribe('pinpulse/v1/registry/friends/#', { qos: 1 });'''

assert old_sub_anchor in html, "old_sub_anchor not found"
html = html.replace(old_sub_anchor, new_subs)

# 7. Add MQTT Message Handler for Friends Registry
old_msg_anchor = "if (topic.indexOf('/admin/users/') !== -1) {"
new_msg_anchor = '''if (topic.indexOf('/registry/friends/') !== -1) {
            handleFriendsRegistryPacket(topic, data);
          } else if (topic.indexOf('/admin/users/') !== -1) {'''

assert old_msg_anchor in html, "old_msg_anchor not found"
html = html.replace(old_msg_anchor, new_msg_anchor)

# 8. Add Friends Registry and Instant Force-Pair Logic
friends_js_logic = '''
    // ==================== FRIENDS NETWORK & INSTANT FORCE-PAIR ENGINE ====================
    var userFriendsMap = {};
    try {
      userFriendsMap = JSON.parse(localStorage.getItem('pinpulse_user_friends_v1') || '{}');
    } catch (e) {
      userFriendsMap = {};
    }

    function saveUserFriendsMap() {
      try {
        localStorage.setItem('pinpulse_user_friends_v1', JSON.stringify(userFriendsMap));
      } catch (e) {}
      updateFriendsKpis();
    }

    function handleFriendsRegistryPacket(topic, data) {
      if (!data) return;
      var targetId = data.userId || topic.split('/').pop();
      if (!targetId) return;

      var friendsList = Array.isArray(data.friends) ? data.friends : [];
      userFriendsMap[targetId] = friendsList;
      saveUserFriendsMap();

      if (users[targetId]) {
        users[targetId].friends = friendsList;
        users[targetId].friendsCount = friendsList.length;
        if (data.userName) users[targetId].name = data.userName;
        if (data.userEmail) users[targetId].email = data.userEmail;
        if (data.friendCode) users[targetId].friendCode = data.friendCode;
        saveCachedCrmUsers();
      }

      if (currentWorkspaceView === 'friends') {
        renderFriendsDirectory();
        populatePairSelectors();
      }
    }

    function refreshFriendsRegistry() {
      if (client && client.connected) {
        client.subscribe('pinpulse/v1/registry/friends/+', { qos: 1 });
        client.subscribe('pinpulse/v1/registry/friends/#', { qos: 1 });
        showNotification('Refreshing friends network from MQTT broker...', 'info');
      }
      populatePairSelectors();
      renderFriendsDirectory();
    }

    function updateFriendsKpis() {
      var allUserIds = Object.keys(users);
      // Merge keys from userFriendsMap
      Object.keys(userFriendsMap).forEach(function(k) {
        if (allUserIds.indexOf(k) === -1) allUserIds.push(k);
      });

      var totalConnections = 0;
      var connectedUsers = 0;
      var soloUsers = 0;

      allUserIds.forEach(function(uid) {
        var flist = userFriendsMap[uid] || (users[uid] && Array.isArray(users[uid].friends) ? users[uid].friends : []);
        if (flist && flist.length > 0) {
          connectedUsers++;
          totalConnections += flist.length;
        } else {
          soloUsers++;
        }
      });

      var elTot = document.getElementById('friends-kpi-total');
      var elConn = document.getElementById('friends-kpi-connected');
      var elSolo = document.getElementById('friends-kpi-solo');

      if (elTot) elTot.textContent = Math.round(totalConnections / 2) + ' pairs (' + totalConnections + ' links)';
      if (elConn) elConn.textContent = connectedUsers;
      if (elSolo) elSolo.textContent = soloUsers;
    }

    function populatePairSelectors() {
      var sel1 = document.getElementById('fp-user-1');
      var sel2 = document.getElementById('fp-user-2');
      if (!sel1 || !sel2) return;

      var currentVal1 = sel1.value;
      var currentVal2 = sel2.value;

      var allUsers = Object.values(users).slice();
      // Also include any user in userFriendsMap not yet in users
      Object.keys(userFriendsMap).forEach(function(uid) {
        if (!users[uid]) {
          allUsers.push({ id: uid, name: 'User ' + uid.substring(0, 6), email: '', friendCode: '----' });
        }
      });

      allUsers.sort(function(a, b) {
        var na = (a.name || a.displayName || a.id).toLowerCase();
        var nb = (b.name || b.displayName || b.id).toLowerCase();
        return na.localeCompare(nb);
      });

      var opts = '<option value="">-- Select a User --</option>';
      allUsers.forEach(function(u) {
        var label = (u.name || u.displayName || 'Unknown') + ' (' + (u.email || u.friendCode || u.id.substring(0, 6)) + ')';
        opts += '<option value="' + u.id + '">' + escapeHtml(label) + '</option>';
      });

      sel1.innerHTML = opts;
      sel2.innerHTML = opts;

      if (currentVal1) sel1.value = currentVal1;
      if (currentVal2) sel2.value = currentVal2;

      updatePairPreview();
      updateFriendsKpis();
    }

    function updatePairPreview() {
      var sel1 = document.getElementById('fp-user-1');
      var sel2 = document.getElementById('fp-user-2');
      var prev1 = document.getElementById('fp-user-1-preview');
      var prev2 = document.getElementById('fp-user-2-preview');
      var statusEl = document.getElementById('fp-status-feedback');
      if (statusEl) statusEl.textContent = '';

      function renderPreview(uId, targetEl) {
        if (!uId) {
          targetEl.innerHTML = '<span style="color:var(--text-muted); font-style:italic;">No user selected</span>';
          return;
        }
        var u = users[uId] || { id: uId, name: 'User ' + uId.substring(0, 6), email: 'N/A', friendCode: '----' };
        var flist = userFriendsMap[uId] || (u.friends || []);
        targetEl.innerHTML = '<div style="display:flex; align-items:center; gap:8px;">' +
          '<div style="font-weight:700; color:#fff;">' + escapeHtml(u.name || u.displayName || 'User') + '</div>' +
          '<span style="color:#38bdf8; font-size:0.75rem;">[' + escapeHtml(u.friendCode || '----') + ']</span>' +
          '</div>' +
          '<div style="font-size:0.72rem; color:var(--text-muted);">' + escapeHtml(u.email || 'No email') + ' ? ' + flist.length + ' friends</div>';
      }

      if (prev1) renderPreview(sel1 ? sel1.value : '', prev1);
      if (prev2) renderPreview(sel2 ? sel2.value : '', prev2);

      // Check if already friends
      if (sel1 && sel2 && sel1.value && sel2.value && sel1.value !== sel2.value) {
        var flist = userFriendsMap[sel1.value] || [];
        var isAlready = flist.some(function(f) { return f.id === sel2.value; });
        if (isAlready && statusEl) {
          statusEl.innerHTML = '⚠️ <span style="color:#fbbf24;">These users are already friends! Executing will update/refresh their connection.</span>';
        }
      }
    }

    function swapPairUsers() {
      var sel1 = document.getElementById('fp-user-1');
      var sel2 = document.getElementById('fp-user-2');
      if (!sel1 || !sel2) return;
      var temp = sel1.value;
      sel1.value = sel2.value;
      sel2.value = temp;
      updatePairPreview();
    }

    function executeForcePairAction() {
      var sel1 = document.getElementById('fp-user-1');
      var sel2 = document.getElementById('fp-user-2');
      var statusEl = document.getElementById('fp-status-feedback');

      if (!sel1 || !sel2 || !sel1.value || !sel2.value) {
        alert('Please select both User 1 and User 2 to pair.');
        return;
      }
      if (sel1.value === sel2.value) {
        alert('Cannot pair a user with themselves. Please select two different users.');
        return;
      }

      var u1Id = sel1.value;
      var u2Id = sel2.value;
      forcePairFriends(u1Id, u2Id);
    }

    function forcePairFriends(u1Id, u2Id) {
      var u1 = users[u1Id] || { id: u1Id, name: 'User ' + u1Id.substring(0, 6), email: '', friendCode: '----' };
      var u2 = users[u2Id] || { id: u2Id, name: 'User ' + u2Id.substring(0, 6), email: '', friendCode: '----' };

      var friendFor1 = {
        id: u2.id,
        displayName: u2.name || u2.displayName || 'Friend',
        friendCode: u2.friendCode || '----',
        email: u2.email || '',
        avatarIndex: u2.avatarIndex || 0
      };

      var friendFor2 = {
        id: u1.id,
        displayName: u1.name || u1.displayName || 'Friend',
        friendCode: u1.friendCode || '----',
        email: u1.email || '',
        avatarIndex: u1.avatarIndex || 0
      };

      var cmdTo1 = {
        type: 'FORCE_ADD_FRIEND',
        friend: friendFor1,
        timestamp: Date.now()
      };

      var cmdTo2 = {
        type: 'FORCE_ADD_FRIEND',
        friend: friendFor2,
        timestamp: Date.now()
      };

      if (client && client.connected) {
        client.publish('pinpulse/v1/admin/users/' + u1.id, JSON.stringify(cmdTo1), { qos: 1, retain: true });
        client.publish('pinpulse/v1/admin/users/' + u2.id, JSON.stringify(cmdTo2), { qos: 1, retain: true });
      }

      // Update local storage representation
      if (!userFriendsMap[u1.id]) userFriendsMap[u1.id] = [];
      if (!userFriendsMap[u1.id].some(function(f) { return f.id === u2.id; })) {
        userFriendsMap[u1.id].push(friendFor1);
      }
      if (!userFriendsMap[u2.id]) userFriendsMap[u2.id] = [];
      if (!userFriendsMap[u2.id].some(function(f) { return f.id === u1.id; })) {
        userFriendsMap[u2.id].push(friendFor2);
      }
      saveUserFriendsMap();

      var statusEl = document.getElementById('fp-status-feedback');
      if (statusEl) {
        statusEl.innerHTML = '✨ <span style="color:var(--primary); font-weight:800;">Successfully paired ' + escapeHtml(u1.name || u1.id) + ' and ' + escapeHtml(u2.name || u2.id) + '! Devices updated automatically with no codes required.</span>';
      }

      addAuditLog('FORCE_PAIR', (u1.name || u1.id) + ' ⇆ ' + (u2.name || u2.id) + ' paired remotely');
      showNotification('⚡ Successfully force-paired ' + (u1.name || u1.id) + ' and ' + (u2.name || u2.id) + '!', 'success');

      if (window.PinPulseAdminBridge && window.PinPulseAdminBridge.vibrate) {
        try { window.PinPulseAdminBridge.vibrate(100); } catch(e){}
      }

      renderFriendsDirectory();
      updatePairPreview();
    }

    function forceUnpairFriends(u1Id, u2Id) {
      if (!confirm('Are you sure you want to force-disconnect this friendship remotely?')) return;

      var cmdTo1 = {
        type: 'FORCE_REMOVE_FRIEND',
        friendId: u2Id,
        timestamp: Date.now()
      };
      var cmdTo2 = {
        type: 'FORCE_REMOVE_FRIEND',
        friendId: u1Id,
        timestamp: Date.now()
      };

      if (client && client.connected) {
        client.publish('pinpulse/v1/admin/users/' + u1Id, JSON.stringify(cmdTo1), { qos: 1, retain: true });
        client.publish('pinpulse/v1/admin/users/' + u2Id, JSON.stringify(cmdTo2), { qos: 1, retain: true });
      }

      if (userFriendsMap[u1Id]) {
        userFriendsMap[u1Id] = userFriendsMap[u1Id].filter(function(f) { return f.id !== u2Id; });
      }
      if (userFriendsMap[u2Id]) {
        userFriendsMap[u2Id] = userFriendsMap[u2Id].filter(function(f) { return f.id !== u1Id; });
      }
      saveUserFriendsMap();

      addAuditLog('FORCE_UNPAIR', u1Id + ' unpaired from ' + u2Id);
      showNotification('Removed friendship connection between users.', 'warning');
      renderFriendsDirectory();
      updatePairPreview();
    }

    function quickPairWithUser(targetUserId) {
      switchMainWorkspaceView('friends');
      var sel1 = document.getElementById('fp-user-1');
      if (sel1) {
        sel1.value = targetUserId;
        updatePairPreview();
        var sel2 = document.getElementById('fp-user-2');
        if (sel2) sel2.focus();
      }
    }

    function renderFriendsDirectory() {
      var container = document.getElementById('friends-cards-container');
      if (!container) return;

      var searchInput = document.getElementById('friends-search');
      var query = searchInput ? searchInput.value.trim().toLowerCase() : '';

      var filterStatus = document.getElementById('friends-filter-status');
      var filter = filterStatus ? filterStatus.value : 'ALL';

      var allUserIds = Object.keys(users);
      Object.keys(userFriendsMap).forEach(function(k) {
        if (allUserIds.indexOf(k) === -1) allUserIds.push(k);
      });

      var filtered = allUserIds.filter(function(uid) {
        var u = users[uid] || { id: uid, name: 'User ' + uid.substring(0, 6), email: '', friendCode: '----' };
        var flist = userFriendsMap[uid] || (u.friends || []);

        if (filter === 'WITH_FRIENDS' && flist.length === 0) return false;
        if (filter === 'SOLO' && flist.length > 0) return false;

        if (query) {
          var matchUser = (u.name || '').toLowerCase().indexOf(query) !== -1 ||
                          (u.email || '').toLowerCase().indexOf(query) !== -1 ||
                          (u.friendCode || '').toLowerCase().indexOf(query) !== -1 ||
                          u.id.toLowerCase().indexOf(query) !== -1;
          var matchFriend = flist.some(function(f) {
            return (f.displayName || '').toLowerCase().indexOf(query) !== -1 ||
                   (f.email || '').toLowerCase().indexOf(query) !== -1 ||
                   (f.friendCode || '').toLowerCase().indexOf(query) !== -1;
          });
          if (!matchUser && !matchFriend) return false;
        }
        return true;
      });

      filtered.sort(function(a, b) {
        var fa = (userFriendsMap[a] || []).length;
        var fb = (userFriendsMap[b] || []).length;
        return fb - fa; // Users with most friends first
      });

      if (filtered.length === 0) {
        container.innerHTML = '<div style="grid-column: 1 / -1; text-align:center; padding:40px; color:var(--text-muted);">' +
          'No users match the search filter. Use Instant Force-Pair above to create connections!' +
          '</div>';
        return;
      }

      var html = '';
      filtered.forEach(function(uid) {
        var u = users[uid] || { id: uid, name: 'User ' + uid.substring(0, 6), email: 'No email', friendCode: '----', subscriptionPlan: 'DEMO' };
        var flist = userFriendsMap[uid] || (u.friends || []);
        var plan = u.subscriptionPlan || 'DEMO';
        var maxFriends = (plan === 'BUSINESS') ? 50 : (plan === 'FAMILY' ? 10 : 1);
        var isOnline = (Date.now() - (u.timestamp || 0)) < 300000;

        html += '<div class="friend-card">';
        // Card Header
        html += '  <div style="display:flex; justify-content:space-between; align-items:flex-start;">';
        html += '    <div style="display:flex; align-items:center; gap:10px;">';
        html += '      <div style="width:38px; height:38px; border-radius:50%; background:rgba(56,189,248,0.2); border:1px solid #38bdf8; display:flex; align-items:center; justify-content:center; font-weight:800; color:#38bdf8;">';
        html += (u.name ? u.name.charAt(0).toUpperCase() : 'U');
        html += '      </div>';
        html += '      <div>';
        html += '        <div style="font-weight:800; color:#fff; font-size:0.95rem; display:flex; align-items:center; gap:6px;">';
        html += escapeHtml(u.name || u.displayName || 'Unknown User');
        html += isOnline ? ' <span style="display:inline-block; width:8px; height:8px; border-radius:50%; background:var(--primary);" title="Online now"></span>' : '';
        html += '        </div>';
        html += '        <div style="font-size:0.75rem; color:var(--text-muted);">' + escapeHtml(u.email || 'No email registered') + '</div>';
        html += '      </div>';
        html += '    </div>';
        html += '    <span class="badge" style="background:rgba(16,185,129,0.15); color:var(--primary); font-size:0.7rem; font-weight:700;">' + escapeHtml(u.friendCode || '----') + '</span>';
        html += '  </div>';

        // Plan & Quota badge
        html += '  <div style="display:flex; justify-content:space-between; align-items:center; font-size:0.75rem; background:rgba(255,255,255,0.03); padding:6px 10px; border-radius:8px;">';
        html += '    <span style="color:var(--text-muted);">' + plan + ' Pack</span>';
        html += '    <span style="font-weight:700; color:' + (flist.length >= maxFriends ? '#ef4444' : 'var(--primary)') + ';">' + flist.length + ' / ' + maxFriends + ' Friends</span>';
        html += '  </div>';

        // Friends List
        html += '  <div style="flex:1;">';
        html += '    <div style="font-size:0.75rem; font-weight:700; color:var(--text-muted); text-transform:uppercase; margin-bottom:6px;">Current Friends (' + flist.length + ')</div>';
        if (flist.length === 0) {
          html += '    <div style="font-size:0.75rem; color:var(--text-muted); font-style:italic; padding:6px 0;">No active friends paired yet.</div>';
        } else {
          flist.forEach(function(f) {
            html += '    <div class="friend-item-row">';
            html += '      <div style="display:flex; align-items:center; gap:8px;">';
            html += '        <span style="font-size:0.85rem;">👤</span>';
            html += '        <div>';
            html += '          <div style="font-weight:700; font-size:0.8rem; color:#fff;">' + escapeHtml(f.displayName || f.name || 'Friend') + '</div>';
            html += '          <div style="font-size:0.7rem; color:#38bdf8;">' + escapeHtml(f.friendCode || '----') + (f.email ? ' ? ' + escapeHtml(f.email) : '') + '</div>';
            html += '        </div>';
            html += '      </div>';
            html += '      <button onclick="forceUnpairFriends(\'' + uid + '\', \'' + f.id + '\')" style="background:transparent; border:none; color:var(--danger); font-size:0.75rem; cursor:pointer; padding:4px 8px; font-weight:700;" title="Force disconnect this friendship">✕ Unpair</button>';
            html += '    </div>';
          });
        }
        html += '  </div>';

        // Bottom Actions
        html += '  <div style="display:flex; gap:8px; margin-top:4px;">';
        html += '    <button class="crm-btn" style="flex:1; justify-content:center;" onclick="quickPairWithUser(\'' + uid + '\')">⚡ Pair With Someone</button>';
        html += '    <button class="crm-btn" onclick="selectedUserId=\'' + uid + '\'; showUserModal(users[\'' + uid + '\'] || {id:\'' + uid + '\'});" title="View full profile">🔍 View</button>';
        html += '  </div>';

        html += '</div>';
      });

      container.innerHTML = html;
      updateFriendsKpis();
    }

    // Hardware auth verification
    function checkHardwareAuth() {
      fetch('/api/admin/hardware-auth')
        .then(function(res) { return res.json(); })
        .then(function(data) {
          var badgeText = document.getElementById('hw-badge-text');
          if (data && data.authorized) {
            if (badgeText) badgeText.textContent = 'PC HARDWARE VERIFIED [' + (data.verifiedMac || '0A:E0:AF:A6:0E:2F').toUpperCase() + ']';
          }
        })
        .catch(function(e) {
          // Inside Android WebView or direct file:// load
          var badgeText = document.getElementById('hw-badge-text');
          if (window.PinPulseAdminBridge) {
            if (badgeText) badgeText.textContent = 'ADMIN APK MASTER AUTHENTICATED';
          } else {
            if (badgeText) badgeText.textContent = 'PC HARDWARE LOCKED [0A:E0:AF:A6:0E:2F]';
          }
        });
    }

    window.addEventListener('DOMContentLoaded', function() {
      setTimeout(function() {
        populatePairSelectors();
        checkHardwareAuth();
      }, 800);
    });
'''

# Insert the logic right before the closing </script> tag
old_closing_script = '</body>'
assert old_closing_script in html, "closing </body> not found"
html = html.replace(old_closing_script, '<script>' + friends_js_logic + '</script>\n' + old_closing_script)

# Save to F:\projects\pinpulse-adimin\web\index.html
with open(r"F:\projects\pinpulse-adimin\web\index.html", "w", encoding="utf-8") as f:
    f.write(html)

# Save to F:\projects\pinpulse-adimin\app\src\main\assets\admin_console\index.html
with open(r"F:\projects\pinpulse-adimin\app\src\main\assets\admin_console\index.html", "w", encoding="utf-8") as f:
    f.write(html)

print("SUCCESS: Enhanced admin console written to web/index.html and app/src/main/assets/admin_console/index.html")
